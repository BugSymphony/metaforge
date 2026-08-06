---
id: metaforge-compute-engine.application-service
protocol: Java Interface
version: 1.0.0
owner: metaforge-compute-engine
description: 语义查询与推理引擎对外暴露的 Application Service 接口契约。下游 BC（agent-consumption）通过 Maven 依赖 metaforge-compute-engine-api 模块后进行进程内调用。
type: business
---

# Application Service Contract: metaforge-compute-engine

**Protocol**: Application Service（进程内 Java Interface 调用）
**Module**: `metaforge-compute-engine-api`
**Version**: 1.0.0

> 下游 BC（`agent-consumption`）通过 Maven 依赖 `metaforge-compute-engine-api` 模块，注入以下接口的 Spring Bean 实例进行进程内调用。严禁依赖 `metaforge-compute-engine-core` 模块。

---

## Maven 依赖

```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-compute-engine-api</artifactId>
    <version>${revision}</version>
</dependency>
```

---

## 1. GraphQueryService

**职责**: 多维图查询——邻接查询、组合层级树、子图提取、图模式匹配、多条件复合检索、批量语义查询。所有结果载体包含统一截断标记。

### @OpenHostService

```java
/**
 * 多维图查询服务。
 * <p>
 * 提供六种核心图查询能力：多度邻接查询、组合层级树查询、子图提取查询、
 * 图模式匹配查询、多条件复合检索、批量语义查询。
 * <p>
 * 所有查询默认基于生效态数据执行（metadata_entity.status='ACTIVE'、relation_instance.status='ACTIVE'），
 * 历史版本与草稿版本不参与计算。遍历深度受两阶段约束：全局 max-depth 与 per-AssociationType maxDepth。
 * 所有结果载体统一包含 truncated（boolean）+ truncatedReason（DEPTH_EXCEEDED/COUNT_EXCEEDED/TIMEOUT）截断标记。
 * <p>
 * 过滤参数 {@link FilterCriteria} 为 7 维可选条件，维度间 AND，维度内 OR。
 * 各 FQN 维度通过 matchMode 显式指定 PREFIX/EXACT 匹配策略，relationInstanceFqns 额外支持 PATTERN 模式。
 * 被过滤的实体/关系不参与遍历且不计入深度。
 */
public interface GraphQueryService {

    /**
     * 多度邻接查询。
     * <p>
     * 以指定起点实体为中心，沿关系边多度扩展，返回每度发现的实体与关系。
     * 同一实体在多路径出现时仅返回一次，标注最短到达深度。
     * 遍历深度同时受全局 max-depth 与各 AssociationType per-type maxDepth 约束，取两者较小值。
     *
     * @param request 邻接查询请求（包含起点实体 FQN、遍历方向、最大深度、关注关系类型、过滤条件）
     * @return 图查询结果（实体集合、关系集合、邻接映射、截断标记）
     * @throws EntityNotFoundException 如果起点实体 FQN 不存在或已下线
     */
    GraphQueryResult queryAdjacency(AdjacencyQueryRequest request);

    /**
     * 组合层级树查询。
     * <p>
     * 基于 COMPOSITION 关系递归展开指定节点的组合结构。
     * direction 参数指定遍历方向：
     * <ul>
     *   <li>FORWARD：从当前节点向下展开子树，保留树形嵌套结构，每个节点含子节点列表与深度</li>
     *   <li>BACKWARD：从当前节点向上追溯完整父链，返回扁平层级列表</li>
     *   <li>BOTH：双向展开，向上父链 + 向下子树合并输出</li>
     * </ul>
     * COMPSITION 关系可传递，权重连乘表示层级衰减。
     *
     * @param request 组合层级树查询请求（包含根节点 FQN、遍历方向、最大深度）
     * @return 图查询结果（树形或扁平结构，含截断标记）
     * @throws EntityNotFoundException 如果根节点实体不存在
     */
    GraphQueryResult queryCompositionTree(CompositionTreeQueryRequest request);

    /**
     * 子图提取查询。
     * <p>
     * 以一个或多个中心实体为种子，在指定深度内扩展，返回子图内的全部实体集合、关系集合及邻接映射。
     *
     * @param request 子图提取查询请求（中心实体 FQN 列表、扩展深度 1~3、过滤条件）
     * @return 图查询结果（实体集合、关系集合、邻接映射、截断标记）
     * @throws EntityNotFoundException 如果所有中心实体均不存在
     */
    GraphQueryResult querySubgraph(SubgraphQueryRequest request);

    /**
     * 图模式匹配查询。
     * <p>
     * 在线性路径模式中匹配符合模式的路径实例。模式格式：
     * <pre>EntityTypeA -[RelationType]-&gt; EntityTypeB -[RelationType]-&gt; EntityTypeC ...</pre>
     * 通配符 '*' 匹配任意完整的 EntitySchema FQN（不拆分名称段），'?' 匹配任意完整的 RelationSchema FQN。
     * 模式长度上限 4 段（3 条关系边）。
     * 返回所有匹配路径实例，每条标注实体 FQN、关系 FQN、实体类型、关系类型。
     *
     * @param request 模式匹配请求（模式字符串、匹配上限）
     * @return 图查询结果（匹配路径列表、截断标记）
     * @throws InvalidPatternException 如果模式语法非法或长度超限
     */
    GraphQueryResult queryPatternMatch(PatternMatchRequest request);

    /**
     * 多条件复合检索。
     * <p>
     * 按实体类型、属性条件（精准/模糊/范围匹配）、关系条件组合过滤。
     * 支持与/或逻辑组合，支持分页与排序。
     * 此接口为唯一支持分页的查询模式。
     *
     * @param request 复合检索请求（实体类型、属性条件、关系条件、分页参数）
     * @return 分页结果（实体摘要列表）
     */
    PageResult<EntitySummary> searchCompound(CompoundSearchRequest request);

    /**
     * 批量语义查询。
     * <p>
     * 一次传入最多 200 个 FQN，返回每个 FQN 对应的实体摘要及关联关系摘要。
     * 不存在的 FQN 在结果中单独标记状态，不影响其他 FQN 的正常返回。
     *
     * @param request 批量查询请求（FQN 列表，上限 200）
     * @return 图查询结果（实体摘要列表、关系摘要列表、未找到 FQN 列表）
     * @throws BatchSizeExceededException 如果 FQN 列表超过 200 条
     */
    GraphQueryResult queryBatch(BatchQueryRequest request);
}
```

---

## 2. PathReasoningService

**职责**: 路径推理与语义关联分析——两点间路径查询、传递闭包推理、多跳语义推理、路径可达性判定。

### @OpenHostService

```java
/**
 * 路径推理与语义关联分析服务。
 * <p>
 * 提供四种推理能力：两点间路径查询（支持最短路径优先）、传递闭包推理、
 * 多跳语义推理（跨语义类型跳跃）、路径可达性快速判定。
 * <p>
 * 所有推理规则基于 {@code metaforge.compute-engine.transitivity-rules} 配置中定义的
 * AssociationType 传递性、方向与传导矩阵执行。实体/关系结构来源于已发布元模型定义。
 * 传递路径的遍历深度受各 AssociationType per-type maxDepth 约束。
 * 多跳语义推理中相邻跳的关系类型须满足配置中定义的传导兼容性。
 */
public interface PathReasoningService {

    /**
     * 两点间路径查询。
     * <p>
     * 查询指定源实体与目标实体之间的可达路径，支持指定遍历方向、关系类型过滤、最大深度约束。
     * 可选择返回全部路径或仅返回最短路径（按边数最少）。
     *
     * @param request 路径查询请求（源/目标 FQN、遍历方向、关系类型、最大深度、最短路径模式）
     * @return 路径推理结果（路径列表、路径总数、截断标记）
     * @throws EntityNotFoundException 如果源实体或目标实体 FQN 不存在
     */
    PathResult findPaths(PathQueryRequest request);

    /**
     * 传递闭包推理。
     * <p>
     * 基于配置中定义的可传递关系类型（transitive=true），计算指定起点沿传递关系的完整闭包。
     * 结果按传递层级分层分组，每层包含该深度发现的可达实体。
     * 循环引用自动去重截断，遇 non-transitive 关系类型或深度超限时该分支截断，
     * 但不影响其他可传递类型边在深度范围内的继续遍历。
     *
     * @param request 闭包推理请求（起点 FQN、关系类型过滤）
     * @return 传递闭包结果（按层级分组的可达实体、可达总数、关系类型统计、截断标记）
     * @throws EntityNotFoundException 如果起点实体 FQN 不存在
     */
    ClosureResult computeClosure(ClosureQueryRequest request);

    /**
     * 多跳语义推理。
     * <p>
     * 基于传导规则配置中的传导矩阵，组合多种关系类型进行跨语义跳跃推理。
     * 每步跳跃的关系类型须满足配置中定义的传导兼容性。
     * 权重策略（multiply/add/max）用于计算路径的置信度或成本。
     * 最大跳跃步数 3 步。
     *
     * @param request 多跳推理请求（起点 FQN、跳步序列（每步的关系类型+方向））
     * @return 路径推理结果（推理路径、每步语义说明、截断标记）
     * @throws EntityNotFoundException 如果起点实体 FQN 不存在
     * @throws NoLegalConductionPathException 如果指定的关系类型序列无合法传导定义
     */
    PathResult multiHopReasoning(MultiHopQueryRequest request);

    /**
     * 路径可达性快速判定。
     * <p>
     * 快速判定源实体到目标实体是否存在可达路径。
     * 在找到任意一条可达路径后立即返回结果，不继续搜索更多路径。
     * 性能优先于完整性，使用 LIMIT 1 截断搜索。
     *
     * @param request 可达性判定请求（源/目标 FQN、关系类型、最大深度）
     * @return 可达性结果（可达标志、最短深度、首条路径）
     * @throws EntityNotFoundException 如果源实体或目标实体 FQN 不存在
     */
    PathResult checkReachability(ReachabilityCheckRequest request);
}
```

---

## 3. ImpactTracingService

**职责**: 影响溯源与变更评估——正向影响扩散、反向依赖溯源、影响路径详情。

### @OpenHostService

```java
/**
 * 影响溯源与变更评估服务。
 * <p>
 * 提供三种影响分析能力：正向影响扩散（沿出边 BFS 扩展）、反向依赖溯源（沿入边 BFS 追溯）、
 * 影响路径详情查询（两点间所有传导路径）。
 * <p>
 * 正向/反向分析沿指定关系类型 BFS 扩展，按层级分组，同一实体被多路径影响时仅统计一次。
 * 影响路径详情返回两点间所有传导路径，按长度排序，路径内联实体与关系摘要（无需下游额外补查询）。
 */
public interface ImpactTracingService {

    /**
     * 正向影响扩散查询。
     * <p>
     * 从指定起点实体出发，沿指定关系类型沿出边正向 BFS 扩散。
     * 返回按层级分组的影响实体统计（实体总数、按类型分层统计、影响实体明细含 FQN/类型/层级/深度）。
     * 同一实体被多路径影响时仅统计一次，标注最短影响深度。
     *
     * @param request 影响扩散请求（起点 FQN、关系类型列表、最大深度）
     * @return 影响溯源结果（影响实体总数、分层统计、实体明细、关系明细、截断标记）
     * @throws EntityNotFoundException 如果起点实体 FQN 不存在
     */
    ImpactTraceResult diffuseForward(ImpactDiffusionRequest request);

    /**
     * 反向依赖溯源查询。
     * <p>
     * 从指定实体出发，沿指定关系类型沿入边反向 BFS 追溯。
     * 返回所有依赖该实体的上游实体列表，按层级分组展示，附带实体与关系内联摘要。
     *
     * @param request 依赖溯源请求（起点 FQN、关系类型列表、最大深度）
     * @return 影响溯源结果（依赖实体总数、分层统计、实体明细、关系明细、截断标记）
     * @throws EntityNotFoundException 如果起点实体 FQN 不存在
     */
    ImpactTraceResult traceBackward(ImpactDiffusionRequest request);

    /**
     * 影响路径详情查询。
     * <p>
     * 查询两指定实体间的所有影响传导路径（不限于单关系类型）。
     * 按路径长度升序排列，每条路径标注途经实体 FQN、关系 FQN、关系类型、传导方向。
     * 路径自包含全部内联摘要，无需下游额外查询。
     *
     * @param sourceFqn 源实体 FQN
     * @param targetFqn 目标实体 FQN
     * @param relationTypes 关注的关系类型列表（空=全类型）
     * @param maxDepth 最大路径深度
     * @return 影响溯源结果（路径列表、路径总数）
     * @throws EntityNotFoundException 如果源实体或目标实体 FQN 不存在
     */
    ImpactTraceResult getImpactPaths(String sourceFqn, String targetFqn,
                                      List<AssociationType> relationTypes, int maxDepth);
}
```

---

## 输入/输出 DTO

### 请求 DTO

```java
/**
 * 邻接查询请求。
 */
public record AdjacencyQueryRequest(
        @NotBlank String sourceFqn,                          // 起点实体 FQN
        @NotNull TraversalDirection direction,               // 遍历方向（FORWARD/BACKWARD/BOTH）
        @Max(10) int maxDepth,                               // 最大深度（默认 5）
        List<AssociationType> relationTypes,                 // 关注关系类型列表（空=全类型）
        FilterCriteria filterCriteria                        // 7 维过滤条件（可选）
) {}

/**
 * 组合层级树查询请求。
 */
public record CompositionTreeQueryRequest(
        @NotBlank String rootFqn,                            // 根节点 FQN
        @NotNull TraversalDirection direction,               // 遍历方向（FORWARD/BACKWARD/BOTH）
        @Max(10) int maxDepth,                               // 最大深度（默认由 COMPOSITION per-type 限制）
        FilterCriteria filterCriteria                        // 7 维过滤条件（可选）
) {}

/**
 * 子图提取查询请求。
 */
public record SubgraphQueryRequest(
        @NotEmpty List<String> centerFqns,                   // 中心实体 FQN 列表
        @Min(1) @Max(3) int expandDepth,                     // 扩展深度（1~3）
        List<AssociationType> relationTypes,                 // 关注关系类型列表（空=全类型）
        FilterCriteria filterCriteria                        // 7 维过滤条件（可选）
) {}

/**
 * 图模式匹配请求。
 */
public record PatternMatchRequest(
        @NotBlank String pattern,                            // 模式字符串
        @Max(500) int maxResults,                            // 最大结果数（默认 500）
        FilterCriteria filterCriteria                        // 7 维过滤条件（可选）
) {}

/**
 * 多条件复合检索请求。
 */
public record CompoundSearchRequest(
        List<String> entityTypes,                            // 实体类型过滤
        List<AttributeCondition> attributeConditions,        // 属性条件列表
        List<String> relationTypes,                          // 关系类型过滤
        LogicOperator logicOperator,                         // 条件逻辑（AND/OR）
        PageRequest pageRequest,                             // 分页参数
        FilterCriteria filterCriteria                        // 7 维过滤条件（可选）
) {}

/**
 * 批量语义查询请求。
 */
public record BatchQueryRequest(
        @NotEmpty @Size(max = 200) List<String> fqns,        // FQN 列表（上限 200）
        FilterCriteria filterCriteria                        // 7 维过滤条件（可选）
) {}

/**
 * 路径查询请求。
 */
public record PathQueryRequest(
        @NotBlank String sourceFqn,                          // 源实体 FQN
        @NotBlank String targetFqn,                          // 目标实体 FQN
        TraversalDirection direction,                        // 遍历方向（默认 BOTH）
        List<AssociationType> relationTypes,                 // 关注关系类型
        @Min(1) @Max(10) int maxDepth,                       // 最大深度
        boolean shortestOnly,                                // 仅返回最短路径
        FilterCriteria filterCriteria                        // 7 维过滤条件（可选）
) {}

/**
 * 传递闭包推理请求。
 */
public record ClosureQueryRequest(
        @NotBlank String sourceFqn,                          // 起点实体 FQN
        List<AssociationType> relationTypes,                 // 关注关系类型（空=所有可传递类型）
        FilterCriteria filterCriteria                        // 7 维过滤条件（可选）
) {}

/**
 * 多跳语义推理请求。
 */
public record MultiHopQueryRequest(
        @NotBlank String sourceFqn,                          // 起点实体 FQN
        @NotEmpty @Size(max = 3) List<HopStep> hopSteps,     // 跳步序列（每步：关系类型+方向）
        FilterCriteria filterCriteria                        // 7 维过滤条件（可选）
) {}

/**
 * 多跳推理的单个跳步。
 */
public record HopStep(
        @NotNull AssociationType relationType,               // 关系类型
        @NotNull TraversalDirection direction                // 遍历方向
) {}

/**
 * 路径可达性判定请求。
 */
public record ReachabilityCheckRequest(
        @NotBlank String sourceFqn,                          // 源实体 FQN
        @NotBlank String targetFqn,                          // 目标实体 FQN
        List<AssociationType> relationTypes,                 // 关注关系类型
        @Min(1) @Max(10) int maxDepth                        // 最大检查深度
) {}

/**
 * 影响扩散/溯源源请求。
 */
public record ImpactDiffusionRequest(
        @NotBlank String sourceFqn,                          // 起点实体 FQN
        List<AssociationType> relationTypes,                 // 关注关系类型列表
        @Min(1) @Max(10) int maxDepth,                       // 最大扩散深度
        FilterCriteria filterCriteria                        // 7 维过滤条件（可选）
) {}

/**
 * 属性查询条件。
 */
public record AttributeCondition(
        @NotBlank String field,                              // 属性字段名
        @NotBlank String value,                              // 属性值
        MatchMode matchMode                                  // 匹配模式（EXACT/PREFIX/RANGE）
) {}
```

### 7 维过滤参数

```java
/**
 * 统一 7 维过滤参数集合。
 * <p>
 * 全部维度可选（null 或空集合表示不限），维度间取交集（外层 AND），维度内集合取并集（内层 OR）。
 * 每个 FQN 维度通过 matchMode 显式指定匹配策略。
 * 过滤在遍历过程中实时生效（CTE WHERE 子句前置），被过滤内容不参与遍历且不计入深度。
 * 属性字段过滤采用精确等值匹配，不使用 matchMode。
 */
public record FilterCriteria(
        List<AssociationType> associationTypes,              // 关联类型枚举列表，精确匹配
        FqnFilterGroup sourceFqns,                           // 源实体 FQN 过滤组
        FqnFilterGroup targetFqns,                           // 目标实体 FQN 过滤组
        FqnFilterGroup relationInstanceFqns,                 // 关系实例 FQN 过滤组（支持 PATTERN）
        FqnFilterGroup entityTypes,                          // 实体类型（EntitySchema FQN）过滤组
        FqnFilterGroup relationTypes,                        // 关系类型（RelationSchema FQN）过滤组
        List<PropertyFilter> propertyFilters                 // 属性字段精确等值匹配列表
) {}

/**
 * FQN 过滤组（多值 OR 逻辑 + 匹配模式）。
 */
public record FqnFilterGroup(
        List<String> values,                                 // FQN 值列表
        MatchMode matchMode                                  // 匹配模式（PREFIX/EXACT/PATTERN）
) {}

/**
 * 属性字段精确等值过滤。
 */
public record PropertyFilter(
        @NotBlank String field,                              // 属性字段路径
        @NotNull Object value                                // 等值匹配值
) {}
```

### 响应 DTO

```java
/**
 * 图查询结果载体。
 * <p>
 * 包含实体集合（去重，含内联摘要）、关系集合（去重）、实体-关系邻接映射。
 * 统一截断标记 truncated + truncatedReason 用于下游区分完整结果与截断结果。
 */
public record GraphQueryResult(
        List<EntitySummary> entities,                        // 实体集合（去重）
        List<RelationSummary> relations,                     // 关系集合（去重）
        Map<String, List<String>> adjacencyMap,              // 实体FQN → 关联关系FQN列表
        boolean truncated,                                   // 是否截断
        TruncatedReason truncatedReason                      // 截断原因
) {}

/**
 * 路径推理结果载体。
 * <p>
 * 包含路径列表（按长度或权重排序）、路径总数、每步途经的实体与关系排序。
 * 每条路径自包含，标注各跳语义说明。
 */
public record PathResult(
        List<PathDetail> paths,                              // 路径列表
        int totalPaths,                                      // 路径总数
        boolean truncated,                                   // 是否截断
        TruncatedReason truncatedReason                      // 截断原因
) {}

/**
 * 单条路径详情。
 */
public record PathDetail(
        List<PathStep> steps,                                // 路径步序列
        int length,                                          // 路径长度（边数）
        double totalWeight                                   // 路径总权重（按 weightStrategy 计算）
) {}

/**
 * 单个路径步。
 */
public record PathStep(
        String fromEntityFqn,                                // 起始实体 FQN
        String toEntityFqn,                                  // 终止实体 FQN
        String relationFqn,                                  // 关系实例 FQN
        AssociationType relationType,                        // 关系类型
        String semanticDescription                           // 该步传导语义说明
) {}

/**
 * 传递闭包结果载体。
 * <p>
 * 包含按层级分组的可达实体列表、到达最短距离、途经关系类型统计。
 */
public record ClosureResult(
        Map<Integer, List<ClosuredEntityDetail>> layers,     // 按层级分组（key=深度）
        int totalReachable,                                  // 可达实体总数
        Map<AssociationType, Integer> typeStats,             // 途经关系类型统计
        boolean truncated,
        TruncatedReason truncatedReason
) {}

/**
 * 闭包中的可达实体详情。
 */
public record ClosuredEntityDetail(
        String fqn,                                          // 实体 FQN
        String name,                                         // 展示名
        String entitySchemaFqn,                              // 元模型 EntitySchema FQN
        int minDepth,                                        // 最短到达深度
        List<AssociationType> arrivedByTypes                 // 到达该实体途经的关系类型
) {}

/**
 * 影响溯源结果载体。
 * <p>
 * 包含影响实体总数、按类型分层统计、影响实体明细及关联关系明细。
 */
public record ImpactTraceResult(
        int totalImpacted,                                   // 影响实体总数
        Map<String, Integer> typeStats,                      // 按实体类型统计数（key=EntitySchema FQN）
        Map<Integer, List<ImpactEntityDetail>> layers,       // 按层级分组
        List<EntitySummary> entities,                        // 影响实体摘要列表
        List<RelationSummary> relations,                     // 关联关系摘要列表
        boolean truncated,
        TruncatedReason truncatedReason
) {}

/**
 * 影响实体详情。
 */
public record ImpactEntityDetail(
        String fqn,                                          // 实体 FQN
        String name,                                         // 展示名
        String entitySchemaFqn,                              // 元模型 EntitySchema FQN
        int depth,                                           // 影响传播层级
        List<PathDetail> impactPaths                         // 影响传导路径
) {}
```

### 内联摘要

```java
/**
 * 实体内联摘要。
 * <p>
 * 以 FQN 为核心标识，内联展示名与元模型类型 FQN，确保下游无需额外补查询。
 */
public record EntitySummary(
        String fqn,                                          // 实体 FQN
        String name,                                         // 展示名
        String entitySchemaFqn                               // 元模型 EntitySchema FQN
) {}

/**
 * 关系内联摘要。
 * <p>
 * 以 FQN 为核心标识，内联关系类型与端点 FQN。
 */
public record RelationSummary(
        String fqn,                                          // 关系实例 FQN
        AssociationType associationType,                     // 关联类型
        String sourceEntityFqn,                              // 源实体 FQN
        String targetEntityFqn                               // 目标实体 FQN
) {}
```

---

## 枚举

### AssociationType

```java
public enum AssociationType {
    COMPOSITION,              // 组合关系
    DEPENDENCY_INFLUENCE,     // 依赖/影响关系
    PROCESS_SEQUENCE,         // 流程序列关系
    ASSOCIATION_REFERENCE,    // 关联引用关系
    MAPPING_CORRESPONDENCE    // 映射对应关系
}
```

### TraversalDirection

```java
public enum TraversalDirection {
    FORWARD,      // 正向（沿出边）
    BACKWARD,     // 反向（沿入边）
    DIRECTED,     // 单向（不隐含反向传递性）
    BIDIRECTIONAL // 双向
}
```

### MatchMode

```java
public enum MatchMode {
    PREFIX,   // 前缀匹配（FQN LIKE 'prefix%'）
    EXACT,    // 精确匹配（FQN = value）
    PATTERN   // 模式匹配（FQN LIKE 'pattern'，仅 relationInstanceFqns 支持）
}
```

### TruncatedReason

```java
public enum TruncatedReason {
    DEPTH_EXCEEDED,   // 遍历深度超过上限
    COUNT_EXCEEDED,   // 结果数量超过上限
    TIMEOUT           // 查询超时中断
}
```

### WeightStrategy

```java
public enum WeightStrategy {
    MULTIPLY,   // 连乘（概率/强度衰减）
    ADD,        // 累加（步长/成本/耗时求和）
    MAX,        // 取最大（置信度合并）
    NONE        // 无权重计算
}
```

---

## 错误码

本 BC 使用错误码范围 **33000-33999**（定义于 `ComputeEngineErrorCodes` 常量类）。

| 错误码 | 常量名 | HTTP 状态 | 描述 |
|--------|--------|----------|------|
| 33001 | `ENTITY_NOT_FOUND` | 404 | 查询起点实体 FQN 不存在或已下线 |
| 33002 | `TRAVERSAL_DEPTH_EXCEEDED` | 422 | 遍历深度超过配置上限 |
| 33003 | `QUERY_TIMEOUT` | 504 | 查询超时中断 |
| 33004 | `RESULT_COUNT_EXCEEDED` | 422 | 结果数量超过上限（500） |
| 33005 | `INVALID_PATTERN` | 400 | 图模式匹配语法非法 |
| 33006 | `INVALID_FILTER` | 400 | 过滤参数组合非法 |
| 33007 | `NO_LEGAL_CONDUCTION_PATH` | 422 | 指定关系类型序列无合法传导定义 |
| 33008 | `PATTERN_LENGTH_EXCEEDED` | 400 | 模式长度超过最大段数（4 段） |
| 33009 | `BATCH_SIZE_EXCEEDED` | 400 | 批量查询 FQN 数量超过上限（200） |
| 33010 | `UPSTREAM_SERVICE_UNAVAILABLE` | 502 | 上游模块不可用 |
| 33011 | `CIRCULAR_REFERENCE_DETECTED` | 422 | 循环引用已检测并截断 |

---

## 调用示例

```java
// 引入 metaforge-compute-engine-api 模块依赖后直接注入
@Autowired
private GraphQueryService graphQueryService;

@Autowired
private PathReasoningService pathReasoningService;

@Autowired
private ImpactTracingService impactTracingService;

// 1. 邻接查询：3 度正向扩展，只关注 COMPOSITION 类型
GraphQueryResult adjResult = graphQueryService.queryAdjacency(
    new AdjacencyQueryRequest(
        "order:1.0.0.pkg_order.Order_001",
        TraversalDirection.FORWARD,
        3,
        List.of(AssociationType.COMPOSITION),
        null
    )
);

// 2. 组合层级树：向上追溯完整父链
GraphQueryResult treeResult = graphQueryService.queryCompositionTree(
    new CompositionTreeQueryRequest(
        "order:1.0.0.pkg_order.Order_001",
        TraversalDirection.BACKWARD,
        10,
        null
    )
);

// 3. 传递闭包推理
ClosureResult closure = pathReasoningService.computeClosure(
    new ClosureQueryRequest(
        "order:1.0.0.pkg_order.Order_001",
        null,  // 所有可传递类型
        null
    )
);

// 4. 正向影响扩散：COMPOSITION + DEPENDENCY_INFLUENCE，深度 3
ImpactTraceResult impact = impactTracingService.diffuseForward(
    new ImpactDiffusionRequest(
        "order:1.0.0.pkg_order.Order_001",
        List.of(AssociationType.COMPOSITION, AssociationType.DEPENDENCY_INFLUENCE),
        3,
        null
    )
);

// 5. 带 7 维过滤的查询
GraphQueryResult filteredResult = graphQueryService.queryAdjacency(
    new AdjacencyQueryRequest(
        "order:1.0.0.pkg_order.Order_001",
        TraversalDirection.BOTH,
        5,
        null,
        new FilterCriteria(
            List.of(AssociationType.COMPOSITION),                    // 仅 COMPOSITION 类型
            new FqnFilterGroup(List.of("order:1.0.0."), MatchMode.PREFIX),  // 源实体前缀
            null,                                                    // 目标实体不限
            null,                                                    // 关系实例不限
            new FqnFilterGroup(List.of("order:1.0.0.pkg_order."), MatchMode.PREFIX),  // 实体类型前缀
            null,                                                    // 关系类型不限
            null                                                     // 属性不限
        )
    )
);
```

---

## 事务边界

本 BC 为纯无状态计算层，不参与事务管理。每个 Application Service 方法为一次查询操作，不涉及数据库写操作。查询超时通过 `statement.setQueryTimeoutMs(timeoutMs)` 由 jOOQ DSLContext 层面控制。
