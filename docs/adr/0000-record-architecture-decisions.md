# [ADR-0000] Record Architecture Decisions / 架构决策记录规范

> **Metadata / 元数据**
> - **Status / 状态**: Accepted
> - **Date / 日期**: 2026-09-20
> - **Deciders / 决策者**: @core-maintainers
> - **Technical Story / 关联背景**: [PLANS.md (项目规划与路线图)](../../PLANS.md)

---

## Executive Summary / 双语摘要

### English Summary
We establish the Architecture Decision Record (ADR) methodology to record significant architectural decisions and technical trade-offs across the project lifecycle. To bridge international open-source collaboration across the Android ecosystem with core project maintainers, each ADR adopts a unified single-file layout: Bilingual Executive Summary $\rightarrow$ English Full Text $\rightarrow$ Chinese Full Text.

### 中文摘要
本项目正式确立架构决策记录（ADR）体系，用于记录系统生命周期中的关键架构决策与技术权衡。为了兼顾面向国际 Android 开源生态的协作与核心维护团队的沟通，所有 ADR 统一采用单文件三段式版式：“双语摘要 $\rightarrow$ 英文全文 $\rightarrow$ 中文全文”。

---

## English Full Text

### 1. Context and Problem Statement
As the Wallpaper Picker (`foo-wallpaper-picker-next`) project evolves, critical technical choices—including multi-generation Android OS compatibility (API 23 to API 36+), background scheduling strategies (WorkManager), low-memory anti-OOM image downsampling, SAF storage persistence, and pluggable wallpaper source abstractions—must be documented with explicit rationale. Without structured architectural records:
* Historical rationale and discarded alternatives are lost over time (e.g., rejecting direct Google Photos cloud APIs).
* Onboarding new community contributors leads to repeated debates on solved trade-offs.
* Open-source collaborators in the broader Android/FLOSS community lack clarity on downstream engineering choices.

### 2. Decision Drivers
* **Bilingual Transparency**: Support both English-speaking Android open-source contributors and Chinese-speaking core maintainers without maintaining divergent documentation trees.
* **Single Source of Truth**: Prevent drift between localized files by co-locating English and Chinese texts within the same Markdown file.
* **Traceable Immutability**: Ensure every architectural change is tied to reproducible Git commits and contextual references (such as `PLANS.md`).

### 3. Considered Options
* **Option A: Markdown Any Decision Record (MADR) with separate directories (`docs/adr/en/` & `docs/adr/zh/`)**
  * *Pros*: Cleaner per-file views for single-language readers.
  * *Cons*: High synchronization overhead; translation drift is frequent; multi-file PR reviews complicate code review.
* **Option B: Single-file Bilingual Structure (Bilingual Summary + English Full Text + Chinese Full Text)**
  * *Pros*: Single source of truth; zero translation drift during PR reviews; summaries give both audiences an immediate bird's-eye view.
  * *Cons*: Longer single files.
* **Option C: Informal commit messages and scattered wiki/chat notes**
  * *Pros*: Zero upfront documentation cost.
  * *Cons*: Loss of architectural memory; high context recovery cost.

### 4. Decision Outcome
* **Chosen Option**: **Option B (Single-file Bilingual Structure)**.
* **Format Specification**:
  1. **Filename**: `NNNN-kebab-case-title.md` (4-digit zero-padded index, e.g., `0001-background-scheduling-workmanager-vs-alarms.md`).
  2. **Top Metadata Block**: Status, Date, Deciders, and Technical Story (linking to relevant PR, Issue, or `../../PLANS.md`).
  3. **Executive Summary**: English Summary followed by Chinese Summary.
  4. **English Full Text**: Context, Drivers, Options, Outcome, Consequences, Thresholds.
  5. **Chinese Full Text**: 对称的中文完整版（背景、驱动因素、备选权衡、最终决策、影响代价、演进阈值）.

### 5. Consequences & Trade-offs
* **Positive Consequences**:
  * Complete transparency for both global Android FLOSS peers and Chinese contributors.
  * PR review allows simultaneous verification of technical content across both languages.
  * Append-only history ensures decision lineage is never silently edited.
* **Negative Consequences & Mitigations**:
  * Writing bilingual documents takes extra effort $\rightarrow$ *Mitigation*: Contributors can draft in their primary language and leverage LLM assistance to scaffold the counterpart, followed by manual terminology verification.

### 6. Thresholds & Triggers (When to Revisit)
* If the documentation volume grows so large that tooling or static site generators mandate separate language subtrees, directory splitting may be reconsidered.

---

## 中文全文

### 1. 背景与问题描述
随着 Wallpaper Picker（`foo-wallpaper-picker-next`）项目从构想走向落地，系统涉及多项底层架构决策——包括跨代 Android 版本的 API 兼容适配（最低支持 Android 6.0 API 23，最高适配现代 Android API 36+）、后台保活与调度选型（WorkManager）、低内存（1GB~2GB RAM）防 OOM 图像采样管线、SAF 存储权限持久化以及可插拔壁纸源（WallpaperSource）架构设计。如果缺乏结构化的架构决策记录：
* 历史权衡与被否决的方案随时间流逝而遗失（例如为何在深度调研后明确放弃接入 Google Photos 云端 API 等决策）。
* 新协作者加入时容易在已达成共识的技术选型上陷入重复讨论。
* 面向国际 Android 开源社区及 GitHub 贡献者时缺乏标准英语工程文档支持。

### 2. 决策驱动因素
* **双语透明性**：同时满足国际 Android 开源协作者与本土核心开发团队的阅读与交流需求。
* **单一事实来源（Single Source of Truth）**：避免拆分目录导致的中英文档内容漂移或翻译孤儿化。
* **不可变与可追溯性**：确保每一次架构演进均与具体的 Git Commit 及业务上下文（如 `PLANS.md`）紧密锚定。

### 3. 备选方案权衡
* **方案 A: 独立目录多语言结构（`docs/adr/en/` 与 `docs/adr/zh/`）**
  * *优势*: 单一语言读者阅读体验纯粹，单文件篇幅短。
  * *劣势*: 同步成本极高，容易出现中文更新而英文未更新的漂移现象；PR 审查时需要跨文件对比。
* **方案 B: 单文件双语三段式（双语摘要 + 英文全文 + 中文全文）**
  * *优势*: 保证同一文件内严格同步；Reviewer 在单个 PR Diff 中即可完成双语审核；双语摘要让两类读者都能秒级理解核心决策。
  * *劣势*: 单个 Markdown 文件篇幅较长。
* **方案 C: 散落在 Commit 信息与聊天记录中的非结构化沉淀**
  * *优势*: 前期几乎零编写成本。
  * *劣势*: 架构资产不可检索、不可审计，极易遗忘。

### 4. 决策结果与推导论据
* **选定方案**: **方案 B (单文件双语三段式)**。
* **规范细则**:
  1. **文件命名**: `NNNN-kebab-case-title.md`（四位补零编号，如 `0001-background-scheduling-workmanager-vs-alarms.md`）。
  2. **顶部元数据区**: 记录状态（Status）、日期（Date）、决策者（Deciders）和关联背景（Technical Story，如链接到具体的 PR、Issue 或 `../../PLANS.md`）。
  3. **双语摘要区**: 包含英文摘要与中文摘要各一段（2~4 句话提炼背景、决策与折衷）。
  4. **英文全文区**: Context, Drivers, Options, Outcome, Consequences, Thresholds.
  5. **中文全文区**: 背景与问题描述、决策驱动因素、备选方案权衡、决策结果与推导论据、影响评估与技术代价、演进阈值与推翻临界点。

### 5. 影响评估与技术代价
* **积极收益**:
  * 架构决策全局透明，既能向上游社区提 Issue/PR 时引用，又能作为团队内部治理与后续迭代依据。
  * 强化 PR 审查机制，双语对齐保证关键 Android 术语（如 `WorkManager`、`Subsampling`、`Single Source of Truth`）理解一致。
* **消极代价与缓解措施**:
  * 双语起草需要投入更多时间 $\rightarrow$ *缓解措施*：允许协作者以母语为主起草，借助 AI 工具辅助补全另一语言并人工核对术语。

### 6. 演进阈值与推翻临界点
* 仅当后续引入大型静态文档编译工具（如 VitePress / Starlight）且其多语言路由强制要求分目录放置时，方可评估是否将单文件拆分为多语言目录。
