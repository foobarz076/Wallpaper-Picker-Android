# [ADR-0005] Minimum SDK Baseline Selection and Future API Deprecation Strategy / 最低 SDK 基线选型与未来 API 收紧策略

> **Metadata / 元数据**
> - **Status / 状态**: Accepted / 已采纳
> - **Date / 日期**: 2026-09-27
> - **Deciders / 决策者**: @Foo, @Antigravity
> - **Technical Story / 关联背景**: [`build.gradle.kts`](../../app/build.gradle.kts), Conversation [`10dfed40`](conversation://10dfed40-8842-4421-9745-276b70117cdf)

---

## Executive Summary / 双语摘要

### English Summary
This record documents the architectural decision to abandon legacy Android 4.4 (KitKat, API 19) support and establish **Android 6.0 (Marshmallow, API 23)** as the definitive `minSdkVersion` baseline. We justify this choice across modern toolchains (Jetpack Compose, D8 desugaring, multi-dex elimination), transport security (TLS 1.2/1.3, CA root expiration, HTTP/2), and runtime performance (ART vs. Dalvik, RenderThread, Baseline Profiles). Furthermore, we formalize a 4-tier trigger matrix governing future `minSdkVersion` bumps (targeting API 24, 26, 28/29, and 31) driven by dependency deprecation, OS market distribution (< 0.5%), and platform capabilities such as Notification Channels, Scoped Storage, and RenderEffect.

### 中文摘要
本决策记录正式归档了放弃兼容 Android 4.4 (KitKat, API 19) 并确立 **Android 6.0 (Marshmallow, API 23)** 为项目最低 `minSdkVersion` 基线的技术决策。我们从现代工具链（Jetpack Compose 准入门槛、D8 脱糖与根绝 Legacy MultiDex）、传输安全与网络栈（TLS 1.2/1.3 强制要求、系统根证书过期与 HTTP/2 协议栈）以及运行时渲染与性能（ART 混合编译、独立 RenderThread、基线配置文件与内存模型）全面论证了其必然性。同时，本文档制定了未来进一步收紧并提升最低 API Level 的触发准则与演进路线（涵盖向 API 24、26、28/29 及 31 演进的条件），由上游依赖废弃、存量设备占比（低于 0.5%）及系统原生关键能力（通知渠道、分区存储与硬件渲染特效）严格驱动。

---

## English Full Text

### 1. Context and Problem Statement
During initial architectural scoping, backward compatibility down to Android 4.4 (KitKat, API 19) was considered to maximize reach on vintage devices. However, modern Android Studio project initialization templates have abandoned API 19, defaulting to Android 6.0 (API 23) or higher.

Attempting to force API 19 compatibility introduces severe architectural tension with our core technology stack:
1. **Modern Declarative UI**: The project is built entirely on Jetpack Compose and Material 3, which natively require API 21+ at a minimum.
2. **Transport Security Collapse**: Over 95% of modern HTTPS backends, CDNs (e.g., Cloudflare), and image hosts require TLS 1.2+ and modern cipher suites. Android 4.4 lacks default TLS 1.2 support and has obsolete system CA certificates.
3. **Dalvik Runtime Bottlenecks**: API 19 predates the ART runtime, enforcing Dalvik's 64K method limit (requiring legacy MultiDex with significant cold-start ANR risks) and synchronous Stop-The-World garbage collection that halts UI frames during bitmap decoding.
4. **Engineering ROI Inversion**: Android 4.4 currently represents less than 0.05% of active global devices, meaning compatibility maintenance would consume disproportionate engineering resources while degrading build times and binary size for 99.9% of users.

### 2. Decision Drivers
* **Toolchain Compatibility**: Full synergy with Jetpack Compose, Kotlin 2.x, AGP 8+, and modern AndroidX libraries without fragile legacy shims.
* **Network & Security Integrity**: Native out-of-the-box support for TLS 1.2/1.3, valid modern root certificate trust chains, and HTTP/2 multiplexing.
* **UI Smoothness & Memory Resilience**: Hardware-accelerated rendering pipelines, independent RenderThread, and ART runtime garbage collection capable of handling high-resolution wallpaper decoding.
* **Unified Permission Architecture**: Modern runtime permission workflows (`ActivityResultContracts.RequestPermission`) introduced in API 23.

### 3. Considered Options

* **Option A: Legacy Support (minSdk 19 / KitKat)**
  * *Pros*: Theoretically runs on extremely old hardware and obsolete specialized devices.
  * *Cons*: Requires completely abandoning Jetpack Compose in favor of legacy XML Views; requires `androidx.multidex` with severe cold-start performance penalties; requires embedding Conscrypt or Google Play Services ProviderInstaller for HTTPS; massive maintenance burden.

* **Option B: Interim Support (minSdk 21 / Lollipop)**
  * *Pros*: Satisfies the absolute minimum bar for Jetpack Compose; native ART runtime.
  * *Cons*: Still requires dealing with pre-runtime permissions; inconsistent TLS 1.2 support across certain OEM ROMs; lacks Doze mode optimizations, battery restrictions, and fingerprint/biometric standards.

* **Option C: Modern Baseline (minSdk 23 / Marshmallow) [Chosen]**
  * *Pros*: Unifies runtime permission model; full native TLS 1.2+ support; introduces Doze mode and App Standby for background scheduling; coverage exceeds 99.5% of active devices; supported as default baseline in modern toolchains.
  * *Cons*: Excludes devices manufactured before 2015 (< 0.5% market share).

### 4. Decision Outcome
* **Chosen Option**: **Option C: minSdk 23 (Android 6.0 Marshmallow)**.
* **Rationale**: API 23 represents the inflection point where the platform stabilized runtime permissions, modern ART compilation, and modern networking baselines. It enables our modern Compose and Kotlin Coroutines/Flow stack to function without deadweight compatibility polyfills.

### 5. Consequences & Trade-offs
* **Positive Consequences**:
  - Zero legacy MultiDex overhead during APK build and application cold launch.
  - Native runtime permission flow simplifies storage and notification access logic.
  - Seamless integration with OkHttp 4+, Coil, and modern image-loading pipelines without SSL fallback layers.
  - Reduced test matrix and simplified CI/CD emulators.
* **Negative Consequences & Mitigations**:
  - Legacy devices (API < 23) cannot install the application.
  - *Mitigation*: The target audience for high-resolution wallpaper customization rarely operates devices with 1GB RAM or Android 4.x/5.x hardware; verified device distribution metrics demonstrate negligible impact.

### 6. Thresholds & Triggers: When to Revisit and Tighten minSdk
The project will not arbitrarily raise `minSdk` without clear engineering justification. We establish four explicit architectural triggers for future baseline bumps:

```
[Upstream Library Deprecation]
              OR
[Target OS Share < 0.5%]        ───>  [Re-evaluate & Bump minSdk]
              OR
[Architectural Capability Unlock]
```

#### Detailed Milestone Targets:
1. **Target: Bump to API 24 (Android 7.0 Nougat)**
   * *Trigger*: Upstream Jetpack libraries or Compose compiler drop API 23 support, or API 23 active share falls below 0.3%.
   * *Key Gain*: Native Java 8 Language APIs (Streams, `java.time`, Functional interfaces) without D8 Core Library Desugaring overhead; native multi-window/split-screen support; AOT/JIT hybrid compilation profiling.
2. **Target: Bump to API 26 (Android 8.0 Oreo)**
   * *Trigger*: Need for unified Notification Channels and background execution parity; WorkManager / AlarmManager battery optimizations.
   * *Key Gain*: Native Notification Channels eliminating pre-Oreo channel fallbacks; native Adaptive Icons; native XML font resource rendering; background service execution limits enforcement.
3. **Target: Bump to API 28/29 (Android 9.0 Pie / Android 10 Q)**
   * *Trigger*: Complete deprecation of legacy storage access mechanisms; requirement for native Dark Theme (`isSystemInDarkTheme`).
   * *Key Gain*: Total migration to Scoped Storage and modern Photo Picker; native BiometricPrompt API; hardware-accelerated HEIF / WebP decoding.
4. **Target: Bump to API 31 (Android 12 Snow Cone)**
   * *Trigger*: Wallpaper picker deep integration with Material You Dynamic Color (Monet engine) and system UI blurs.
   * *Key Gain*: Hardware `RenderEffect` for live wallpaper blur and glassmorphism; native SplashScreen API; exact alarm permission alignment.

---

## 中文全文

### 1. 背景与问题描述
在项目最早的技术规划中，曾设想兼容至 Android 4.4 (KitKat, API 19)，以覆盖远古设备与复古设备。然而，现代 Android Studio 官方模板与 Android Gradle Plugin (AGP) 早已淘汰 API 19，默认将最低门槛定在 Android 6.0 (API 23) 或更高。

在深度技术分析后发现，强行支持 API 19 将与本项目的基础技术选型产生根本性冲突：
1. **现代化声明式 UI 框架限制**：本项目全量采用 Jetpack Compose 与 Material 3 构建，其底层绘制与状态机制的官方硬性准入门槛即为 API 21+（Android 5.0）。
2. **现代传输安全体系断代**：当今 95% 以上的图片服务器、自建私有云（如 Immich）及主流 CDN（如 Cloudflare）均强制启用 TLS 1.2+ 并淘汰旧协议。Android 4.4 默认关闭 TLS 1.2 且内置系统 CA 根证书已大面积过期，发起网络请求将大面积触发 `SSLHandshakeException`。
3. **Dalvik 运行时与性能瓶颈**：API 19 处于 Dalvik 虚拟机时代，受限于 64K 方法数限制，必须引入 Legacy MultiDex。冷启动时在主线程解压 secondary dex 极易造成 ANR；同时简陋的 GC 机制在高分辨率壁纸解码和快速滚动时会引起灾难性卡顿。
4. **工程投入产出比（ROI）严重倒挂**：全球 Android 4.4 活跃设备份额已低于 0.05%。为不足万分之五的设备妥协架构，会导致 99.9% 的现代用户承担包体积膨胀、构建变慢与性能妥协的代价。

### 2. 决策驱动因素
* **工具链与现代化依赖生态**：无缝支持 Jetpack Compose、Kotlin 2.x、协程 Flow、现代 AndroidX 体系，杜绝兼容性黑魔法。
* **网络与传输安全保障**：原生支持 TLS 1.2/1.3、现代化受信任证书链与 HTTP/2 多路复用，支撑高并发网络图源加载。
* **图形渲染性能与内存韧性**：依托 ART 虚拟机、独立渲染线程（RenderThread）及 Baseline Profiles 基线配置文件，保障壁纸预览与列表滑动的丝滑度。
* **统一的系统规范与权限模型**：采用 Android 6.0+ 原生运行时权限架构（`ActivityResultContracts.RequestPermission`）。

### 3. 备选方案权衡

* **方案 A: 坚持向下兼容 (minSdk 19 / KitKat)**
  * *优势*: 理论上可在极老旧设备、工控平板或早期墨水屏上安装。
  * *劣势*: 必须彻底弃用 Jetpack Compose 回退到 XML View；引入 Legacy MultiDex 导致冷启动恶化；必须集成 Conscrypt 替换底层 SSL Provider；构建速度变慢，维护成本极高。

* **方案 B: 折中支持 (minSdk 21 / Lollipop)**
  * *优势*: 满足 Jetpack Compose 运行的底线；全面迈入 ART 虚拟机时代。
  * *劣势*: 缺少运行时权限模型（需处理安装时授权与不同厂商魔改）；部分 OEM 早期 5.0 ROM 的 TLS 1.2 支持极不稳定；缺少 Doze 低电耗省电机制与指纹认证规范。

* **方案 C: 现代基线 (minSdk 23 / Marshmallow) [选定]**
  * *优势*: 规范统一的运行时权限体系；原生成熟的 TLS 1.2+ 与网络协议支持；引入 Doze 与 App Standby 优化后台轮播耗电；覆盖全球 99.5% 以上活跃设备；与现代 IDE 模板及主流 AndroidX 库生命周期完全对齐。
  * *劣势*: 彻底放弃 2015 年前发布的设备（全球占比不足 0.5%）。

### 4. 决策结果与推导论据
* **选定方案**: **方案 C: minSdk 23 (Android 6.0 Marshmallow)**。
* **论证逻辑**: API 23 是 Android 历史上走向现代化的里程碑分水岭（权限模型、省电机制、网络与 ART 趋于稳定）。在 API 23 基线下，开发团队可专注于现代响应式架构，无需在包体积、构建时间与兼容补丁上做妥协。

### 5. 影响评估与技术代价
* **积极收益**:
  - 彻底摆脱 Legacy MultiDex，提升 APK 编译速度与应用冷启动性能。
  - 统一权限申请链路，无需编写 pre-M 权限适配分支。
  - 原生适配 OkHttp 4+ 与 Coil 图片加载库，无缝支持高并发图片网络管道。
  - 降低自动化测试矩阵与模拟器维护成本。
* **消极代价与缓解措施**:
  - API < 23 的旧机型无法安装使用。
  - *缓解措施*: 本项目定位为高质量、高分辨率壁纸选择与轮换工具，目标用户群体重度依赖高清屏幕与现代存储接口，旧版设备硬件亦无法承载高画质解码体验，该部分受众流失对产品实际影响几乎为零。

### 6. 演进阈值与推翻临界点（何时考虑进一步收紧最低 API Level）
本项目不会无根据地随意拔高 `minSdk`，而是设立了以下由**依赖驱动**、**设备分布驱动**和**关键系统特性驱动**的收紧评估准则：

#### 触发准则矩阵：
1. **上游核心依赖硬性淘汰**：当 Jetpack Compose、Lifecycle、Coroutines 或官方核心 AndroidX 库在新版本中强制宣布放弃某旧版本时，项目跟随升级。
2. **存量设备市场份额临界点**：当某 Android 版本的全球（或目标用户群）活跃份额低于 **0.3% ~ 0.5%** 时，纳入收紧候选。
3. **关键架构收益拐点**：

| 目标基线 | 关键系统特性驱动 (Architectural Drivers) | 预期收割的技术收益 |
| :--- | :--- | :--- |
| **API 24** (Android 7.0) | • 原生 Java 8 语言特性支持<br>• 分屏/多窗口模式原生支持<br>• Direct Boot 模式 | • 彻底移除 D8 `coreLibraryDesugaring` 脱糖依赖，减小 APK 体积并加快编译<br>• 消除 Java 8 Stream/Time 脱糖的运行时桥接开销 |
| **API 26** (Android 8.0) | • 必须的通知渠道（Notification Channels）<br>• 自适应图标（Adaptive Icons）<br>• 后台执行限制（Background Limits）统一 | • 移除预设通知渠道的 `Build.VERSION.SDK_INT >= 26` 分支判断<br>• 原生全面采用 `java.time.*` 无任何降级依赖<br>• 统一后台前台服务（Foreground Service）行为模型 |
| **API 28/29** (Android 9/10) | • 分区存储（Scoped Storage）与 Photo Picker<br>• 原生暗黑模式（System Dark Theme）<br>• 生物识别库统一（BiometricPrompt） | • 彻底删除所有传统文件路径读写兼容层与全盘存储权限申请<br>• 原生感知系统深色/浅色主题，无需自定义适配层<br>• 原生硬件加速 HEIF 图片格式支持，加速壁纸解码 |
| **API 31** (Android 12) | • Material You 动态色彩（Dynamic Color Monet）<br>• 硬件级高斯模糊特效（`RenderEffect`）<br>• 系统原生启动页（SplashScreen API） | • 实现纯原生 Monet 主题壁纸色彩提取与全应用动态换肤<br>• 在壁纸预览界面使用硬件级实时高斯模糊与毛玻璃质感，无需 RenderScript 替代库<br>• 对齐精确闹钟与 WorkManager 现代唤醒限制 |
