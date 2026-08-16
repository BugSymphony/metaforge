# DELEGATE 模板契约文档 —— 子任务上下文委派

> 为子 Agent 收窄认知 scope——以任务入口实体为锚点，产出收窄后的认知边界（entityFqns/schemas/updated_scope），上下文详情由 BRIEF 模板承接。

- **templateId**: `DELEGATE`
- **入口**: `POST http://localhost:8080/api/v1/cognition/DELEGATE`
- **公共约定**: 见 [00-common.md](./00-common.md)

---

## 1. 模板定位

DELEGATE 是"子 Agent 认知边界收窄"服务。给定一个任务/子任务锚点，DELEGATE 计算该子任务**专属认知范围**：

- 流程邻域（锚点 + PROCESS_SEQUENCE 前后驱 + 任务入口展开）
- 范围内实体 FQN 集合
- 去重后的 EntitySchema 集合
- **收窄后的 updatedScope**（供子 Agent 继续调用 BRIEF 等模板时限定认知边界）

DELEGATE 只做**scope 收窄**（单一算子），不承载详情查询——详情由 BRIEF 在收窄 scope 下承接。这是与 BRIEF 的协作分工：

```
编排链路:
  DELEGATE(子任务)  →  updatedScope(收窄认知边界)
        ↓
  BRIEF(子任务, scope=收窄后)  →  实体画像 / 流程蓝图 / 工具等详情
```

**适用场景**
- 编排器把子任务委派给子 Agent 前，先收窄子 Agent 认知边界。
- 子 Agent 启动时用 DELEGATE 拿"我该关注什么"的范围声明。
- 与 BRIEF/DELEGATE 的协作编排（跨模板）。

---

## 2. 请求

### 2.1 请求体

```json
{
  "scope": {
    "bundles": ["metaforge:1.0.0"],
    "packages": [],
    "domainGroups": [],
    "domains": [],
    "entitySchemas": []
  },
  "params": {
    "entity_fqn": "metaforge:1.0.0.agent.Task_DemoSub",
    "task_fqn": "metaforge:1.0.0.agent.Task_DemoSub",
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
| `entity_fqn` | string | **是** | - | 任务入口实体 FQN（scope 收窄锚点） |
| `task_fqn` | string | 否 | - | 委派任务全限定标识符（可省略，与 entity_fqn 同） |
| `selectOperators` | array | 否 | `[]` | 算子子集（当前仅 `governance.scope-narrowing`） |

### 2.3 scopeBehavior

| 项 | 值 | 说明 |
|----|----|------|
| acceptsScope | true | 接受 scope |
| scopeRequired | **true** | **必须提供 scope 入参**，否则 34005 |
| producesUpdatedScope | **true** | **产出收窄后的 updatedScope**（唯一产出该字段的模板） |
| scopeFields | `[bundles, packages, domains, entity_schemas]` | 收窄涉及 4 个维度 |

---

## 3. 响应

### 3.1 响应体

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "template": "DELEGATE",
    "contextMeta": { "template": "DELEGATE", "scopeApplied": { }, "tokenEstimate": 8000 },
    "dimensions": [
      { "operatorId": "governance.scope-narrowing", "name": "范围收窄", "category": "GOVERNANCE", "data": { }, "success": true }
    ],
    "format": "JSON",
    "updatedScope": {
      "bundles": ["metaforge:1.0.0"],
      "entity_schemas": ["metaforge:1.0.0.agent.Task", "metaforge:1.0.0.agent.ExecutionStep", "metaforge:1.0.0.agent.DecisionStep"]
    }
  },
  "traceId": "..."
}
```

### 3.2 算子列表

| operatorId | 名称 | 优先级 | 是否强制 | 适用 archetype |
|-----------|------|--------|----------|----------------|
| governance.scope-narrowing | 范围收窄 | 100 | **是** | execution, orchestration |

---

## 4. 算子详解

### 4.1 governance.scope-narrowing（范围收窄）— 必选

**功能**：三层收窄：
1. **流程邻域遍历**：以锚点为中心，沿 `relationType=PROCESS_SEQUENCE` 前后遍历（覆盖执行步骤/决策步骤/子任务全部流程关系）+ Task 锚点展开入口执行单元（`TaskHasEntryStep`/`TaskHasEntryDecisionStep`/`TaskHasEntrySubtask`），多轮收敛（默认 4 轮，可用 `maxRounds` 调整）。
2. **实体 FQN 收集**：邻域内实体的关联能力（`StepUsesCapability`/`TaskRequiresCapability`）、规则（`RuleAppliesTo`）、决策步骤及其后继。
3. **Schema 反查去重**：对收集到的实体 FQN 反查 EntitySchema 并去重。

**适用场景**：任务/子任务锚点；计算子 Agent 的认知边界。

**输出 data**：

```json
{
  "blueprint_scope": ["metaforge:1.0.0.agent.Task_DemoSub", "metaforge:1.0.0.agent.Step_DemoSubInner"],
  "entityFqns": ["metaforge:1.0.0.agent.Task_DemoSub", "metaforge:1.0.0.agent.Step_DemoSubInner", "..."],
  "schemas": ["metaforge:1.0.0.agent.Task", "metaforge:1.0.0.agent.ExecutionStep", "metaforge:1.0.0.agent.DecisionStep"],
  "entryFqn": "metaforge:1.0.0.agent.Task_DemoSub",
  "updated_scope": { "bundles": ["metaforge:1.0.0"], "entity_schemas": ["metaforge:1.0.0.agent.Task", "..."] }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `blueprint_scope` | array | 流程邻域实体 FQN（蓝图范围） |
| `entityFqns` | array | 收窄范围内全部实体 FQN（含能力/规则/决策步骤） |
| `schemas` | array | 去重后的 EntitySchema FQN 集合 |
| `entryFqn` | string | 收窄锚点 |
| `updated_scope` | object | 内部视角的收窄 scope（`bundles` + `entity_schemas`） |

> 模板响应顶层 `data.updatedScope` 由该算子 `updated_scope` 聚合产出。

### 4.2 updatedScope 使用方式（与 BRIEF 协作）

把 `data.updatedScope` 作为下一次请求的 `scope` 传入 BRIEF：

```json
{
  "scope": { "bundles": ["metaforge:1.0.0"], "entitySchemas": ["metaforge:1.0.0.agent.Task", "metaforge:1.0.0.agent.ExecutionStep", "metaforge:1.0.0.agent.DecisionStep"] },
  "params": { "entity_fqn": "metaforge:1.0.0.agent.Task_DemoSub" }
}
```

---

## 5. 错误场景

| 场景 | 结果 |
|------|------|
| **未提供 scope** | **34005 MISSING_SCOPE** |
| 缺少 `entity_fqn` | 10000（required 算子失败） |
| `selectOperators` 含模板未声明算子 | 34014 |
| `entity_fqn` 不在 scope 范围 | 34004 |

---

## 6. 完整示例

```bash
curl -s -X POST http://localhost:8080/api/v1/cognition/DELEGATE \
  -H 'Content-Type: application/json' \
  -d '{
    "scope": {"bundles":["metaforge:1.0.0"],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]},
    "params": {"entity_fqn":"metaforge:1.0.0.agent.Task_DemoSub"},
    "format":"JSON","cognitionDepth":"L3","agentArchetype":"EXECUTION","maxTokens":8000
  }'
```

返回 `data.dimensions` 含 1 个算子（scope-narrowing），`data.updatedScope` 为收窄后的认知边界。
