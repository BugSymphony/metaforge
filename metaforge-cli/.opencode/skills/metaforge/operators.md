# MetaForge 算子语义目录

> 本目录是 MetaForge 认知服务 6 个场景模板所用全部算子的语义契约，供 AI 在"判别该选哪些算子"时参考。
> 消费端内置（先跑通），后续迁上游由服务端暴露为算子目录端点。

## 使用说明

每个算子一条结构化语义，字段含义：

| 字段 | 说明 |
|------|------|
| `id` | 算子唯一标识（`selectOperators` 使用） |
| `category` | 分类（ontological / procedural / deontic / capability / relational / governance） |
| `语义` | 一句话：这个算子算什么、回答什么问题 |
| `适用锚点` | 对什么实体类型有意义（Task=任务 / ExecutionStep=执行步骤 / DecisionStep=决策步骤 / Capability=能力 / 任意=通用） |
| `关键输出` | 结果 data 的核心字段 |
| `何时选` | 用户诉求信号（判断该不该选这个算子的依据） |

---

## 模板 → 算子总览

| 模板 | 算子 |
|------|------|
| DISCOVER（元模型发现） | bundle-discovery, package-explorer, entity-schema-inventory, relation-schema-inventory |
| ORIENT（业务域定位） | domain-drilldown |
| BRIEF（实体/任务全景） | entity-profile, flow-blueprint, adjacent-step, rule-listing, tool-discovery, direct-link |
| GUIDE（单步执行指南） | entity-profile, rule-listing, tool-discovery, protocol-detail, adjacent-step, decision-branch, direct-link |
| FORECAST（变更影响链路） | neighborhood, impact-forward, impact-backward, risk-assessment, constraint-check, regression-scope |
| DELEGATE（子任务委派） | scope-narrowing |

---

## ontological（本体发现与实体画像）

### ontological.bundle-discovery
- **语义**：列出平台已发布的语义包（Bundle）清单。
- **适用锚点**：任意（平台级，无实体锚点）
- **关键输出**：lazy 节点列表 `{data:{fqn,name,owner,description}, has_children, suggested_next_call}`
- **何时选**：用户问"平台有哪些库/包"、盘点平台总览。

### ontological.package-explorer
- **语义**：按层级探索 Package 命名空间树（需 parent_fqn 锚点）。
- **适用锚点**：任意（Bundle/Package 为锚点）
- **关键输出**：lazy 节点列表 `{data:{fqn,description,depth,bundleVersionFqn}, has_children, suggested_next_call}`
- **何时选**：用户问"某库下有哪些包/命名空间"、下钻包结构。

### ontological.entity-schema-inventory
- **语义**：盘点 EntitySchema 类型清单，含各类型实例数量与关键属性。
- **适用锚点**：任意（Bundle/Package 为锚点）
- **关键输出**：lazy 节点列表 `{data:{schema:{fqn,name,description,enabled}, key_attributes, instance_count}, has_children, suggested_next_call}`
- **何时选**：用户问"平台有哪些实体类型/各类型有多少实例"。

### ontological.relation-schema-inventory
- **语义**：盘点 RelationSchema 类型清单（关系端点与关联类型）。
- **适用锚点**：任意（Bundle/Package 为锚点）
- **关键输出**：lazy 节点列表 `{data:{fqn,name,description,associationType,enabled}, has_children}`
- **何时选**：用户问"平台有哪些关系类型/谁连谁"。

### ontological.domain-drilldown
- **语义**：沿主题域树（L1-L5 + Task + Agent）逐层下钻，返回当前父节点的子节点按 entity_type 分组。
- **适用锚点**：任意（域/业务对象/任务/Agent 为锚点）
- **关键输出**：`{children_grouped: {entitySchemaFqn: [节点...]}, level}`
- **何时选**：用户问"某域下有什么 / 定位业务域 / 下钻到任务或 Agent"。**FQN 解析（域/任务/Agent）的主要数据源**。

### ontological.entity-profile
- **语义**：实体完整画像——content 全属性字段平铺 + 名称/描述/所属 EntitySchema + 领域定位路径。
- **适用锚点**：任意实体
- **关键输出**：`{entity:{fqn,name,description,entitySchemaFqn,...content字段}, domain_location:[...]}`
- **何时选**：用户问"这个实体是什么 / 有哪些属性"，需要实体完整详情。

---

## procedural（流程与执行）

### procedural.flow-blueprint
- **语义**：解析任务端到端步骤序列，标注每一步 ENTRY/STEP/DECISION/EXIT；支持起点步骤/起点决策步骤/起点子任务三类入口。
- **适用锚点**：Task（任务）
- **关键输出**：`{annotated_path:[{fqn,name,entitySchemaFqn,marker,relationType}], length, entryStepFqn, entityFqn}`
- **何时选**：用户问"任务的流程/步骤顺序是什么"。

### procedural.adjacent-step
- **语义**：当前执行单元的前一步（入边）与后一步（出边），PROCESS_SEQUENCE 全类型。
- **适用锚点**：ExecutionStep / DecisionStep（步骤）
- **关键输出**：`{current, previous:[关系], next:[关系]}`
- **何时选**：用户问"这一步的上一步/下一步是什么"。

### procedural.decision-branch
- **语义**：决策分支——目标是 DecisionStep 时输出决策内容（条件/推荐/依据）+ 后继分支 + 所属任务；目标是 ExecutionStep 时识别下游决策步骤与简单分支。
- **适用锚点**：DecisionStep / ExecutionStep（决策或执行步骤）
- **关键输出**：`{targetType, current(决策内容+successors), downstreamDecision, branches, decisionSteps, taskFqn}`
- **何时选**：用户问"这一步怎么决策 / 有哪些分支 / 决策后去哪"。

---

## deontic（约束与规则）

### deontic.rule-listing
- **语义**：适用于该实体的约束规则清单（约束级别、条件、执行动作）。
- **适用锚点**：ExecutionStep（执行步骤）
- **关键输出**：`{rules:[{fqn,name,constraint_level,condition,action,...}], count, entityFqn}`
- **何时选**：用户问"这一步有哪些约束/规则要遵守"。

### deontic.constraint-check
- **语义**：评估变更（MODIFY/DELETE/CREATE）在影响范围内是否触及约束规则，判定冲突与阻断。
- **适用锚点**：任意实体（变更起点）
- **关键输出**：`{change_type, conflicts:[{ruleFqn,constraintLevel,condition,impact}], conflict_count, blocking}`
- **何时选**：用户问"改动会不会违反规则/合规检查"。

---

## capability（能力与工具）

### capability.tool-discovery
- **语义**：实体关联的能力/工具清单（Agent/Task/Step → Capability）。
- **适用锚点**：Task / ExecutionStep / Agent
- **关键输出**：`{capabilities:[{fqn,name,entitySchemaFqn}], count, entityFqn}`
- **何时选**：用户问"执行它需要哪些能力/工具"。

### capability.protocol-detail
- **语义**：能力接口协议细节（HTTP endpoint / MCP 工具 / CLI / 本地方法），含 interface_spec 结构化展开。
- **适用锚点**：Capability（能力）
- **关键输出**：`{protocol:{type,endpoint,method}, interface_spec, protocol_subtypes:[...]}`
- **何时选**：用户问"这个能力的接口协议细节/怎么调用"。

### capability.regression-scope
- **语义**：根据变更正向影响范围，反查关联的能力/步骤/任务/协议，形成回归验证清单。
- **适用锚点**：任意实体（变更起点）
- **关键输出**：`{affected_capabilities, affected_steps, affected_tasks, regression_checklist, count}`
- **何时选**：用户问"改动后哪些要回归验证"。

---

## relational（关系与影响）

### relational.direct-link
- **语义**：实体 1 度出边与入边，按方向分组。
- **适用锚点**：任意实体
- **关键输出**：`{outbound:[关系], inbound:[关系], entityFqn}`
- **何时选**：用户问"这个实体直接关联了谁"。

### relational.neighborhood
- **语义**：实体 N 度邻域内的关联实体列表（默认 2 度，双向）。
- **适用锚点**：任意实体
- **关键输出**：`{entities:[{fqn,name,entitySchemaFqn}], maxDepth, entityFqn}`
- **何时选**：用户问"围绕它的关联视野有哪些"。

### relational.impact-forward
- **语义**：从变更起点沿出边 BFS 扩散，列出受影响实体及影响规模。
- **适用锚点**：任意实体（变更起点）
- **关键输出**：`{direction, forward_diffusion:{totalImpacted,entities}, count, maxDepth}`
- **何时选**：用户问"改动它会影响谁"。

### relational.impact-backward
- **语义**：从变更起点沿入边逆 BFS 追溯，列出依赖方及依赖规模。
- **适用锚点**：任意实体（变更起点）
- **关键输出**：`{direction, backward_trace:{totalImpacted,entities}, count, maxDepth}`
- **何时选**：用户问"谁依赖它 / 改它谁受影响"。

### relational.risk-assessment
- **语义**：综合影响规模/依赖强度/约束冲突加权打分，输出风险等级与处理建议。
- **适用锚点**：任意实体（变更起点）
- **关键输出**：`{risk_level(HIGH/MEDIUM/LOW), risk_score, factors:{impact_scope,dependency_strength,constraint_conflicts}, recommendation}`
- **何时选**：用户问"变更风险多大 / 要不要审批"。

---

## governance（治理与边界）

### governance.scope-narrowing
- **语义**：以任务为锚点收窄子任务认知边界——流程邻域遍历 + 实体收集 + Schema 反查去重，产出收窄后的 updated_scope。
- **适用锚点**：Task（任务/子任务）
- **关键输出**：`{blueprint_scope, entityFqns, schemas, entryFqn, updated_scope:{bundles,entity_schemas}}`
- **何时选**：用户问"把子任务委派出去，它该知道什么（认知边界）"。**产出 updatedScope 供子 Agent 使用**。
