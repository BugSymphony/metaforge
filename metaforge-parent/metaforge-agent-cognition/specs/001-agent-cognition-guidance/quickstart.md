# Quickstart Validation Guide: Agent 元认知指导层

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Data Model**: [data-model.md](./data-model.md)

本指南提供 `metaforge-agent-cognition` BC 的端到端验证场景。该 BC 为纯无状态计算/编排层——无数据库、无 Flyway、无持久化，所有运行时数据从 4 个上游 BC 即席获取。

---

## 1. 前提条件

| 前提 | 版本/说明 |
|------|----------|
| JDK | 21 |
| Maven | 3.9+ |
| PostgreSQL | 16（已运行，上游 BC 的 Flyway 迁移已执行） |
| 上游 BC 数据 | metamodel-governance、metadata-management、semantic-relation-network、metaforge-compute-engine 已构建并注册到 metaforge-boot，数据已就绪 |
| foundation-core | `metaforge-common`、`metaforge-framework`、`metaforge-server` 已构建 |

---

## 2. 构建与注册

### 2.1 构建 foundation-core

```bash
cd /data/ext/source-8/metaforge/metaforge-parent
mvn clean install -pl metaforge-common,metaforge-framework,metaforge-server -am -DskipTests
```

**预期**: `BUILD SUCCESS`，三个 foundation-core 模块编译通过。

### 2.2 注册 BC 到平台构建体系

**在 `metaforge-parent/pom.xml` 的 `<modules>` 中添加**：

```xml
<module>metaforge-agent-cognition</module>
```

**在 `metaforge-boot/pom.xml` 的 `<dependencies>` 中添加**：

```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-agent-cognition-core</artifactId>
</dependency>
```

### 2.3 构建本 BC

```bash
cd /data/ext/source-8/metaforge/metaforge-parent
mvn clean install -pl metaforge-agent-cognition -am
```

**预期**: `BUILD SUCCESS`，api 与 core 模块编译通过，代码扫描通过，单元测试通过。

---

## 3. 配置

在 `metaforge-boot/src/main/resources/application.yml` 中添加 BC 配置：

```yaml
metaforge:
  agent-cognition:
    templates:
      base-path: classpath:cognition/
      cache:
        enabled: true
        ttl: 30m
        max-size: 50
    perspectives:
      base-path: classpath:cognition/
      cache:
        enabled: true
        ttl: 30m
        max-size: 20
      timeout-ms: 200
    query:
      max-bundle-fqns: 20
      max-perspectives: 14
      default-depth: L2
      default-tokens: 8000
      min-tokens-for-auto-degrade: 500
    traversal:
      max-composition-depth: 5
      max-relationship-degree: 3
      max-impact-depth: 3
    format:
      default: json
      allowed:
        - json
        - prompt
```

> 本 BC 不配置 `spring.datasource.*`、`spring.flyway.*`（无数据库）。

---

## 4. 启动应用

```bash
cd /data/ext/source-8/metaforge/metaforge-parent
mvn spring-boot:run -pl metaforge-boot
```

**预期**: 控制台输出含 TraceId 的结构化日志，日志中出现 `metaforge-agent-cognition` 相关 Bean 初始化信息，最终显示 `Started MetaforgeApplication`。

---

## 5. 验证场景

以下场景假设上游 BC 已填充测试数据（参考各上游 BC 的 quickstart 完成数据准备）。

---

### 场景 1: 健康检查 — agent-cognition 就绪

**验证**: `/actuator/health` 报告 agent-cognition 组件健康状态为 UP。

```bash
curl -s http://localhost:8080/actuator/health | jq .
```

**预期响应**:
```json
{
  "status": "UP",
  "components": {
    "agent-cognition": {
      "status": "UP",
      "details": {
        "message": "YAML 配置加载正常，所有视角可用"
      }
    }
  }
}
```

**验证点**:
- [ ] `agent-cognition` 组件出现在 health 响应中
- [ ] `status` 为 `UP`
- [ ] YAML 模板配置与视角配置加载正常

---

### 场景 2: Bundle 目录查询

**验证**: 调用 `bundle-catalog` 端点，返回平台已发布 Bundle 列表及其主题域树。

```bash
curl -s -X POST http://localhost:8080/api/v1/cognition/bundle-catalog \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

**预期响应结构**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "contextMeta": {
      "bundleFqns": [],
      "contextMode": "BUNDLE_LEVEL",
      "dataVersionAnchors": [],
      "queriedAt": "2026-08-01T..."
    },
    "bundleDirectory": {
      "bundles": [
        {
          "fqn": "...",
          "name": "...",
          "description": "...",
          "domainTree": []
        }
      ]
    }
  },
  "traceId": "..."
}
```

**验证点**:
- [ ] 返回 `ApiResponse<T>` 标准格式（code/message/data/traceId）
- [ ] `data.bundleDirectory.bundles` 为已发布 Bundle 列表
- [ ] 每个 Bundle 含 `fqn`、`name`、`description` 字段

---

### 场景 3: 一站式任务简报 — 10 视角全覆盖

**验证**: 调用 `task-brief`，传入已知 Bundle FQN，返回 10 个认知视角的完整简报。

```bash
curl -s -w "\n%{time_total}s\n" -X POST http://localhost:8080/api/v1/cognition/task-brief \
  -H "Content-Type: application/json" \
  -d '{
    "bundleFqns": ["order:1.0.0"],
    "cognitionDepth": "L2",
    "agentArchetype": "execution"
  }' | jq .
```

**预期响应**: 10 个视角章节全部返回，响应时间 < 500ms。

| 视角 | 字段路径 | 预期行为 |
|------|---------|---------|
| entity_profile | `data.entityProfile` | 含 FQN、name、content、schemaAttributes |
| domain_location | `data.domainLocation` | 含从实体沿 COMPOSITION 入边的归属路径 |
| composition_tree | `data.compositionTree` | 含 rootFQN 及树形节点 |
| relationship_graph | `data.relationshipGraph` | 按 AssociationType 分组的关系列表 |
| constraint_set | `data.constraintSet` | 含 constraints + hardBoundaries + softBoundaries |
| capability_catalog | `data.capabilityCatalog` | 含 capabilities 列表 |
| flow_blueprint | `data.flowBlueprint` | 基于 PROCESS_SEQUENCE 的步骤序列 |
| decision_matrix | `data.decisionMatrix` | 含决策点及其可选路径 |
| impact_trace | `data.impactTrace` | 含 forwardImpact + backwardDependency |
| prerequisite_chain | `data.prerequisiteChain` | 含层级化依赖树 |

**验证点**:
- [ ] `contextMeta.cognitionDepth` = `L2`
- [ ] `contextMeta.appliedPerspectives` 含 10 个视角编码
- [ ] 每个视角章节结构完整（空章节标注 `empty: true`）
- [ ] 总响应时间 ≤ 500ms
- [ ] `dataVersionAnchors` 非空

---

### 场景 4: 实体即时指导 — 实体级过滤

**验证**: 调用 `step-guide`，传入已知实体 FQN，仅返回与该实体直接相关的认知内容。

```bash
curl -s -X POST http://localhost:8080/api/v1/cognition/step-guide \
  -H "Content-Type: application/json" \
  -d '{
    "entityFqn": "order:1.0.0.pkg_order.Order_001"
  }' | jq .
```

**预期**: 仅返回实体级 6 个视角（entity_profile、constraint_set、capability_catalog、decision_matrix、impact_trace、relationship_graph）+ adjacent_context。Bundle 级视角（flow_blueprint、domain_navigation 等）在 `contextMeta.skippedPerspectives` 中标注。

**验证点**:
- [ ] `contextMeta.contextMode` = `ENTITY_LEVEL`
- [ ] `contextMeta.skippedPerspectives` 含 `flow_blueprint`、`bundle_directory` 等 Bundle 级视角
- [ ] `adjacentContext` 含 `previousSteps`、`nextSteps`
- [ ] constraint_set 仅含关联到 `Order_001` 的约束（经图边过滤）
- [ ] capability_catalog 仅含关联到 `Order_001` 的能力

---

### 场景 5: Prompt 格式输出 — Markdown 语义等价

**验证**: 以 `format: "prompt"` 调用，验证 Markdown 输出与 JSON 输出语义一致。

```bash
# 先用 JSON 格式获取一份输出
curl -s -X POST http://localhost:8080/api/v1/cognition/task-brief \
  -H "Content-Type: application/json" \
  -d '{
    "bundleFqns": ["order:1.0.0"],
    "cognitionDepth": "L1",
    "format": "json"
  }' > /tmp/task-brief-json.json

# 再用 prompt 格式获取
curl -s -X POST http://localhost:8080/api/v1/cognition/task-brief \
  -H "Content-Type: application/json" \
  -H "Accept: text/markdown" \
  -d '{
    "bundleFqns": ["order:1.0.0"],
    "cognitionDepth": "L1",
    "format": "prompt"
  }' > /tmp/task-brief-prompt.md

# 验证：JSON 中的实体 FQN 出现在 Markdown 中
grep -o '"fqn":\s*"[^"]*"' /tmp/task-brief-json.json | head -3
grep -o 'order:1\.0\.0\.[^ \n]*' /tmp/task-brief-prompt.md | head -3
```

**验证点**:
- [ ] Prompt 格式输出为 Markdown 文本
- [ ] JSON 中的关键 FQN、实体名、约束描述均出现在 Prompt 中
- [ ] 两种格式的视角章节数量一致
- [ ] contextMeta 版本锚信息在两种格式中一致

---

### 场景 6: L1 深度裁剪 — 最多 3 视角、≤2000 Token

**验证**: 以 `cognition_depth: "L1"` 调用，验证视角数量上限与 Token 预算。

```bash
curl -s -X POST http://localhost:8080/api/v1/cognition/task-brief \
  -H "Content-Type: application/json" \
  -d '{
    "bundleFqns": ["order:1.0.0"],
    "cognitionDepth": "L1"
  }' | jq '.data | {
    depth: .contextMeta.cognitionDepth,
    perspectiveCount: (.contextMeta.appliedPerspectives | length),
    tokenCount: .contextMeta.totalTokenCount,
    trimmed: .contextMeta.tokenTrimmed
  }'
```

**预期**:
```json
{
  "depth": "L1",
  "perspectiveCount": 3,
  "tokenCount": 1500,
  "trimmed": false
}
```

**验证点**:
- [ ] `appliedPerspectives` 数量 ≤ 3
- [ ] `totalTokenCount` ≤ 2000
- [ ] `cognitionDepth` = `L1`
- [ ] 若 max_tokens < 500 则自动降为 L1 并在 contextMeta 中标注

---

### 场景 7: 非法模板 ID — 34001 错误

**验证**: 传入不存在的 templateId，返回对应错误码。

```bash
curl -s -X POST http://localhost:8080/api/v1/cognition/invalid-template-id \
  -H "Content-Type: application/json" \
  -d '{"bundleFqns": ["order:1.0.0"]}' | jq .
```

**预期响应**:
```json
{
  "code": 34001,
  "message": "无效的模板标识: invalid-template-id",
  "data": null,
  "traceId": "..."
}
```

**验证点**:
- [ ] `code` = `34001`
- [ ] `message` 提示模板 ID 无效
- [ ] 响应复用 `ApiResponse<T>` 格式

---

### 场景 8: 非法 Bundle FQN — 34002 错误

**验证**: 传入不存在的或格式非法的 Bundle FQN，返回对应错误码。

```bash
curl -s -X POST http://localhost:8080/api/v1/cognition/task-brief \
  -H "Content-Type: application/json" \
  -d '{
    "bundleFqns": ["nonexistent:999.999.999"]
  }' | jq .
```

**预期响应**:
```json
{
  "code": 34002,
  "message": "Bundle FQN 不存在或格式非法",
  "data": null,
  "traceId": "..."
}
```

**验证点**:
- [ ] `code` = `34002`
- [ ] `message` 提示 Bundle FQN 无效
- [ ] 传入空列表 `/api/v1/cognition/task-brief -d '{"bundleFqns": []}'` 同样返回 34002

---

### 场景 9（附加）: 其他错误码验证

快速验证其他错误码：

```bash
# entity_fqn 前缀不属于已发布 Bundle → 34003
curl -s -X POST http://localhost:8080/api/v1/cognition/step-guide \
  -H "Content-Type: application/json" \
  -d '{"entityFqn": "ghost:1.0.0.pkg.FakeEntity"}' | jq '.code'

# 非法 cognition_depth → 34008
curl -s -X POST http://localhost:8080/api/v1/cognition/task-brief \
  -H "Content-Type: application/json" \
  -d '{"bundleFqns": ["order:1.0.0"], "cognitionDepth": "L99"}' | jq '.code'

# 非法 agent_archetype → 34009
curl -s -X POST http://localhost:8080/api/v1/cognition/task-brief \
  -H "Content-Type: application/json" \
  -d '{"bundleFqns": ["order:1.0.0"], "agentArchetype": "invalid"}' | jq '.code'
```

---

### 场景 10: 自由视角组合查询 — cognitionGuidance

**验证**: 通过统一入口按需指定任意视角组合，验证引擎按 scope 规则正确激活/跳过视角。

```bash
# 指定 3 个跨 scope 视角（entity + bundle + both）
curl -s -X POST http://localhost:8080/api/v1/cognition/cognition-guidance \
  -H "Content-Type: application/json" \
  -d '{
    "bundleFqns": ["order:1.0.0"],
    "perspectives": ["entity_profile", "flow_blueprint", "constraint_set"]
  }' | jq '.data | {applied: .contextMeta.appliedPerspectives, skipped: .contextMeta.skippedPerspectives}'
```

**验证点**:
- [ ] 若未传 entity_fqn → flow_blueprint(BUNDLE)、entity_profile(BOTH)、constraint_set(BOTH) 全部激活
- [ ] 若传入 entity_fqn → flow_blueprint 出现在 `skippedPerspectives`
- [ ] `appliedPerspectives` 与请求的 perspectives 一致（scope 兼容）

---

### 场景 11: 代理原型排序 — 四种 Archetype 对比

**验证**: 四种代理原型产生不同的视角优先序。

```bash
# execution 原型 — 约束和蓝图前置
curl -s -X POST http://localhost:8080/api/v1/cognition/task-brief \
  -H "Content-Type: application/json" \
  -d '{"bundleFqns": ["order:1.0.0"], "agentArchetype": "execution", "cognitionDepth": "L2"}' \
  | jq '.data.contextMeta.appliedPerspectives[0:3]'

# exploration 原型 — 组成结构和关系图谱前置
curl -s -X POST http://localhost:8080/api/v1/cognition/task-brief \
  -H "Content-Type: application/json" \
  -d '{"bundleFqns": ["order:1.0.0"], "agentArchetype": "exploration", "cognitionDepth": "L2"}' \
  | jq '.data.contextMeta.appliedPerspectives[0:3]'

# audit 原型 — 约束和影响追溯前置
curl -s -X POST http://localhost:8080/api/v1/cognition/task-brief \
  -H "Content-Type: application/json" \
  -d '{"bundleFqns": ["order:1.0.0"], "agentArchetype": "audit", "cognitionDepth": "L2"}' \
  | jq '.data.contextMeta.appliedPerspectives[0:3]'

# orchestration 原型 — 流程蓝图和决策图谱前置
curl -s -X POST http://localhost:8080/api/v1/cognition/task-brief \
  -H "Content-Type: application/json" \
  -d '{"bundleFqns": ["order:1.0.0"], "agentArchetype": "orchestration", "cognitionDepth": "L2"}' \
  | jq '.data.contextMeta.appliedPerspectives[0:3]'
```

**验证点**:
- [ ] execution 原型首视角为 `constraint_set`
- [ ] exploration 原型首视角为 `composition_tree`
- [ ] audit 原型首视角为 `constraint_set`
- [ ] orchestration 原型首视角为 `flow_blueprint`
- [ ] 未知原型回退 execution

---

### 场景 12: L3 全量深度 — 14 视角全展开

**验证**: L3 深度返回全部 14 个视角，总 Token 可能触发裁剪。

```bash
curl -s -X POST http://localhost:8080/api/v1/cognition/task-brief \
  -H "Content-Type: application/json" \
  -d '{
    "bundleFqns": ["order:1.0.0"],
    "cognitionDepth": "L3",
    "maxTokens": 8000
  }' | jq '.data | {
    depth: .contextMeta.cognitionDepth,
    count: (.contextMeta.appliedPerspectives | length),
    tokens: .contextMeta.totalTokenCount,
    trimmed: .contextMeta.tokenTrimmed,
    chapters: (.contextMeta.appliedPerspectives)
  }'
```

**验证点**:
- [ ] `cognitionDepth` = `L3`
- [ ] `appliedPerspectives` 包含全部 14 个视角编码
- [ ] 若 Token 超限则 `tokenTrimmed` = `true`
- [ ] 未知深度回退 L2

---

### 场景 13: 渐进式导航 — navigate 端点

**验证**: navigate 端点支持懒加载逐层下钻。

```bash
# 顶层导航（anchorFqn 为空，从 L1 开始）
curl -s -X POST http://localhost:8080/api/v1/cognition/navigate \
  -H "Content-Type: application/json" \
  -d '{
    "level": "L1",
    "pageSize": 5,
    "expand": "lazy"
  }' | jq '.data.domainNavigation | {currentLevel, hasMore, childCount: (.children | length)}'

# 下钻到 L2（传入 L1 锚点 FQN）
curl -s -X POST http://localhost:8080/api/v1/cognition/navigate \
  -H "Content-Type: application/json" \
  -d '{
    "anchorFqn": "order:1.0.0.order_subdomain_group",
    "level": "L2",
    "pageSize": 5,
    "expand": "lazy"
  }' | jq '.data.domainNavigation | {children: [.children[].name], hasMore}'

# 全量展开（不懒加载）
curl -s -X POST http://localhost:8080/api/v1/cognition/navigate \
  -H "Content-Type: application/json" \
  -d '{
    "anchorFqn": "order:1.0.0.order_subdomain_group",
    "level": "L2",
    "expand": "all"
  }' | jq '.data.domainNavigation | {children: [.children[].name], hasMore}'
```

**验证点**:
- [ ] 每层返回 `currentLevel`、`children`、`hasMore`
- [ ] `expand=lazy` 时 `children` 仅含概要（无深层内容），`hasMore` 可能为 true
- [ ] `expand=all` 时全量展开，`hasMore=false`
- [ ] 分页游标 `nextCursor` 可继续翻页

---

### 场景 14: 层级化子任务 — subTaskBrief 收窄

**验证**: INHERITED 模式三层收窄后简报范围精确限定。

```bash
# INHERITED 收窄（入口实体为蓝图某步骤）
curl -s -X POST http://localhost:8080/api/v1/cognition/sub-task-brief \
  -H "Content-Type: application/json" \
  -d '{
    "entryEntityFqn": "order:1.0.0.pkg_order.UserValidation_Step002",
    "scopeMode": "INHERITED",
    "cognitionDepth": "L2"
  }' | jq '.data | {
    mode: .contextMeta.scopeMode,
    perspectives: .contextMeta.appliedPerspectives,
    skipped: .contextMeta.skippedPerspectives
  }'

# PURE 模式（仅 entity_profile）
curl -s -X POST http://localhost:8080/api/v1/cognition/sub-task-brief \
  -H "Content-Type: application/json" \
  -d '{
    "entryEntityFqn": "order:1.0.0.pkg_order.UserValidation_Step002",
    "scopeMode": "PURE"
  }' | jq '.data | {
    mode: .contextMeta.scopeMode,
    perspectives: .contextMeta.appliedPerspectives
  }'
```

**验证点**:
- [ ] INHERITED 模式 `appliedPerspectives` 仅含 scope=BOTH 且经三层收窄过滤的视角
- [ ] PURE 模式 `appliedPerspectives` 仅含 `entity_profile`
- [ ] 同级子任务的简报内容相互隔离
- [ ] scope_mode 为必填字段，缺失时返回错误

---

### 场景 15: Token 预算触发自动降级

**验证**: max_tokens < 500 时自动降为 L1。

```bash
# 请求 L2 但 max_tokens=300（低于 500 阈值）
curl -s -X POST http://localhost:8080/api/v1/cognition/task-brief \
  -H "Content-Type: application/json" \
  -d '{
    "bundleFqns": ["order:1.0.0"],
    "cognitionDepth": "L2",
    "maxTokens": 300
  }' | jq '.data.contextMeta | {
    depth: .cognitionDepth,
    count: (.appliedPerspectives | length),
    tokens: .totalTokenCount,
    trimmed: .tokenTrimmed
  }'
```

**验证点**:
- [ ] `cognitionDepth` 自动降为 `L1`
- [ ] `appliedPerspectives` 数量 ≤ 3
- [ ] `tokenTrimmed` = `true`
- [ ] contextMeta 中包含降级原因标注

---

### 场景 16: 跨 Bundle 多 Bundle 查询

**验证**: 同时传入多个 bundle_fqns，引擎分别处理后合并。

```bash
curl -s -X POST http://localhost:8080/api/v1/cognition/cognition-guidance \
  -H "Content-Type: application/json" \
  -d '{
    "bundleFqns": ["order:1.0.0", "payment:1.0.0"],
    "perspectives": ["constraint_set", "instance_catalog"]
  }' | jq '.data | {
    anchors: [.contextMeta.dataVersionAnchors[].bundleFqn],
    constraintCount: (.data.constraintSet.constraints | length)
  }'
```

**验证点**:
- [ ] `dataVersionAnchors` 包含两个 Bundle 的版本锚
- [ ] `constraint_set` 合并了两个 Bundle 的约束
- [ ] 来源 Bundle 在每个约束条目中标注

---

### 场景 17: 幂等性验证

**验证**: 相同参数连续两次调用返回一致内容（时间戳除外）。

```bash
for i in 1 2; do
  curl -s -X POST http://localhost:8080/api/v1/cognition/cognition-guidance \
    -H "Content-Type: application/json" \
    -d '{"bundleFqns": ["order:1.0.0"], "perspectives": ["constraint_set"]}' \
    | jq '.data.constraintSet' > /tmp/run-$i.json
done

# 比较两次结果（忽略时间戳字段）
diff <(jq 'del(.contextMeta.queriedAt)' /tmp/run-1.json) \
     <(jq 'del(.contextMeta.queriedAt)' /tmp/run-2.json)
```

**验证点**:
- [ ] 两次结果的 `dataVersionAnchors` 完全一致
- [ ] 视角章节内容（除 `queriedAt`）完全一致
- [ ] 无任务 ID、无会话状态残留

---

### 场景 18: 视角超时保护 — 200ms 单视角超时

**验证**: 单视角执行超过 200ms 时标注 truncated。

```bash
# 触发全量 14 视角 + 高深度（可能触发上游慢查询）
curl -s -X POST http://localhost:8080/api/v1/cognition/cognition-guidance \
  -H "Content-Type: application/json" \
  -d '{
    "bundleFqns": ["order:1.0.0"],
    "perspectives": ["entity_profile", "domain_location", "composition_tree",
      "relationship_graph", "constraint_set", "capability_catalog",
      "flow_blueprint", "decision_matrix", "impact_trace",
      "prerequisite_chain", "domain_navigation", "instance_catalog",
      "bundle_directory", "schema_inventory"],
    "cognitionDepth": "L3"
  }' | jq '.data.contextMeta | {truncated, truncations}'
```

**验证点**:
- [ ] 超时视角在 `truncations` 列表中
- [ ] 超时视角的 `truncatedReason` = `TIMEOUT`
- [ ] 非超时视角正常返回完整结果
- [ ] `contextMeta.truncated` = `true`

---

### 场景 19: 空视角标注验证

**验证**: Bundle 无关系实例时，perspective 返回空结构并明确标注。

```bash
# 查询一个无关系实例的 Bundle（假设 test-empty Bundle 无关系）
curl -s -X POST http://localhost:8080/api/v1/cognition/cognition-guidance \
  -H "Content-Type: application/json" \
  -d '{
    "bundleFqns": ["test-empty:1.0.0"],
    "perspectives": ["relationship_graph", "constraint_set", "flow_blueprint"]
  }' | jq '.data | {
    relEmpty: .relationshipGraph.empty,
    relNote: .relationshipGraph.emptyNote,
    constEmpty: .constraintSet.empty,
    constNote: .constraintSet.emptyNote,
    flowEmpty: .flowBlueprint.empty,
    flowNote: .flowBlueprint.emptyNote
  }'
```

**验证点**:
- [ ] 无关系实例 → `relationshipGraph.empty` = `true`，`emptyNote` 含说明
- [ ] 无约束 → `constraintSet.empty` = `true`
- [ ] 无蓝图 → `flowBlueprint.empty` = `true`
- [ ] 空视角仍然保留章节结构（标题/章节字段）

---

### 场景 20: 并发请求稳定性验证

**验证**: 5 个并发 agent 同时请求，引擎无状态隔离。

```bash
# 5 个并发 task-brief 请求，模拟多 agent 场景
for i in $(seq 1 5); do
  curl -s -X POST http://localhost:8080/api/v1/cognition/task-brief \
    -H "Content-Type: application/json" \
    -d '{"bundleFqns": ["order:1.0.0"], "cognitionDepth": "L1"}' \
    | jq '.code' &
done
wait

# 检查是否有 429 限流响应（当前 MVP 不设限流，但需记录行为）
```

**验证点**:
- [ ] 5 个请求全部返回 `200`
- [ ] 各请求的 `traceId` 互不相同
- [ ] 各请求的视角内容在数据未变更时一致
- [ ] 无并发异常、无死锁、无数据交叉污染

```bash
# 运行本 BC 全部测试（含单元测试 + 集成测试）
mvn verify -pl metaforge-agent-cognition -am

# 仅核心模块单元测试
mvn test -pl metaforge-agent-cognition/metaforge-agent-cognition-core -am

# 视角执行器集成测试
mvn test -pl metaforge-agent-cognition/metaforge-agent-cognition-core \
  -Dtest="*IntegrationTest"
```

---

## 7. 验收检查清单

### 构建与启动
- [ ] foundation-core 三个模块编译通过
- [ ] `metaforge-parent/pom.xml` `<modules>` 中已注册 `metaforge-agent-cognition`
- [ ] `metaforge-boot/pom.xml` `<dependencies>` 中已注册 `metaforge-agent-cognition-core`
- [ ] BC 编译通过：`mvn clean install -pl metaforge-agent-cognition -am`
- [ ] 应用启动成功，`agent-cognition` 相关 Bean 初始化无异常

### YAML 配置
- [ ] `cognition-templates.yml` 成功加载（HealthCheck 验证 `templatesLoaded=true`）
- [ ] `cognition-perspectives.yml` 成功加载（HealthCheck 验证 `perspectivesLoaded=true`）
- [ ] 14 个内置视角 ID 全部注册到 `PerspectiveRegistry`

### 查询端点
- [ ] `/api/v1/cognition/bundle-catalog` — Bundle 目录与主题域树返回
- [ ] `/api/v1/cognition/task-brief` — 10 视角一站式简报生成 ≤500ms
- [ ] `/api/v1/cognition/step-guide` — 实体级过滤，Bundle 级视角正确跳过
- [ ] `/api/v1/cognition/navigate` — 渐进式懒加载导航
- [ ] `/api/v1/cognition/cognitionGuidance` — 任意视角组合自由查询

### 认知深度与代理原型
- [ ] L1 深度 — 返回 ≤3 个视角
- [ ] L2 深度（默认） — 返回 ≤7 个视角
- [ ] L3 深度 — 返回全部 14 个视角
- [ ] execution 原型 — 约束和蓝图前置
- [ ] exploration 原型 — 组成结构和关系图谱前置

### 输出格式
- [ ] JSON 格式 — 结构完整，统一根结构含 contextMeta
- [ ] Prompt 格式 — Markdown 输出，语义与 JSON 完全一致
- [ ] contextMeta 含 dataVersionAnchors（每 Bundle 的版本号 + 查询时间戳）

### 错误处理
- [ ] 34001 — 无效 templateId
- [ ] 34002 — 无效 bundle FQN / 空 bundleFqns
- [ ] 34003 — entity_fqn 前缀不属于已发布 Bundle
- [ ] 34008 — 无效 cognition_depth（回退 L2）
- [ ] 34009 — 无效 agent_archetype（回退 execution）
- [ ] 单视角超时 > 200ms 时标注 truncated + TIMEOUT

### 架构合规
- [ ] 无状态：两次相同请求返回一致内容（除时间戳外）
- [ ] 无数据库：不引入 JPA/Flyway，不连接 datasource
- [ ] REST 响应复用 `ApiResponse<T>` 格式
- [ ] Actuator health 代理 `agent-cognition` 状态
- [ ] swagger-ui 中 `agent-cognition` 分组可见
- [ ] 不直接依赖 `metaforge-boot` / `metaforge-server`
- [ ] domain 层不直接依赖 Spring MVC / JPA 框架
