---
id: agent-cognition.port
protocol: Java Interface
version: 1.0.0
owner: metaforge-agent-cognition
description: 认知引擎上游只读端口契约。通过 MetamodelReadPort/MetadataReadPort/GraphReadPort/ComputeEngineReadPort 消费上游四 BC 公开 API，全部为生效态只读查询。
type: business
---

# Port Contract: metaforge-agent-cognition

**Protocol**: Java Interface（进程内依赖倒置接口，由 `-core` 基础设施层适配器实现）
**Module**: `metaforge-agent-cognition-api`
**Version**: 1.0.0

> 本 BC 为纯无状态计算层，不持有数据存储主权。所有数据与计算能力通过以下 Port 接口从上游四 BC 获取。Port 接口定义于 `-api` 模块，由 `-core` 的 `infrastructure/adapter` 适配器实现，供 `-core` 与未来 `-dimensions` 模块共享。禁止在 core 层直接注入上游 api Service。

---

## Interface Definition

### MetamodelReadPort

**职责**: 元模型治理 BC 只读端口，生效态数据查询。上游 Provider: `metamodel-governance`。

```java
package com.metaforge.agent.cognition.api.port;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;

import java.util.List;

/**
 * 元模型治理 BC 只读端口，生效态数据查询。
 * 上游 Provider: metamodel-governance (BundleManagementService, ElementDefinitionService, PackageManagementService, ExportManifestService, BundleDependencyService, BundleVersionManagementService)
 */
public interface MetamodelReadPort {

    Object getBundle(String fqn);

    PageResult<?> listBundles(PageRequest pageRequest);

    Object getEntitySchema(String fqn);

    PageResult<?> listEntitySchemas(Object query);

    Object getRelationSchema(String fqn);

    PageResult<?> listRelationSchemas(Object query);

    List<?> listPackages(String bundleVersionFqn);

    Object getExport(String bundleVersionFqn);

    boolean isPackageExported(String bundleVersionFqn, String packageFqn);

    Object getDependencyGraph(String bundleFqn);

    List<?> listBundleVersions(String bundleFqn);
}
```

**输入/输出定义**:

| 方法 | 输入参数 | 返回类型 | 描述 |
|------|---------|---------|------|
| getBundle | fqn: String | BundleDto | 按 FQN 查询 Bundle（生效态） |
| listBundles | pageRequest: PageRequest | PageResult\<BundleDto\> | 分页查询 Bundle 列表 |
| getEntitySchema | fqn: String | EntitySchemaDto | 按 FQN 查询 EntitySchema（生效态） |
| listEntitySchemas | query: ElementQueryRequest | PageResult\<EntitySchemaDto\> | 按 FQN 前缀过滤查询 |
| getRelationSchema | fqn: String | RelationSchemaDto | 按 FQN 查询 RelationSchema（生效态） |
| listRelationSchemas | query: ElementQueryRequest | PageResult\<RelationSchemaDto\> | 按 FQN 前缀过滤查询 |
| listPackages | bundleVersionFqn: String | List\<PackageDto\> | 列出版本下所有 Package |
| getExport | bundleVersionFqn: String | ExportManifestDto | 查询导出清单 |
| isPackageExported | bundleVersionFqn: String, packageFqn: String | boolean | 判断 Package 是否导出 |
| getDependencyGraph | bundleFqn: String | DependencyGraphDto | 获取 Bundle 依赖图 |
| listBundleVersions | bundleFqn: String | List\<BundleVersionDto\> | 查询 Bundle 所有版本 |

### MetadataReadPort

**职责**: 元数据管理 BC 只读端口，生效态数据查询。上游 Provider: `metadata-management`。

```java
package com.metaforge.agent.cognition.api.port;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;

import java.util.List;

/**
 * 元数据管理 BC 只读端口，生效态数据查询。
 * 上游 Provider: metadata-management (MetadataQueryService)
 */
public interface MetadataReadPort {

    Object getByFqn(String fqn);

    PageResult<?> listByFqnPrefixes(List<String> fqnPrefixes, PageRequest pageRequest);

    PageResult<?> listByEntitySchema(String entitySchemaFqn, PageRequest pageRequest);
}
```

**输入/输出定义**:

| 方法 | 输入参数 | 返回类型 | 描述 |
|------|---------|---------|------|
| getByFqn | fqn: String | MetadataEntityDto | FQN 精准查询生效实体 |
| listByFqnPrefixes | fqnPrefixes: List\<String\>, pageRequest: PageRequest | PageResult\<MetadataEntityDto\> | FQN 前缀范围查询 |
| listByEntitySchema | entitySchemaFqn: String, pageRequest: PageRequest | PageResult\<MetadataEntityDto\> | 按 EntitySchema 类型查询 |

### GraphReadPort

**职责**: 语义关系网络 BC 只读端口，生效态数据查询。上游 Provider: `semantic-relation-network`。

```java
package com.metaforge.agent.cognition.api.port;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;

import java.util.List;

/**
 * 语义关系网络 BC 只读端口，生效态数据查询。
 * 上游 Provider: semantic-relation-network (RelationQueryService, RelationTopologyService)
 */
public interface GraphReadPort {

    Object getByFqn(String fqn);

    List<?> getOutboundRelations(String entityFqn, String relationType, String targetEntityType);

    List<?> getInboundRelations(String entityFqn, String relationType, String sourceEntityType);

    PageResult<?> multiFilter(Object request);

    Object getRelationCount(String entityFqn);

    PageResult<?> listByConditions(String fqnPrefix, String relationSchemaFqn, PageRequest pageRequest);
}
```

**输入/输出定义**:

| 方法 | 输入参数 | 返回类型 | 描述 |
|------|---------|---------|------|
| getByFqn | fqn: String | RelationInstanceDto | FQN 精准查询生效关系 |
| getOutboundRelations | entityFqn: String, relationType: String, targetEntityType: String | List\<RelationInstanceDto\> | 查询实体出边关系 |
| getInboundRelations | entityFqn: String, relationType: String, sourceEntityType: String | List\<RelationInstanceDto\> | 查询实体入边关系 |
| multiFilter | request: RelationQueryRequest | PageResult\<RelationInstanceDto\> | 多维过滤查询 |
| getRelationCount | entityFqn: String | RelationCount | 查询实体的出入边计数 |
| listByConditions | fqnPrefix: String, relationSchemaFqn: String, pageRequest: PageRequest | PageResult\<RelationInstanceDto\> | 条件查询 |

### ComputeEngineReadPort

**职责**: 语义查询引擎 BC 只读端口，全量查询与推理能力。上游 Provider: `semantic-query-engine`。全部 compute-engine 对外能力收敛于此 Port。

```java
package com.metaforge.agent.cognition.api.port;

import com.metaforge.common.dto.PageResult;

import java.util.List;

/**
 * 语义查询引擎 BC 只读端口，全量查询与推理能力。
 * 上游 Provider: semantic-query-engine (GraphQueryService, PathReasoningService, ImpactTracingService)
 */
public interface ComputeEngineReadPort {

    Object queryAdjacency(Object request);

    Object queryCompositionTree(Object request);

    Object querySubgraph(Object request);

    Object queryPatternMatch(Object request);

    PageResult<?> searchCompound(Object request);

    Object queryBatch(Object request);

    Object findPaths(Object request);

    Object computeClosure(Object request);

    Object multiHopReasoning(Object request);

    Object checkReachability(Object request);

    Object diffuseForward(Object request);

    Object traceBackward(Object request);

    Object getImpactPaths(String sourceFqn, String targetFqn, List<?> relationTypes, int maxDepth);
}
```

**输入/输出定义**:

| 方法 | 输入参数 | 返回类型 | 描述 |
|------|---------|---------|------|
| queryAdjacency | request: AdjacencyQueryRequest | GraphQueryResult | 多度邻接查询 |
| queryCompositionTree | request: CompositionTreeQueryRequest | GraphQueryResult | 组合层级树查询 |
| querySubgraph | request: SubgraphQueryRequest | GraphQueryResult | 子图提取查询 |
| queryPatternMatch | request: PatternMatchRequest | GraphQueryResult | 图模式匹配查询 |
| searchCompound | request: CompoundSearchRequest | PageResult\<EntitySummary\> | 多条件复合检索 |
| queryBatch | request: BatchQueryRequest | GraphQueryResult | 批量语义查询 |
| findPaths | request: PathQueryRequest | PathResult | 两点间路径查询 |
| computeClosure | request: ClosureQueryRequest | ClosureResult | 传递闭包推理 |
| multiHopReasoning | request: MultiHopQueryRequest | PathResult | 多跳语义推理 |
| checkReachability | request: ReachabilityCheckRequest | PathResult | 路径可达性判定 |
| diffuseForward | request: ImpactDiffusionRequest | ImpactTraceResult | 正向影响扩散 |
| traceBackward | request: ImpactDiffusionRequest | ImpactTraceResult | 反向依赖溯源 |
| getImpactPaths | sourceFqn: String, targetFqn: String, relationTypes: List, maxDepth: int | ImpactTraceResult | 影响路径详情 |

---

## 调用边界约束（Special Constraints）

- **只读边界**: 所有 Port 方法均为生效态数据查询，不涉及草稿态、历史版本；本 BC 不执行任何写操作，不持有数据主权。
- **统一访问入口**: 对上游 BC 的访问统一收敛于对应 Port 接口，禁止在 `-core` 层直接注入上游 api Service，必须经 `-core` 的 `infrastructure/adapter` 适配器间接调用。
- **能力收敛**: `ComputeEngineReadPort` 收敛 compute-engine 全部对外能力（GraphQueryService、PathReasoningService、ImpactTracingService），确保本 BC 对 compute-engine 的访问只有唯一入口。
- **上游不可用**: 调用上游 BC 失败统一映射为 `UPSTREAM_UNAVAILABLE`(34011)（错误码定义见 `agent-cognition.application-service` 与 `agent-cognition.rest-api` 契约）。
