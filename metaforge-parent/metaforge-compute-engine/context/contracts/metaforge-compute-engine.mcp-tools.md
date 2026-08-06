---
id: metaforge-compute-engine.mcp-tools
protocol: Java Interface
version: 1.0.0
owner: metaforge-compute-engine
description: 语义查询与推理引擎 MCP 工具集契约。工具方法通过 @Tool 注解定义，由 agent-consumption BC 统一通过 Spring AI MCP Server 对外发布。
type: business
---

# MCP Tools Contract: metaforge-compute-engine

**Protocol**: MCP（Model Context Protocol）
**Framework**: Spring AI
**Version**: 1.0.0

> 本 BC 通过 Spring AI `@Tool` 注解定义 MCP 工具方法契约。实际的 MCP Server 发布由 `agent-consumption` BC 统一执行（遵循 BC 宪法 Override 1：MCP 协议统一由 agent-consumption 发布）。
>
> 调用链路：Agent → MCP Client → agent-consumption MCP Server → compute-engine Application Service → jOOQ 跨 Schema 查询

---

## 工具集分类

### 1. 多维图查询工具（Graph Query Tools）

| 工具名称 | 对应 Application Service 方法 | 描述 |
|---------|------------------------------|------|
| `compute_engine_adjacency_query` | `GraphQueryService.queryAdjacency()` | 多度邻接查询 |
| `compute_engine_composition_tree` | `GraphQueryService.queryCompositionTree()` | 组合层级树查询（支持上溯父链） |
| `compute_engine_subgraph_extract` | `GraphQueryService.querySubgraph()` | 子图提取查询 |
| `compute_engine_pattern_match` | `GraphQueryService.queryPatternMatch()` | 图模式匹配查询 |
| `compute_engine_compound_search` | `GraphQueryService.searchCompound()` | 多条件复合检索 |
| `compute_engine_batch_query` | `GraphQueryService.queryBatch()` | 批量语义查询 |

### 2. 路径推理工具（Path Reasoning Tools）

| 工具名称 | 对应 Application Service 方法 | 描述 |
|---------|------------------------------|------|
| `compute_engine_find_paths` | `PathReasoningService.findPaths()` | 两点间路径查询 |
| `compute_engine_compute_closure` | `PathReasoningService.computeClosure()` | 传递闭包推理 |
| `compute_engine_multi_hop_reasoning` | `PathReasoningService.multiHopReasoning()` | 多跳语义推理 |
| `compute_engine_check_reachability` | `PathReasoningService.checkReachability()` | 路径可达性判定 |

### 3. 影响溯源工具（Impact Tracing Tools）

| 工具名称 | 对应 Application Service 方法 | 描述 |
|---------|------------------------------|------|
| `compute_engine_diffuse_forward` | `ImpactTracingService.diffuseForward()` | 正向影响扩散 |
| `compute_engine_trace_backward` | `ImpactTracingService.traceBackward()` | 反向依赖溯源 |
| `compute_engine_get_impact_paths` | `ImpactTracingService.getImpactPaths()` | 影响路径详情 |

---

## 工具方法签名

### 1. `compute_engine_adjacency_query`

```java
@Tool(description = "以指定实体为起点，按遍历方向沿语义关系边多度扩展，返回每度发现的实体与关系。支持7维过滤条件。"
        + " direction: FORWARD(正向出边)/BACKWARD(反向入边)/BOTH(双向)")
public GraphQueryResult adjacencyQuery(
        @ToolParam(description = "起点实体 FQN") String sourceFqn,
        @ToolParam(description = "遍历方向：FORWARD/BACKWARD/BOTH") String direction,
        @ToolParam(description = "最大扩展深度（1-10，默认5）") int maxDepth,
        @ToolParam(description = "关注的关系类型列表（可选，如 COMPOSITION,DEPENDENCY_INFLUENCE）") String relationTypes,
        @ToolParam(description = "源实体 FQN 列表（可选，逗号分隔）") String sourceFqns,
        @ToolParam(description = "目标实体 FQN 列表（可选，逗号分隔）") String targetFqns,
        @ToolParam(description = "实体类型 FQN 列表（可选，逗号分隔）") String entityTypes,
        @ToolParam(description = "匹配模式：PREFIX/EXACT") String matchMode
) {
    // 委托至 GraphQueryService.queryAdjacency()
}
```

### 2. `compute_engine_composition_tree`

```java
@Tool(description = "基于 COMPOSITION 关系递归展开指定节点的组合结构。"
        + " direction=FORWARD 向下展开子树，BACKWARD 向上追溯父链，BOTH 双向展开")
public GraphQueryResult compositionTree(
        @ToolParam(description = "根节点实体 FQN") String rootFqn,
        @ToolParam(description = "展开方向：FORWARD/BACKWARD/BOTH") String direction,
        @ToolParam(description = "最大展开深度") int maxDepth
) {
    // 委托至 GraphQueryService.queryCompositionTree()
}
```

### 3. `compute_engine_pattern_match`

```java
@Tool(description = "在线性路径模式中匹配符合模式的路径实例。模式格式: EntityType -[RelationType]-> EntityType ...。"
        + " 通配符 * 匹配任意实体类型完整FQN，? 匹配任意关系类型完整FQN。模式长度上限4段(3条关系边)。"
        + " 示例: * -[COMPOSITION]-> * -[DEPENDENCY_INFLUENCE]-> *")
public GraphQueryResult patternMatch(
        @ToolParam(description = "模式字符串，如 '* -[?]-> * -[?]-> *'") String pattern,
        @ToolParam(description = "最大结果数（默认500）") int maxResults
) {
    // 委托至 GraphQueryService.queryPatternMatch()
}
```

### 4. `compute_engine_find_paths`

```java
@Tool(description = "查询两指定实体之间的可达路径，支持全部路径或仅最短路径模式")
public PathResult findPaths(
        @ToolParam(description = "源实体 FQN") String sourceFqn,
        @ToolParam(description = "目标实体 FQN") String targetFqn,
        @ToolParam(description = "遍历方向：FORWARD/BACKWARD/BOTH") String direction,
        @ToolParam(description = "关注的关系类型列表（可选，逗号分隔）") String relationTypes,
        @ToolParam(description = "最大搜索深度（1-10）") int maxDepth,
        @ToolParam(description = "是否仅返回最短路径") boolean shortestOnly
) {
    // 委托至 PathReasoningService.findPaths()
}
```

### 5. `compute_engine_compute_closure`

```java
@Tool(description = "基于配置中定义的可传递关系类型，计算指定起点的完整传递闭包。按层级分层输出。")
public ClosureResult computeClosure(
        @ToolParam(description = "起点实体 FQN") String sourceFqn,
        @ToolParam(description = "关注的关系类型列表（可选，逗号分隔，空=所有可传递类型）") String relationTypes
) {
    // 委托至 PathReasoningService.computeClosure()
}
```

### 6. `compute_engine_multi_hop_reasoning`

```java
@Tool(description = "基于传导矩阵组合多种关系类型进行跨语义跳跃推理。每步指定关系类型与方向，最大3步。"
        + " hopSteps 格式: 'COMPOSITION:FORWARD,DEPENDENCY_INFLUENCE:FORWARD'")
public PathResult multiHopReasoning(
        @ToolParam(description = "起点实体 FQN") String sourceFqn,
        @ToolParam(description = "跳步序列：'类型:方向,类型:方向'（如 COMPOSITION:FORWARD）") String hopSteps
) {
    // 委托至 PathReasoningService.multiHopReasoning()
}
```

### 7. `compute_engine_check_reachability`

```java
@Tool(description = "快速判定源实体到目标实体是否存在可达路径。找到首条路径即返回。")
public PathResult checkReachability(
        @ToolParam(description = "源实体 FQN") String sourceFqn,
        @ToolParam(description = "目标实体 FQN") String targetFqn,
        @ToolParam(description = "关注的关系类型列表（可选，逗号分隔）") String relationTypes,
        @ToolParam(description = "最大检查深度") int maxDepth
) {
    // 委托至 PathReasoningService.checkReachability()
}
```

### 8. `compute_engine_diffuse_forward`

```java
@Tool(description = "从指定起点发出沿关系类型正向 BFS 扩散，返回按层级分组的影响实体明细与统计")
public ImpactTraceResult diffuseForward(
        @ToolParam(description = "起点实体 FQN") String sourceFqn,
        @ToolParam(description = "关注的关系类型列表（逗号分隔）") String relationTypes,
        @ToolParam(description = "最大扩散深度") int maxDepth
) {
    // 委托至 ImpactTracingService.diffuseForward()
}
```

### 9. `compute_engine_trace_backward`

```java
@Tool(description = "从指定实体沿入边反向 BFS 追溯，返回所有依赖该实体的上游实体列表")
public ImpactTraceResult traceBackward(
        @ToolParam(description = "起点实体 FQN") String sourceFqn,
        @ToolParam(description = "关注的关系类型列表（逗号分隔）") String relationTypes,
        @ToolParam(description = "最大追溯深度") int maxDepth
) {
    // 委托至 ImpactTracingService.traceBackward()
}
```

### 10. `compute_engine_get_impact_paths`

```java
@Tool(description = "查询两指定实体间的所有影响传导路径，按长度排序，路径自包含内联摘要")
public ImpactTraceResult getImpactPaths(
        @ToolParam(description = "源实体 FQN") String sourceFqn,
        @ToolParam(description = "目标实体 FQN") String targetFqn,
        @ToolParam(description = "关注的关系类型列表（可选，逗号分隔）") String relationTypes,
        @ToolParam(description = "最大路径深度") int maxDepth
) {
    // 委托至 ImpactTracingService.getImpactPaths()
}
```

---

## MCP 工具调用示例（Agent 侧）

```json
// MCP Client 调用示例 — 查看订单001的3度邻接关系
{
  "method": "tools/call",
  "params": {
    "name": "compute_engine_adjacency_query",
    "arguments": {
      "sourceFqn": "order:1.0.0.pkg_order.Order_001",
      "direction": "BOTH",
      "maxDepth": 3,
      "relationTypes": "COMPOSITION,DEPENDENCY_INFLUENCE",
      "sourceFqns": "",
      "targetFqns": "",
      "entityTypes": "",
      "matchMode": "PREFIX"
    }
  }
}
```

```json
// MCP Client 调用示例 — 图模式匹配
{
  "method": "tools/call",
  "params": {
    "name": "compute_engine_pattern_match",
    "arguments": {
      "pattern": "* -[COMPOSITION]-> * -[DEPENDENCY_INFLUENCE]-> *",
      "maxResults": 100
    }
  }
}
```

---

## 发布态说明

### 当前 MVP 阶段

- 工具方法通过 `@Tool` 注解定义在 `metaforge-compute-engine-core` 的 `interfaces/mcp/ComputeEngineMcpTools.java` 中
- agent-consumption BC 尚未实现（属于 M3 里程碑）
- MVP 阶段 MCP 工具集可先通过 Application Service 暴露，REST API 作为对外主要接口

### 后续接入 agent-consumption 时

1. agent-consumption BC 依赖 `metaforge-compute-engine-api`，注入 Application Service
2. agent-consumption BC 的 MCP Server 将各 `@Tool` 方法发布为标准 MCP 工具
3. 白名单过滤在 agent-consumption 层执行后再调用 compute-engine Application Service
4. 上下文格式化（Agent 友好型输出）由 agent-consumption 层完成
