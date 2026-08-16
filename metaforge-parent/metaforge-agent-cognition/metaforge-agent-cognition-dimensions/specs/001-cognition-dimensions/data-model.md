# Data Model: 认知算子实现层

**Feature**: 001-cognition-dimensions | **Date**: 2026-08-11

> 本模块为无状态计算层，不持有持久化数据。以下实体为算子运行时使用的内存数据结构，均来自 `-api` 契约层（本模块不定义新 DTO）。

## 核心实体（来自上游 API 契约）

### CognitionOperator（SPI 接口）

算子接口——本 BC 的核心实现目标，每个算子为一个实现类（Spring Bean）。

| 属性 | 类型 | 描述 |
|------|------|------|
| operatorId | String | 算子唯一标识（如 `ontological.bundle-discovery`），命名格式 `{category}.{能力名}` |
| category | DimensionCategory | 所属 8 分类枚举 |
| execute(context) -> CognitionResult | 方法 | 执行认知查询，返回结果 |

### CognitionQueryContext（入参 record）

`execute()` 的唯一入参，由 `-core` 的引擎在调用算子前构造。

| 字段 | 类型 | 描述 |
|------|------|------|
| templateId | String | 模板标识 |
| operatorId | String | 算子标识 |
| category | DimensionCategory | 算子分类 |
| scope | Scope | 认知边界（bundles/packages/domainGroups/domains/entitySchemas） |
| bundleFqns | List\<String\> | scope 解析后的 Bundle FQN 列表 |
| entityFqn | String | 查询目标实体 FQN |
| templateParams | Map\<String, Object\> | 模板专用参数 |
| agentArchetype | AgentArchetype | Agent 原型 |
| cognitionDepth | CognitionDepth | 认知深度 |
| cursor | String | 分页游标 |
| pageSize | int | 每页大小 |
| timeoutMs | long | 执行超时（毫秒） |

### CognitionResult（出参 record）

算子执行结果，每个 `execute()` 调用必须返回此对象。

| 字段 | 类型 | 描述 |
|------|------|------|
| operatorId | String | 算子标识 |
| category | DimensionCategory | 算子分类 |
| data | Object | 结果数据——算子产出，各算子语义不同。Lazy 模式下为 `{ data, has_children, suggested_next_call }` |
| success | boolean | 是否成功执行 |
| error | String | 失败时的错误信息（success=false 时携带） |

**工厂方法**:
- `CognitionResult.success(operatorId, category, data)` → success=true
- `CognitionResult.failure(operatorId, category, error)` → success=false

### DimensionCategory（枚举）

| 值 | displayName | layer |
|----|------------|-------|
| ONTOLOGICAL | 本体论 | object |
| STRUCTURAL | 结构论 | object |
| RELATIONAL | 关系论 | object |
| PROCEDURAL | 流程论 | object |
| DEONTIC | 约束论 | action |
| CAPABILITY | 能力论 | action |
| EPISTEMIC | 认知论 | meta |
| GOVERNANCE | 治理 | meta |

### Scope（边界 record）

| 字段 | 类型 |
|------|------|
| bundles | List\<String\> |
| packages | List\<String\> |
| domainGroups | List\<String\> |
| domains | List\<String\> |
| entitySchemas | List\<String\> |

### Port 接口（上游只读访问通道）

| Port | 上游 Provider | 关键方法 |
|------|--------------|---------|
| MetamodelReadPort | metamodel-governance | getBundle, listBundles, listPackages, getEntitySchema, listEntitySchemas, getRelationSchema, listRelationSchemas, getExport, isPackageExported, getDependencyGraph, listBundleVersions |
| MetadataReadPort | metadata-management | getByFqn, listByFqnPrefixes, listByEntitySchema |
| GraphReadPort | semantic-relation-network | getOutboundRelations, getInboundRelations, multiFilter, getRelationCount, listByConditions |
| ComputeEngineReadPort | semantic-query-engine | queryAdjacency, queryCompositionTree, findPaths, diffuseForward, traceBackward, getImpactPaths, querySubgraph, computeClosure, multiHopReasoning, checkReachability |

## 本模块内部结构（无持久化）

### AbstractCognitionOperator（抽象基类）

| 字段 | 类型 | 描述 |
|------|------|------|
| metamodelReadPort | MetamodelReadPort | @Autowired，元模型查询 |
| metadataReadPort | MetadataReadPort | @Autowired，元数据查询 |
| graphReadPort | GraphReadPort | @Autowired，图谱查询 |
| computeEngineReadPort | ComputeEngineReadPort | @Autowired，计算引擎查询 |

| 方法 | 描述 |
|------|------|
| applyScope(data, scope) | 按 scope 裁剪结果，标注越界实体 |
| buildLazyNode(data, hasChildren, nextCall) | 构造惰性节点 Map |
| wrapFailure(error) | 统一失败 CognitionResult 工厂 |
| executeWithPort(supplier) | Port 调用异常模板——捕获异常返回 failure |

## 算子与 Port 映射关系

| 分类 | 算子数 | 依赖 Port |
|------|-------|----------|
| ONTOLOGICAL | 7 | MetamodelReadPort, MetadataReadPort, GraphReadPort |
| STRUCTURAL | 3 | MetadataReadPort, GraphReadPort, ComputeEngineReadPort |
| RELATIONAL | 3 | GraphReadPort, ComputeEngineReadPort |
| PROCEDURAL | 3 | GraphReadPort, ComputeEngineReadPort |
| DEONTIC | 3 | GraphReadPort, MetadataReadPort |
| CAPABILITY | 3 | GraphReadPort, MetadataReadPort |
| EPISTEMIC | 1 | 无（纯 context_meta 操作） |
| GOVERNANCE | 1 | GraphReadPort, MetadataReadPort, MetamodelReadPort |
