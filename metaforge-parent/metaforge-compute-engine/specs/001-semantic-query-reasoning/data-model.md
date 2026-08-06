# Data Model: 语义查询与推理引擎

**Feature**: 001-semantic-query-reasoning
**Date**: 2026-08-01

---

## 模型设计原则

- 本 BC 为纯无状态计算层，**无自有数据库表**。所有领域对象为计算过程中的瞬时值对象或结果载体
- 查询结果内联上游实体/关系摘要，确保下游无需额外补查询
- 所有结果载体统一包含 `truncated: boolean` 与 `truncatedReason` 截断标记
- 遵循 DDD 三级分包规范：聚合根 / 实体 / 值对象

---

## 1. 值对象（Value Objects）

### 1.1 FQN

含义：Fully Qualified Name，全局唯一的命名标识。

| 属性 | 类型 | 必填 | 描述 |
|------|------|------|------|
| `value` | String | 是 | 完整 FQN 字符串 |
| `bundleCode` | String | 派生 | Bundle 编码（通过 FQN 解析器提取） |
| `version` | String | 派生 | 版本号 |
| `packageFqn` | String | 派生 | Package FQN 前缀 |
| `segment` | String | 派生 | 最后一段名称（如实体名/关系名） |

约束：不可变（Immutable），通过全属性值相等判断同一性。

### 1.2 GraphPattern

含义：图模式匹配的路径模式描述。

| 属性 | 类型 | 默认 | 描述 |
|------|------|------|------|
| `segments` | `List<PatternSegment>` | — | 路径段序列 |
| `patternLength` | int | 派生 | 段数（≤ 4，即最多 3 条关系边） |

### 1.3 PatternSegment

含义：模式中的单个路径段（Entity —[Relation]—> Entity）。

| 属性 | 类型 | 描述 |
|------|------|------|
| `sourceEntityType` | `FQN \| WILDCARD` | 源实体类型（完整 FQN 或 `*` 通配） |
| `relationType` | `FQN \| WILDCARD` | 关系类型（完整 FQN 或 `?` 通配） |
| `targetEntityType` | `FQN \| WILDCARD` | 目标实体类型（完整 FQN 或 `*` 通配） |

约束：WILDCARD 匹配整个 EntitySchema/RelationSchema FQN，不拆分名称段。

### 1.4 TraversalDepth

含义：遍历深度约束值对象。

| 属性 | 类型 | 默认 | 描述 |
|------|------|------|------|
| `globalMaxDepth` | int | 5 | 全局最大深度（范围 1-10） |
| `perTypeMaxDepths` | `Map<AssociationType, Integer>` | — | 各 AssociationType 差异化深度上限 |
| `effectiveDepth(type)` | int | 派生 | 取 `min(globalMaxDepth, perTypeMaxDepths[type])` |

约束：遍历时取全局深度与类型深度中的较小值作为有效深度。

### 1.5 EntitySnapshot

含义：查询过程中获取的实体瞬时快照（只读）。

| 属性 | 类型 | 描述 |
|------|------|------|
| `fqn` | FQN | 实体 FQN |
| `name` | String | 展示名 |
| `entitySchemaFqn` | FQN | 元模型 EntitySchema FQN |
| `content` | `Map<String, Object>` | 属性内容（JSONB 解析后） |
| `depth` | int | 到达该实体的最短遍历深度 |

### 1.6 RelationSnapshot

含义：查询过程中获取的关系瞬时快照（只读）。

| 属性 | 类型 | 描述 |
|------|------|------|
| `fqn` | FQN | 关系实例 FQN |
| `sourceEntityFqn` | FQN | 源实体 FQN |
| `targetEntityFqn` | FQN | 目标实体 FQN |
| `relationSchemaFqn` | FQN | 元模型 RelationSchema FQN |
| `associationType` | AssociationType | 关联类型枚举 |
| `content` | `Map<String, Object>` | 属性内容（JSONB） |

### 1.7 TransitivityRule

含义：单个 AssociationType 的传导规则配置值对象。

| 属性 | 类型 | 描述 |
|------|------|------|
| `type` | AssociationType | 关联类型 |
| `transitive` | boolean | 是否可传递 |
| `direction` | TraversalDirection | 方向（FORWARD/BACKWARD/DIRECTED/BIDIRECTIONAL） |
| `weightStrategy` | WeightStrategy | 权重策略（MULTIPLY/ADD/MAX/NONE） |
| `maxDepth` | int | 该类型遍历深度上限 |
| `description` | String | 传导语义说明 |

约束：来源于 `metaforge.compute-engine.transitivity-rules` 配置，启动时加载。

### 1.8 PathSegment

含义：路径推理中的单个跳步。

| 属性 | 类型 | 描述 |
|------|------|------|
| `fromEntity` | FQN | 起始实体 FQN |
| `toEntity` | FQN | 终止实体 FQN |
| `relation` | FQN | 关系实例 FQN |
| `relationType` | AssociationType | 关系类型 |
| `direction` | TraversalDirection | 遍历方向（出边/入边） |
| `weight` | double | 该步的权重值（由 weightStrategy 计算） |

### 1.9 InfluenceScope

含义：影响溯源的范围描述值对象。

| 属性 | 类型 | 描述 |
|------|------|------|
| `centerEntityFqn` | FQN | 中心实体 FQN |
| `direction` | TraversalDirection | 正向影响 / 反向依赖 / 双向 |
| `maxDepth` | int | 最大影响深度 |
| `relationTypes` | `List<AssociationType>` | 关注的关系类型（空=全类型） |

### 1.10 FilterCriteriaVO

含义：领域层过滤条件值对象（对应 API 层 FilterCriteria DTO）。

| 属性 | 类型 | 描述 |
|------|------|------|
| `associationTypes` | `Set<AssociationType>` | 关联类型过滤集合 |
| `sourceFqns` | `List<FqnFilter>` | 源实体 FQN 过滤 + 匹配模式 |
| `targetFqns` | `List<FqnFilter>` | 目标实体 FQN 过滤 + 匹配模式 |
| `relationInstanceFqns` | `List<FqnFilter>` | 关系实例 FQN 过滤 + 匹配模式 |
| `entityTypes` | `List<FqnFilter>` | 实体类型（EntitySchema FQN）过滤 |
| `relationTypes` | `List<FqnFilter>` | 关系类型（RelationSchema FQN）过滤 |
| `propertyFilters` | `List<PropertyFilter>` | 属性字段精确等值匹配列表 |

每个 FqnFilter 包含：`value: String` + `matchMode: MatchMode`（PREFIX/EXACT/PATTERN）。
维度间 AND 逻辑，维度内集合 OR 逻辑。

---

## 2. 领域实体（Domain Entities）

### 2.1 TraversalPath

含义：遍历过程中追踪的路径实体。

| 属性 | 类型 | 描述 |
|------|------|------|
| `pathId` | String | 路径标识（聚合内唯一） |
| `segments` | `List<PathSegment>` | 路径段序列 |
| `totalWeight` | double | 路径总权重（按 weightStrategy 累加/连乘） |

### 2.2 ClosuredEntity

含义：传递闭包中的单个可达实体。

| 属性 | 类型 | 描述 |
|------|------|------|
| `fqn` | FQN | 实体 FQN |
| `depth` | int | 到达的最短深度 |
| `arrivedByTypes` | `Set<AssociationType>` | 到达该实体途经的关系类型集合 |

### 2.3 ImpactEntity

含义：影响溯源中的受影响实体。

| 属性 | 类型 | 描述 |
|------|------|------|
| `fqn` | FQN | 实体 FQN |
| `depth` | int | 影响传播层级 |
| `impactPaths` | `List<TraversalPath>` | 影响传导路径列表 |
| `affectedByTypes` | `Set<AssociationType>` | 影响传导途经的关系类型 |

---

## 3. 聚合根（Aggregate Roots）

### 3.1 GraphQuery（图查询聚合根）

职责：封装单次图查询的完整生命周期——接收查询参数、执行图遍历、收集结果、应用过滤、组装输出。

**聚合内实体**：无（查询为无状态操作，不持有可变状态实体）

**核心业务方法**：
- `execute()` — 执行图遍历，返回 `GraphQueryResult`
- `applyFilters(FilterCriteriaVO)` — 应用 7 维过滤条件
- `collectEntities()` — 收集遍历路径中的不重复实体
- `collectRelations()` — 收集遍历路径中的关系边
- `computeAdjacency()` — 构建邻接映射
- `assembleResult()` — 组装结果载体

**业务规则**：
- 遍历深度受 `TraversalDepth` 约束（全局 + per-type）
- 同一实体多路径出现时仅返回一次，标注最短深度
- 被过滤内容不参与遍历且不计入深度
- 超时/深度超限/数量超限返回截断标记

### 3.2 PathQuery（路径推理聚合根）

职责：封装单次路径推理——路径搜索、闭包计算、多跳推理、可达性判定。

**核心业务方法**：
- `findPaths()` — 两点间路径搜索
- `computeClosure()` — 传递闭包推理
- `multiHopTraverse()` — 多跳语义推理
- `checkReachability()` — 路径可达性判定

**业务规则**：
- 路径推理仅沿传导规则中 `transitive=true` 的关系类型展开
- 多跳推理每步跳跃需满足传导兼容性
- 循环引用自动去重截断
- 最大跳跃步数 ≤ 3
- 可达性判定找到首条路径即返回（早期终止）

### 3.3 ImpactQuery（影响溯源聚合根）

职责：封装单次影响溯源——正向影响扩散、反向依赖追溯、影响路径详情。

**核心业务方法**：
- `diffuseForward()` — 正向影响扩散（BFS 沿指定关系类型正向扩展）
- `traceBackward()` — 反向依赖溯源（BFS 沿入边反向追溯）
- `getImpactPaths(source, target)` — 查询两点间的所有影响传导路径

**业务规则**：
- 沿指定关系类型 BFS 扩展
- 同一实体被多路径影响时仅统计一次
- 影响路径按长度排序
- 路径内联实体与关系摘要

---

## 4. 结果载体（Result DTOs）

### 4.1 GraphQueryResult

| 属性 | 类型 | 描述 |
|------|------|------|
| `entities` | `List<EntitySummary>` | 实体集合（去重，含内联摘要） |
| `relations` | `List<RelationSummary>` | 关系集合（去重） |
| `adjacencyMap` | `Map<String, List<String>>` | 实体 FQN → 关联关系 FQN 列表 |
| `truncated` | boolean | 是否截断 |
| `truncatedReason` | TruncatedReason | 截断原因枚举 |

### 4.2 PathResult

| 属性 | 类型 | 描述 |
|------|------|------|
| `paths` | `List<TraversalPath>` | 路径列表（按长度排序） |
| `totalPaths` | int | 路径总数 |
| `truncated` | boolean | 是否截断 |
| `truncatedReason` | TruncatedReason | 截断原因枚举 |

### 4.3 ClosureResult

| 属性 | 类型 | 描述 |
|------|------|------|
| `layers` | `Map<Integer, List<ClosuredEntity>>` | 按层级分组的可达实体 |
| `totalReachable` | int | 可达实体总数 |
| `relationTypeStats` | `Map<AssociationType, Integer>` | 途经关系类型统计 |
| `truncated` | boolean | 是否截断 |
| `truncatedReason` | TruncatedReason | 截断原因枚举 |

### 4.4 ImpactTraceResult

| 属性 | 类型 | 描述 |
|------|------|------|
| `totalImpacted` | int | 影响实体总数 |
| `layerStats` | `Map<Integer, List<ImpactEntity>>` | 按层级分组的影响实体 |
| `typeStats` | `Map<String, Integer>` | 按实体类型分层统计数 |
| `entities` | `List<ImpactEntity>` | 影响实体明细 |
| `relations` | `List<RelationSummary>` | 关联关系明细 |
| `truncated` | boolean | 是否截断 |
| `truncatedReason` | TruncatedReason | 截断原因枚举 |

### 4.5 EntitySummary（内联摘要）

| 属性 | 类型 | 描述 |
|------|------|------|
| `fqn` | String | 实体 FQN |
| `name` | String | 展示名 |
| `entitySchemaFqn` | String | 元模型 EntitySchema FQN |

### 4.6 RelationSummary（内联摘要）

| 属性 | 类型 | 描述 |
|------|------|------|
| `fqn` | String | 关系实例 FQN |
| `associationType` | AssociationType | 关联类型 |
| `sourceEntityFqn` | String | 源实体 FQN |
| `targetEntityFqn` | String | 目标实体 FQN |

---

## 5. 枚举

### 5.1 AssociationType

| 值 | 描述 |
|----|------|
| `COMPOSITION` | 组合关系 |
| `DEPENDENCY_INFLUENCE` | 依赖/影响关系 |
| `PROCESS_SEQUENCE` | 流程序列关系 |
| `ASSOCIATION_REFERENCE` | 关联引用关系 |
| `MAPPING_CORRESPONDENCE` | 映射对应关系 |

### 5.2 WeightStrategy

| 值 | 描述 |
|----|------|
| `MULTIPLY` | 连乘（概率/强度衰减） |
| `ADD` | 累加（步长/成本/耗时求和） |
| `MAX` | 取最大（置信度合并） |
| `NONE` | 无权重计算 |

### 5.3 TraversalDirection

| 值 | 描述 |
|----|------|
| `FORWARD` | 正向（沿出边） |
| `BACKWARD` | 反向（沿入边） |
| `DIRECTED` | 单向（不隐含反向传递性） |
| `BIDIRECTIONAL` | 双向 |

### 5.4 MatchMode

| 值 | 描述 |
|----|------|
| `PREFIX` | 前缀匹配 |
| `EXACT` | 精确匹配 |
| `PATTERN` | 模式匹配（仅 relationInstanceFqns 支持，按 FQN 格式做 LIKE 匹配） |

### 5.5 TruncatedReason

| 值 | 描述 |
|----|------|
| `DEPTH_EXCEEDED` | 遍历深度超限 |
| `COUNT_EXCEEDED` | 结果数量超限 |
| `TIMEOUT` | 查询超时中断 |

---

## 6. 领域服务接口（端口）

### 6.1 EntityDataPort

```java
public interface EntityDataPort {
    EntitySnapshot findByFqn(FQN fqn);
    List<EntitySnapshot> findByFqnPrefixes(List<String> fqnPrefixes, int limit);
    List<EntitySnapshot> findByEntitySchemaFqn(String entitySchemaFqn, int limit);
    List<EntitySnapshot> batchFindByFqns(List<FQN> fqns);
}
```

### 6.2 RelationDataPort

```java
public interface RelationDataPort {
    List<RelationSnapshot> findOutboundRelations(FQN entityFqn, Set<AssociationType> types, int limit);
    List<RelationSnapshot> findInboundRelations(FQN entityFqn, Set<AssociationType> types, int limit);
    List<RelationSnapshot> findRelations(FQN entityFqn, TraversalDirection direction, Set<AssociationType> types, int limit);
    RelationSnapshot findByFqn(FQN relationFqn);
}
```

### 6.3 MetamodelSemanticPort

```java
public interface MetamodelSemanticPort {
    EntitySchemaDto getEntitySchema(String fqn);
    RelationSchemaDto getRelationSchema(String fqn);
    boolean isEntitySchemaExists(String fqn);
    boolean isRelationSchemaExists(String fqn);
}
```

---

## 7. 上游参考数据表结构

本 BC 通过 jOOQ 跨 Schema 只读查询以下上游表（表结构定义属于上游 BC，此处仅列出本 BC 查询涉及的关键列）：

### 7.1 metadata_management.metadata_entity（实体主表）

| 列名 | 类型 | 用途 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `fqn` | VARCHAR | 实体 FQN（唯一，索引） |
| `name` | VARCHAR | 展示名 |
| `entity_schema_fqn` | VARCHAR | 元模型 EntitySchema FQN |
| `content` | JSONB | 属性内容 |
| `status` | VARCHAR | 状态（ACTIVE 参与查询） |
| `current_version` | INTEGER | 当前版本号 |

### 7.2 semantic_relation_network.relation_instance（关系主表）

| 列名 | 类型 | 用途 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `fqn` | VARCHAR | 关系实例 FQN（索引） |
| `source_entity_fqn` | VARCHAR | 源实体 FQN（索引） |
| `target_entity_fqn` | VARCHAR | 目标实体 FQN（索引） |
| `relation_schema_fqn` | VARCHAR | 元模型 RelationSchema FQN |
| `association_type` | VARCHAR | AssociationType 枚举值 |
| `content` | JSONB | 属性内容 |
| `status` | VARCHAR | 状态（ACTIVE 参与查询） |

### 7.3 semantic_relation_network.entity_relation_index（双向索引）

| 列名 | 类型 | 用途 |
|------|------|------|
| `entity_fqn` | VARCHAR | 实体 FQN（索引） |
| `relation_fqn` | VARCHAR | 关系实例 FQN |
| `direction` | VARCHAR | 方向（OUTBOUND/INBOUND） |
