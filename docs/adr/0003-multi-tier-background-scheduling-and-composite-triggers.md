# [ADR-0003] Multi-Tier Background Scheduling and Composite Trigger Architecture / 多轨后台调度与复合触发器分流架构

> **Metadata / 元数据**
> - **Status / 状态**: Accepted / 已采纳
> - **Date / 日期**: 2026-09-26
> - **Deciders / 决策者**: @Foo, @Antigravity
> - **Technical Story / 关联背景**: [PLANS.md (Phase 4)](../../PLANS.md#phase-4-体验与进阶特性-ux--advanced-features), Commits `fd6fbe1`, `35c657b`, `ff7c209`, `94d32d0`

---

## Executive Summary / 双语摘要

### English Summary
Because Android's WorkManager enforces a 15-minute minimum interval with elastic execution drift (Doze batching) and cannot capture screen-off events or guarantee exact wall-clock anchors (e.g. 08:00 AM daily switch), and because hard WorkManager network constraints completely stall offline rotation, we overhauled the scheduling architecture into a multi-tier trigger system. We decoupled the execution pipeline into a standalone engine (`WallpaperChangeExecutor`); established a four-dimensional dispatch matrix (WorkManager for macro cycles, AlarmManager for wall-clock anchors, ScreenOffWatcherService for screen events, and Widgets/Tiles for immediate actions); delegated network checks to application logic to guarantee offline LRU cache fallback; and introduced a three-layer suppression funnel (Quiet Hours, Cooldown, Interactive Deferral) alongside a dynamic rule engine (`ScheduleRuleEngine`).

### 中文摘要
鉴于 Android 原生 `WorkManager` 周期任务受限于 15 分钟最小间隔与弹性窗口漂移、无法感知熄屏事件、无法保障生活化定点打卡（如早晨 08:00 准点切壁纸），且系统级网络硬约束会导致无 Wi-Fi 或离线断网时壁纸轮换彻底停摆，本项目对后台调度与执行架构进行了全面重构。我们将壁纸更换管线彻底解耦为单例引擎 `WallpaperChangeExecutor`；确立了由 WorkManager（宏观周期兜底）、AlarmManager（准点打卡）、ScreenOffWatcherService（熄屏感知）及桌面/磁贴（即时触发）组成的四维分流调度矩阵；将网络约束移至应用层以实现离线 LRU 缓存优雅降级；并引入三层前置防碰撞漏斗（夜间免打扰 + 冷却抑制 + 交互避让）与动态规则引擎（`ScheduleRuleEngine`），在保障极致低功耗的同时实现生活化无感切换。

---

## English Full Text

### 1. Context and Problem Statement
In Phase 1, the wallpaper rotation lifecycle was entirely bound to Jetpack `WorkManager` inside `WallpaperWorker.doWork()`. As the app evolved towards Phase 4, real-world user lifestyles introduced demands that conflicted with WorkManager's fundamental design constraints:
1. **Wall-Clock Anchors**: Users expect exact timing (e.g., waking up to a fresh morning wallpaper at 08:00 sharp). WorkManager's `PeriodicWorkRequest` enforces a minimum 15-minute interval and allows arbitrary execution drift under Android Doze mode.
2. **Instant Screen-Off Triggers**: Changing the wallpaper while the user is actively reading or playing games creates visual stutter and battery spikes. Users strongly prefer switching when the screen turns off. However, Android 8.0+ (API 26) strictly forbids static broadcast registration for `ACTION_SCREEN_OFF` in `AndroidManifest.xml`.
3. **Offline & Cellular Stalling**: Early implementations attached `NetworkType.CONNECTED` or `UNMETERED` to `PeriodicWorkRequest`. When users went offline or switched to cellular data, WorkManager completely ceased waking up the worker, ignoring existing high-quality images stored in the local LRU cache.
4. **Trigger Collisions & Duplication**: Introducing UI buttons, App Shortcuts, Quick Settings Tiles, and AppWidgets alongside timers created severe race conditions (e.g., a manual switch at 07:58 colliding with an alarm switch at 08:00).

### 2. Decision Drivers
* **Lifestyle Precision**: Guarantee exact wall-clock anchors without draining the battery.
* **Offline Resilience**: Ensure the app continues rotating wallpapers seamlessly even in airplane mode or on unmetered cellular connections.
* **Non-Intrusive UX**: Prevent jarring visual disruptions or frame drops while the user is actively interacting with the device.
* **Architecture Cohesion**: Provide a single, idempotent execution pipeline shared across all invocation sources (timers, alarms, system broadcasts, tiles, and widgets).

### 3. Considered Options

* **Option A: Pure Jetpack WorkManager with Expedited One-Off Requests**
  * *Pros*: Pure Android Jetpack solution, minimal system service footprint.
  * *Cons*: Cannot monitor `ACTION_SCREEN_OFF`; cannot guarantee exact wall-clock firing in Doze mode; suffers from execution delay and cannot bypass the 15-minute periodicity ceiling.

* **Option B: Persistent Foreground Service with Permanent Notification**
  * *Pros*: Trivially captures all screen broadcasts; holds continuous wakelocks; bypasses all background restrictions.
  * *Cons*: Unacceptable battery drain; persistent notification clutters user's notification shade; risks being labeled a rogue app on modern Android (API 34+ foreground service types).

* **Option C: Multi-Tier Hybrid Matrix with Unified Executor (Chosen)**
  * *Pros*: Combines the battery benefits of WorkManager, the precision of AlarmManager, and the responsiveness of a dynamic-lifecycle ScreenOffWatcher, funneling all triggers through a single execution and suppression engine.
  * *Cons*: Requires coordinating multiple Android system components and managing exact alarm permissions.

### 4. Decision Outcome
* **Chosen Option**: **Option C (Multi-Tier Hybrid Matrix with Unified Executor)**.

#### Architecture Implementation Details:

1. **Decoupled Execution Pipeline (`WallpaperChangeExecutor`)**:
   * All triggers (`WallpaperWorker`, `WallpaperAlarmReceiver`, `ScreenOffWatcherService`, `CurrentWallpaperWidgetProvider`, `QuickSettingsTile`, `MainActivity`) are reduced to dumb trigger channels.
   * `WallpaperChangeExecutor.execute()` handles the end-to-end lifecycle: suppression checks $\rightarrow$ rule evaluation $\rightarrow$ source instantiation $\rightarrow$ fetching $\rightarrow$ deduplication $\rightarrow$ decoding/cropping $\rightarrow$ system wallpaper injection $\rightarrow$ history & widget notification.

2. **Four-Dimensional Dispatch Matrix**:
   * **Macro Cycle / Fallback**: Jetpack `WorkManager` runs background periodic requests for general multi-hour rotations.
   * **Wall-Clock Anchors**: `AlarmManager.setExactAndAllowWhileIdle` schedules exact morning/evening switches, automatically scheduling the next alarm upon execution.
   * **Real-time Screen Events**: `ScreenOffWatcherService` runs only when the screen-off trigger is enabled, dynamically registering `ACTION_SCREEN_OFF` and triggering upon display sleep.
   * **Interactive Shortcuts**: Quick Settings Tiles, App Shortcuts, and Glance AppWidgets invoke the executor asynchronously on-demand.

3. **Application-Layer Network Constraint & LRU Fallback**:
   * We stripped `NetworkType` constraints away from WorkManager's `Constraints.Builder`.
   * The worker always wakes up. At runtime, the source checks network availability: if Wi-Fi is available, it downloads new images; if offline or on cellular, it gracefully falls back to picking an existing image from the local LRU disk cache (`context.cacheDir/wallpapers/`).

4. **Three-Layer Composite Suppression Funnel**:
   * **Quiet Hours**: Bypassed for manual requests; checks whether current time falls within `[quietHoursStart, quietHoursEnd]`.
   * **Cooldown Suppression**: Skips execution if `now - lastChangedTimestamp < cooldownMinutes`, preventing rapid back-to-back switching (bypassed for manual requests and screen-off triggers, which are independently governed by the switching delay).
   * **Interactive Deferral**: Queries `PowerManager.isInteractive`. If the user is actively using the screen, background rotation is deferred until the screen turns off.
   * **Deduplication**: If the chosen candidate matches the current wallpaper URI and title, redundant decoding and re-application are aborted early.

5. **Dynamic Schedule Rule Engine (`ScheduleRuleEngine`)**:
   * Replaced monolithic global settings with a Room-backed rule database (`ScheduleRulesDatabase`).
   * Evaluates prioritized rules matching specific time windows, weekdays, and trigger contexts to assign dedicated wallpaper sources and display targets (Home / Lock / Both). Falls back to global preferences if no rule matches.

### 5. Consequences & Trade-offs
* **Positive Consequences**:
  * Delivers exact 08:00 AM freshness without maintaining a battery-draining continuous background service.
  * Seamless zero-data offline rotation prevents the app from stalling during travel or poor connectivity.
  * Eliminates race conditions, duplicate execution, and frame drops during device interaction.
* **Negative Consequences & Mitigations**:
  * *Increased Component Surface*: Coordinating Alarms, Workers, and Services increases state complexity $\rightarrow$ *Mitigation*: Centralized state monitoring via `PreferencesManager.lastExecutionStatus` and `lastErrorMessage`, visualized in the UI.
  * *Exact Alarm Permissions*: Android 12+ requires `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM` $\rightarrow$ *Mitigation*: Declared standard permissions and provided friendly battery-optimization exemption guidance.

### 6. Thresholds & Triggers (When to Revisit)
* This architecture should only be reconsidered if the Android OS introduces an official, native wallpaper scheduling provider that integrates exact wall-clock anchors and screen-state hooks while maintaining backwards compatibility to Android 6.0 (API 23).

---

## 中文全文

### 1. 背景与问题描述
在 Phase 1 阶段，壁纸轮播的完整生命周期全部绑定在 Jetpack `WorkManager`（位于 `WallpaperWorker.doWork()`）中。随着项目向 Phase 4 演进，真实用户的生活化作息诉求与 WorkManager 的原生设计约束产生了不可调和的矛盾：
1. **生活化时钟定点需求**：用户强烈希望“早晨 08:00 醒来看到一张全新晨间壁纸”。而 `PeriodicWorkRequest` 强制限制最小调度周期为 15 分钟，且在 Android Doze（低电耗模式）下存在严重的系统弹性批处理时间漂移，无法做到准时。
2. **即时熄屏感知换图**：在用户全屏打游戏、阅读或观看视频时突兀更换壁纸会导致掉帧与视觉干扰，用户更偏好在“熄屏瞬间”静默换图。然而 Android 8.0+（API 26）明确禁止在 `AndroidManifest.xml` 中静态注册 `ACTION_SCREEN_OFF` 隐式广播。
3. **离线与蜂窝网络卡死**：早先在 `PeriodicWorkRequest` 上配置了 `NetworkType.CONNECTED` 或 `UNMETERED` 约束。当用户处于无 Wi-Fi 或飞行模式时，系统判定约束不满足，**完全不唤醒 Worker**，导致本地 LRU 缓存池中储备的几十张高质量壁纸形同虚设，壁纸直接停摆。
4. **多触发源并发碰撞**：在定时任务之外，后续新增了桌面小部件、快捷方式、控制中心磁贴（Quick Settings Tile）及前台即时测试按钮，多入口并发调用极易造成严重竞态（例如 07:58 手动换图与 08:00 定时闹钟连环触发）。

### 2. 决策驱动因素
* **生活化作息精准度**：在绝不常驻唤醒耗电的前提下，保障晨间定点打卡等强时间要求。
* **离线容灾韧性**：保障飞行模式或无 Wi-Fi 环境下依然能够持续平滑轮播，不产生报错或停更。
* **无侵扰用户体验（Non-Intrusive UX）**：绝不在用户亮屏高频交互期间突兀切壁纸造成卡顿。
* **架构内聚性**：实现一套无状态、幂等的单一核心执行流水线，供所有外部唤醒入口统一调用。

### 3. 备选方案权衡

* **方案 A: 纯 Jetpack WorkManager 方案（配合一次性 Expedited 任务）**
  * *优势*: 纯标准 Jetpack 组件，系统组件负担最小。
  * *劣势*: 无法动态监听 `ACTION_SCREEN_OFF` 广播；Doze 模式下定点唤醒时间严重漂移；受限于 15 分钟周期硬顶，无法满足高频熄屏即切。

* **方案 B: 常驻前台保活服务（Foreground Service + 常驻通知）**
  * *优势*: 能够随时动态监听熄屏广播；随时持有 Wakelock；完全绕过系统后台限制。
  * *劣势*: 造成显著电量消耗；常驻通知栏严重影响用户体验；现代 Android（API 34+）对前台服务类型审核极严，易被系统降级杀死。

* **方案 C: 多轨分流调度矩阵 + 统一执行引擎（选定方案）**
  * *优势*: 完美融合了 WorkManager 的系统级节能、AlarmManager 的微观精准度以及 ScreenOffWatcher 的即时响应性，统一经由单一执行漏斗实现防碰撞。
  * *劣势*: 涉及多个 Android 底层系统组件的协同与状态管理。

### 4. 决策结果与推导论据
* **选定方案**: **方案 C (多轨分流调度矩阵 + 统一执行引擎)**。

#### 详细架构落地细则：

1. **执行引擎单一事实源 (`WallpaperChangeExecutor`)**：
   * 将所有触发通道（`WallpaperWorker`、`WallpaperAlarmReceiver`、`ScreenOffWatcherService`、`CurrentWallpaperWidgetProvider`、`QuickSettingsTile`、UI 界面）彻底降级为无状态的**触发管道**。
   * 由 `WallpaperChangeExecutor.execute()` 统管完整流水线：前置过滤拦截 $\rightarrow$ 动态规则计算 $\rightarrow$ 图源解析实例化 $\rightarrow$ 候选抽取 $\rightarrow$ 幂等去重 $\rightarrow$ 解码下采样 $\rightarrow$ 系统壁纸注入 $\rightarrow$ 历史入库与桌面微件联动。

2. **四维时空分流调度矩阵**：
   * **宏观周期兜底**：使用 Jetpack `WorkManager`（周期任务），利用系统原生批量优化执行常规跨小时级轮换。
   * **时钟精准定点**：通过 `AlarmManager.setExactAndAllowWhileIdle` 注册早晨/夜晚打卡闹钟，唤醒执行后自动计算并注册下一个锚点。
   * **即时熄屏感知**：按需启动轻量 `ScreenOffWatcherService`，在内存中动态注册 `ACTION_SCREEN_OFF`，在用户锁屏熄屏的瞬间无感触发。
   * **快捷交互入口**：状态栏 Quick Settings Tile、桌面长按 Shortcuts 及 Glance AppWidget 提供即时异步调用。

3. **网络约束应用层下放与离线降级容灾**：
   * 彻底移除 WorkManager `Constraints.Builder` 中的系统级网络约束，确保无论是否有网 Worker 均能被系统唤醒。
   * 约束检查完全下放至业务层：有 Wi-Fi 则向网络源拉取新图并写入 LRU 缓存池；无网或离线时，优雅降级为直接从本地 `cacheDir/wallpapers/` 缓存池中随机抽选旧图轮播，达成“断网不掉线、零流量消耗”。

4. **三层前置防碰撞复合漏斗**：
   * **夜间免打扰（Quiet Hours）**：时段命中则静默跳过，免除夜间无谓唤醒耗电。
   * **冷却时间抑制（Cooldown Suppression）**：检查 `now - lastChangedTimestamp < cooldownMinutes`，时间间隔过近主动拦截，规避高密刷新（注：手动触发与即时熄屏切换豁免此限制，避免与后台大冷却打架，由独立切换延迟保护）。
   * **交互避让（Interactive Deferral）**：查询 `PowerManager.isInteractive`，若用户正在亮屏交互，推迟后台轮换直至熄屏。
   * **内容幂等去重（Deduplication）**：抽选到的壁纸若与当前正在生效的壁纸 URI 和标题一致，提前中止后续解码与系统设置，节约 CPU 算力。

5. **动态规则日程表 (`ScheduleRuleEngine`)**：
   * 淘汰早期的全局单一配置，引入 Room 独立规则库 `ScheduleRulesDatabase`。
   * 触发时由规则引擎动态计算当前时间戳与触发事件上下文，优先匹配高优先级规则所绑定的专属图源与指定屏幕（桌面/锁屏）；无规则匹配时，无缝回退至全局默认源。

### 5. 影响评估与技术代价
* **积极收益**:
  * 完美满足早晨 08:00 准点打卡的高阶生活化诉求，无需常驻后台服务吞噬电量。
  * 具备极强的离线韧性，飞行模式或弱网下自动复用本地缓存池。
  * 统一了系统壁纸切换的所有调用入口，彻底消灭了并发冲突与交互卡顿。
* **消极代价与缓解措施**:
  * *消极代价*: 调度系统组件较多（Worker、Alarm、Service、Receiver），增加了系统级调试与排错成本。
  * *缓解措施*: 在 `PreferencesManager` 中统一记录 `lastExecutionStatus` 与 `lastErrorMessage`，并在 UI 控制台直观向用户呈现最近执行状态。

### 6. 演进阈值与推翻临界点
* 仅当 Android 官方未来在 AOSP 中原生引入支持熄屏感知、精准定时且最低兼容至 API 23 的全量壁纸调度接口时，方可评估废弃本多轨分流架构。
