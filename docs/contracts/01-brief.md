# BRIEF 模板契约文档 —— 任务/实体全景

> 获取单个实体或任务的完整信息概览——从实体属性画像到流程蓝图、约束规则、关联能力与直连关系，形成结构化全景视图。

- **templateId**: `BRIEF`
- **入口**: `POST http://localhost:8080/api/v1/cognition/BRIEF`
- **公共约定**: 见 [00-common.md](./00-common.md)（请求体、scope、响应结构、错误码）

---

## 1. 模板定位

BRIEF 是"实体/任务全景速览"服务。给定一个实体 FQN（任务、步骤、能力、Agent、域等），一次性返回该实体的：

- **属性画像**（完整 content 字段 + 类型结构 + 领域定位路径）
- **流程蓝图**（若为任务，展开端到端步骤序列）
- **前后步导航**（若为步骤，前一步/后一步）
- **约束规则清单**（适用于该实体的 ExecutionRule）
- **关联能力/工具清单**
- **直连关系**（1 度出入边）

**适用场景**
- Agent 接到新任务/新实体时，先 BRIEF 建立"这是什么"的完整认知。
- 编排器在 DELEGATE 收窄 scope 后，用 BRIEF 在子 Agent 范围内获取细节（见 DELEGATE 文档的协作链路）。
- 人工排查实体属性、流程走向、关联依赖时的全景速览。

---

## 2. 请求

### 2.1 请求体

```json
{
  "scope": { "bundles": ["metaforge:1.0.0"], "packages": [], "domainGroups": [], "domains": [], "entitySchemas": [] },
  "params": {
    "entity_fqn": "metaforge:1.0.0.agent.Task_InventoryCheck",
    "selectOperators": [],
    "cursor": 0,
    "page_size": 20
  },
  "format": "JSON",
  "cognitionDepth": "L3",
  "agentArchetype": "EXECUTION",
  "maxTokens": 8000
}
```

### 2.2 params 参数

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `entity_fqn` | string | **是** | - | 目标实体全限定名（FQN） |
| `selectOperators` | array | 否 | `[]` | 算子子集。空=全部算子 |
| `cursor` | integer | 否 | 0 | 分页页码（1-based） |
| `page_size` | integer | 否 | 20 | 每页数量（1-100） |

### 2.3 scopeBehavior

| 项 | 值 | 说明 |
|----|----|------|
| acceptsScope | true | 接受可选 scope |
| scopeRequired | false | 不强制 scope |
| producesUpdatedScope | false | 不产出更新 scope |
| scopeFields | `[bundles, domains]` | 仅 bundles/domains 维度参与过滤 |

---

## 3. 响应

### 3.1 响应体

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "template": "BRIEF",
    "contextMeta": { "template": "BRIEF", "scopeApplied": { }, "tokenEstimate": 8000 },
    "dimensions": [
      { "operatorId": "ontological.entity-profile", "name": "实体画像", "category": "ONTOLOGICAL", "data": { }, "success": true },
      { "operatorId": "procedural.flow-blueprint", "name": "流程蓝图", "category": "PROCEDURAL", "data": { }, "success": true },
      { "operatorId": "procedural.adjacent-step", "name": "前后步导航", "category": "PROCEDURAL", "data": { }, "success": true },
      { "operatorId": "deontic.rule-listing", "name": "规则清单", "category": "DEONTIC", "data": { }, "success": true },
      { "operatorId": "capability.tool-discovery", "name": "工具发现", "category": "CAPABILITY", "data": { }, "success": true },
      { "operatorId": "relational.direct-link", "name": "直连关系", "category": "RELATIONAL", "data": { }, "success": true }
    ],
    "format": "JSON",
    "updatedScope": null
  },
  "traceId": "..."
}
```

### 3.2 算子列表（dimensions 顺序 = 优先级降序）

| operatorId | 名称 | 优先级 | 是否强制 | 适用 archetype |
|-----------|------|--------|----------|----------------|
| ontological.entity-profile | 实体画像 | 100 | **是** | execution, exploration, audit, orchestration |
| procedural.flow-blueprint | 流程蓝图 | 90 | 否 | execution, orchestration |
| procedural.adjacent-step | 前后步导航 | 80 | 否 | execution, orchestration |
| deontic.rule-listing | 规则清单 | 70 | 否 | execution, audit |
| capability.tool-discovery | 工具发现 | 50 | 否 | execution, exploration |
| relational.direct-link | 直连关系 | 30 | 否 | execution, exploration, audit |

> 说明：`procedural.flow-blueprint` / `procedural.adjacent-step` 对"任务/执行步骤"语义最强；对非步骤实体（如 Agent、域）返回空或降级结果。`relational.direct-link` 通用。

---

## 4. 算子详解

### 4.1 ontological.entity-profile（实体画像）— 必选

**功能**：返回实体完整画像——content 全属性字段平铺、名称、描述、所属 EntitySchema、领域定位路径（所在 bundle/package/domain 链）。

**适用场景**：任何实体；了解"它是什么、属性值有哪些"。

**输出 data**：

```json
{
  "entity": {
    "fqn": "metaforge:1.0.0.agent.Task_InventoryCheck",
    "name": "库存盘点任务",
    "description": "库存盘点任务——以入口步 CheckInventory 起始的库存检查流程",
    "entitySchemaFqn": "metaforge:1.0.0.agent.Task",
    "delegation_depth_limit": 1,
    "priority_default": "HIGH",
    "estimated_complexity": "MODERATE"
  },
  "domain_location": ["metaforge:1.0.0.agent.Task_InventoryCheck"]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `entity` | object | 实体摘要 + content 字段平铺（字段随实体类型变化） |
| `domain_location` | array | 领域定位路径 |

---

### 4.2 procedural.flow-blueprint（流程蓝图）

**功能**：以任务锚点为起点，沿执行单元流程关系（`StepHasNextStep`/`StepHasNextDecisionStep`/`StepHasNextTask`/`DecisionStepHasNext*`/`TaskHasNextStep`，即全部 `PROCESS_SEQUENCE`）解析端到端步骤序列，标注每一步的 ENTRY/STEP/DECISION/EXIT 标记。

**输入 config**（模板固定）：
- `taskEntitySchemaFqn` = `metaforge:1.0.0.agent.Task`（识别任务锚点）
- `entitySchemaFqn` = `metaforge:1.0.0.agent.ExecutionStep`

**支持三种任务起点**（`findEntryStep` 按序解析）：
1. 起点普通步骤（`TaskHasEntryStep`）
2. 起点决策步骤（`TaskHasEntryDecisionStep`）
3. 起点子任务（`TaskHasEntrySubtask`，递归解析子任务入口）

**适用场景**：任务锚点；展示任务完整执行链路（含跨层级子任务/下游任务节点）。

**输出 data**：

```json
{
  "annotated_path": [
    { "fqn": "metaforge:1.0.0.agent.Step_CheckInventory", "name": "检查库存",
      "description": "检查库存——入口步骤，读取订单对应库存数量",
      "entitySchemaFqn": "metaforge:1.0.0.agent.ExecutionStep",
      "marker": "ENTRY", "relationType": null },
    { "fqn": "...Step_VerifyStock", "marker": "STEP", "relationType": "PROCESS_SEQUENCE" }
  ],
  "length": 3,
  "entityFqn": "metaforge:1.0.0.agent.Task_InventoryCheck",
  "entryStepFqn": "metaforge:1.0.0.agent.Step_CheckInventory"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `annotated_path` | array | 步骤序列；每节点 `{fqn, name, description, entitySchemaFqn, marker, relationType, sourceEntityFqn, targetEntityFqn}` |
| `marker` | string | `ENTRY`（起点）/ `STEP`（中间，1 个后继）/ `DECISION`（中间，>1 后继）/ `EXIT`（终点） |
| `length` | integer | 序列长度 |
| `entryStepFqn` | string | 解析出的入口步骤 FQN |
| `entityFqn` | string | 原始锚点 |

> 分支场景（>1 后继）取第一个后继作为主路径；多后继节点标 `DECISION`。

---

### 4.3 procedural.adjacent-step（前后步导航）

**功能**：返回当前实体的前一步（1 度入边）与后一步（1 度出边），以 `relationType=PROCESS_SEQUENCE` 统一查询，自动覆盖执行步骤/决策步骤/子任务。

**适用场景**：步骤/决策步骤锚点；了解"上一步是什么、下一步去哪"。

**输出 data**：

```json
{
  "current": "metaforge:1.0.0.agent.Step_CheckInventory",
  "previous": [],
  "next": [
    { "fqn": "metaforge:1.0.0.agent.Rel_StepCheckToVerify", "name": "StepCheckToVerify",
      "relationType": "PROCESS_SEQUENCE", "relationSchemaFqn": "metaforge:1.0.0.agent.StepHasNextStep",
      "sourceEntityFqn": "...Step_CheckInventory", "targetEntityFqn": "...Step_VerifyStock" }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `current` | string | 当前实体 FQN |
| `previous` | array | 前驱关系列表（入边） |
| `next` | array | 后继关系列表（出边） |

---

### 4.4 deontic.rule-listing（规则清单）

**功能**：返回适用于该实体的约束规则（`RuleAppliesTo` 入边关联的 `ExecutionRule`）。

**输入 config**（模板固定）：
- `relationSchemaFqn` = `metaforge:1.0.0.agent.RuleAppliesTo`
- `entitySchemaFqn` = `metaforge:1.0.0.agent.ExecutionRule`

**适用场景**：执行步骤锚点；了解该步骤受哪些约束（MANDATORY/RECOMMENDED、条件、动作）。

**输出 data**：

```json
{
  "rules": [
    { "fqn": "metaforge:1.0.0.agent.Rule_InventoryAboveZero", "name": "库存必须大于零",
      "entitySchemaFqn": "metaforge:1.0.0.agent.ExecutionRule",
      "constraint_level": "MANDATORY", "condition": "库存数量 <= 0",
      "action": "must_trigger_restock", "exception": "force_majeure",
      "applicable_scenarios": ["盘点", "出库"] }
  ],
  "count": 1,
  "entityFqn": "metaforge:1.0.0.agent.Step_VerifyStock"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `rules` | array | 规则列表（含 content 字段平铺） |
| `count` | integer | 规则数 |
| `entityFqn` | string | 锚点实体 |

---

### 4.5 capability.tool-discovery（工具发现）

**功能**：返回实体关联的能力/工具清单。模板配置三个关系源：
- `AgentHasCapability`（Agent→Capability）
- `TaskRequiresCapability`（Task→Capability）
- `StepUsesCapability`（Step→Capability）

**适用场景**：任务/步骤/Agent 锚点；了解"执行它需要哪些能力/工具"。

**输出 data**：

```json
{
  "capabilities": [ { "fqn": "metaforge:1.0.0.agent.Cap_InventoryAPI", "name": "库存查询API", "entitySchemaFqn": "metaforge:1.0.0.agent.Capability" } ],
  "count": 1,
  "entityFqn": "metaforge:1.0.0.agent.Step_CheckInventory"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `capabilities` | array | 能力摘要列表 |
| `count` | integer | 能力数 |
| `entityFqn` | string | 锚点实体 |

---

### 4.6 relational.direct-link（直连关系）

**功能**：返回实体 1 度出边与入边，按关系类型/方向分组。

**适用场景**：通用；了解实体的直接关联网络（邻居）。

**输出 data**：

```json
{
  "outbound": [ { "fqn": "...", "name": "...", "relationType": "COMPOSITION", "relationSchemaFqn": "...", "targetEntityFqn": "..." } ],
  "inbound": [],
  "entityFqn": "metaforge:1.0.0.agent.Step_CheckInventory"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `outbound` | array | 出边关系列表 |
| `inbound` | array | 入边关系列表 |
| `entityFqn` | string | 锚点实体 |

---

## 5. 错误场景

| 场景 | 结果 |
|------|------|
| 缺少 `entity_fqn` | 各算子返回 failure（required 算子失败 → 10000） |
| `selectOperators` 含模板未声明算子 | 34014 |
| `entity_fqn` 不在 scope 范围 | 34004 |
| 模板不存在 | 34001 |

---

## 6. 完整示例

```bash
curl -s -X POST http://localhost:8080/api/v1/cognition/BRIEF \
  -H 'Content-Type: application/json' \
  -d '{
    "scope": {"bundles":["metaforge:1.0.0"],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]},
    "params": {"entity_fqn":"metaforge:1.0.0.agent.Task_InventoryCheck"},
    "format":"JSON","cognitionDepth":"L3","agentArchetype":"EXECUTION","maxTokens":8000
  }'
```

返回 `data.dimensions` 含 6 个算子结果（实体画像 + 流程蓝图 + 前后步 + 规则 + 工具 + 直连）。
