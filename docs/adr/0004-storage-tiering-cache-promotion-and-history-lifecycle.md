# [ADR-0004] Storage Tiering, Cache Promotion, and Resilient History Lifecycle / 存储分层、缓存转正与容错历史生命周期架构

> **Metadata / 元数据**
> - **Status / 状态**: Accepted / 已采纳
> - **Date / 日期**: 2026-09-26
> - **Deciders / 决策者**: @Foo, @Antigravity
> - **Technical Story / 关联背景**: [PLANS.md (Phase 2 & Phase 4)](../../PLANS.md#phase-2-拓展网络源immich--通用-http-api), Conversations [`168188a3`](conversation://168188a3-c965-40dc-a454-c4a8c5dd4ffa), [`b4a61361`](conversation://b4a61361-fd4a-45e2-a8f6-e1ef6116aa01), [`f0509754`](conversation://f0509754-f951-4ca6-91cb-7428042dea49)

---

## Executive Summary / 双语摘要

### English Summary
To prevent permanent user data loss from automated cache eviction and system-level disk purges, we establish a tiered storage architecture that decouples ephemeral rotation caches from user favorites and operational history. We strictly reject time-based TTL expiration for favorites; isolate ephemeral downloads in `cacheDir/wallpapers/` under a pure capacity-bounded LRU pool (25 items / 50MB) for graceful offline degradation; introduce an explicit "Cache Promotion" mechanism that clones favorited network images into `filesDir/favorites/`; integrate Android's `ManageSpaceActivity` to protect SAF permissions from accidental "Clear All Data" resets; and decouple history records from physical file lifecycles with broken-item indicators, bulk cleaning, and on-demand redownload capabilities.

### 中文摘要
为防止自动缓存淘汰和系统磁盘清理导致用户收藏与历史数据丢失，本项目确立了将“临时轮播缓存”、“用户持久收藏”与“足迹历史记录”彻底解耦的分层存储架构。我们坚决禁止对收藏应用任何基于时间（TTL）的过期策略；将临时网络图源隔离于 `cacheDir/wallpapers/`，采用纯容量/数量驱动的 LRU 缓存池（25 张 / 50MB）实现断网优雅降级；建立“缓存转正（Cache Promotion）”机制，在用户点击收藏瞬间将网络图固化至私有持久目录 `filesDir/favorites/`；接入 Android 官方 `ManageSpaceActivity`，避免用户因存储焦虑点击系统“清除全部数据”导致 SAF 授权与配置丢失；并在历史记录中实现与物理文件解耦的容错治理（原图失效标记、一键清理失效记录与按需重新下载）。

---

## English Full Text

### 1. Context and Problem Statement
In an automated wallpaper switcher with diverse sources (Local SAF folders, System MediaStore, Immich, and custom HTTP APIs), storage lifecycle management presents three critical conflicts:
1. **User Assets vs. Temporary Cache Conflict**: Treating favorited wallpapers and temporary rotation downloads under a single cache directory causes system low-storage cleanup or third-party cleaning tools to silently wipe away favorited pictures, resulting in irreversible user data loss.
2. **Offline Resilience vs. Time-based Expiration**: If cached network wallpapers enforce a time-based Time-To-Live (TTL) (e.g., purge after 7 days), users in airplane mode, traveling, or with prolonged weak connectivity suffer complete rotation failure once the cache expires.
3. **Storage Anxiety & Accidental Authorization Revocation**: When users inspect application storage in Android system settings, seeing hundreds of megabytes of cached images often prompts them to tap "Clear All Data". This catastrophically revokes persistable SAF folder URI permissions, deletes preferences, and halts background workers.
4. **History Invalidation After Cache Pruning**: Purging uncollected temporary cache files leaves dangling records in the user's wallpaper history, causing broken image thumbnails and confusing error states.

### 2. Decision Drivers
* **Absolute Asset Safety (Zero Data Loss)**: User favorites are personal assets and must never be automatically purged by any background job or OS memory-reclamation routine.
* **Offline-First Resilience**: Enable seamless wallpaper rotation without mobile data consumption or during flight mode by recycling existing local cache files.
* **Storage Transparency & User Autonomy**: Give users granular control over temporary cache purges without endangering application configurations or permissions.
* **Decoupled Historical Integrity**: Ensure browsing historical wallpaper logs remains visually clean and functional even after underlying temporary files are removed.

### 3. Considered Options

* **Option A: Unified Cache Directory with Database Flags**
  * *Pros*: Simplest file organization; all images reside in `cacheDir`.
  * *Cons*: Catastrophic risk. Android OS automatically clears `cacheDir` when internal storage is low, completely ignoring database flags and permanently erasing user favorites.
* **Option B: Store Everything in Permanent Internal Storage (`filesDir`)**
  * *Pros*: Zero risk of OS auto-purging.
  * *Cons*: Disk bloat. Continuous network cycling without strict quotas quickly consumes gigabytes of internal storage on 16GB/32GB legacy devices.
* **Option C: Physically Tiered Storage with Cache Promotion & ManageSpace Integration (Chosen)**
  * *Pros*: Combines the safety of `filesDir` for favorites, the volatility of `cacheDir` for rolling LRU, system-level space management, and resilient history management.
  * *Cons*: Requires coordinating file copy operations, database state transitions, and distinct UI dialogues.

### 4. Decision Outcome
* **Chosen Option**: **Option C (Physically Tiered Storage with Cache Promotion & ManageSpace Integration)**.

#### Architectural Pillars:

1. **Physical Directory Isolation & Zero-TTL Favorites**:
   * **Ephemeral Cache (`context.cacheDir/wallpapers/`)**: Stores temporary network downloads. Governed strictly by a count-and-size bounded LRU policy (`maxCount = 25`, `maxSizeBytes = 50MB`). **Explicitly rejects time-based TTL** to ensure offline usability.
   * **Permanent Favorites (`context.filesDir/favorites/`)**: Stores favorited assets. **Strictly exempt from all automated background cleanups**.

2. **Cache Promotion Mechanism**:
   * When a user favorites a network wallpaper (from Immich or HTTP API), the background pipeline immediately clones/promotes the image from `cacheDir/wallpapers/` to `filesDir/favorites/{id}.jpg`.
   * For local SAF or MediaStore images, the app persists only the Content URI reference and a lightweight thumbnail (tens of KB) without redundantly duplicating high-resolution files.

3. **System-Level `ManageSpaceActivity` Integration**:
   * Registered `android:manageSpaceActivity` in `AndroidManifest.xml` pointing to `ManageSpaceActivity`.
   * Replaces the destructive system "Clear All Data" action with a dedicated UI offering two distinct tiers:
     - **Tier 1 (Safe)**: Clear temporary wallpaper cache and Coil image caches while preserving favorites, SAF permissions, and schedule configurations.
     - **Tier 2 (Destructive)**: Total application reset with explicit warnings.

4. **Resilient History Lifecycle & Self-Healing**:
   * Prior to cache clearing, the UI displays a confirmation dialogue stating: *"X non-favorited network wallpapers will become inaccessible; Y favorited wallpapers remain permanently protected."*
   * Includes an optional checkbox: *"Remove invalid history records simultaneously."*
   * History Tab provides a one-click *"Clean Invalid Records"* action (`!isUriAccessible(uri) && !isFavorite`).
   * Provides an *"Attempt Redownload"* action on historical items to re-fetch original images on-demand.

### 5. Consequences & Trade-offs
* **Positive Consequences**:
  * Eliminates user panic over missing favorites while keeping disk footprint tightly capped.
  * Prevents SAF permission loss caused by system-level storage clearing.
  * History log gracefully handles missing files with clear badges and re-download capabilities.
* **Negative Consequences & Mitigations**:
  * *Storage Redundancy on Promotion*: Copying an image upon favoriting temporarily duplicates file bytes until the LRU evicts the ephemeral copy $\rightarrow$ *Mitigation*: The LRU pool naturally purges the redundant temporary file as new wallpapers arrive.

### 6. Thresholds & Triggers (When to Revisit)
* This architecture should only be reconsidered if the Android OS introduces an atomic, OS-managed storage vault that guarantees cross-device backup and immune persistence within standard cache quotas.

---

## 中文全文

### 1. 背景与问题描述
在支持多图源（本地 SAF 文件夹、系统 MediaStore、自建 Immich 及自定义 HTTP API）的自动壁纸切换应用中，存储生命周期管理面临三大核心矛盾：
1. **用户永久资产与临时缓存的冲突**：若将“普通轮换缓存”与“用户收藏壁纸”混存于同一缓存目录，在系统低存储自动清理或用户使用第三方清理软件时，用户的收藏会被物理抹除，构成不可逆的严重数据丢失。
2. **离线容灾韧性与时间过期策略的冲突**：若为临时网络缓存设置时间过期（TTL，如 7 天自动销毁），当用户处于飞行模式、漫游或长期弱网环境下，缓存到期被删会导致后台 Worker 唤醒时无图可用，壁纸直接停摆。
3. **存储焦虑与权限误清空**：当用户在 Android 系统设置中看到本应用占用几十上百兆空间时，极易随手点击“清除全部数据”，导致 SAF 目录授权、配置与定时任务瞬间被系统抹除。
4. **缓存淘汰与历史记录断裂**：清理临时缓存后，历史画廊中的记录失去底层物理文件支持，造成大面积缩略图破损和失效报错。

### 2. 决策驱动因素
* **绝对资产安全（零数据丢失）**：用户收藏属于核心数字资产，绝对禁止被任何后台任务或系统低存储机制自动清理。
* **离线优先与低功耗容灾**：在无网络、弱网或飞行模式下，能够零流量复用本地缓存持续轮播。
* **存储透明化与自主掌控权**：向用户清晰呈现缓存与收藏占用，提供颗粒化的清理通道，避免破坏核心配置。
* **足迹历史与底层存储弹性解耦**：物理文件的清理不应引发历史界面的灾难性崩溃，需具备失效标记、批量治理与重载自愈能力。

### 3. 备选方案权衡

* **方案 A: 统一缓存目录 + 数据库字段标记**
  * *优势*: 文件结构最简单，所有图片均放置在 `context.cacheDir`。
  * *劣势*: 存在致命风险。Android 系统在磁盘告急时拥有自动清理 `cacheDir` 的系统特权，完全无视应用内部数据库标记，导致收藏被物理抹除。
* **方案 B: 全量写入私有持久目录 (`context.filesDir`)**
  * *优势*: 规避系统静默清理风险。
  * *劣势*: 磁盘膨胀。长期联网轮换缺乏配额控制，极易在 16GB/32GB 老旧设备上挤爆内部存储。
* **方案 C: 物理分层存储 + 缓存转正机制 + ManageSpace 深度集成（选定方案）**
  * *优势*: 收藏享绝对安全保障，轮播缓存受纯容量 LRU 硬顶约束，系统设置提供安全清理入口，历史具备弹性容错。
  * *劣势*: 需要维护文件复制、数据库状态流转与更精细的 UI 交互。

### 4. 决策结果与推导论据
* **选定方案**: **方案 C (物理分层存储 + 缓存转正机制 + ManageSpace 深度集成)**。

#### 核心架构落地支柱：

1. **存储物理目录隔离与收藏零 TTL 原则**：
   * **临时网络缓存池 (`context.cacheDir/wallpapers/`)**：受纯容量与数量上限约束（`maxCount = 25`, `maxSizeBytes = 50MB`）。**明确严禁引入时间 TTL**，避免飞行模式和断网时缓存被误删。
   * **永久收藏目录 (`context.filesDir/favorites/`)**：存放用户收藏资产，**绝对豁免于任何自动化后台清理任务**。

2. **缓存转正机制 (Cache Promotion)**：
   * 用户在控制台或画廊点击收藏网络壁纸（Immich / HTTP API）的瞬间，系统立即在后台将其从 `cacheDir/wallpapers/` 复制固化至 `filesDir/favorites/{id}.jpg`，完成从“易失缓存”到“永久资产”的跃迁。
   * 本地 SAF 与 MediaStore 图源仅在数据库持久化 Content URI 引用和几十 KB 的轻量缩略图，避免重复拷贝原图造成存储浪费。

3. **系统级 `ManageSpaceActivity` 接入与防御性治理**：
   * 在 `AndroidManifest.xml` 中配置 `android:manageSpaceActivity`，将系统设置中的“清除数据”强行拦截并导向专有的 [`ManageSpaceActivity`](file:///Users/Foo/repo/foo-wallpaper-picker-next/app/src/main/java/foo/barz/wallpaperpicker/ui/ManageSpaceActivity.kt)。
   * 提供分级清理：
     - **安全清理**：一键清空临时壁纸缓存与 Coil 图像缓存，100% 保全收藏资产、SAF 文件夹授权与定时调度；
     - **彻底重置**：提供二次强警告确认的全局重置通道。

4. **历史记录生命周期解耦与弹性自愈**：
   * 清理缓存前弹窗明确警示：*“未收藏的网络壁纸将会失效，X 张已收藏壁纸受持久保护不受影响”*；
   * 弹窗内嵌复选框：*“同时移除失效记录”*；
   * 历史页支持 *“一键清理失效记录”*（仅清理 `!isUriAccessible(uri) && !isFavorite` 项）；
   * 历史卡片支持针对网络源的 *“重新尝试下载”* 操作，实现业务历史与物理文件的按需重连。

### 5. 影响评估与技术代价
* **积极收益**:
  * 彻底消除了用户收藏在低空间或清理缓存时被误杀的严重隐患。
  * 阻断了用户在系统设置盲目清空数据导致 SAF 权限丢失的痛点。
  * 实现了高韧性的离线无感轮播与断网降级。
* **消极代价与缓解措施**:
  * *存储冗余*: 收藏瞬间产生的原图拷贝会短暂导致双份占用 $\rightarrow$ *缓解措施*: 临时目录受 LRU 机制约束，新壁纸进池后会自动挤出并物理删除旧的临时文件。

### 6. 演进阈值与推翻临界点
* 仅当 Android 官方未来推出原生受系统保护且免疫自动回收的应用内部快照存储规范时，方可重新评估本物理隔离架构。
