# [ADR-0002] Reject Direct Integration with Google Photos Cloud API / 明确放弃接入 Google 相册云端 API

> **Metadata / 元数据**
> - **Status / 状态**: Rejected / 已否决
> - **Date / 日期**: 2026-09-26
> - **Deciders / 决策者**: @Foo, @Antigravity
> - **Technical Story / 关联背景**: [PLANS.md (Section 5.1)](../../PLANS.md#51-google-photos-云端直连源-google-相册)

---

## Executive Summary / 双语摘要

### English Summary
We explicitly reject direct cloud integration with the official Google Photos API. Following Google's 2025 deprecation of the legacy Library API and transition to the restrictive Picker API, third-party apps face fatal constraints for automated background wallpaper switching: mandatory user-interactive picking, a rigid 7-day session expiry, short-lived 60-minute media URLs, and prohibitive CASA Tier 2 commercial security audit costs. We instead establish a local-first and self-hosted strategy: querying locally synced photos via system `MediaStore` and supporting open-source self-hosted solutions (such as Immich and Nextcloud).

### 中文摘要
我们明确否决在应用内直接接入 Google 相册（Google Photos）官方云端 API。鉴于 Google 于 2025 年彻底下线旧版只读 Library API 并全面转向严格受限的 Picker API，第三方应用在实现无人值守的后台自动壁纸切换时面临多重致命瓶颈：必须交互式点选、授权会话仅 7 天强制过期、媒体直链仅 60 分钟有效，以及个人与开源项目无法承担的 CASA Tier 2 商业安全合规审计成本。本项目确立“本地优先与自建私有云”架构路线：通过系统 `MediaStore` 零成本检索本地已同步的照片，并优先支持 Immich 与 Nextcloud 等开源私有云方案。

---

## English Full Text

### 1. Context and Problem Statement
Integrating Google Photos as a dynamic cloud wallpaper source is one of the most frequently requested features in Android wallpaper utilities. Users naturally desire automated wallpaper cycling directly from their cloud albums without manually downloading images.

However, supplying dynamic wallpapers requires unattended, persistent background execution (`WallpaperWorker` / `ScheduleRuleEngine`). As Google overhauled its cloud APIs and security policies between 2024 and 2025, evaluating whether to build native Google Photos cloud integration became a crucial architectural checkpoint.

### 2. Decision Drivers
* **Unattended Background Automation**: The core wallpaper switching pipeline must execute reliably in the background without periodically prompting the user for manual re-authentication or re-selection.
* **Compatibility with De-Googled ROMs and Legacy Devices**: Adhere to the project's baseline requirement to support Android 6.0 (API 23) up to modern Android (API 36+), including AOSP-based custom ROMs without proprietary Google Mobile Services (GMS).
* **Sustainable Compliance & Financial Costs**: Avoid prohibitive annual commercial security audit expenses (e.g., CASA Tier 2) associated with Google Restricted Scopes.
* **Architectural Simplicity**: Keep the binary footprint small, avoiding heavy cloud SDK dependencies (Google Sign-In, Play Services Auth, Google API Client).

### 3. Considered Options

* **Option A: Official Google Photos Picker API (Post-2025 Standard)**
  * *Pros*: Fully compliant with Google's latest security guidelines; uses standard OAuth 2.0.
  * *Cons*:
    1. **Interactive Only (No Album Subscription)**: The Picker API is strictly designed for on-demand, interactive user selections (e.g., attaching a photo to a tweet). It prohibits background querying, subscribing to entire albums, or polling newly added media.
    2. **7-Day Session Expiration (Fatal Constraint)**: Authorized picking sessions expire after 7 days and cannot be refreshed silently in the background. The automated rotation would completely stall every 7 days, forcing intrusive re-authentication prompts.
    3. **60-Minute Media URL Lifespan**: Media `baseUrl` endpoints expire in 60 minutes, making durable offline scheduling or index caching impossible.
    4. **CASA Tier 2 Security Audit**: Requires external commercial security audits costing thousands of dollars annually, which is unviable for independent open-source projects.

* **Option B: Web Scraping Public Shared Albums (`photos.app.goo.gl`)**
  * *Pros*: Bypasses OAuth, GMS, and CASA audit requirements; users can share an unlisted album link.
  * *Cons*:
    1. **Extreme Fragility**: Relies on reverse-engineering obfuscated HTML, serialized JSON, and internal RPC protocols that Google frequently mutates without notice.
    2. **Rate Limiting & IP Blocking**: Aggressive scraping triggers HTTP 429 (Too Many Requests), CAPTCHAs, or silent empty responses.
    3. **Privacy Exposure**: Requires users to make personal photo albums accessible via public web links.

* **Option C: Explicit Rejection of Cloud API in Favor of Local MediaStore & Self-Hosted Alternatives (Chosen)**
  * *Pros*:
    1. **100% Reliable & Zero Maintenance**: Relies on the standard Android `MediaStore` provider. If users synchronize their Google Photos to the device via Google Drive or the Google Photos app (Device Folders / Backup & Sync), our app indexes them instantly with zero network battery drain and zero API limitations.
    2. **Fosters Open-Source Self-Hosting**: Prioritizes open protocols (Immich, Nextcloud Memories, WebDAV), which provide perpetual API keys, predictable REST endpoints, and zero arbitrary vendor restrictions.
    3. **Maintains GMS-Free Independence**: Preserves full operational capability on LineageOS, GrapheneOS, /e/OS, and Android 6.0 devices.
  * *Cons*: Users who keep photos exclusively in Google Photos cloud without local device caching cannot cycle them directly inside this app.

### 4. Decision Outcome
* **Chosen Option**: **Option C (Explicit Rejection)**.
* **Rationale**:
  Google's API ecosystem has made unattended background consumption of cloud photos structurally impossible by design. Pursuing Option A would result in an unusable product (stalling every 7 days), while Option B creates unmaintainable brittle scrapers. Rejecting official cloud integration preserves our project's integrity, low battery footprint, and focus on robust local and self-hosted sources.

### 5. Consequences & Trade-offs
* **Positive Consequences**:
  * Eliminates GMS dependency, reducing APK size and ensuring compatibility across all Android devices (API 23 to 36+).
  * Avoids ongoing CASA audit fees and Google Cloud Platform developer project maintenance.
  * Provides a canonical, unshakeable answer to community feature requests and prevents wasted investigation cycles.
* **Negative Consequences & Mitigations**:
  * *Negative*: Some users expecting one-click cloud integration will be disappointed.
  * *Mitigation*: Clearly document in the UI and documentation that users can enable local device caching in Google Photos, allowing our `MediaStore` camera/album source to seamlessly access the pictures.

### 6. Thresholds & Triggers (When to Revisit)
We will reconsider this decision **only if all** of the following conditions are met:
1. Google officially introduces a persistent, unattended background synchronization API for personal cloud albums with long-lived refresh tokens.
2. The authorization model removes the 7-day session hard cap and permits background querying without active UI interaction.
3. Google provides a verifiable, zero-cost compliance path for open-source / non-commercial developers that exempts them from commercial CASA Tier 2 audits.

Until then, any PR or proposal introducing direct Google Photos cloud integration will be closed with reference to this ADR.

---

## 中文全文

### 1. 背景与问题描述
在各类 Android 壁纸切换工具中，支持将 Google 相册（Google Photos）作为动态云端图源是呼声最高的用户诉求之一。用户普遍期望能够直接绑定自己的云端相册，无需手动下载图片即可实现自动轮播。

然而，壁纸切换器的核心能力依赖后台无人值守的自动化调度（`WallpaperWorker` / `ScheduleRuleEngine`）。随着 Google 在 2024 至 2025 年间对云端相册 API 及隐私安全政策进行彻底重构，深度评估是否原生接入 Google Photos 成为项目面临的重大架构选型。

### 2. 决策驱动因素
* **后台无人值守自动化**：壁纸切换必须在后台长期静默稳定执行，严禁频繁弹出授权弹窗打扰用户。
* **脱离 GMS 依赖与老设备兼容**：坚守项目最低支持 Android 6.0（API 23）并无缝兼容去 Google 化（De-Googled）定制 ROM（如 LineageOS、GrapheneOS、/e/OS 等）的核心底线。
* **可持续的合规与经济成本**：杜绝因申请 Google 受限权限（Restricted Scopes）而导致的昂贵第三方商业安全审计（CASA Tier 2）支出。
* **架构轻量性**：避免引入庞大的 Google Play Services Auth 及 Google API Client 依赖库，保持极小安装包体积与低内存占用。

### 3. 备选方案权衡

* **方案 A: 接入官方新版 Google Photos Picker API（2025 官方主推方案）**
  * *优势*: 完全符合 Google 最新安全规范，采用标准 OAuth 2.0 流程。
  * *劣势*:
    1. **仅限交互式点选（无法整本订阅）**：Picker API 专为一次性交互分享（如发推、打印选图）设计，完全不支持后台静默拉取、不支持整本相册持续监听、不支持增量轮播。
    2. **7 天授权会话强制过期（致命死穴）**：用户授权建立的 Session 寿命严格限制为 7 天且**无法在后台静默刷新**。这意味着每过 7 天，自动换壁纸流程便会彻底停摆并报错，必须强制打断用户重新打开 App 点选授权。
    3. **媒体直链仅 60 分钟有效**：返回的 `baseUrl` 寿命仅 1 小时，无法提前持久化建立轮播队列。
    4. **CASA Tier 2 商业安全审计**：即便试图绕过限制申请更高权限，个人开发者也必须面临每年数千美元的第三方专业安全合规审计费用，开源项目完全无法承受。

* **方案 B: 网页逆向解析公开分享相册（`photos.app.goo.gl`）**
  * *优势*: 无需配置 OAuth、无需 GMS 依赖与安全审计，用户粘贴相册公开分享链接即可使用。
  * *劣势*:
    1. **极度脆弱不可靠**：完全依赖对 Google 前端混淆 HTML、内联 JSON 及私有 RPC 协议的逆向解析，Google 稍作前端重构或混淆变更即会导致解析全面崩溃。
    2. **反爬与限流封禁**：频繁后台轮询极易触发 Google 的 HTTP 429（请求过多）拦截、人机验证（CAPTCHA）或静默返回空列表。
    3. **隐私外溢风险**：强制要求用户将私密个人生活照片生成公开外链，违背隐私安全准则。

* **方案 C: 明确放弃云端 API，全面转向本地系统相册索引与开源私有云（选定方案）**
  * *优势*:
    1. **100% 稳定可靠且零功耗**：直接依托系统原生 `MediaStore` 游标。用户通过 Google Photos 官方客户端自带的离线缓存/设备相册功能同步照片后，本应用发起本地 SQL 查询瞬间完成检索，网络零流量、电池零额外消耗。
    2. **扶持开放开源生态**：重点支持 Immich、Nextcloud Memories、PhotoPrism 等开源自建方案，拥有永久生效的 API Key、规范稳定的 REST 接口和完全自主的数据掌控权。
    3. **保持纯净无 GMS 约束**：在无谷歌框架的纯净 AOSP 设备及 Android 6.0 老设备上均能稳定运作。
  * *劣势*: 纯粹将照片存放在 Google 云端且未开启本地同步的用户，无法在应用内直接跨云读取。

### 4. 决策结果与推导论据
* **选定方案**: **方案 C (明确否决接入)**。
* **论据推导**:
  Google 官方在 API 设计上已经彻底封死了“第三方应用后台长期静默读取云端相册”的可能性。方案 A 会导致应用每 7 天停摆报错，形成极差的用户体验；方案 B 则会陷入无穷无尽的反爬修补泥潭。明确否决官方云端接入，能够保护项目免受不可行的平台限制牵制，集中精力打磨本地极速检索与优秀的开源私有云体验。

### 5. 影响评估与技术代价
* **积极收益**:
  * 摆脱 GMS 束缚，显著缩减 APK 体积，完美实现跨代与全平台兼容。
  * 规避每年高昂的 CASA 商业安全合规审计与 Google Cloud 开发者项目维护开销。
  * 形成权威决策档案，为 GitHub Issue / PR 中的重复功能请求提供最终裁决依据。
* **消极代价与缓解措施**:
  * *消极代价*: 部分习惯依赖云端直接拉取的用户可能会感到失望。
  * *缓解措施*: 在应用文档与帮助说明中明确引导：指导用户在官方 Google Photos 中开启设备相册同步，本应用即可通过“本地相册 (MediaStore)”源无缝无感地享用这些图片。

### 6. 演进阈值与推翻临界点
仅当以下**所有条件同时满足**时，方可重新评估接入 Google 相册云端 API 的可能性：
1. Google 官方重新开放支持长效 Refresh Token、允许第三方应用在后台静默同步个人相册的官方 API。
2. 授权体系废除 7 天 Session 强制失效机制，允许在无前台交互干预的情况下持续运作。
3. Google 为开源及非商业独立开发者提供免费且免除 CASA Tier 2 商业安全审计的合规通道。

在此之前，任何请求或试图引入 Google Photos 官方云端直连的 Issue 和 PR 均将直接引用本 ADR 予以关闭。
