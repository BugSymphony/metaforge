<!--
================================================================================
  Sync Impact Report
================================================================================
  BC Constitution Version Change: N/A (placeholder skeleton) → 1.0.0
  Parent Global Constitution Version: 1.0.0
  Last Amended Date: 2026-08-11

  BC-Specific Principles:
    ✅ Added: I. 纯机制层定位 (MUST)
    ✅ Added: II. 声明式扩展铁律 (MUST)
    ✅ Added: III. 契约与实现分层 (MUST)
    ✅ Added: IV. 统一认知入口 (MUST)
    ✅ Added: V. 无 LLM 依赖 (MUST)
    ✅ Added: VI. 注册与校验治理 (SHOULD)
    ✅ Added: VII. Scope 边界强制 (SHOULD)
    ✅ Added: VIII. 输出自包含与等价双格式 (SHOULD)

  Custom Sections:
    ✅ Added: 扩展机制约束
    ✅ Added: 无状态与数据主权约束
    ✅ Added: 可观测与配置治理约束

  BC Overrides:
    ✅ Added: Override 1 — VI. 合约化双协议标准接口 (SHOULD→MUST)
    ✅ Added: Override 2 — VIII. Agent 友好型输出 (SHOULD→细化)

  Template Synchronization:
    ✅ N/A — No BC-specific plan/spec templates exist; alignment not required.

  Deferred TODOs: None
================================================================================
-->

# metaforge-agent-cognition Bounded Context Constitution
<!-- BC-level governance constitution. Inherits all rules from the global system constitution as read-only baseline. -->
<!-- OVERRIDE RULE: Only SHOULD/MAY level principles from the parent global constitution can be overridden. MUST level principles CANNOT be modified or removed. -->

**BC Identity**: 平台机制层（Platform Mechanism）——负责"怎么做"（路由、加载、编排、输出），不含任何具体维度实现与模板内容。

**Parent Version**: 1.0.0
<!-- Human-readable traceability marker only. No version alignment validation during AI merge. Records the global constitution version referenced when this BC constitution was created/last updated. -->

**拆分架构**: 本 BC 拆分为契约层与实现层两个 Maven 模块；依赖 foundation 与底层 BC 的公开 api。

---

## BC-Specific Principles
<!-- Exclusive core principles for this BC only, not inherited from the global constitution. Follow MUST/SHOULD/MAY level specification. Principle names must not duplicate global constitution principles. -->
### I. 纯机制层定位 (MUST)

本 BC 只承载平台机制，禁止内嵌任何具体维度实现或模板内容；不持有底层数据主权，
不持久化业务数据与任务上下文，仅存储自身配置。所有具体认知维度由下游 BC 通过
SPI 挂载实现，本 BC 仅负责编排与路由。

### II. 声明式扩展铁律 (MUST)

一切新认知能力通过声明式配置 + SPI 实现扩展，禁止修改引擎核心代码。
内置认知分类体系（8 分类）为封闭集合，不可配置扩展。
新增能力 = 声明式元文件 + SPI 实现 + 能力注册，三件套齐备即可扩展，
无需改动引擎核心；新增能力只能在既有分类下扩展，不得创建新分类。

### III. 契约与实现分层 (MUST)

契约层只含接口与数据结构，不依赖任何实现模块；实现层经容器发现 SPI 实现，
不编译依赖具体能力模块；所有能力以共享契约驱动。契约层模块的编译 classpath
不得包含任何第三方实现或具体能力模块的坐标。

### IV. 统一认知入口 (MUST)

全部认知能力经单一统一入口暴露；入参为确定性结构化参数，不接受自然语言；
错误统一经标准化错误体系呈现，不得以内部异常形态外泄。
统一入口负责参数校验、路由分发、结果聚合与异常标准化转换。

### V. 无 LLM 依赖 (MUST)

认知能力为确定性计算，不依赖 LLM 或向量语义相似度作为实现前提。
所有分类、匹配、推理逻辑基于规则引擎或确定性算法，结果可复现、可审计。

### VI. 注册与校验治理 (SHOULD)

模板与维度必须经注册表统一管理；注册前完成完整性、合法性校验，
校验不通过的能力不得对外提供，且不得影响已注册能力。
注册表支持运行时热加载与卸载，变更事件可被监听。

### VII. Scope 边界强制 (SHOULD)

认知查询必须受 scope 边界约束；越界内容不得出现在输出中，
并须在输出中显式标注被裁剪/跳过的范围。
Scope 由 Agent 导入声明与查询参数共同决定，取最小交集。

### VIII. 输出自包含与等价双格式 (SHOULD)

输出必须自包含（消费端无需二次查询底层能力）；支持双格式呈现（JSON Schema +
结构化上下文块），两种格式语义内容完全等价。
输出携带完整上下文元信息（来源 BC、版本、scope 范围、裁剪标记）。

---

## 扩展机制约束
<!-- 定义新认知能力扩展的约束与流程规范。-->

- 新增能力必须提供声明式元文件（描述能力名称、所属分类、依赖、参数 schema），
  元文件格式由契约层定义。
- SPI 实现需满足契约层接口约束，实现类经 `ServiceLoader` 或容器注入机制发现。
- 能力注册在 BC 启动时自动完成；注册失败的 SPI 实现记录错误日志并跳过，
  不得阻塞引擎启动。
- 内置认知分类体系为封闭集合（8 分类），新增分类需修订 BC 宪法
  并升级 MAJOR 版本。

## 无状态与数据主权约束
<!-- 本 BC 无状态运行边界与数据主权声明。-->

- 本 BC 无状态运行，不保存会话、快照、任务上下文或任何形式的运行时状态。
- 不触碰业务数据存储；所有输入数据由调用方传入，输出结果自包含、
  可直接注入消费端，无需消费端回查底层 BC。
- 自身配置（引擎参数、注册表、路由表）以配置文件形式持久化，
  不依赖外部数据库或缓存。

## 可观测与配置治理约束
<!-- 行为参数配置规范与可观测性治理。-->

- 对外错误统一走标准化错误体系（错误码 + 错误级别 + 上下文消息），
  具备可诊断性。错误码由契约层统一定义，实现层不得创建私有错误码。
- 行为参数（默认深度、原型、预算、超时、扫描路径等）必须可配置、
  支持环境变量覆盖，且零配置可用（所有参数具备合理默认值）。
- 引擎关键路径输出结构化日志（入口入参校验、SPI 加载、路由分发、
  结果聚合），支持消费端诊断与审计。

---

## BC Overrides
<!-- Selective override of parent global constitution SHOULD/MAY level principles only. Each entry must reference the exact parent principle name + explicit override rationale. MUST level principles are forbidden to be overridden here. -->
### Override 1: VI. 合约化双协议标准接口 (SHOULD→MUST)

- **Original Parent Rule**: 全局宪法 VI 要求所有对外能力通过 REST + MCP 双协议
  标准化接口发布（SHOULD 级建议）。
- **Override Content**: 本 BC 对外认知能力必须同时经 REST 与 MCP 双通道交付，
  内部实现不对外暴露，不得在实现中被弱化为单一通道。任一通道缺失视为未完成交付。
- **Rationale**: 本 BC 是认知能力统一门面，消费方包括管理系统（REST）与
  Agent 生态（MCP），双通道为默认交付形态，必须强制而非建议。

### Override 2: VIII. Agent 友好型输出 (SHOULD→细化)

- **Original Parent Rule**: 全局宪法 VIII 要求所有查询与推理结果输出为低理解成本
  的结构化格式，可直接注入 Agent 上下文（SHOULD 级建议）。
- **Override Content**: 在全局 VIII 基础上细化增强——双格式语义完全等价、
  输出自包含（含完整上下文元信息）、超限自动裁剪并显式标记。
  每份输出必须携带：来源 BC 标识、元模型版本、scope 范围、裁剪标记、
  生成时间戳与置信度。
- **Rationale**: 输出组装是本 BC 核心职责，直接决定 Agent 认知质量。
  本 Override 属细化强化而非弱化——全局 VIII 的"低理解成本结构化格式"约束
  完全保留，在此基础上增加自包含性与可追溯性要求。

---

**BC Constitution Version**: 1.0.0 | **Created**: 2026-08-11 | **Last Amended**: 2026-08-11
<!-- BC constitution has independent semantic versioning, decoupled from global constitution version. -->
