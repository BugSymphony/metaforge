# MetaForge 认知引擎（agent-cognition）API 公共约定

> 本文档定义调用任意场景模板（BRIEF / DELEGATE / DISCOVER / FORECAST / GUIDE / ORIENT）的公共约定，各模板专属契约见对应文档。

## 1. 服务入口

```
POST http://localhost:8080/api/v1/cognition/{templateId}
Content-Type: application/json
```

| 要素 | 说明 |
|------|------|
| Base URL | `http://localhost:8080` |
| 路径 | `/api/v1/cognition/{templateId}` |
| HTTP 方法 | `POST`（无 GET 入口，所有查询均为 POST） |
| templateId | `BRIEF` / `DELEGATE` / `DISCOVER` / `FORECAST` / `GUIDE` / `ORIENT` |
| 响应格式 | JSON（`ApiResponse` 统一包装） |

## 2. 请求体（CognitionRequest）

```json
{
  "scope": {
    "bundles": ["metaforge:1.0.0"],
    "packages": [],
    "domainGroups": [],
    "domains": [],
    "entitySchemas": []
  },
  "params": { },
  "format": "JSON",
  "cognitionDepth": "L3",
  "agentArchetype": "EXECUTION",
  "maxTokens": 8000
}
```

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `scope` | object | 否 | 空 | 认知边界声明（见 §3）。不传 = 全量（`Scope.EMPTY`） |
| `params` | object | 否 | `{}` | 模板级输入参数（`entity_fqn`、`parent_fqn`、`selectOperators` 等），见各模板文档 |
| `format` | string | 否 | `JSON` | 输出格式：`JSON` / `PROMPT` |
| `cognitionDepth` | string | 否 | `L2` | 认知深度：`L1`/`L2`/`L3` |
| `agentArchetype` | string | 否 | `EXECUTION` | 智能体原型：`EXECUTION`/`EXPLORATION`/`AUDIT`/`ORCHESTRATION`。请求的原型必须被模板任一算子支持，否则 34012 |
| `maxTokens` | integer | 否 | 8000 | token 预算上限 |

### 2.1 `selectOperators`（params 内的算子选择）

- 所有模板支持在 `params.selectOperators` 中声明"本次要执行的算子子集"（数组，元素为 operatorId）。
- `selectOperators` 为空或缺省 → 执行模板声明的**全部**算子。
- `selectOperators` 指定的 operatorId 必须属于该模板声明的算子集合，否则返回 **34014 INVALID_OPERATOR_SELECTION**。
- 示例：`{"selectOperators":["ontological.bundle-discovery"]}` 仅执行 bundle 发现。

## 3. scope（认知边界）

`scope` 是一个 5 维过滤声明，用于把查询限制在特定认知范围内：

| 维度 | 含义 | 示例 |
|------|------|------|
| `bundles` | 语义包（Bundle）FQN 列表 | `["metaforge:1.0.0"]` |
| `packages` | 包（Package）FQN 列表 | `["metaforge:1.0.0.agent"]` |
| `domainGroups` | 主题域分组 FQN | `["metaforge:1.0.0.common.Group_Fulfillment"]` |
| `domains` | 业务域 FQN | `["metaforge:1.0.0.common.Domain_Inventory"]` |
| `entitySchemas` | 实体类型（EntitySchema）FQN | `["metaforge:1.0.0.agent.Task"]` |

- 空 scope（全部维度为空数组）表示无边界。
- 模板 `scopeBehavior` 定义了：是否接受 scope（`acceptsScope`）、是否必须提供（`scopeRequired`）、是否产出更新 scope（`producesUpdatedScope`）、哪些维度生效（`scopeFields`）。

## 4. 响应结构（ApiResponse<CognitionResponse>）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "template": "BRIEF",
    "contextMeta": {
      "template": "BRIEF",
      "versionAnchors": [],
      "scopeApplied": { },
      "tokenEstimate": 8000,
      "generatedAt": "2026-08-15T01:22:04.275061187Z",
      "skippedEntities": [],
      "truncatedPerspectives": []
    },
    "dimensions": [
      {
        "operatorId": "ontological.entity-profile",
        "name": "实体画像",
        "category": "ONTOLOGICAL",
        "description": "...",
        "data": { },
        "success": true
      }
    ],
    "format": "JSON",
    "updatedScope": null
  },
  "traceId": "b5109e0768ab4e80b53406208078c8ca"
}
```

### 4.1 `data`（CognitionResponse）字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `template` | string | 执行的模板 ID |
| `contextMeta` | object | 元信息：`scopeApplied`（实际应用的 scope）、`tokenEstimate`、`generatedAt`、`skippedEntities`、`truncatedPerspectives` 等 |
| `dimensions` | array | **扁平算子结果列表**。每个元素含 `operatorId`/`name`/`category`/`description`/`data`/`success`。**注意：算子并行执行，结果无先后依赖** |
| `format` | string | 实际输出格式 |
| `updatedScope` | object \| null | 仅 DELEGATE（`producesUpdatedScope=true`）产出收窄后的 scope，其他模板为 `null` |

### 4.2 `dimensions[].data`

- 每个算子的业务数据负载，结构见各模板文档的"算子输出"章节。
- **特例**：DISCOVER 模板的若干算子（bundle-discovery / package-explorer / entity-schema-inventory / relation-schema-inventory）`data` 直接是**实体列表（array）**，而非对象。

## 5. 公共错误码

| 错误码 | 名称 | 触发场景 |
|--------|------|----------|
| 34001 | TEMPLATE_NOT_FOUND | 模板未注册或不可用 |
| 34002 | TEMPLATE_INVALID | 模板定义不合法 |
| 34003 | INVALID_SCOPE | scope 中声明的 bundle/package/entitySchema 无效或不存在 |
| 34004 | ENTITY_OUT_OF_SCOPE | 请求中的 entityFqn 不在声明的 scope 范围内 |
| 34005 | MISSING_SCOPE | 模板要求 scope（如 DELEGATE）但请求未提供 |
| 34006 | OPERATOR_EXECUTION_ERROR | 算子执行异常 |
| 34007 | OPERATOR_TIMEOUT | 算子执行超时 |
| 34008 | UPSTREAM_UNAVAILABLE | 上游 BC 服务不可用或超时 |
| 34009 | UNSUPPORTED_OPERATOR | 模板引用的 operatorId 无匹配注册算子 |
| 34010 | INVALID_FORMAT | 请求的 format 不受支持 |
| 34011 | UNKNOWN_OPERATOR_REF | 模板中 operatorId 的分类前缀不存在 |
| 34012 | ARCHETYPE_NOT_SUPPORTED | 请求的 agentArchetype 不被模板任一算子支持 |
| 34013 | INVALID_LEVEL | 请求的 level 无法解析为有效 EntitySchema 类型 |
| 34014 | INVALID_OPERATOR_SELECTION | selectOperators 无任何算子匹配模板声明 |
| 10000 | INTERNAL_ERROR | 系统内部错误（未处理异常） |

错误响应示例：

```json
{ "code": 34014, "message": "请求的 operators [ontological.unknown] 无任何算子匹配模板 DISCOVER 的声明" }
```

## 6. 核心元模型（agent 库）背景

模板针对 MetaForge agent 库（bundle `metaforge:1.0.0`）建模，关键实体类型与关系：

| 类型 | EntitySchema | 说明 |
|------|--------------|------|
| 任务 | `agent.Task` | 可委派的执行单元 |
| 执行步骤 | `agent.ExecutionStep` | 具体执行步骤（含 ENTRY/STEP/EXIT 标记） |
| 决策步骤 | `agent.DecisionStep` | 决策步骤（自然语言条件承载分支/循环走向） |
| 能力 | `agent.Capability` | 能力/工具 |
| 约束规则 | `agent.ExecutionRule` | 声明式约束（含 `constraint_level`：MANDATORY/RECOMMENDED） |

| 关键关系 | 类型 | 语义 |
|----------|------|------|
| `TaskHasEntryStep` | COMPOSITION | 任务→起点执行步骤（一个任务一个起点） |
| `TaskHasEntryDecisionStep` | COMPOSITION | 任务→起点决策步骤 |
| `TaskHasEntrySubtask` | COMPOSITION | 任务→起点子任务 |
| `StepHasNextStep` / `StepHasNextDecisionStep` / `StepHasNextTask` | PROCESS_SEQUENCE | 执行步骤后继 |
| `DecisionStepHasNextStep` / `DecisionStepHasNextDecisionStep` / `DecisionStepHasNextTask` | PROCESS_SEQUENCE | 决策步骤后继 |
| `TaskHasNextStep` | PROCESS_SEQUENCE | 子任务完成后回父流程步骤 |
| `StepUsesCapability` | ASSOCIATION_REFERENCE | 步骤使用能力 |
| `RuleAppliesTo` / `RuleAppliesToTask` | ASSOCIATION_REFERENCE | 约束适用于步骤/任务 |
