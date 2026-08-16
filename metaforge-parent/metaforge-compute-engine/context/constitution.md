<!--
================================================================================
  Sync Impact Report
================================================================================
  BC Constitution Version Change: [PLACEHOLDER] → 1.0.0
  Parent Global Constitution Version: 1.0.0

  BC-Specific Principles:
    ✅ Added: I. 元模型唯一权威原则 (MUST)
    ✅ Added: II. 生效数据基准原则 (MUST)
    ✅ Added: III. 计算存储分离原则 (MUST)
    ✅ Added: IV. 结果结构化原则 (MUST)
    ✅ Added: V. 过滤前置原则 (MUST)
    ✅ Added: VI. 深度上限与安全熔断原则 (MUST)
    ✅ Added: VII. 下游透明原则 (SHOULD)

  Custom Sections:
    ✅ Added: 核心能力 (Core Capabilities)
    ✅ Added: MVP 边界 (MVP Boundaries)

  Override Entries:
    ✅ Added: Override 1 — VI. 合约化双协议标准接口 (SHOULD)
    ✅ Added: Override 2 — VIII. Agent 友好型输出 (SHOULD)

  Deferred TODOs: None
================================================================================
-->

# 语义查询与推理引擎 Bounded Context Constitution
<!-- BC 级治理宪法。继承全局系统宪法全部规则作为只读基线。-->
<!-- OVERRIDE RULE: Only SHOULD/MAY level principles from the parent global constitution can be overridden. MUST level principles CANNOT be modified or removed. -->

**Parent Version**: 1.0.0
<!-- 人类可读的可追溯标记。AI 合并期间不进行版本对齐校验。记录创建/上次修订本 BC 宪法时引用的全局宪法版本。-->

---

## BC-Specific Principles
<!-- 本 BC 独有的核心原则，不继承自全局宪法。遵循 MUST/SHOULD/MAY 级别规范。原则名称不得与全局宪法原则重复。-->

### I. 元模型唯一权威原则 (MUST)

所有推理、查询的语义规则唯一来源于已发布的元模型定义（EntitySchema/RelationSchema 定义、
AssociationType 语义枚举），不引入外部知识、自定义规则或 LLM 辅助推理。
推理结果必须确定、可追溯、可复现。

### II. 生效数据基准原则 (MUST)

所有对外查询与计算默认仅基于底层模块的生效态数据执行。历史版本、草稿版本、
已下线版本不参与默认计算。

### III. 计算存储分离原则 (MUST)

本 BC 为纯无状态计算层，不持有原始元数据与关系数据的存储主权，也不存储任何计算衍生数据。
禁止在本 BC Schema 中创建任何持久化数据表。

### IV. 结果结构化原则 (MUST)

所有输出结果必须以 FQN 为核心标识，内联必要的实体摘要（FQN、展示名、元模型类型 FQN）
与关系摘要（FQN、关系类型、端点 FQN），确保下游模块无需额外补查询。

### V. 过滤前置原则 (MUST)

所有过滤参数在遍历过程中实时生效，被过滤的实体/关系不参与遍历、不计入深度。
过滤条件取各维度交集。

### VI. 深度上限与安全熔断原则 (MUST)

多度查询、路径推理、影响溯源默认最大深度为 5，配置硬上限为 10（可通过 `metaforge.compute-engine.traversal.max-depth` 在 1-10 范围内调整）。超出配置深度返回截断并提示。
单次查询必须设置超时熔断机制。

### VII. 下游透明原则 (SHOULD)

本 BC 不感知下游消费形态，仅提供标准化结构化计算结果。消费方自行决定如何组装、裁剪、
格式化。

---

## 核心能力

<!-- 定义 BC 对外提供的核心能力域及其优先级-->

### 多维图查询域（P0）

多度邻接查询、组合层级树查询、子图提取查询、图模式匹配查询、多条件复合检索、
批量语义查询（FQN 上限 200）。

### 路径推理域（P0）

两点间路径查询、传递闭包推理、多跳语义推理（最大 3 步）、路径可达性快速判定。

### 影响溯源域（P1）

正向影响扩散、反向依赖溯源、影响路径详情。

### 统一过滤维度

所有接口统一支持 FQN 前缀、实体类型、关系类型、属性字段四维过滤。

---

## MVP 边界

<!-- MVP 阶段明确不做和明确不引入的能力、技术与组件边界-->

### 不实现的能力

复杂图算法、流式增量计算、一致性校验、LLM 推理、用户自定义规则、可视化渲染、
多版本并行推理、血缘深度分析。

### 不引入的技术组件

Neo4j、RabbitMQ/Kafka、Redis、LLM 推理服务。

---

## BC Overrides
<!-- 仅可对父级全局宪法 SHOULD/MAY 级原则做选择性覆盖。每条覆盖必须引用准确的父原则名称 + 显式覆盖理由。MUST 级原则禁止在此覆盖。-->

### Override 1: VI. 合约化双协议标准接口 (SHOULD)

- **Original Parent Rule**: 所有对外能力通过 REST + MCP 双协议标准化接口发布。
- **Override Content**: 本 BC 仅通过 REST API + Domain Object 发布能力，
  不独立暴露 MCP 协议。MCP 由 agent-consumption 统一发布。
- **Rationale**: 语义查询与推理引擎 BC 定位为纯计算层，职责聚焦于图查询与推理逻辑；
  MCP 协议暴露职责归属 agent-consumption（消费编排层），
  避免协议职责分散导致接口治理复杂度膨胀。

### Override 2: VIII. Agent 友好型输出 (SHOULD)

- **Original Parent Rule**: 所有查询与推理结果输出为低理解成本的结构化格式，
  可直接注入 Agent 上下文，无需大模型二次解析。
- **Override Content**: 本 BC 返回标准 JSON 结果。Agent 友好型格式化
  由 agent-consumption 统一完成，本 BC 不生成 Agent 上下文块。
- **Rationale**: 遵循计算存储分离原则，计算层不应感知特定消费协议或输出形式；
  JSON 结构化输出已满足下游可消费性，Agent 格式化注入属于编排层职责。

---

**BC Constitution Version**: 1.0.0 | **Created**: 2026-07-30 | **Last Amended**: 2026-07-30
<!-- BC 宪法拥有独立的语义化版本，与全局宪法版本解耦。-->
