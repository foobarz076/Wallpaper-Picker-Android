# Wallpaper Changer 项目规划与路线图 (PLANS.md)

## 1. 项目愿景与需求背景

### 1.1 项目定位
一款轻量、极低功耗、支持多来源（本地相册、自建 Immich 服务、HTTP API 等）的 Android 随机/自动壁纸切换工具。

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
- ❌ **双击桌面切壁纸、手势联动**。
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

### Phase 4: 体验与进阶特性 (UX & Advanced Features)
- **UI 全面重构：从单页演进至多 Tab 架构 (Navigation Bar)**：
  - **重构动因**：单页承载大量图源配置（Local/Immich/HTTP）与历史图片流时将面临严重滚动冲突与信息过载，需将高频操作与低频设置彻底解耦。
  - **Tab 1: 桌面 / 控制台 (Dashboard)**：
    - 当前壁纸大图卡片预览与状态信息（当前来源、上次更换、下次倒计时）。
    - 核心高频操作：一键「立即更换」、快捷「设为收藏」。
  - **Tab 2: 历史与画廊 (Gallery / History)**：
    - 采用双列/三列瀑布流展示历史已用壁纸与收藏列表。
    - 支持大图全屏预览、一键导出到系统相册 (`Pictures`)、删除单张缓存。
  - **Tab 3: 图源与设置 (Settings & Sources)**：
    - 多图源集中管理与切换（本地文件夹、Immich 凭据、HTTP API 自定义与预设）。
    - 调度周期偏好、Wi-Fi / 流量约束。
    - **可配置缓存容量档位**：提供「小 (约 20MB / 10 张)」、「标准 (约 50MB / 25 张，默认)」、「大 (约 100MB / 50 张)」离散档位选择，兼顾极致省空间与离线丰富度，并提供一键清理缓存。
    - 屏幕目标（桌面/锁屏）与壁纸随桌面滚动模式。
- **无重复轮播 (Fair Shuffle)**：记录最近 $N$ 次已用壁纸，避免短期内频繁看到重复图片。
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
