# Wallpaper Picker / 壁纸选择器

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Android Min SDK](https://img.shields.io/badge/Android-6.0%2B%20(API%2023)-brightgreen.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-API%2036-blue.svg)](https://developer.android.com)

[English](#english) | [中文说明](#中文说明)

---

## English

A lightweight, battery-efficient, multi-source automatic wallpaper changer for Android. Designed to run smoothly from legacy Android 6.0 devices up to the latest Android 16 without out-of-memory crashes or battery drain.

### Features

- 📁 **Multiple Sources**:
  - **Local Folders**: Persistent Storage Access Framework (SAF) folder picker.
  - **System MediaStore**: Zero-permission fast access to your camera gallery (`DCIM/Camera`).
  - **Immich**: Direct integration with self-hosted Immich instances.
  - **Custom HTTP API**: Fetch images from custom endpoints with JSONPath response mapping.
  - **Favorites**: Offline-permanent collection with absolute exemption from cache cleanup.
- ⏰ **Flexible Scheduling**:
  - Background periodic rotation via AndroidX `WorkManager`.
  - Advanced multi-rule scheduler engine (exact wall-clock anchors, quiet hours, per-rule source override).
- 🎨 **Smart Display & Composition**:
  - Independent per-image memory for crop focal points, launcher scrolling, and horizontal flip.
  - Screen-aware downsampling to prevent Out-Of-Memory (OOM) errors on 1GB RAM devices.
- 🔒 **Backup & Privacy**:
  - Fully local & privacy-focused (zero telemetry/analytics).
  - Encrypted backup and restore supporting password-based AES-256-GCM and OpenKeychain (OpenPGP).
- ⚡ **Extensive Android Compatibility**:
  - Single-codebase architecture supporting Android 6.0 (API 23) to Android 16 (API 36).
  - Modern TLS 1.3 / ISRG Root X1 support on legacy Android via Conscrypt.
  - Quick Settings Tiles & App Shortcuts for instant switching.

### Supported Sources

| Source | Description | Configuration Required |
| :--- | :--- | :--- |
| **Local Folder** | Scans images via SAF tree URI | Directory selection |
| **System Gallery** | Fast MediaStore query on device | None |
| **Immich** | Fetches random assets from self-hosted Immich | Server URL, API Key, Album (optional) |
| **HTTP API** | Fetches direct images or JSON endpoints | Endpoint URL, headers, JSONPath (optional) |
| **Favorites** | Locally pinned images (never evicted) | Star wallpapers in app |

### Quick Start

1. Install the APK (from GitHub Releases or via Obtainium).
2. Launch the app and select your preferred wallpaper source.
3. Configure rotation interval or schedule rules in **Settings**.
4. Tap **Change Now** to trigger an immediate update.
5. Whitelist the app from battery optimizations when prompted to ensure reliable background execution.

### Building from Source

Prerequisites: Android SDK (API 36) and JDK 21.

```bash
# Clone the repository
git clone https://github.com/foobarz076/Wallpaper-Picker-Android.git
cd Wallpaper-Picker-Android

# Build debug APK (On macOS Darwin)
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home ./gradlew assembleDebug

# Build debug APK (On Linux / Windows)
./gradlew assembleDebug
```

### Documentation & Architecture

- **Architecture Decision Records (ADRs)**: Bilingual records located at [`docs/adr/`](docs/adr/).
- **Detailed Documentation**:
  - English: [`docs/en/`](docs/en/)
  - 中文: [`docs/zh/`](docs/zh/)
- **Roadmap & Planning**: [`PLANS.md`](PLANS.md)

### License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**. See [LICENSE](LICENSE) for details.

---

## 中文说明

一款轻量、极低功耗、支持多图源的 Android 自动壁纸轮换工具。专为跨代兼容设计，从 Android 6.0 老旧设备到最新的 Android 16 均可稳定静默运行，杜绝内存溢出（OOM）与异常耗电。

### 核心特性

- 📁 **多元化图源**：
  - **本地文件夹**：基于系统存储访问框架（SAF）持久授权读取。
  - **系统相册**：基于 `MediaStore` 原生索引，零权限秒级检索相机相册（`DCIM/Camera`）。
  - **Immich 私有云**：无缝对接自建 Immich 相册实例。
  - **通用 HTTP API**：支持任意网络直链或通过 JSONPath 动态解析接口响应。
  - **我的收藏**：离线持久化资产，享受缓存清理绝对豁免权。
- ⏰ **灵活调度机制**：
  - 基于 Jetpack `WorkManager` 的保活周期轮换。
  - 高级独立规则日程表引擎（支持每日定点打卡、夜间免打扰静默、不同时段绑定独立图源）。
- 🎨 **智能构图与显示**：
  - 单图个性化偏好记忆：独立保存每张壁纸的构图焦点（Crop Center）、桌面平移与水平翻转。
  - 屏幕物理分辨率安全下采样解码，1GB 内存老设备亦不闪退。
- 🔒 **备份与隐私安全**：
  - 纯本地运行，不收集任何用户数据与分析日志。
  - 支持配置与收藏安全备份，提供密码型 AES-256-GCM 与 OpenKeychain（OpenPGP）双加密通道。
- ⚡ **跨代系统兼容**：
  - 单工程单分支覆盖 Android 6.0 (API 23) 至 Android 16 (API 36)。
  - 集成 Conscrypt 现代加密套件，解决老设备证书过期与 TLS 1.3 缺失问题。
  - 提供快捷设置磁贴（Quick Settings Tile）与桌面微件。

### 支持图源一览

| 图源类型 | 说明 | 所需配置 |
| :--- | :--- | :--- |
| **本地文件夹** | 通过 SAF 读取本地选定目录 | 选择壁纸文件夹 |
| **系统相册** | 检索本地系统媒体库 | 无需配置 |
| **Immich** | 随机抽取自建私有云相册资产 | 服务器地址、API Key、相册（可选） |
| **HTTP API** | 请求图片直链或抽取 JSON 接口 | 请求地址、请求头、JSONPath 提取规则（可选） |
| **我的收藏** | 已固化到本地的收藏壁纸 | 应用内收藏即可 |

### 快速上手

1. 安装 APK（可通过 GitHub Releases 或 Obtainium 托管更新）。
2. 打开应用，在「图源与设置」中添加并勾选激活源。
3. 设置轮播间隔或配置日程规则。
4. 点击「立即更换」测试壁纸应用。
5. 按照弹窗指引开启电池优化白名单，保障系统后台定时唤醒。

### 源码构建

构建依赖：Android SDK (API 36) 与 JDK 21。

```bash
# 克隆代码仓库
git clone https://github.com/foobarz076/Wallpaper-Picker-Android.git
cd Wallpaper-Picker-Android

# 构建 Debug APK (macOS 环境)
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home ./gradlew assembleDebug

# 构建 Debug APK (Linux / Windows 环境)
./gradlew assembleDebug
```

### 文档与架构规范

- **架构决策记录 (ADR)**：中英双语同篇对照，位于 [`docs/adr/`](docs/adr/)。
- **专题拆分文档**：
  - 英文文档目录：[`docs/en/`](docs/en/)
  - 中文文档目录：[`docs/zh/`](docs/zh/)
- **演进路线与规划**：[`PLANS.md`](PLANS.md)

### 开源协议

本项目采用 **GNU General Public License v3.0 (GPLv3)** 许可证。详情请参阅 [LICENSE](LICENSE)。
