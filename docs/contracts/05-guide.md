# GUIDE 模板契约文档 —— 单步执行指南

> 为单步操作提供精细执行指导——包含实体画像、约束规则、能力工具、协议细节、导航与分支决策的完整上下文。目标执行单元为 ExecutionStep（执行步骤）或 DecisionStep（决策步骤）。

- **templateId**: `GUIDE`
- **入口**: `POST http://localhost:8080/api/v1/cognition/GUIDE`
- **公共约定**: 见 [00-common.md](./00-common.md)

---

## 1. 模板定位

GUIDE 是"单步执行指导"服务。给定一个**执行单元**（ExecutionStep 或 DecisionStep），GUIDE 为该步骤的执行提供精细上下文：

- **实体画像**：该步骤的属性、类型、领域定位
- **约束规则**：该步骤适用的 ExecutionRule
- **能力工具**：该步骤可用/需用的能力
- **前后步导航**：上一步/下一步（PROCESS_SEQUENCE 全类型）
- **决策分支**：
  - 目标是 **DecisionStep** → 输出决策内容（条件/推荐/依据）+ 后继分支 + 所属任务
  - 目标是 **ExecutionStep** → 识别下游决策步骤（StepHasNextDecisionStep）与简单分支
- **直连关系**：1 度出入边

**两类执行单元的 GUIDE 差异**：

| 算子 | ExecutionStep | DecisionStep |
|------|--------------|--------------|
| entity-profile | 通用 | 通用 |
| rule-listing | RuleAppliesTo 步骤约束 | 空（元模型无决策步骤约束） |
| tool-discovery | StepUsesCapability 能力 | 空（决策无工具） |
| adjacent-step | 前后步 | 前后步（PROCESS_SEQUENCE） |
| decision-branch | 下游决策 + 简单分支 | **决策内容 + 分支 + 所属任务** |
| protocol-detail / direct-link | 按需 / 通用 | 按需 / 通用 |

> 子任务（Task）不适用 GUIDE——子任务委托走 **DELEGATE** 模板。

**适用场景**
- Agent 执行某个步骤前，获取"这一步该怎么执行"的完整上下文。
- 决策步骤执行时，获取决策条件/选项/推荐及分支走向。

---

## 2. 请求

### 2.1 请求体

```json
{
  "scope": { "bundles": ["metaforge:1.0.0"], "packages": [], "domainGroups": [], "domains": [], "entitySchemas": [] },
  "params": {
    "entity_fqn": "metaforge:1.0.0.agent.DecisionStep_RiskCheck",
    "selectOperators": [
      "ontological.entity-profile", "deontic.rule-listing", "capability.tool-discovery",
      "procedural.adjacent-step", "procedural.decision-branch", "relational.direct-link"
    ]
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
| `entity_fqn` | string | **是** | - | 目标执行单元 FQN（ExecutionStep / DecisionStep） |
| `selectOperators` | array | 否 | 见下方默认 | 算子子集；**默认不含 protocol-detail**（协议详情按需选择） |

**selectOperators 默认**（不传时执行）：
```json
["ontological.entity-profile", "deontic.rule-listing", "capability.tool-discovery",
 "procedural.adjacent-step", "procedural.decision-branch", "relational.direct-link"]
```
> 协议细节（`capability.protocol-detail`）默认不执行；如需展开接口协议，显式加入 `selectOperators` 且 `entity_fqn` 指向 `Capability`。

### 2.3 scopeBehavior

| 项 | 值 | 说明 |
|----|----|------|
| acceptsScope | true | 接受可选 scope |
| scopeRequired | false | 不强制 scope |
| producesUpdatedScope | false | 不产出更新 scope |
| scopeFields | `[entity_schemas]` | 仅 entity_schemas 维度参与过滤 |

---

## 3. 响应

### 3.1 响应体

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "template": "GUIDE",
    "contextMeta": { "template": "GUIDE", "scopeApplied": { }, "tokenEstimate": 8000 },
    "dimensions": [
      { "operatorId": "ontological.entity-profile", "name": "实体画像", "category": "ONTOLOGICAL", "data": { }, "success": true },
      { "operatorId": "deontic.rule-listing", "name": "规则清单", "category": "DEONTIC", "data": { }, "success": true },
      { "operatorId": "capability.tool-discovery", "name": "工具发现", "category": "CAPABILITY", "data": { }, "success": true },
      { "operatorId": "procedural.adjacent-step", "name": "前后步导航", "category": "PROCEDURAL", "data": { }, "success": true },
      { "operatorId": "procedural.decision-branch", "name": "决策分支", "category": "PROCEDURAL", "data": { }, "success": true },
      { "operatorId": "relational.direct-link", "name": "直连关系", "category": "RELATIONAL", "data": { }, "success": true }
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
| ontological.entity-profile | 实体画像 | 100 | **是** | execution |
| deontic.rule-listing | 规则清单 | 90 | **是** | execution, audit |
| capability.tool-discovery | 工具发现 | 60 | 否 | execution |
| capability.protocol-detail | 协议细节 | 40 | 否（默认不执行） | execution |
| procedural.adjacent-step | 前后步导航 | 30 | 否 | execution |
| procedural.decision-branch | 决策分支 | 20 | 否 | execution |
| relational.direct-link | 直连关系 | 10 | 否 | execution, audit |

---

## 4. 算子详解

### 4.1 ontological.entity-profile（实体画像）— 必选

同 [01-brief.md](./01-brief.md) §4.1。

**输出 data**：
```json
{
  "entity": { "fqn": "...", "name": "风控决策", "entitySchemaFqn": "metaforge:1.0.0.agent.DecisionStep", "condition_expression": "风控结论是否为 HIGH", "recommended_option": "拦截支付", "rationale": "高风险交易需人工复核或拦截", "priority": 1 },
  "domain_location": ["..."]
}
```

### 4.2 deontic.rule-listing（规则清单）— 必选

**输入 config**：`relationSchemaFqn=RuleAppliesTo`，`entitySchemaFqn=ExecutionRule`。

**注意**：仅 ExecutionStep 有关联规则（`RuleAppliesTo` 端点限定 ExecutionStep）；对 DecisionStep 返回空（决策无直接约束）。

**输出 data**：同 [01-brief.md](./01-brief.md) §4.4。

### 4.3 capability.tool-discovery（工具发现）

**输入 config**：`AgentHasCapability` / `TaskRequiresCapability` / `StepUsesCapability` → `Capability`。

**注意**：DecisionStep 无能力关联（`StepUsesCapability` 端点限定 ExecutionStep），返回空。

**输出 data**：同 [01-brief.md](./01-brief.md) §4.5。

### 4.4 capability.protocol-detail（协议细节）— 默认不执行

**功能**：读取能力 `content.interface_spec` 结构化展开，并按关系 schema 前缀（`metaforge:1.0.0.protocol.`）查询能力引用的协议实例（HTTP/MCP/CLI/LocalMethod）。

**适用场景**：需展开接口协议时显式选择；`entity_fqn` 指向 `Capability`。

**输出 data**：
```json
{
  "protocol": { "type": "HTTP", "endpoint": "/api/inventory/query", "method": "GET" },
  "interface_spec": { },
  "protocol_subtypes": [ { "fqn": "metaforge:1.0.0.protocol.Http_InventoryQuery", "type": "HTTP", "endpoint": "..." } ]
}
```

### 4.5 procedural.adjacent-step（前后步导航）

**功能**：以 `relationType=PROCESS_SEQUENCE` 统一查询 1 度前后驱，自动覆盖 ExecutionStep / DecisionStep 前后导航。

**输出 data**：同 [01-brief.md](./01-brief.md) §4.3（previous/next/current）。

### 4.6 procedural.decision-branch（决策分支）

**功能**：按目标执行单元类型**自适应**：
- **DecisionStep 本体**：输出 `current`（决策内容 condition_expression/recommended_option/rationale + successors 分支）+ `taskFqn`（所属任务）+ `decisionSteps`（任务下其他决策步骤，不重复自身）。
- **ExecutionStep**：`downstreamDecision`（StepHasNextDecisionStep 出边的下游决策步骤）+ `branches`（StepHasNextStep 出边 >1 的简单分支）+ `decisionSteps`（任务级决策上下文）。
- **Task**：解析起点决策步骤（TaskHasEntryDecisionStep）及其分支。

**适用场景**：决策步骤执行时看"怎么决策、决策后去哪"；执行步骤看"后面是否有决策关口"。

**输出 data（DecisionStep 本体）**：
```json
{
  "targetType": "metaforge:1.0.0.agent.DecisionStep",
  "isDecisionBranch": false,
  "branchCount": 1,
  "current": {
    "fqn": "metaforge:1.0.0.agent.DecisionStep_RiskCheck",
    "name": "风控决策",
    "entitySchemaFqn": "metaforge:1.0.0.agent.DecisionStep",
    "condition_expression": "风控结论是否为 HIGH",
    "recommended_option": "拦截支付",
    "rationale": "高风险交易需人工复核或拦截",
    "successors": [ { "targetFqn": "metaforge:1.0.0.agent.Step_ConfirmPayment", "relationSchemaFqn": "...DecisionStepHasNextStep", "condition": "无" } ]
  },
  "decisionSteps": [],
  "taskFqn": "metaforge:1.0.0.agent.Task_PaymentValidation",
  "entityFqn": "metaforge:1.0.0.agent.DecisionStep_RiskCheck"
}
```

**输出 data（ExecutionStep）**：
```json
{
  "targetType": "metaforge:1.0.0.agent.ExecutionStep",
  "isDecisionBranch": false,
  "branchCount": 1,
  "branches": [],
  "downstreamDecision": [ { "fqn": "...DecisionStep_DemoGate1", "name": "演示决策步骤1", "condition_expression": "...", "successors": [...] } ],
  "decisionSteps": [],
  "entityFqn": "metaforge:1.0.0.agent.Step_DemoWork"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `targetType` | string | 目标实体类型（DecisionStep/ExecutionStep/Task） |
| `current` | object | 仅 DecisionStep 本体：决策内容 + successors 分支 |
| `downstreamDecision` | array | 仅 ExecutionStep：下游决策步骤 |
| `branches` | array | 简单分支（StepHasNextStep >1） |
| `decisionSteps` | array | 任务级决策步骤上下文（DecisionStep 本体时不含自身） |
| `taskFqn` | string | 所属任务（DecisionStep 本体） |
| `isDecisionBranch` / `branchCount` | - | 分支判定 |

### 4.7 relational.direct-link（直连关系）

同 [01-brief.md](./01-brief.md) §4.6。

---

## 5. 错误场景

| 场景 | 结果 |
|------|------|
| 缺少 `entity_fqn` | 10000（required 算子失败） |
| `selectOperators` 含模板未声明算子 | 34014 |
| entity_fqn 为 Task（子任务） | decision-branch 解析任务起点决策；其余算子对 Task 降级。子任务场景建议走 DELEGATE |

---

## 6. 完整示例

```bash
# 决策步骤 GUIDE（决策内容 + 分支 + 所属任务）
curl -s -X POST http://localhost:8080/api/v1/cognition/GUIDE \
  -H 'Content-Type: application/json' \
  -d '{
    "scope": {"bundles":["metaforge:1.0.0"],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]},
    "params": {"entity_fqn":"metaforge:1.0.0.agent.DecisionStep_RiskCheck"},
    "format":"JSON","cognitionDepth":"L3","agentArchetype":"EXECUTION","maxTokens":8000
  }'

# 执行步骤 GUIDE（含下游决策识别）
curl -s -X POST http://localhost:8080/api/v1/cognition/GUIDE \
  -H 'Content-Type: application/json' \
  -d '{
    "scope": {"bundles":["metaforge:1.0.0"],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]},
    "params": {"entity_fqn":"metaforge:1.0.0.agent.Step_DemoWork"},
    "format":"JSON","cognitionDepth":"L3","agentArchetype":"EXECUTION","maxTokens":8000
  }'
```
