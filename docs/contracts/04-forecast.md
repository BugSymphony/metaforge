# FORECAST 模板契约文档 —— 变更影响链路

> 评估变更的波及影响——从邻域探索、正向影响扩散、反向依赖溯源，到风险评级、约束冲突检测、回归范围建议，形成完整的变更影响分析链路。

- **templateId**: `FORECAST`
- **入口**: `POST http://localhost:8080/api/v1/cognition/FORECAST`
- **公共约定**: 见 [00-common.md](./00-common.md)

---

## 1. 模板定位

FORECAST 是"变更影响分析（Change Impact Analysis）"服务，即**变更前评估**。给定一个"将要变更的实体"（修改/删除/新增），FORECAST 回答：

- **影响视野**：该实体在 N 度邻域内关联了谁（neighborhood）
- **波及范围**：变更会正向影响谁（impact-forward）
- **依赖方**：谁依赖/引用该实体（impact-backward）
- **风险等级**：综合影响规模/依赖强度/约束冲突打分（risk-assessment）
- **规则合规**：变更是否触及约束规则（constraint-check）
- **回归清单**：需要回归验证的能力/步骤/任务（regression-scope）

**适用场景**
- Agent 修改/删除实体前评估波及范围与风险，决定是否需要审批。
- 变更流程中生成回归测试范围。
- 判断变更是否违反领域约束规则。

---

## 2. 请求

### 2.1 请求体

```json
{
  "scope": { "bundles": ["metaforge:1.0.0"], "packages": [], "domainGroups": [], "domains": [], "entitySchemas": [] },
  "params": {
    "entity_fqn": "metaforge:1.0.0.agent.Step_CheckInventory",
    "change_type": "MODIFY",
    "max_depth": 3,
    "selectOperators": []
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
| `entity_fqn` | string | **是** | - | 变更起始实体 FQN |
| `change_type` | string | 否 | MODIFY | 变更类型：`MODIFY` / `DELETE` / `CREATE`（影响 constraint-check 冲突判定） |
| `max_depth` | integer | 否 | 3 | 最大追溯深度（1-5） |
| `selectOperators` | array | 否 | `[]` | 算子子集。空=全部算子 |

### 2.3 scopeBehavior

| 项 | 值 | 说明 |
|----|----|------|
| acceptsScope | true | 接受可选 scope |
| scopeRequired | false | 不强制 scope |
| producesUpdatedScope | false | 不产出更新 scope |
| scopeFields | `[bundles]` | 仅 bundles 维度参与过滤 |

---

## 3. 响应

### 3.1 响应体

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "template": "FORECAST",
    "contextMeta": { "template": "FORECAST", "scopeApplied": { }, "tokenEstimate": 8000 },
    "dimensions": [
      { "operatorId": "relational.neighborhood", "name": "邻域探索", "category": "RELATIONAL", "data": { }, "success": true },
      { "operatorId": "relational.impact-forward", "name": "正向影响扩散", "category": "RELATIONAL", "data": { }, "success": true },
      { "operatorId": "relational.impact-backward", "name": "反向依赖溯源", "category": "RELATIONAL", "data": { }, "success": true },
      { "operatorId": "relational.risk-assessment", "name": "风险评级", "category": "RELATIONAL", "data": { }, "success": true },
      { "operatorId": "deontic.constraint-check", "name": "规则冲突检测", "category": "DEONTIC", "data": { }, "success": true },
      { "operatorId": "capability.regression-scope", "name": "回归范围建议", "category": "CAPABILITY", "data": { }, "success": true }
    ],
    "format": "JSON",
    "updatedScope": null
  },
  "traceId": "..."
}
```

### 3.2 算子列表

| operatorId | 名称 | 优先级 | 是否强制 | 适用 archetype |
|-----------|------|--------|----------|----------------|
| relational.neighborhood | 邻域探索 | 100 | **是** | execution, audit |
| relational.impact-forward | 正向影响扩散 | 95 | **是** | execution, audit |
| relational.impact-backward | 反向依赖溯源 | 90 | **是** | execution, audit |
| relational.risk-assessment | 风险评级 | 85 | 否 | execution, orchestration |
| deontic.constraint-check | 规则冲突检测 | 80 | 否 | execution, audit |
| capability.regression-scope | 回归范围建议 | 70 | 否 | execution, orchestration |

---

## 4. 算子详解

### 4.1 relational.neighborhood（邻域探索）

**功能**：返回实体 N 度（BIDIRECTIONAL）邻域内的关联实体列表，默认深度 2。

**适用场景**：快速建立"围绕变更点的关联视野"。

**输出 data**：

```json
{
  "maxDepth": 2,
  "entityFqn": "metaforge:1.0.0.agent.Step_CheckInventory",
  "entities": [
    { "fqn": "metaforge:1.0.0.agent.Step_CheckInventory", "name": "检查库存", "entitySchemaFqn": "metaforge:1.0.0.agent.ExecutionStep" },
    { "fqn": "metaforge:1.0.0.agent.Cap_InventoryAPI", "name": "库存查询API", "entitySchemaFqn": "metaforge:1.0.0.agent.Capability" }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `entities` | array | 邻域实体摘要（含锚点自身） |
| `maxDepth` | integer | 遍历深度 |
| `entityFqn` | string | 锚点 |

---

### 4.2 relational.impact-forward（正向影响扩散）

**功能**：从变更起点沿出边 BFS 扩散 N 度，列出受影响实体及影响规模。底层调用 compute-engine `diffuseForward`。

**适用场景**：回答"我改了 X，会影响到谁"。

**输出 data**：

```json
{
  "direction": "forward",
  "entityFqn": "metaforge:1.0.0.agent.Step_CheckInventory",
  "maxDepth": 3,
  "forward_diffusion": {
    "totalImpacted": 6,
    "entities": [ { "fqn": "...", "depth": 1, "affectedByTypes": ["PROCESS_SEQUENCE"] } ],
    "relations": [ ],
    "layerStats": { },
    "typeStats": { },
    "truncated": false
  },
  "count": 6
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `forward_diffusion` | object | `ImpactTraceResult`：`totalImpacted`、`entities[]`（fqn/depth/affectedByTypes）、`relations[]`、`layerStats`、`typeStats`、`truncated` |
| `count` | integer | 影响实体总数（= totalImpacted） |
| `direction` / `maxDepth` / `entityFqn` | - | 执行参数回显 |

### 4.3 relational.impact-backward（反向依赖溯源）

**功能**：从变更起点沿入边逆 BFS 追溯 N 度，列出依赖方及依赖规模。底层调用 compute-engine `traceBackward`。

**适用场景**：回答"谁依赖/引用 X，我改了 X 会影响谁"。

**输出 data**：

```json
{
  "direction": "backward",
  "entityFqn": "metaforge:1.0.0.agent.Step_CheckInventory",
  "maxDepth": 3,
  "backward_trace": { "totalImpacted": 8, "entities": [ ], "relations": [ ], "layerStats": { }, "typeStats": { }, "truncated": false },
  "count": 8
}
```

字段语义同 4.2（`backward_trace` + `count`）。

> 历史：原 `relational.impact-trace`（both）已拆分为 impact-forward / impact-backward，`direction` 参数不再需要（算子选择即表达方向）。兼容别名保留。

---

### 4.4 relational.risk-assessment（风险评级）

**功能**：综合影响规模（forward 实体数）、依赖强度（backward 直接/传递依赖数）、约束冲突（影响范围内规则数）加权打分，输出风险等级与处理建议。

**权重**（模板默认）：impact 0.5 / dependency 0.3 / constraint 0.2；各因子得分 = `min(count, 饱和值) / 饱和值 × 100`（影响饱和 10、依赖饱和 10、约束饱和 3）。

**分级**：`risk_score ≥ 70` → HIGH；`40-69` → MEDIUM；`< 40` → LOW。

**适用场景**：变更决策——高影响需审批、中影响需复核、低影响直接执行。

**输出 data**：

```json
{
  "entityFqn": "metaforge:1.0.0.agent.Task_DelegationDemo",
  "risk_level": "LOW",
  "risk_score": 23.0,
  "factors": {
    "impact_scope":        { "count": 4, "weight": 0.5, "score": 40 },
    "dependency_strength": { "total": 1, "direct_dependents": 0, "transitive": 1, "weight": 0.3, "score": 10 },
    "constraint_conflicts": { "count": 0, "weight": 0.2, "score": 0 }
  },
  "recommendation": "可直接执行",
  "maxDepth": 3
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `risk_level` | string | `HIGH` / `MEDIUM` / `LOW` |
| `risk_score` | number | 0-100 加权分值 |
| `factors` | object | 三个因子各自的 count/weight/score |
| `recommendation` | string | 处理建议（可直接执行/需复核/需人工审批） |

---

### 4.5 deontic.constraint-check（规则冲突检测）

**功能**：评估变更（change_type）在影响范围内是否触及约束规则（`RuleAppliesTo`/`RuleAppliesToTask` 关联的 ExecutionRule），按规则级别（如 MANDATORY）与变更类型判定冲突与阻断。

**冲突判定规则**：
- `MANDATORY` 且 `DELETE` → `高冲突`（blocking=true）
- `DELETE` → `可能违反`
- `MODIFY` → `需检查`
- `CREATE` → `低风险`

**适用场景**：变更前合规检查——判断变更是否触碰关键约束（如"库存必须大于零"）。

**输出 data**：

```json
{
  "entityFqn": "metaforge:1.0.0.agent.Step_CheckInventory",
  "change_type": "MODIFY",
  "conflicts": [
    { "ruleFqn": "metaforge:1.0.0.agent.Rule_InventoryAboveZero", "ruleName": "库存必须大于零",
      "constraintLevel": "MANDATORY", "condition": "库存数量 <= 0",
      "appliedEntityFqn": "metaforge:1.0.0.agent.Step_VerifyStock",
      "relationSchemaFqn": "metaforge:1.0.0.agent.RuleAppliesTo",
      "impact": "需检查" }
  ],
  "conflict_count": 1,
  "blocking": false
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `conflicts` | array | 影响范围内触达的规则（含级别/条件/影响判定） |
| `conflict_count` | integer | 规则数 |
| `blocking` | boolean | 是否存在 `高冲突`（阻断性冲突） |

---

### 4.6 capability.regression-scope（回归范围建议）

**功能**：根据正向影响范围，反查关联的执行单元（能力/步骤/任务，能力附加协议引用），形成回归验证清单。

**适用场景**：变更后生成回归测试范围。

**输出 data**：

```json
{
  "entityFqn": "metaforge:1.0.0.agent.Step_CheckInventory",
  "affected_capabilities": [ { "fqn": "metaforge:1.0.0.agent.Cap_InventoryAPI", "name": "库存查询API", "entitySchemaFqn": "metaforge:1.0.0.agent.Capability", "protocolFqn": "metaforge:1.0.0.protocol.Http_InventoryQuery" } ],
  "affected_steps": [ { "fqn": "metaforge:1.0.0.agent.Step_CheckInventory", "name": "检查库存", "entitySchemaFqn": "metaforge:1.0.0.agent.ExecutionStep" } ],
  "affected_tasks": [ { "fqn": "metaforge:1.0.0.agent.Task_InventoryCheck", "name": "库存盘点任务", "entitySchemaFqn": "metaforge:1.0.0.agent.Task" } ],
  "regression_checklist": ["库存查询API 需回归", "检查库存 需回归", "库存盘点任务 需回归"],
  "count": 6,
  "maxDepth": 3
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `affected_capabilities` | array | 影响范围内的能力（含协议引用 protocolFqn） |
| `affected_steps` | array | 影响范围内的执行步骤 |
| `affected_tasks` | array | 影响范围内的任务 |
| `regression_checklist` | array | 可读回归清单 |
| `count` | integer | 三类实体总数 |

---

## 5. 错误场景

| 场景 | 结果 |
|------|------|
| 缺少 `entity_fqn` | 10000（required 算子失败） |
| `selectOperators` 含模板未声明算子 | 34014 |
| `change_type` 非法（不在枚举内） | 按 MODIFY 兜底或算子失败 |

---

## 6. 完整示例

```bash
curl -s -X POST http://localhost:8080/api/v1/cognition/FORECAST \
  -H 'Content-Type: application/json' \
  -d '{
    "scope": {"bundles":["metaforge:1.0.0"],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]},
    "params": {"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory","change_type":"MODIFY","max_depth":3},
    "format":"JSON","cognitionDepth":"L3","agentArchetype":"EXECUTION","maxTokens":8000
  }'
```

返回 `data.dimensions` 含 6 个算子结果（邻域 + 正向影响 + 反向依赖 + 风险 + 冲突检测 + 回归范围）。
