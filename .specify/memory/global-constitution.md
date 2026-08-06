<!--
================================================================================
  Sync Impact Report
================================================================================
  Version Change: N/A (initial) → 1.0.0
  Ratification Date: 2026-08-01

  Principle Summary:
    ✅ Added: I. 元模型唯一权威性 (MUST)
    ✅ Added: II. 显式导入边界管控 (MUST)
    ✅ Added: III. 全链路权限过滤 (MUST)
    ✅ Added: IV. 版本统一收敛 (MUST)
    ✅ Added: V. 纯组合无继承设计 (SHOULD)
    ✅ Added: VI. 合约化双协议标准接口 (SHOULD)
    ✅ Added: VII. Bundle模块化治理 (SHOULD)
    ✅ Added: VIII. Agent友好型输出 (SHOULD)
    ✅ Added: IX. 纯元数据边界坚守 (MUST)
    ✅ Added: X. 文档中文规范 (MUST)
    ✅ Added: XI. 代码注释中文规范 (SHOULD)
    ✅ Added: 战略假设与阶段约束 (Strategic Assumptions & Phase Constraints)
    ✅ Added: 接口与生态标准 (Interface & Ecosystem Standards)
    ✅ Added: 治理 (Governance)

  Template Synchronization:
    ✅ Synced: .specify/templates/global-specify-template.md — no changes required
    ✅ Synced: .specify/templates/global-plan-template.md — no changes required
    ✅ Synced: .specify/scripts/bash/*.sh — no oudated governance references

  Deferred TODOs: None

  Rationale: Initial constitution adoption for MetaForge system, codifying all 11
  core governance principles, strategic MVP assumptions, and interface standards.
================================================================================
-->

# MetaForge 全局宪法

<!-- 全系统顶层治理基线。所有限界上下文（BC）的父宪法。MUST 级原则不可被任何 BC 级宪法覆盖。-->

## 核心原则

### I. 元模型唯一权威性 (MUST)

元模型（M2层）是全平台语义规则的唯一源头。所有元数据定义、关系约束、导出边界
均以元模型为准，禁止任何绕过元模型的直接元数据操作。元模型采用
Bundle（领域模块）→ Package（分类包）→ 元模型元素的三层正交架构，
每层职责明确分离，确保语义一致性可追溯。

### II. 显式导入边界管控 (MUST)

Agent 必须通过声明式方式显式导入指定版本的元模型及对应命名空间的元数据范围，
完成授权后方可消费相关元数据。未导入的内容对 Agent 完全不可见，
从入口处严格约束 Agent 的认知范围与执行边界，杜绝未授权访问。

### III. 全链路权限过滤 (MUST)

所有元数据查询、推理请求均基于 Agent 的导入范围做白名单权限过滤，
仅返回授权范围内的结构化语义结果，保障 Agent 获取的上下文精准匹配
目标业务领域，杜绝越权执行风险。

### IV. 版本统一收敛 (MUST)

元模型采用整体式版本管理，以 Bundle 为单元完成版本固化，
禁止模块间出现多层版本不一致。版本升级必须遵循语义化版本规范
（Semantic Versioning），breaking change 需 MAJOR 版本升级并通知所有消费方。

### V. 纯组合无继承设计 (SHOULD)

元模型元素间采用纯组合关系（Composition）而非继承（Inheritance），
保持结构扁平化。与 JSON Schema 原生兼容，降低理解与适配成本，
避免多层级继承导致的语义混乱与复杂度膨胀。

### VI. 合约化双协议标准接口 (SHOULD)

所有对外能力通过 REST + MCP 双协议标准化接口发布：
REST 覆盖通用系统集成与管理需求，MCP 原生适配 Agent 生态。
内部实现不对外暴露，接口变更需保持向后兼容或走版本升级流程。

### VII. Bundle 模块化治理 (SHOULD)

每个 Bundle 通过导出清单（Export Manifest）明确对外暴露的命名空间边界，
未导出的内容仅模块内部可见。依赖关系可治理，支持跨 Bundle 引用管控，
构建清晰的模块依赖拓扑图。

### VIII. Agent 友好型输出 (SHOULD)

所有查询与推理结果必须输出为低理解成本的结构化格式（如 JSON Schema、
结构化上下文块等），可直接注入 Agent 上下文，无需大模型二次解析，
降低 Token 消耗与推理偏差。

### IX. 纯元数据边界坚守 (MUST)

系统仅存储描述业务概念、关系、规则的元数据，不触碰任何具体业务数据。
既保障核心业务数据的安全性，也让语义底座具备跨业务系统的通用适配性，
保持纯基础设施工件的定位。

### X. 文档中文规范 (MUST)

所有 SDD（软件设计文档）及宪法、规约、计划等治理文档的正文内容
必须使用简体中文撰写填充。术语（如 Bundle、MCP）、代码标识符、
协议名称等专有名词保留英文原文，不强制翻译。

### XI. 代码注释中文规范 (SHOULD)

生成代码时，关键业务逻辑、复杂算法、接口说明等处必须添加注释。
注释统一使用简体中文，确保团队成员和 AI Agent 能准确理解逻辑意图。
代码标识符、变量名、函数名等仍使用英文，注释与代码职责分离。

## 战略假设与阶段约束

<!-- MVP 阶段范围界定与战略取舍声明，指导当前阶段架构决策的优先级与边界。-->

### MVP 阶段聚焦范围

- **核心能力验证优先**：MVP 阶段聚焦元数据全链路（定义 → 导入 → 查询 → 消费）
  核心能力验证，不涉及多租户、高可用集群、全链路审计等企业级特性。
- **声明式配置优先**：MVP 阶段仅支持声明式配置文件建模，
  不提供可视化元模型编辑器（GUI Editor）。
- **AI 增强功能后置**：元数据自动发现（Metadata Discovery）、
  LLM 辅助智能建模等 AI 增强功能待核心链路验证后扩展。
- **Agent 接入方式**：Agent 生态通过 MCP 协议原生接入，
  REST 接口覆盖管理类操作。

### 战略取舍声明

- **消费端心智模型复用**：消费端复用编程领域模块化导入/导出
  （import/export）的成熟心智模型，降低学习成本。
- **后端优先策略**：MVP 以服务端 API 为交付重心，
  前端管理界面为辅助，不做完整 UI 产品化。

## 接口与生态标准

<!-- 定义系统对外接口的统一标准与生态集成规范。-->

### 双协议接口规范

| 协议 | 适用场景 | 标准规范 | 强制要求 |
|------|----------|----------|----------|
| REST API | 通用系统集成、元模型管理、配置管理 | RESTful HTTP/1.1 + JSON | 统一错误码体系、版本化 URL 路径 |
| MCP | Agent 上下文注入、语义查询、推理消费 | MCP 协议标准 | 原生 JSON Schema 输出、零解析开销 |

### 接口变更约束

- 接口变更必须保持向后兼容（Backward Compatible）；
  不兼容变更需走 MAJOR 版本升级流程。
- 内部实现细节不得通过接口泄露，所有外部可见行为以合约文档为准。
- 新增接口需同步更新合约文档与 API 版本号。

## 治理

<!-- 全局宪法为全系统最高治理文件，所有 BC 级宪法与特性级规约必须服从本宪法。-->

### 修订流程

1. **提议**：任何架构变更提案需明确本次修订的影响范围与兼容性评估。
2. **评审**：MUST 级原则变更需全架构评审（Architecture Review）；
   SHOULD/MAY 级原则变更需至少两位核心贡献者批准。
3. **同步**：修订生效后，所有 BC 级宪法需在下一个迭代周期内完成对齐。

### 版本控制

- 遵循语义化版本（Semantic Versioning）规范：
  - **MAJOR**：MUST 级原则的重新定义或移除，向后不兼容的治理变更。
  - **MINOR**：新增原则或章节，SHOULD 级治理规则的实质性扩展。
  - **PATCH**：措辞澄清、错别字修正、非语义性调整。
- 每次修订需更新版本号与 `Last Amended` 日期，
  并在文件头部 Sync Impact Report 中记录变更摘要。

### BC 继承规则

- **MUST 级原则**：所有 BC 级宪法必须无条件遵循，不得覆盖或弱化。
  任何 BC 不得制定与 MUST 原则冲突的本地规则。
- **SHOULD 级原则**：BC 可在充分说明理由的前提下覆盖或细化，
  但必须在 BC 宪法中显式声明覆盖项及其 Rationale。
- **MAY 级原则**：BC 可自由裁量，无需特别声明。
- **冲突解决**：BC 宪法与全局宪法冲突时，以全局宪法为准。
  BC 必须在发现冲突的迭代周期内完成修正。

**版本**: 1.0.0 | **批准日期**: 2026-08-01 | **最后修订**: 2026-08-01
