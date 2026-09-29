# Wallpaper Changer 项目规划与路线图 (PLANS.md)

## 1. 项目愿景与需求背景

### 1.1 项目定位
一款轻量、极低功耗、支持多来源（本地相册、自建 Immich 服务、Nextcloud / PhotoPrism 私有云、HTTP API 等）的 Android 随机/自动壁纸切换工具。

### 1.2 核心诉求与约束
- **跨代系统兼容**：最低支持至 **Android 6.0 (API 23)** 老旧设备，同时适配现代 Android（**API 34+**），确保在低内存（1GB~2GB RAM）设备上不闪退、不耗电。
- **单分支架构**：坚持单工程单分支开发，依靠 AndroidX / Jetpack 标准库与良好的抽象设计抹平多代系统差异，拒绝维护双分支。
- **来源可插拔**：壁纸获取逻辑必须与壁纸应用、后台调度彻底解耦，后续接入新源（如 Immich、Wallhaven 等）时实现零侵入即插即用。

---

## 2. 技术规格与兼容底线

| 配置项 | 设定值 | 理由与说明 |
| :--- | :--- | :--- |
| `minSdkVersion` | `23` (Android 6.0) | 覆盖存量老旧设备，最低保障系统支持 |
| `targetSdkVersion` | `36` (Android 16) | 严格遵循 Google Play 2026 强制规范（新应用与更新需 API 36+） |
| `compileSdkVersion` | `36` | 允许使用最新的平台编译特性 |
| **后台调度** | Jetpack `WorkManager` | 原生兼容 API 21+，系统自动分配 JobScheduler/Alarm 调度 |
| **网络层 (后续)** | `OkHttp` + `Conscrypt` | 解决 Android 6.0 老设备 Let's Encrypt (ISRG Root X1) CA 证书过期与 TLS 1.3 缺失问题 |
| **图像加载与裁切** | `Coil` (或轻量自定义管线) | 必须强制以物理屏幕分辨率下采样 (Subsampling)，杜绝老设备 OOM |
| **本地存储访问** | SAF (`ACTION_OPEN_DOCUMENT_TREE`) | 规范访问用户选择目录，必须持久化 URI 权限 |

---

## 3. MVP 阶段规划 (Phase 1: Local-First Minimal Loop)

> **MVP 核心策略**：排除网络不确定性，**先只做本地文件夹来源**，但把**核心分层抽象**做完整。集中验证后台保活调度、防 OOM 图像下采样以及老设备兼容性。

### 3.1 核心架构抽象（必须先行）

```
[ WallpaperSource (接口) ]
        │ 输出 WallpaperData (流/URI/元信息)
        ▼
[ WallpaperProcessor (图像处理器) ]
        │ 根据屏幕物理尺寸进行 Downsampling / Center Crop，规避 OOM
        ▼
[ WallpaperApplier (系统注入器) ]
        │ 处理 API 23 (仅双改) 与 API 24+ (支持分设桌面/锁屏) 差异
        ▼
[ WallpaperManager (系统服务) ]
```

### 3.2 MVP 包含功能 (In Scope)

1. **抽象接口定义**：
   - 定义 `WallpaperSource` 接口规范与通用结果模型 `WallpaperData`。
2. **本地文件夹源 (`LocalFolderSource`)**：
   - 通过系统 SAF (`OpenDocumentTree`) 选择目标壁纸文件夹。
   - **关键机制**：执行 `takePersistableUriPermission`，确保持久授权在重启与后台 Worker 醒来时有效。
   - 过滤支持的图片格式（`.jpg` / `.png` / `.webp`），随机抽取一张。
3. **图像防爆处理器 (`WallpaperProcessor`)**：
   - 动态获取当前屏幕物理分辨率（`DisplayMetrics`）。
   - 使用 `inSampleSize` 进行安全内存采样解码，并进行 Center-Crop，严格防止大图撑爆 1GB 设备内存。
4. **壁纸注入器 (`WallpaperApplier`)**：
   - 适配 Android 6.0（API 23）：执行 `WallpaperManager.setBitmap()`（同时更改桌面与锁屏）。
   - 适配 Android 7.0+（API 24+）：根据用户偏好分别支持仅桌面、仅锁屏或两者都改。
5. **后台定时调度引擎 (`WallpaperWorker`)**：
   - 基于 `PeriodicWorkRequestBuilder` 注册周期任务。
   - 预设可选轮换间隔：`15 分钟`、`1 小时`、`6 小时`、`每天`。
6. **极简用户界面 (Single-Screen UI)**：
   - **控制中枢**：当前激活源状态、上次更换时间显示。
   - **「立即更换」按钮**：点击即刻触发一次执行流程，方便开发调试与日常即时切换。
   - **源选择与配置**：本地文件夹路径选择、权限状态显示。
   - **调度偏好**：轮播间隔选择、桌面/锁屏目标单选（Android 6.0 自动置灰分设选项）。
7. **保活与运行保障**：
   - 引导用户开启**电池优化白名单**（Doze 模式豁免弹窗）。

---

### 3.3 MVP 明确排除的功能 (Explicit Non-Goals / Out of Scope)

为确保在短期内高质量跑通闭环，以下功能**严禁进入 MVP**：
- ❌ **网络相关源（Immich / HTTP API）**：推迟至 Phase 2。
- ❌ **海量目录索引与扫描（> 1,000 张）**：MVP 仅支持几百张以内的专属壁纸目录，不引入 SQLite / Room。
- ❌ **历史记录与防重样去重（Shuffle 队列）**：MVP 仅做纯随机挑选。
- ❌ **桌面小部件（AppWidget）与快捷方式**。
- ❌ **双击桌面切壁纸、手势联动与外部自动化（Tasker 等）**。
- ❌ **壁纸特效（模糊、滤镜、暗色模式遮罩）**。
- ❌ **多源加权混合轮播**。

---

## 4. 后续演进路线图 (Future Roadmap)

### Phase 2: 拓展网络源（Immich & 通用 HTTP API）
- **实现 `HttpApiSource` (网络底座与通用 HTTP 验证)**：
  - 响应自动分流：`Content-Type: image/*` 直接流式传递给处理器；JSON 响应支持点分路径（Dot Notation，如 `images[0].url`）与常用预设（Bing 每日壁纸等）。
  - 支持相对路径自拼接与数组元素随机抽取（如 `data[*].path`）。
- **实现 `ImmichSource`**：
  - 配置：服务器地址、API Key、相册选择（可选）。
  - 调用 Immich 接口拉取随机照片并安全流式下载。
  - *(注：对于 Nextcloud Memories、PhotoPrism 等更多自建相册服务，规划在阶段 5.6 依托统一抽象基类拓展接入)*。
- **老设备 TLS 修复**：
  - 引入 `Conscrypt`，修复 Android 6.0 连现代 HTTPS (Let's Encrypt ISRG Root X1) 报证书无效与 TLS 1.3 缺失的问题。
- **网络与电量策略 (Network & Battery Constraints)**：
  - **网络约束**：WorkManager 增加 `NetworkType.UNMETERED`（仅在 Wi-Fi 下下载开关，默认开启）与 `NetworkType.CONNECTED`（允许蜂窝移动数据）。
  - **电量约束**：开启 `setRequiresBatteryNotLow(true)`（低电量保护，低于 15% 时暂停自动下载）。
  - **前台手动豁免**：用户点击「立即更换」作为明确操作意图，豁免 Wi-Fi 限制执行单次刷新。
- **轻量 LRU 滚动缓存与离线容灾策略**：
  - **存储目录**：统一管理在 `context.cacheDir/wallpapers/`，免申请外部存储权限且系统低存储时可安全回收。
  - **容量硬顶与纯 LRU 淘汰**：
    - 采用纯容量/数量驱动的 LRU 淘汰（默认上限约 25 张 / 50MB），池满时淘汰最久未展示的旧图。
    - **明确不设时间过期 (No Time-based TTL)**：杜绝长期断网或飞行模式下因时间到期导致缓存被误删、壁纸停更报错。
  - **手动清理与存储透明化 (Phase 2 标配)**：
    - 界面常态展示缓存大小（如 `当前缓存: 35.8 MB`）并提供「一键清理缓存」按钮。
    - 赋予用户掌控感，消除老设备存储焦虑，避免用户去系统设置点击“清除全部数据”导致丢失 SAF 授权与配置；同时提供坏图自愈能力。
  - **断网/蜂窝优雅降级**：
    - 当开启“仅在 Wi-Fi 下下载”，若处于移动网络或离线状态，Worker 自动从本地 LRU 缓存池中随机挑选旧图复用切换，实现“零流量消耗、断网不掉线”。
    - 若彻底无网且缓存为空，静默跳过本次调度，等待下次网络恢复。
- **UI 过渡方案**：
  - 此阶段保持单页架构，各网络源的高级配置（URL、API Key、JSONPath、Wi-Fi 开关等）采用 `ModalBottomSheet`（底部弹窗）承载，避免主页视觉臃肿。

### Phase 3: 大型目录与系统相册索引 (Large Directory & MediaStore)
- **阶段 3.1（极速读取优化）**：
  - 绕过慢速的 `DocumentFile` 包装，使用底层 `DocumentsContract` 原生游标直查，几千张图遍历延迟从 8s 压降到 200ms。
- **阶段 3.2（系统相册原生支持）**：
  - 支持直接绑定系统相机相册（`DCIM/Camera`），利用 Android 系统的 `MediaStore` 发起高效 SQL 查询，瞬间检索数万张生活照。
- **阶段 3.3（本地轻量索引缓存）**：
  - 引入 Room / SQLite，对选定的大型目录在后台做一次性建表。
  - 定时更换时直接执行 `SELECT uri FROM wallpapers ORDER BY RANDOM() LIMIT 1`，实现 0ms 零功耗换图。
- **阶段 3.4（当前壁纸交互与动作：打开、保存与分享）**：
  - **在图库中打开 (View in Gallery)**：通过 `Intent.ACTION_VIEW`，结合 `FileProvider`（针对网络/缓存图源）或原生 Content URI（本地图源）拉起外部图库/照片查看器，支持未裁切原图全屏缩放、EXIF 详情查看与第三方编辑。
  - **保存到相册 (Save to Gallery)**：
    - **Android 10+ (API 29+)**：利用 `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` 零权限无摩擦写入公共媒体目录（`Pictures/Wallpapers`）。
    - **Android 6.0~9.0 (API 23~28)**：老设备适配运行时 `WRITE_EXTERNAL_STORAGE` 权限检查与兼容写入。
  - **系统分享 (System Share Sheet)**：
    - 发送 `Intent.ACTION_SEND` 调起原生分享面板，支持一键发送到社交应用（微信、Telegram 等）。
    - 天然复用系统分享面板内置的“另存为 / 复制到文件管理”动作，零成本满足高阶用户的自定义目录归档需求。

### Phase 4: 体验与进阶特性 (UX & Advanced Features)
- **UI 全面重构：从单页演进至多 Tab 架构 (Navigation Bar)**：
  - **重构动因**：单页承载大量图源配置（Local/Immich/HTTP）与历史图片流时将面临严重滚动冲突与信息过载，需将高频操作与低频设置彻底解耦。
  - **Tab 1: 桌面 / 控制台 (Dashboard)**：
    - 当前壁纸大图卡片预览与状态信息（当前来源、上次更换、下次倒计时）。
    - 核心高频操作：一键「立即更换」、快捷「设为收藏」。
    - 当前壁纸操作栏：提供快捷动作「在图库中打开」、「保存到相册」、「系统分享」、「微调构图/滚动」。
  - **Tab 2: 历史与画廊 (Gallery / History)**：
    - 采用双列/三列瀑布流展示历史已用壁纸与收藏列表。
    - 支持大图全屏预览、微调裁切焦点与滚动偏好、一键导出到系统相册 (`Pictures`)、删除单张缓存。
  - **Tab 3: 图源与设置 (Settings & Sources)**：
    - 多图源集中管理与切换（本地文件夹、Immich 凭据、HTTP API 自定义与预设）。
    - **海量相册选择器重构 (Album Picker Refactor)**：
      - 淘汰早期横向滚动 Chip（彻底解决相册数十上百时 $O(N)$ 盲目滑动、长相册名截断与 Android 边缘返回手势冲突的痛点）。
      - 升级为独立选择器面板 / 专有二级选择页（支持即时搜索过滤 `Search Filter` 与流畅纵向滚动）。
      - 进阶特性：展示相册封面缩略图预览，并支持「多相册多选组合轮播」（如同时勾选“旅行”与“风景”相册混合抽取）。
    - 调度周期偏好、Wi-Fi / 流量约束。
    - **可配置缓存容量档位**：提供「小 (约 20MB / 10 张)」、「标准 (约 50MB / 25 张，默认)」、「大 (约 100MB / 50 张)」离散档位选择，兼顾极致省空间与离线丰富度，并提供一键清理缓存。
    - 屏幕目标（桌面/锁屏）与壁纸随桌面滚动模式。
- **收藏系统与离线持久化策略 (Favorites System & Storage Guarantees)**：
  - **存储与缓存物理隔离（永不自动过期）**：
    - 收藏属于用户个人资产，**严格禁止任何形式的基于时间 (TTL) 或容量淘汰 (LRU) 的自动清理**。
    - **网络源资产转正 (Cache Promotion)**：用户收藏网络图片（Immich/HTTP API）时，触发静默提升机制，将其从临时目录 `cacheDir/wallpapers/` 拷贝固化至私有持久目录 `filesDir/favorites/{id}.jpg`，断网或远程图删除时依然离线永久可用。
    - **本地源轻量持久化**：针对本地 SAF/MediaStore 图源，仅在 Room 数据库记录 Content URI、文件名与极轻量缩略图，避免重复拷贝原图占用存储；若外部原图被删除，友好提示并引导处理。
    - **LRU 清理保护机制**：后台或用户手动触发清理临时缓存时，SQL 严格限制 `WHERE is_favorite = 0`，对已收藏数据具备绝对豁免权。
    - **透明容量看板**：在设置页清晰展示收藏数量与空间占用（如 `已收藏: 32 张 (45.2 MB)`），支持批量管理、取消收藏与一键导出到相册，清理控制权 100% 交由用户。
- **[已实现] 单图个性化偏好记忆 (Per-Image Scroll, Crop Center & Flip Preferences)**：
  - **覆写机制 (Inherit & Override)**：默认 100% 继承全局设置（全局 `WallpaperScrollMode` 与居中裁切 `centerCrop`），用户无需逐张配置。
  - **构图微调入口**：在 Dashboard 当前壁纸操作栏与画廊/收藏详情页提供「微调构图与滚动」交互入口，允许用户实时模拟预览与调整。
  - **数据库持久化设计 (SQLite Database)**：
    - 表字段扩展：`custom_scroll_mode`（AUTO / ALWAYS / NEVER，可为 NULL）、`crop_focus_x`（0.0 ~ 1.0 归一化相对水平焦点）、`crop_focus_y`（0.0 ~ 1.0 归一化相对垂直焦点）、`flip_horizontal`（水平镜像翻转布尔值）。
    - 采用相对百分比而非固定像素坐标，屏幕旋转或系统分辨率调整时自动维持最佳构图。
  - **处理管线联动**：`WallpaperProcessor` 解码并裁切时优先读取当前图片的数据库覆写参数；存在覆写时按指定焦点、水平镜像与滚动策略裁切，无覆写时自动回退全局规则。
- **无重复轮播 (Fair Shuffle)**：记录最近 $N$ 次已用壁纸，避免短期内频繁看到重复图片。
- **复合触发器与时钟定点调度 (Composite Triggers & Wall-Clock Schedules)**：
  - **核心痛点与生活化诉求**：单一的相对周期轮换（如每 1 小时）无法满足“每天早晨 08:00 必须有一张全新壁纸（定点打卡）”、“白天办公时段保持新鲜度”、“夜间睡眠免打扰静默”以及“按时段切换不同风格（白天风景/夜晚暗黑）”等复合日程诉求。
  - **分层递进架构**：
    - **第一阶段（主从复合模式 Anchor + Interval + Quiet Hours）**：
      - **每日定点基准点 (Daily Anchor)**：支持设置每天固定时刻（如 08:00），通过 WorkManager `initialDelay` 或按需 AlarmManager 准时刷新一张壁纸，并将后续相对周期轮换以此基准点自动对齐。
      - **夜间免打扰时段 (Quiet Hours)**：用户可自定义休眠区间（如 23:00 ~ 07:00），Worker 唤醒时检测当前系统时间处于静默期则自动跳过，杜绝夜间无谓唤醒耗电。
      - **防碰撞冷却抑制 (Cooldown Suppression)**：当定点打卡时刻与周期轮换任务接近（如 07:55 与 08:00）时，基于 `lastChangedTimestamp` 实施最小时间间隔拦截，避免过密刷新壁纸。
    - **[已实现] 第二阶段（独立规则日程表 Schedule Rule Engine）**：
      - 支持多条调度规则组合（如规则 1：每日 08:00 切换专属 Morning 摄影相册；规则 2：09:00~18:00 每 2 小时随机轮播；规则 3：20:00 切换深色暗黑图源）。
      - 独立 SQLite 规则数据库（`ScheduleRulesDatabase`）持久化存储与管理。
      - 规则引擎（`ScheduleRuleEngine`）动态按触发时刻与事件（定点打卡、时段窗口、熄屏切换）匹配对应规则，并解析专属图源（指定图源实体、我的收藏或全局源）与指定屏幕目标。
      - 调度引擎分流：常规任务交由 WorkManager，精确定点与短间隔借力 AlarmManager 高精度时钟与 ScreenOffWatcherService。
      - 设置页提供直观的可视化多规则卡片管理、新增/编辑弹窗（`ScheduleRuleEditDialog`）、启停开关与默认预设一键载入。
    - **外部环境感知扩展**：
      - 对于基于网络（特定 Wi-Fi SSID）、车载蓝牙连接、充电状态、NFC 触碰、地理围栏等更复杂的外部情境触发，规划收拢至阶段 5.5，通过标准的 **Tasker / Locale 插件** 由专业自动化工具编排触发，避免本应用自身常驻前台监听。
- **桌面微件 (AppWidget)**：在手机桌面上放置一个快捷按钮，无需打开应用一键切壁纸。
- **设计升级**：支持 Material You 动态主题取色，优化弱网/失败时的静默降级与通知提示。

---

## 5. 调研废弃与明确不予支持的设想 (Rejected Ideas & Non-Goals)

记录经过深度技术评估后明确拒绝、放弃或不可行的技术路径，避免日后重复调研踩坑。

### 5.1 Google Photos 云端直连源 (Google 相册)
- **结论**：**永久放弃云端 API 原生接入**。替代方案统一收拢至 **Phase 3.2 (本地相册 MediaStore 索引)** 或 **Phase 2 (用户自建 Immich 导入)**。
- **核心阻碍与废弃理由**：
  1. **官方 API 政策全面收紧**：Google 于 2024 年末起废弃、并在 2025 年 3 月底彻底停用了具备后台静默全量读取权限的 `Google Photos Library API`（`photoslibrary.readonly` 等）。
  2. **Picker API 寿命硬伤（死穴）**：替代方案 `Google Photos Picker API` 专为即时单次导入（如发推/印照片）设计：
     - **无法整本订阅**：不支持直接勾选整个相簿并持续监听，仅能单次交互式点选单张照片。
     - **7 天授权会话强制过期**：Session 寿命仅 7 天，过期后无法刷新，意味着用户每 7 天壁纸就会停摆并被强制打断要求重新授权。
     - **直链仅 60 分钟有效**：`baseUrl` 极其短命，无法直接持久化。
  3. **合规审计与包体成本过高**：需要集成庞大的 GMS/OAuth 依赖（老旧 Android 6.0/精简 ROM 易不兼容），且个人开发者极难通过 Google 的受限敏感权限第三方安全审计（CASA Tier 2）。
  4. **公开分享相册网页爬取不可靠**：解析 `photos.app.goo.gl` 共享网页极易受 Google 前端混淆变更、反爬限流（429）、动态 RPC 截断影响，稳定性极差。

### 5.2 端侧生成式大模型 (On-Device Generative AI / Diffusion Models)
- **结论**：**永久放弃端侧 AI 文生图模型原生接入**。替代方案统一收拢至 **Phase 2 (通用 HTTP API 图源)** 接入远程云端生图接口。
- **核心阻碍与废弃理由**：
  1. **低配设备内存爆炸 (OOM 必发)**：现代移动端扩散模型（如 Mobile Diffusion / On-Device Stable Diffusion）即使经过极限轻量量化，模型权重与运行时显存/内存峰值通常仍需数百 MB 至数 GB，直接击穿本项目基线设备（Android 6.0、1GB~2GB RAM）的物理限制。
  2. **后台功耗与温升不可承受**：端侧单次推理耗时常达数十秒甚至分钟级，全核 CPU/GPU/NPU 满载。若由 `WorkManager` 或 `AlarmManager` 在后台静默唤醒触发，会导致设备严重发烫与异常掉电，彻底违背本项目“极低功耗、长效稳定静默保活”的核心宗旨。
  3. **硬件加速跨代断层**：老旧 Android 系统（API 23~28）缺失现代 NNAPI 或 Vulkan 算子支持，模型移植与运行时依赖库体积膨胀剧烈。
- **云端接入指引**：若用户有 AI 生成图片诉求，完全无需端侧算力承担，直接在 Phase 2 的 `HttpApiSource` 中配置云端生图端点（如自建 ComfyUI / SD-WebUI，或免鉴权 AI 图源如 Pollinations.ai `https://image.pollinations.ai/prompt/...`），由设备发起轻量 HTTP 请求流式下载即可。


---

## 6. 远期生态与多渠道分发规划 (Phase 5: Future Ecosystem, Distribution & Store Readiness)

> **核心哲学 (Self-use & Lightweight First)**：
> 当前核心重心为个人高频自用与代码底座健壮性。上架应用商店（Google Play / F-Droid）属于远期储备特性。
> 架构设计遵循**分阶段解耦**：自用阶段依靠「GitHub Release + Obtainium」实现零代码侵入的极客自动更新；当未来有跨国开源或商店上架需求时，再按变体（Flavors）平滑过渡。

---

### 6.1 阶段 5.1：自用极客流——GitHub Release 与 Obtainium 生态集成

自用场景下，在 App 内编写复杂的 APK 文件下载器、安装器并申请高危敏感安装权限（`REQUEST_INSTALL_PACKAGES`）会增加包体并消耗后台电量。首选利用 Android 极客开源更新管理器 **Obtainium** 实现低成本托管更新。

1. **标准化 GitHub Release 规范**：
   - **Tag 规范**：严格遵循 `vX.Y.Z`（如 `v1.1.0`），且与 `build.gradle.kts` 的 `versionName` 严格对齐。
   - **Asset 命名标准化**：固定输出 `WallpaperPicker-vX.Y.Z.apk`（或带架构的 `WallpaperPicker-vX.Y.Z-arm64-v8a.apk` / `universal.apk`），便于自动化工具识别。
   - **Changelog 与校验码**：Release Body 保持清晰 Markdown 格式，文末附带 APK 的 SHA-256 Checksum。
2. **应用内对 Obtainium 的友好支持**：
   - **一键托管 Deep Link**：在「关于」或「设置」页提供「在 Obtainium 中跟踪更新」快捷按钮，点击调用 Intent：
     `obtainium://app/https://github.com/foobarz076/Wallpaper-Picker-Android`
     已安装 Obtainium 的设备可直接一键添加该项目，获得丝滑静默更新体验。
3. **应用内轻量只读提示（只提示、不强制下载）**：
   - 自用阶段不在 App 内集成复杂的 APK 文件下载器与安装器。
   - 仅通过 GitHub API（带 ETag / `If-None-Match` 缓存避免消耗 60 次/小时限制）检测新 Tag。发现新版后，在界面轻量弹出卡片，提供两个纯跳转按钮：
     - `前往 GitHub Release 网页`
     - `复制仓库链接至更新器 (如 Obtainium)`

---

### 6.2 阶段 5.2：国际化与本地化支持 (i18n & l10n)

面向开源社区与多语言用户的体验优化。

1. **资源解耦与兜底策略 (Fallback Architecture)**：
   - **默认 Base 语言强制为英文**：将 `app/src/main/res/values/strings.xml` 全部重构为英文。当未适配语言（如法语、德语、日语）用户使用时，安全降级到英文，绝不回退至乱码或中文。
   - **中文本地化目录**：建立 `res/values-zh-rCN/strings.xml`（简体中文）与 `res/values-zh-rTW/strings.xml`（繁体中文）。
   - **Compose 界面彻底脱敏**：消除所有 Tab 和 Activity 中的中文字符串硬编码，全部替换为 `stringResource()` 与 `pluralStringResource()`。
   - **法律与固定文本隔离**：开源协议（GPL-3.0）全文与 GitHub 仓库 URL 声明 `translatable="false"`。
2. **Android 13+ 单应用语言偏好 (Per-App Language)**：
   - 增加 `res/xml/locales_config.xml`，并在 `AndroidManifest.xml` 中配置 `android:localeConfig="@xml/locales_config"`。
   - 用户无需修改整机语言，即可在 Android 13+ 系统设置中单独将 App 切换为中文或英文。
   - 老设备（Android 6.0~12）在设置页提供切换项，基于 AndroidX `AppCompatDelegate.setApplicationLocales` 向下兼容。
3. **开源协同翻译工作流**：
   - 接入免费的 **Hosted Weblate** 平台，由社区志愿者协同翻译小语种并自动向 GitHub 提交 PR。
   - 建立 Fastlane 标准元数据目录（`fastlane/metadata/android/{en-US,zh-CN}/`），用于商店标题与介绍的本地化展示。

---

### 6.3 阶段 5.3：多渠道变体与功能差异矩阵 (Product Flavors Matrix)

当项目正式准备对外多渠道分发时，通过 Gradle `productFlavors` 物理隔离不同平台的代码与配置：

| 功能维度 | `standalone` (GitHub / Obtainium 自用版) | `fdroid` (F-Droid 官方社区版) | `play` (Google Play 官方版) |
| :--- | :--- | :--- | :--- |
| **更新机制** | GitHub API 检查 + Obtainium 链接 + 可选内置下载安装 | **完全禁用应用内更新**（由 F-Droid 客户端统一部署） | **禁止自下载 APK**，可选接入官方 Play In-App Updates |
| **安装权限 (`REQUEST_INSTALL_PACKAGES`)** | 按需保留（若启用应用内直装） | **严禁声明**（触发反特性阻碍收录） | **严禁声明**（非应用商店类应用必被拒审） |
| **电池优化权限 (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)** | 允许声明，直接拉起系统豁免弹窗 | 允许声明（开源无妨） | **严禁声明**（Play 商店高危红线，仅允许跳系统通用设置页） |
| **网络安全策略 (Cleartext Traffic)** | 宽容：允许明文 HTTP（方便自用私有部署的内网 Immich / NAS） | 标准：推荐默认 HTTPS，仅对局域网私网放行明文 | 严格：强制网络安全配置，禁止全域明文，禁止非加密公共 HTTP |
| **HTTP API 预设图源** | 预置丰富源（Bing、Wallhaven、二次元 API 等） | **仅提供通用自定义模板**，避免被标记 `NonFreeNet` 反特性 | 审查图源版权与合规性 |
| **开源纯洁度与代码签名** | 开发者个人 Release Keystore 签名 | F-Droid 构建机源码编译 + F-Droid 独立密钥签名 | Google Play App Signing (AAB 格式分发) |

#### 网络安全策略分渠道配置设计：
- `standalone`: `android:usesCleartextTraffic="true"`（保障自用阶段老旧路由器、内网 HTTP NAS 顺畅连通）。
- `play` / `fdroid`: 引入 `res/xml/network_security_config.xml`：
  ```xml
  <network-security-config>
      <base-config cleartextTrafficPermitted="false" />
      <domain-config cleartextTrafficPermitted="true">
          <!-- Allow plain HTTP only for private LAN subnets -->
          <domain includeSubdomains="true">192.168.*.*</domain>
          <domain includeSubdomains="true">10.*.*.*</domain>
          <domain includeSubdomains="true">172.16.*.*</domain>
          <domain includeSubdomains="true">localhost</domain>
      </domain-config>
  </network-security-config>
  ```

---

### 6.4 阶段 5.4：应用商店上架前审计清册 (Store Pre-flight Checklist)

正式提交发布前必须逐项核对的“避坑检查清单”：

#### 1. Google Play 商店合规清册
- [ ] **高危权限彻底剥离**：
  - 清单中彻底移除 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`。
  - 清单中彻底移除 `REQUEST_INSTALL_PACKAGES`。
  - 检查存储权限：完全收拢至 SAF（`ACTION_OPEN_DOCUMENT_TREE`），移除全量多媒体广播扫描权限。
- [ ] **网络安全规范**：移除 `usesCleartextTraffic="true"`，引入细粒度白名单。
- [ ] **Android 15+ 16KB 页对齐**：验证依赖中的 C/C++ 动态链接库（如 `conscrypt-android` 的 `.so` 文件）是否符合 16KB 内存页对齐标准。
- [ ] **法律与隐私合规**：
  - 在 GitHub Pages 部署独立的静态 `PRIVACY_POLICY.html`。
  - 在 Play Console 完成 Data Safety（数据安全表单）声明：标明“不收集任何用户隐私数据”。
- [ ] **账号与分发准备**：个人开发者账号完成 20 人 / 14 天封闭测试门槛；导出 AAB 产物。

#### 2. F-Droid 社区源收录清册
- [ ] **全流程源码构建验证**：
  - 确认全项目零闭源 SDK、零追踪统计、零私有二进制 blob。
  - 根目录确认存在标准 `LICENSE` (GPL-3.0) 与英文版 `README.md`。
- [ ] **构建脚本与元数据匹配**：
  - `gradle-wrapper.jar` SHA-256 签名干净未篡改。
  - Git Tag（如 `v1.0.0`）与 `app/build.gradle.kts` 的 `versionName`、`versionCode` 严格一致。
  - 构建脚本兼容 F-Droid 编译机环境（明确声明 JDK 17）。
- [ ] **Anti-Features（反特性）审查**：
  - 审查预设 HTTP API：避免硬编码闭源商业图源，避免被打上 `NonFreeNet` 标签。
  - 确认禁用或剥离一切应用内外部 APK 自下载代码，避免被标记 `UpstreamNonFree`。

---

### 6.5 阶段 5.5：第三方自动化与 Tasker 插件生态 (Tasker / Locale Automation Plugin)

为满足高阶极客用户与情境感知联动需求，在不增加本应用常驻后台电量消耗的前提下，接入 Android 经典自动化生态。

1. **架构原则：专业的事交给专业工具 (Delegation Philosophy)**：
   - 避免本应用自行常驻后台监听 Wi-Fi 变更、车载蓝牙配对、地理围栏或充电状态（杜绝电池隐形损耗与申请高危敏感权限）。
   - 遵循业界成熟的 **Tasker / Locale Plugin 协议** 与标准 **Intent 广播机制**，由外部专业自动化 App（Tasker、MacroDroid、Automate 等）完成复杂的环境上下文感知与条件编排，本应用作为无状态执行器响应。

2. **核心动作插件 (Tasker Action Plugin - `TwoFortyFourAM` 标准)**：
   - **动作 1：立即触发壁纸更换 (Trigger Next Wallpaper)**：
     - 提供独立轻量设置 Activity（`EditSettingActivity`），支持用户在 Tasker 内配置动作参数并持久化为 Bundle。
     - **可选覆写参数 (Optional Override Parameters)**：
       - `target_screen`：指定更换屏幕目标（`SYSTEM` 桌面 / `LOCK` 锁屏 / `BOTH` 两者）。
       - `source_type`：临时覆盖当前激活源（例如强行从本地缓存或指定私有相册拉取）。
       - `tag_or_album`：按特定相册/标签过滤抽取（如连接车载蓝牙时限定抽取“公路/驾驶”相册，睡眠时抽取“夜景”）。
       - `crop_override`：临时指定构图裁切偏好。
   - **动作 2：无缝切换当前激活源与预设配置 (Switch Active Profile)**：
     - 允许通过场景一键切流（例如：离开家时自动从 NAS 图源切回本地文件夹，避免消耗移动蜂窝流量；连上家庭 Wi-Fi 自动切回自建相册服务）。

3. **事件通知与双向变量回传 (Wallpaper Changed Broadcast & Variable Return)**：
   - 壁纸切换完成后，应用向系统发出标准广播 `foo.barz.wallpaperpicker.action.WALLPAPER_CHANGED`。
   - **携带元数据 Bundle**：
     - 当前壁纸 URI / 本地缓存绝对路径 (`%wp_path`)。
     - 图片所属相册名称与来源类型 (`%wp_album`, `%wp_source`)。
     - 提取的核心调色板色彩（Palette Primary / Dominant Color 十六进制值，如 `%wp_color`, `%wp_color_muted`）。
   - **极客联动场景**：Tasker 捕获广播后，可提取 `%wp_title`、`%wp_color` 动态调整第三方桌面部件（KWGT）色彩、系统状态栏颜色或同步控制房间智能灯光氛围（Home Assistant / 米家）。

4. **系统限制适配与安全边界 (System Limits & Security Boundaries)**：
   - **Android 8.0+ 后台广播穿透**：接收到 Tasker 的 `FIRE_SETTING` 广播后，内部转由 WorkManager 的 `OneTimeWorkRequest` 异步派发，确保在系统 Doze 模式与限制后台服务策略下可靠执行。
   - **防死循环与冷却抑制**：内置最小调用时间保护（如 5 秒防抖），拦截外部脚本异常死循环连续触发，保护网络带宽与低配设备内存。

---

### 6.6 阶段 5.6：多私有云自建相册服务矩阵 (Expanded Self-Hosted Photo Services: Nextcloud Memories & PhotoPrism Matrix)

在 Phase 2 完成 Immich 接入的基础上，进一步拓展对自建私有云生态主流相册服务的原生支持，满足 NAS 与自托管用户的多元化资产管理习惯。

1. **Nextcloud Memories 深度适配**：
   - **背景与痛点**：Nextcloud 作为全球装机量第一的开源自托管网盘，用户照片量庞大；且官方原生 Photos 缺乏高效大相册索引，而高性能第三方插件 **Nextcloud Memories**（基于高效 SQL 索引）已成为自建相册事实标准。
   - **安全凭据体系**：
     - 严禁保存用户账户主密码，全面接入 Nextcloud 标准 **App Password (应用专用密码)** 或 OAuth2 授权。
     - 宽容支持非标端口、反向代理二级路径（如 `/nextcloud`）与局域网自签名证书（结合 Conscrypt 与专用安全配置）。
   - **双通路自适应访问引擎 (Dual Engine Architecture)**：
     - **通路 A：Nextcloud Memories REST API（高性能首选）**：
       - 自动检测并优先借力 Memories 扩展 API（`/apps/memories/api/timeline`、`/apps/memories/api/albums`、`/apps/memories/api/random`）。
       - 享受服务端数十万张照片的毫秒级极速随机索引与标签筛选能力，单次换图请求耗时从数百毫秒降低至几十毫秒，极大减轻树莓派等弱性能 NAS 负载。
     - **通路 B：原生 WebDAV / OCS 协议（高通用兜底）**：
       - 若服务端未部署 Memories 插件，无缝自动降级至 WebDAV `PROPFIND` 命令遍历指定云端相册文件夹（如 `/Photos`、`/InstantUpload`），解析 XML 元数据抽取图片直链。
       - 无需服务端额外安装插件，任意标准 Nextcloud 实例即配即用。

2. **PhotoPrism 原生接入**：
   - **背景与特性**：PhotoPrism 是基于 Go 语言构建的极速、高颜值自托管相册服务，原生具备强大的 AI 自动标签、色彩分析、人脸识别与强大的搜索语法，拥有极高的摄影与极客社群占有率。
   - **安全凭据体系**：
     - 支持 PhotoPrism 专用 API Token（通过请求头 `X-Auth-Token` 传递）及 HTTP Basic 认证。
     - 兼容支持公开分享相簿与私有实例。
   - **检索与抽取引擎 (Query & Retrieval Pipeline)**：
     - **深度对接 PhotoPrism REST API**：调用 `/api/v1/photos` 与 `/api/v1/albums`。
     - **高阶检索语法支持**：
       - 支持按相簿 UID 过滤 (`album:UID`)。
       - 支持按条件检索与随机抽取：利用 `order:random` 服务端随机排序，结合收藏夹筛选 (`s:favorite`)、色彩主题 (`color:...`)、分类标签 (`label:landscape`) 精准抽取心仪壁纸。
     - **图像高效流式传输**：
       - 对接 `/api/v1/photos/{uid}/dl` 高清原图端点或高质量预生成缩略图（如 `fit_2048`），配合 Android 端物理分辨率安全采样，兼顾极致画质与低内存消耗。

3. **自建私有相册源抽象层 (`SelfHostedSource` 架构泛化)**：
   - 提炼通用的自托管相册基类：
     - **智能网络探测**：自适应区分局域网直连（LAN IP）与公网访问（DDNS / Tailscale / 域名），配置差异化网络超时策略。
     - **通用流式管道**：无缝对接 Phase 2 的容量限制 LRU 临时缓存池与弱网/断网容灾机制（断网自动降级至本地缓存池轮播）。
   - **交互与体验整合 (UI & Album Picker Integration)**：
     - 深度复用 Phase 4 的海量相册选择器（Album Picker）：
       - 远程相册树状分页检索与纵向平滑滚动，淘汰横向滑动。
       - 支持勾选多个云端相册进行混合轮播抽取。
       - 云端缩略图预览与离线缓存一键清理。
   - **向前兼容矩阵预留**：
     - 抽象层保留对 **Synology Photos (群晖相册 API)** 以及通用 **WebDAV 挂载源** 的即插即用扩展槽位，未来新增私有云类型仅需实现元数据提取器。

---

### 6.7 阶段 5.7：程序化生成与算法几何艺术源 (Procedural & Generative Geometric Art Source)

致敬 Android 经典壁纸神器 Tapet。通过纯数学公式、矢量几何与程序化算法，直接在设备端实时生成高品质壁纸，构建无需任何外部素材与网络的“终极自足”图源。

1. **核心价值与设计哲学 (Core Philosophy: Tapet-Style)**：
   - **0 外部依赖与 0 流量消耗**：不依赖任何网络连接、不占用外部存储，彻底摆脱断网与服务器停机困扰。
   - **无限分辨率与零 OOM 风险**：纯原生 2D 绘图引擎（`android.graphics.Canvas` / `Paint` / `Path`）驱动，根据设备屏幕物理分辨率（`DisplayMetrics`）精准 1:1 动态光栅化生成，天然避免了大图解码时的内存膨胀与二次裁切损耗。
   - **极致轻量与极低功耗**：毫秒级渲染完成，包体增量几乎为零，从 Android 6.0 到最新系统绝对通杀。
   - **数学意义上的无穷组合**：结合随机种子（PRNG Seed）与调色板生成算法，实现理论上永不重复的壁纸流。

2. **视觉风格体系与算法规划 (Generative Styles)**：
   - **网格渐变 (Mesh Gradient)**：
     - 在 3x3 或 4x4 网格上随机分布色标控制点，基于双三次样条插值（Bicubic Interpolation）生成柔和、现代的高级弥散渐变，支持联动 Android 12+ Material You 动态壁纸提取色或夜间低饱和度调色板。
   - **多边形晶格与低多边形 (Low-Poly / Voronoi 泰森多边形)**：
     - 基于 Delaunay 三角剖分与 Voronoi 图算法，在平面随机扰动撒点并赋予光影梯度，生成极具科技感与立体感的折纸/晶格纹理。
   - **流体噪声与丝绸波纹 (Perlin / Simplex Noise Flow)**：
     - 利用二维柏林噪声生成平滑流场，绘制多层半透明丝绸状色带与波浪渐变，呈现极简有机流动视觉。
   - **孟菲斯与包豪斯几何平铺 (Memphis & Bauhaus Tessellation)**：
     - 基于网格排布圆形、多边形、折线与条纹图元，引入波普艺术风格的高对比度撞色与几何对称。

3. **运行时管线与架构整合 (Architecture Integration)**：
   - **实现 `ProceduralGeometrySource` 继承自 `WallpaperSource`**：
     - 覆写壁纸拉取契约，直接在内部动态创建目标分辨率 Bitmap 并执行 Canvas 绘制流程。
     - 输出内存流或私有轻量临时文件包装为标准 `WallpaperData`，对现有 `WallpaperProcessor` 与 `WallpaperApplier` 保持 100% 契合与透明。
   - **双重角色定位**：
     - **独立激活源**：用户可主动选择为系统壁纸源，配置偏好风格、复杂度因子与明暗基调。
     - **离线容灾“保底自愈”引擎 (Self-Healing Fallback)**：当配置为网络图源（Immich / HTTP / 私有云）但设备处于飞行模式/断网、且本地 LRU 缓存池已耗尽时，系统可配置自动触发程序化几何源即时生成一张保底壁纸，确保后台定时轮换逻辑永不空转报错。


