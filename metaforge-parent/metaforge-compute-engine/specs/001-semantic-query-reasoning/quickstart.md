# Quickstart Validation Guide: 语义查询与推理引擎

**Feature**: 001-semantic-query-reasoning
**Date**: 2026-08-01

---

## 前提条件

- Java 21 + Maven 3.8+
- Docker（用于 TestContainers 集成测试，自动启动 PostgreSQL 容器）
- 或本地 PostgreSQL 16 实例（配置见 `application.yml`）

---

## 1. 模块脚手架搭建

### 1.1 创建 Maven 模块结构

```bash
REPO_ROOT=/data/ext/source-8/metaforge/metaforge-parent

# 1. 创建 BC 根模块
mkdir -p $REPO_ROOT/metaforge-compute-engine

# 2. 创建 api 子模块
mkdir -p $REPO_ROOT/metaforge-compute-engine/metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/{constant,dto/{request,response,common},enums,service,event}
mkdir -p $REPO_ROOT/metaforge-compute-engine/metaforge-compute-engine-api/src/test/java/com/metaforge/computeengine/api

# 3. 创建 core 子模块（DDD 分层）
mkdir -p $REPO_ROOT/metaforge-compute-engine/metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/{application/service,domain/{model/{aggregate,entity,valueobject},port,service,exception},infrastructure/{config,persistence/jooq/converter,gateway,mapper,spi},interfaces/{rest,mcp}}
mkdir -p $REPO_ROOT/metaforge-compute-engine/metaforge-compute-engine-core/src/main/resources
mkdir -p $REPO_ROOT/metaforge-compute-engine/metaforge-compute-engine-core/src/test/java/com/metaforge/computeengine
```

### 1.2 创建模块 POM 文件

| 模块 | 关键配置 |
|------|---------|
| `metaforge-compute-engine/pom.xml` | parent=`metaforge-parent`，packaging=`pom`，modules=[api, core] |
| `metaforge-compute-engine-api/pom.xml` | parent=BC根，依赖=`metaforge-framework` |
| `metaforge-compute-engine-core/pom.xml` | parent=BC根，依赖=api + `metaforge-framework` + `metaforge-metamodel-api` + `metaforge-metadata-api` + `metaforge-graph-api` + jOOQ + MapStruct |

### 1.3 注册到平台构建体系

1. 在 `metaforge-parent/pom.xml` `<modules>` 中添加 `<module>metaforge-compute-engine</module>`
2. 在 `metaforge-parent/pom.xml` `<dependencyManagement>` 中添加 `metaforge-compute-engine-api` 和 `metaforge-compute-engine-core` 的版本声明
3. 在 `metaforge-boot/pom.xml` 中添加 `metaforge-compute-engine-core` 依赖

### 1.4 创建 BC 配置

在 `metaforge-boot/src/main/resources/application-metaforge-compute-engine.yml` 中配置传导规则（见 research.md §3）。

---

## 2. 验证场景（按 FR 顺序）

### 场景 1: 多度邻接查询（FR-001）

**前提**: `metadata_entity` 有实体 `order:1.0.0.pkg_order.Order_001`，有关系边关联到若干邻居

**验证命令**: 启动应用后调用 API
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/adjacency \
  -H "Content-Type: application/json" \
  -d '{
    "sourceFqn": "order:1.0.0.pkg_order.Order_001",
    "direction": "FORWARD",
    "maxDepth": 3,
    "relationTypes": null,
    "filterCriteria": null
  }'
```

**预期结果**: `truncated=false`，`entities` 数组包含起点及 3 度内邻居，`relations` 数组包含关联关系边，无重复实体。

---

### 场景 2: 组合层级树查询（FR-002 上溯父链）

**验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/composition-tree \
  -H "Content-Type: application/json" \
  -d '{
    "rootFqn": "order:1.0.0.pkg_order.Item_003",
    "direction": "BACKWARD",
    "maxDepth": 10,
    "filterCriteria": null
  }'
```

**预期结果**: 返回从 Item_003 向上追溯的完整 COMPOSITION 父链（扁平列表）。

---

### 场景 3: 图模式匹配（FR-004）

**验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/pattern-match \
  -H "Content-Type: application/json" \
  -d '{
    "pattern": "* -[?]-> * -[?]-> *",
    "maxResults": 100
  }'
```

**预期结果**: 返回匹配 2 跳路径的所有实例，通配符 `*` 匹配任意 EntitySchema FQN，`?` 匹配任意 RelationSchema FQN。

---

### 场景 4: 7 维过滤（FR-015）

**验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/adjacency \
  -H "Content-Type: application/json" \
  -d '{
    "sourceFqn": "order:1.0.0.pkg_order.Order_001",
    "direction": "BOTH",
    "maxDepth": 5,
    "relationTypes": ["COMPOSITION"],
    "filterCriteria": {
      "associationTypes": ["COMPOSITION"],
      "sourceFqns": {"values": ["order:1.0.0."], "matchMode": "PREFIX"},
      "targetFqns": null,
      "relationInstanceFqns": null,
      "entityTypes": {"values": ["order:1.0.0.pkg_order.Order", "order:1.0.0.pkg_order.Item"], "matchMode": "EXACT"},
      "relationTypes": null,
      "propertyFilters": null
    }
  }'
```

**预期结果**: 同时满足 associationTypes + sourceFqns 前缀 + entityTypes 精确匹配三个维度的交集。

---

### 场景 5: 传递闭包推理（FR-009）

**验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/closure \
  -H "Content-Type: application/json" \
  -d '{
    "sourceFqn": "order:1.0.0.pkg_order.Order_001",
    "relationTypes": null,
    "filterCriteria": null
  }'
```

**预期结果**: 按层级分组的可达实体列表，仅包含配置中 `transitive=true` 的关系类型展开的实体。

---

### 场景 6: 多跳语义推理（FR-010）

**验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/multi-hop \
  -H "Content-Type: application/json" \
  -d '{
    "sourceFqn": "order:1.0.0.pkg_order.Order_001",
    "hopSteps": [
      {"relationType": "COMPOSITION", "direction": "FORWARD"},
      {"relationType": "ASSOCIATION_REFERENCE", "direction": "FORWARD"}
    ],
    "filterCriteria": null
  }'
```

**预期结果**: 返回沿 COMPOSITION→ASSOCIATION_REFERENCE 展开的跨语义推理路径。

---

### 场景 7: 结果截断标记（FR-023）

**验证方法**: 设置极小的 `maxDepth=1` 和一个存在深层关系的起点实体，观察返回的 `truncated=true, truncatedReason=DEPTH_EXCEEDED`。

---

### 场景 8: per-type 深度差异化（FR-020 + 传导规则配置）

**验证方法**: 配置中将 ASSOCIATION_REFERENCE 的 `max-depth: 1`，COMPOSITION 的 `max-depth: 5`。执行包含两种类型边的遍历，验证 ASSOCIATION_REFERENCE 边在第 1 度后被截断，而 COMPOSITION 边继续扩展至第 5 度。

---

### 场景 9: 子图提取查询（FR-003）

**前提**: 图中有 2 个以上中心实体，各自带有若干关联边。

**验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/subgraph \
  -H "Content-Type: application/json" \
  -d '{
    "centerFqns": ["order:1.0.0.pkg_order.Order_001", "order:1.0.0.pkg_order.Item_003"],
    "maxDepth": 2,
    "filterCriteria": null
  }'
```

**预期结果**: 返回两个中心实体 2 度范围内的实体与关系并集，实体去重、关系去重，`adjacencyMap` 覆盖所有返回实体。

---

### 场景 10: 多条件复合检索（FR-005）

**验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/search \
  -H "Content-Type: application/json" \
  -d '{
    "entityTypes": ["order:1.0.0.pkg_order.Item"],
    "attributes": [
      {"field": "status", "operator": "EQ", "value": "active"},
      {"field": "price", "operator": "GT", "value": "100"}
    ],
    "relationTypes": null,
    "page": 0,
    "size": 20,
    "sortField": null,
    "sortDirection": null
  }'
```

**预期结果**: 返回类型为 `Item`、`status=active` 且 `price>100` 的实体摘要分页结果，`total` 反映符合条件总数。

---

### 场景 11: 批量语义查询（FR-006）

**验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/batch \
  -H "Content-Type: application/json" \
  -d '{
    "fqns": ["order:1.0.0.pkg_order.Order_001", "order:1.0.0.pkg_order.NotExist_999"]
  }'
```

**预期结果**: `entities` 包含存在的 FQN 摘要，`notFoundFqns` 数组标记不存在的 `order:1.0.0.pkg_order.NotExist_999`，其他 FQN 不受影响正常返回。

---

### 场景 12: 两点间路径查询（FR-008）

**验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/paths \
  -H "Content-Type: application/json" \
  -d '{
    "sourceFqn": "order:1.0.0.pkg_order.Order_001",
    "targetFqn": "order:1.0.0.pkg_order.Item_003",
    "direction": "BIDIRECTIONAL",
    "maxDepth": 5,
    "relationTypes": null,
    "findShortest": false,
    "filterCriteria": null
  }'
```

**预期结果**: 返回从 Order_001 到 Item_003 的全部可达路径，每条路径含步骤序列与总权重，`totalPaths` 反映路径数。

**最短路径验证**: 将 `findShortest` 置为 `true`，返回单条最短路径（边数最少）。

---

### 场景 13: 路径可达性判定（FR-011）

**验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/reachability \
  -H "Content-Type: application/json" \
  -d '{
    "sourceFqn": "order:1.0.0.pkg_order.Order_001",
    "targetFqn": "order:1.0.0.pkg_order.Item_003",
    "relationTypes": null
  }'
```

**预期结果**: `paths` 包含一条标识可达的路径记录（首个 `steps` 为空的 `reachable` 路径），表示存在可达路径。若不可达则 `paths` 为空数组。

---

### 场景 14: 正向影响扩散（FR-012）

**验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/impact/diffuse \
  -H "Content-Type: application/json" \
  -d '{
    "centerFqn": "order:1.0.0.pkg_order.Order_001",
    "direction": "FORWARD",
    "maxDepth": 3,
    "relationTypes": ["COMPOSITION", "DEPENDENCY_INFLUENCE"]
  }'
```

**预期结果**: 返回沿 COMPOSITION/DEPENDENCY_INFLUENCE 出边扩散的受影响实体，`layerStats` 按深度分组，`totalImpacted` 为去重后总数，`typeStats` 按实体类型统计。

---

### 场景 15: 反向依赖溯源（FR-013）

**验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/impact/trace \
  -H "Content-Type: application/json" \
  -d '{
    "centerFqn": "order:1.0.0.pkg_order.Item_003",
    "direction": "BACKWARD",
    "maxDepth": 3,
    "relationTypes": ["COMPOSITION"]
  }'
```

**预期结果**: 返回所有依赖 Item_003 的上游实体（沿入边追溯），按层级分组展示完整依赖父链。

---

### 场景 16: 影响路径详情查询（FR-014）

**POST 版本验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/impact/paths \
  -H "Content-Type: application/json" \
  -d '{
    "sourceFqn": "order:1.0.0.pkg_order.Order_001",
    "targetFqn": "order:1.0.0.pkg_order.Item_003",
    "relationTypes": ["COMPOSITION"]
  }'
```

**GET 版本验证命令**:
```bash
curl -X GET "http://localhost:8080/api/v1/compute-engine/impact/paths?sourceFqn=order:1.0.0.pkg_order.Order_001&targetFqn=order:1.0.0.pkg_order.Item_003&relationTypes=COMPOSITION&maxDepth=5"
```

**预期结果**: 两种方式均返回两点间全部影响传导路径，按长度升序排列，每条路径含途经实体/关系 FQN 与关系类型。

---

### 场景 17: 循环引用检测与去重（FR-022）

**前提**: 图中存在 A→B→C→A 的循环引用结构。

**验证方法**: 对循环图中的任意节点执行传递闭包推理（`/closure`）或 3 度邻接查询。

**预期结果**: 闭包/邻接结果中每个实体仅出现一次（最短深度标注），无无限递归，`truncatedReason` 不为 `null` 时表示为避免循环已截断。

---

### 场景 18: 批量查询超限错误（错误码 33009）

**验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/batch \
  -H "Content-Type: application/json" \
  -d "{\"fqns\": [\"fqn:1.0.0.\" + i for i in range(201)]}"
```

**预期结果**: 返回 `code=33009`，`message` 提示批量查询 FQN 数量超过上限（200）。

---

### 场景 19: 实体不存在错误（错误码 33001）

**验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/adjacency \
  -H "Content-Type: application/json" \
  -d '{
    "sourceFqn": "order:1.0.0.pkg_order.NotExist_999",
    "direction": "FORWARD",
    "maxDepth": 3,
    "relationTypes": null,
    "filterCriteria": null
  }'
```

**预期结果**: 返回 `code=33001`，`message` 提示起点实体 FQN 不存在或已下线。

---

### 场景 20: 图模式语法错误（错误码 33005）

**验证命令**:
```bash
curl -X POST http://localhost:8080/api/v1/compute-engine/pattern-match \
  -H "Content-Type: application/json" \
  -d '{
    "pattern": "* -[?]-> * -[?]-> * -[?]-> * -[?]-> * -[?]-> *",
    "maxResults": 100
  }'
```

**预期结果**: 模式段数超过 4 段上限，返回 `code=33008` 或 `33005`，提示模式长度超限。

---

## 3. 集成测试

### 3.1 测试环境启动

```bash
# 启动 TestContainers PostgreSQL + Spring Boot Test Context
mvn test -pl metaforge-compute-engine/metaforge-compute-engine-core -am
```

### 3.2 核心测试场景

| 测试类 | 覆盖 FR | 验证点 |
|--------|---------|--------|
| `GraphQueryServiceTest` | FR-001~007, FR-015 | 六种图查询 + 7 维过滤 + 去重 |
| `PathReasoningServiceTest` | FR-008~011 | 路径搜索 + 闭包 + 多跳推理 + 可达性 |
| `ImpactTracingServiceTest` | FR-012~014 | 正向扩散 + 反向溯源 + 影响路径 |
| `FilterCriteriaTest` | FR-015 | 7 维过滤 A∩B∩C... 交集逻辑 |
| `TransitivityRuleTest` | FR-024 | 传导规则加载与 per-type depth 生效 |
| `TruncationTest` | FR-023 | 截断标记 DEPTH_EXCEEDED/COUNT_EXCEEDED/TIMEOUT |
| `TimeoutTest` | FR-019 | 超时熔断 2000ms 生效 |

### 3.3 运行所有测试

```bash
# 运行本级所有集成测试
mvn verify -pl metaforge-compute-engine/metaforge-compute-engine-core -am -P integration-test
```

---

## 4. 性能验证

### 4.1 基准查询场景

使用预置的 100 实体 + 50 关系的测试数据集，执行以下查询并验证响应时间：

| 查询类型 | 预期响应时间 | 验证命令（JMH/基准测试） |
|---------|------------|------------------------|
| 3 度邻接查询 | < 200ms | `jmh:GraphTraversalBenchmark.adjacency3Hop` |
| 组合层级树（3 层） | < 150ms | `jmh:GraphTraversalBenchmark.compositionTree3Layer` |
| 子图提取（3 度） | < 300ms | `jmh:GraphTraversalBenchmark.subgraph3Hop` |
| 最短路径查询 | < 300ms | `jmh:GraphTraversalBenchmark.shortestPath` |
| 传递闭包推理 | < 300ms | `jmh:GraphTraversalBenchmark.closure3Hop` |
| 批量 200 FQN | < 200ms | `jmh:GraphTraversalBenchmark.batchQuery200` |

### 4.2 超时熔断验证

发送一个到复杂图的 10 度全向遍历请求，验证在 2000ms 内返回 `truncated=true, truncatedReason=TIMEOUT`。

---

## 5. 验收清单

- [ ] api 模块包含全部 DTO、枚举、Service 接口、错误码常量类
- [ ] core 模块实现全部 Application Service
- [ ] 递归 CTE 遍历正确性（不重不漏）
- [ ] 7 维过滤交集逻辑正确
- [ ] `matchMode` PREFIX/EXACT/PATTERN 各模式正确
- [ ] per-type `maxDepth` 深度差异生效
- [ ] 组合祖先链上溯（direction=BACKWARD）返回完整父链
- [ ] 图模式匹配通配符匹配整个 FQN
- [ ] 截断标记（truncated + truncatedReason）在所有载体中出现
- [ ] 超时熔断在 2000ms 内生效
- [ ] 循环引用去重截断
- [ ] 上游 BC 不可用时返回明确错误
- [ ] API 响应复用 foundation-core `ApiResponse<T>` 格式
- [ ] OpenAPI 文档自动生成（`/swagger-ui.html` 可见 compute-engine 分组）
- [ ] 集成测试基于 TestContainers 可重复执行

### 场景覆盖矩阵（共 20 个验证场景）

| # | 场景 | 端点 | 覆盖 FR |
|---|------|------|---------|
| 1 | 多度邻接查询 | POST /adjacency | FR-001 |
| 2 | 组合层级树上溯父链 | POST /composition-tree | FR-002 |
| 3 | 图模式匹配通配符 | POST /pattern-match | FR-004 |
| 4 | 7 维过滤交集 | POST /adjacency | FR-015 |
| 5 | 传递闭包推理 | POST /closure | FR-009 |
| 6 | 多跳语义推理 | POST /multi-hop | FR-010 |
| 7 | 结果截断标记 | 任意遍历端点 | FR-023 |
| 8 | per-type 深度差异化 | 任意遍历端点 | FR-020 |
| 9 | 子图提取查询 | POST /subgraph | FR-003 |
| 10 | 多条件复合检索 | POST /search | FR-005 |
| 11 | 批量语义查询 + 未找到标记 | POST /batch | FR-006 |
| 12 | 两点间路径查询（含最短路径） | POST /paths | FR-008 |
| 13 | 路径可达性判定 | POST /reachability | FR-011 |
| 14 | 正向影响扩散 | POST /impact/diffuse | FR-012 |
| 15 | 反向依赖溯源 | POST /impact/trace | FR-013 |
| 16 | 影响路径详情（POST + GET 双形态） | POST/GET /impact/paths | FR-014 |
| 17 | 循环引用检测与去重 | POST /closure | FR-022 |
| 18 | 批量查询超限错误 | POST /batch | 错误码 33009 |
| 19 | 实体不存在错误 | POST /adjacency | 错误码 33001 |
| 20 | 图模式语法/长度错误 | POST /pattern-match | 错误码 33005/33008 |
