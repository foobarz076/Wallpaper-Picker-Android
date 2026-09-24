# [ADR-0001] Decouple Wallpaper Crop and Launcher Scroll Strategies / 解耦壁纸裁切与桌面滚动策略

> **Metadata / 元数据**
> - **Status / 状态**: Accepted / 已采纳
> - **Date / 日期**: 2026-09-25
> - **Deciders / 决策者**: @Foo, @Antigravity
> - **Technical Story / 关联背景**: [PLANS.md](../../PLANS.md) (Phase 3 & Phase 4), User Issue on Portrait Cropping and Non-scrolling ROMs

---

## Executive Summary / 双语摘要

### English Summary
Previously, enabling wallpaper scrolling forcefully scaled portrait images up to double screen width via Center Crop, which unintentionally blew up portrait images by 200%+ and cut off subjects' heads, while failing on custom ROMs or launchers that do not support wallpaper offset panning. We decouple the image fitting/cropping strategy (`WallpaperCropMode`) from the launcher parallax scrolling strategy (`WallpaperScrollMode`), refactoring the sizing pipeline to prioritize height preservation (`FIT_HEIGHT`) and prevent forced horizontal over-magnification.

### 中文摘要
原实现将“桌面平移滚动”与“宽屏居中裁切 (Center Crop)”强行绑定，导致竖版人像为了凑足双倍屏幕宽而被放大 200% 以上并严重“砍头”，且在不支持壁纸滚动的定制 ROM（如 HyperOS/MIUI 或关闭了平移的桌面）上产生静态大头贴问题。本决策将“裁切填充策略 (`WallpaperCropMode`)”与“桌面视差滚动 (`WallpaperScrollMode`)”解耦为独立正交维度，重构图形管线尺寸计算模型，以高度基准（`FIT_HEIGHT`）对齐原生行为并杜绝盲目放大。

---

## English Full Text

### 1. Context and Problem Statement
When users enabled wallpaper scrolling on portrait images (e.g. 9:16 or 9:20 character art), the existing `WallpaperProcessor` blindly targeted `targetWidth = screenWidth * 2`. `centerCrop` then calculated `scale = max(targetWidth / srcWidth, targetHeight / srcHeight) ≈ 2.0`, zooming into the vertical center and discarding the top 50% of the image (cutting off faces and heads).
Furthermore, in Android ecosystem fragmentation, many OEM launchers (e.g., HyperOS, Flyme) and users who disable wallpaper scrolling in third-party launchers ignore wallpaper window offsets. Forcing wide bitmaps in these environments brings double disadvantages: severe cropping without any actual scrolling benefit.

### 2. Decision Drivers
* **Visual Integrity**: Prevent clipping character heads and essential subjects on portrait wallpapers.
* **ROM Compatibility**: Support launchers and OEM ROMs that do not pan wallpapers across desktop pages.
* **Orthogonality**: Separation of visual composition (how the picture fits the screen) from interaction dynamics (how the picture moves during swipes).
* **Zero Runtime Overhead**: Lightweight, deterministic math within `WallpaperProcessor` without requiring heavy ML libraries.

### 3. Considered Options
* **Option A: Pure Algorithm Heuristics inside Single Scroll Option**
  * *Pros*: Zero UI changes, keeps settings minimal.
  * *Cons*: Cannot satisfy users on non-scrolling ROMs who want exact single-screen bounds, nor users who explicitly want uncropped letterboxing.
* **Option B: Full Decoupling into Orthogonal Crop & Scroll Dimensions (Chosen)**
  * *Pros*: Mathematically clean, gives full control to users on any ROM, aligns with future Phase 4 per-image preference override plans.
  * *Cons*: Adds one new chip group to UI, requires slight user comprehension.

### 4. Decision Outcome
* **Chosen Option**: **Option B**
* **Rationale**: Decoupling `WallpaperCropMode` (`FIT_HEIGHT`, `CENTER_CROP`, `FIT_CENTER`) from `WallpaperScrollMode` (`AUTO`, `NEVER`, `ALWAYS`) provides the cleanest architecture. `WallpaperProcessor` scales based on height for portrait images, completely curing the 200% magnification bug.

### 5. Consequences & Trade-offs
* **Positive Consequences**:
  * Portrait wallpapers maintain full head-to-toe integrity matching system default behavior.
  * Non-scrolling ROM users can choose `NEVER` to save memory and guarantee exact single-screen fit.
  * Extensible foundation for Phase 4 per-image crop overrides.
* **Negative Consequences & Mitigations**:
  * Introduces an additional preference key (`crop_mode`). Mitigated with sensible defaults (`FIT_HEIGHT`).

### 6. Thresholds & Triggers
Revisit when implementing Phase 4 interactive crop focus micro-adjustments (`crop_focus_x`, `crop_focus_y`) in Room.

---

## 中文全文

### 1. 背景与问题描述
在原先的实现中，当开启滚动选项时，`WallpaperProcessor` 强制将目标宽度设定为双倍屏幕宽（`screenWidth * 2`）。对于 9:16 / 9:20 的典型竖版人像，`centerCrop` 算出的缩放比高达 2.0x 以上，导致上部约 50% 画面被裁剪（人物头部被严重腰斩）。
此外，国内定制 ROM（如 HyperOS、ColorOS）或关闭了壁纸随动的 Launcher 根本不处理壁纸平移。强行输出宽屏壁纸导致用户既享受不到视差滚动，又白白遭受过度裁切的“双重暴击”。

### 2. 决策驱动因素
* **画面构图保全**：竖版人像严禁砍头砍脚，保障画面主体完整度。
* **ROM 碎片化兼容**：适配不支持壁纸滑动的第三方桌面与定制系统。
* **维度正交性**：视觉展示（裁切策略）与翻页交互（平移滚动）在概念与实现上彻底解耦。
* **低功耗无感知**：依靠纯几何变换算法，不引入臃肿的人脸识别或外部依赖。

### 3. 备选方案权衡
* **方案 A：单选项内做隐式启发式算法**
  * *优势*：UI 保持单项，用户无需学习新概念。
  * *劣势*：无法应对不支持滚动的 ROM，用户无法主动指定是否允许裁切留黑边。
* **方案 B：裁切偏好与视差滚动彻底解耦（选定）**
  * *优势*：概念正交清晰，各系统用户各取所需，为 Phase 4 单图自定义构图打下坚实基础。
  * *劣势*：UI 增加一组设置项，需配置默认值保障开箱即用。

### 4. 决策结果与推导论据
* **选定方案**：**方案 B**
* **论证逻辑**：引入独立枚举 `WallpaperCropMode`（`FIT_HEIGHT` 高度优先、`CENTER_CROP` 居中充满、`FIT_CENTER` 原图完整），并在 `WallpaperProcessor` 中重构计算管线。对于竖图以屏幕高度为基准（`FIT_HEIGHT`），彻底终结了无脑放大 200% 的逻辑漏洞。

### 5. 影响评估与技术代价
* **积极收益**：
  * 竖图彻底告别“断头壁纸”，表现与系统原生高度对齐。
  * 不支持滚动的定制 ROM 可锁定 `NEVER`，输出严格单屏物理尺寸，节省显存并规避误裁切。
  * 为路线图 Phase 4 的 Room 单图独立偏好记忆提供了清晰的枚举定义。
* **消极代价与缓解措施**：
  * 新增持久化字段 `crop_mode`。通过默认设置为 `FIT_HEIGHT` 确保老用户无缝升级体验。

### 6. 演进阈值与推翻临界点
当在 Phase 4 引入基于触摸手势的交互式焦点微调（`crop_focus_x`, `crop_focus_y`）时，再次检验并补充此裁切管线。
