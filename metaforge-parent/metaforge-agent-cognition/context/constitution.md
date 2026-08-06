<!--
================================================================================
  Sync Impact Report
================================================================================
  BC Constitution Version: N/A (placeholder skeleton) → 1.0.0
  Parent Global Constitution Version: 1.0.0

  BC-Specific Principles:
    ✅ Added: 无状态 FQN 寻址原则 (MUST)
    ✅ Added: 交付 M1 仅过滤 M2 原则 (MUST)
    ✅ Added: 零 LLM 零向量原则 (MUST)
    ✅ Added: 数据主权不持有原则 (MUST)
    ✅ Added: 结构化参数入参原则 (MUST)
    ✅ Added: 自包含标准化输出原则 (SHOULD)

  Custom Sections:
    ✅ Added: 核心能力 — 14 内置认知视角、统一查询引擎、深度裁剪与 Agent 原型适配、
              双输出格式、changeWatch 变更感知
    ✅ Added: MVP 边界 — 不实现完整授权体系、向量语义匹配、LLM 辅助推理

  BC Overrides:
    ✅ Added: Override VI. 合约化双协议标准接口 (SHOULD) — REST only, MCP 委托
              metaforge-consumer
    ✅ Added: Override VIII. Agent 友好型输出 (SHOULD) — json/prompt 双格式原生交付

  Deferred TODOs: None

  Rationale: Initial concrete constitution for 元认知指导层 (CognitionGuidance) BC.
  The skeleton was fully placeholder; this revision fills all BC-specific principles,
  custom sections, and override entries with substantive governance content.
================================================================================
-->

# 元认知指导层 (CognitionGuidance) Bounded Context Constitution

<!-- BC 级治理宪法。以全局系统宪法为只读基线继承所有规则。 -->
<!-- 覆盖规则：仅可覆盖父全局宪法中的 SHOULD/MAY 级原则，MUST 级原则不得修改或移除。 -->

**Parent Version**: 1.0.0
<!-- 可读追溯标记。不执行版本对齐验证。记录本 BC 宪法创建/最后更新时引用的全局宪法版本。-->

---

## BC-Specific Principles

<!-- 本 BC 独有核心原则，不从全局宪法继承。遵循 MUST/SHOULD/MAY 级别规范。原则名称不得与全局宪法原则重复。-->

### I. 无状态 FQN 寻址原则 (MUST)

所有查询端点幂等、无任务 ID、无会话上下文；每次请求自包含，不依赖服务端状态存储。
Bundle 范围通过 `entity_fqn` 的 FQN 前缀即席恢复，无需预先注册或会话绑定。
任何查询的结果仅由传入参数决定，不受历史请求或外部状态影响。

### II. 交付 M1 仅过滤 M2 原则 (MUST)

元模型（EntitySchema / RelationSchema）仅作为查询过滤条件与语义解释来源使用，
永不作为交付主体返回给消费端。当 M1 层（实例数据/具体业务实体）缺失时，
必须返回空目录并在响应中明确标注缺失原因，禁止降级输出 M2 层骨架结构作为替代结果。
M2 层始终处于过滤角色而非填充角色。

### III. 零 LLM 零向量原则 (MUST)

本 BC 不依赖任何大语言模型（LLM）或向量语义相似度计算。
所有查询相关性排序通过 AssociationType 语义约束表达，结合图拓扑距离实现排序逻辑。
不使用 embedding、vector store 或任何基于语义近似度的匹配算法。
自然语言到结构化参数的转换职责完全归属 metaforge-consumer。

### IV. 数据主权不持有原则 (MUST)

本 BC 不存储任何元数据副本、关系副本、历史快照、会话上下文或继承链信息。
仅持久化自身运营所需的 YAML 配置（cognition-templates.yml / cognition-perspectives.yml）。
所有运行时元数据通过即席查询上游 BC 获取，查询完成后不保留任何数据痕迹。

### V. 结构化参数入参原则 (MUST)

所有查询端点仅接受结构化参数（JSON body / query params），不接受自然语言文本作为查询条件。
自然语言到结构化查询参数的转换由 metaforge-consumer 层完成，
本 BC 收到的一定是已解析完成的确定型结构化参数。

### VI. 自包含标准化输出原则 (SHOULD)

所有查询输出以 FQN 为核心标识，内联完整语义信息，消费端无需对同一 BC 发起二次查询
即可获取全部所需上下文。统一根 JSON 结构包含 `context_meta`（含 `data_version_anchors`）
及按认知视角组织的章节内容，每项输出均为自包含的完整认知单元。

---

## 核心能力

<!-- 元认知指导层的功能能力边界与约束。-->

### 内置认知视角

本 BC 提供 14 个内置认知视角维度，覆盖元认知查询的完整语义空间：

| 视角 | 用途 |
|------|------|
| `entity_profile` | 实体概要信息（类型、属性、描述） |
| `domain_location` | 实体在 Bundle/Package 层级中的定位 |
| `composition_tree` | 实体的组合关系树 |
| `relationship_graph` | 实体关联关系图谱 |
| `constraint_set` | 实体约束条件集合 |
| `capability_catalog` | 实体支持的操作/能力目录 |
| `flow_blueprint` | 业务流程蓝图 |
| `decision_matrix` | 决策条件与分支矩阵 |
| `impact_trace` | 变更影响追踪链路 |
| `prerequisite_chain` | 前置依赖链 |
| `domain_navigation` | 领域导航路径 |
| `instance_catalog` | 业务实例目录 |
| `bundle_directory` | Bundle 级目录概览 |
| `schema_inventory` | 元模型清单 |

### 统一查询引擎

`cognitionGuidance` 端点作为统一元认知查询入口，基于 `context_mode` 参数
（`BUNDLE_LEVEL` / `ENTITY_LEVEL`）自动推导上下文范围，调度相应视角维度执行器。

### 深度裁剪与 Agent 原型适配

支持三级深度裁剪：
- **L1**：概要级，仅返回核心标识与关键属性
- **L2**：标准级，返回完整语义但省略扩展引用
- **L3**：深度级，返回全量信息含关联实体引用

支持四种 Agent 原型适配：
- **execution**：执行型 Agent，侧重操作指引与参数约束
- **exploration**：探索型 Agent，侧重关系图谱与领域导航
- **audit**：审计型 Agent，侧重变更追踪与约束校验
- **orchestration**：编排型 Agent，侧重流程蓝图与依赖链

### 层级化作用域收窄

通过 `scope_mode` 参数控制作用域行为：
- **INHERITED**：包含父级领域继承的约束与元数据
- **PURE**：仅返回当前实体声明的内容，排除继承来源

### 变更影响感知

`changeWatch` 机制支持对指定 FQN 关联的变更事件进行影响范围追溯，
输出受影响的下游实体与关联链路。

### 双输出格式

所有端点支持两种输出格式，语义完全一致：
- **json**：结构化 JSON，可直接程序化消费
- **prompt**：Markdown 格式语义说明，可直接注入大模型上下文窗口

---

## MVP 边界

<!-- MVP 阶段范围界定，明确当前交付范围与后续扩展边界。-->

本 BC 在 MVP 阶段**不实现**以下能力：

- **完整授权体系**：跨 Bundle 可见性过滤的细粒度权限控制不在 MVP 范围内；
  当前阶段仅依赖上游 BC 传递的已授权数据范围。
- **向量语义匹配**：不实现基于向量语义相似度的 Bundle 匹配或实体推荐。
- **LLM 辅助推理**：不集成 LLM 进行查询意图补全、结果摘要或相关性重排序。

以上能力纳入后续迭代规划，MVP 阶段聚焦结构化认知查询引擎的核心链路验证。

---

## BC Overrides

<!-- 选择性覆盖父全局宪法的 SHOULD/MAY 级原则。每项必须引用父原则确切名称并提供 Rationale。-->

### Override 1: VI. Agent 友好型输出 (SHOULD)

- **原始父规则**：所有查询与推理结果必须输出为低理解成本的结构化格式（如 JSON Schema、
  结构化上下文块等），可直接注入 Agent 上下文，无需大模型二次解析。
- **覆盖后内容**：本 BC 直接产出面向 Agent 的元认知交付物——统一根 JSON 结构化输出与
  Markdown 语义说明（prompt 格式，可直接注入大模型上下文窗口），两种格式语义完全一致。
  输出以 FQN 为核心标识，内联完整语义，消费端无需二次查询。
- **Rationale**：本 BC 本质是 Agent 认知中间层，Agent 友好型输出是其核心交付契约，
  应由本 BC 原生承担而非依赖上层转换。json + prompt 双格式确保同时满足程序化消费
  与 LLM 上下文注入两种路径。

---

**BC Constitution Version**: 1.0.0 | **Created**: 2026-08-01 | **Last Amended**: 2026-08-01
<!-- BC 宪法采用独立语义化版本管理，与全局宪法版本解耦。-->
