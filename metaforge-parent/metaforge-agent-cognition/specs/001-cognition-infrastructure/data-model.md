# Data Model: 认知基础架构层

**Feature**: `001-cognition-infrastructure` | **Date**: 2026-08-11

> 本 BC 无自有数据表、无 JPA 实体、无 jOOQ 记录、无 Flyway 脚本。所有数据来自上游 BC 查询（经 Port 接口）或调用方传入。以下为领域模型（内存对象）的完整定义。

---

## 1. 聚合根（Aggregate）

### CognitionQuery

认知查询聚合根 —— 单次认知查询请求的全生命周期聚合。

| 字段 | 类型 | 描述 |
|------|------|------|
| templateId | TemplateId | 模板唯一标识 |
| request | CognitionRequest | 原始请求参数 |
| templateDefinition | TemplateDefinition | 解析后的模板定义 |
| operators | List\<OperatorDefinition\> | 解析并过滤后的算子清单 |
| scope | Scope | 经校验的认知边界 |
| outputFormat | OutputFormat | 输出格式（JSON/PROMPT） |
| agentArchetype | AgentArchetype | Agent 原型 |
| cognitionDepth | CognitionDepth | 认知深度（L1/L2/L3） |
| tokenBudget | TokenBudget | Token 预算上限 |
| executionResults | Map\<OperatorId, CognitionResult\> | 算子执行结果映射 |

**核心业务规则（内聚在聚合根）**：
1. **算子加载与编排**：从 `TemplateRegistry` 解析模板获取 `operators` 清单 → `OperatorRegistry` 加载算子实例 → 校验
2. **Scope 校验**：若 `scopeRequired=true` 且 scope 为空 → `MISSING_SCOPE(34005)`；若 scope 中 bundle FQN 无效 → `INVALID_SCOPE(34003)`
3. **Archetype 过滤**：按模板算子条目的 `archetypes` 白名单过滤，若无匹配 → `ARCHETYPE_NOT_SUPPORTED(34012)`
4. **Token 预算分配**：若 `max_tokens < 500` → 自动降为 L1
5. **深度裁剪**：required=true 算子豁免；required=false 算子按 priority + 比例裁剪
6. **结果聚合**：按 category 分组输出为 `dimensions.<category>`

---

## 2. 领域实体（Entity）

### TemplateDefinition

模板定义实体 —— YAML 模板文件在内存中的结构化表示。

| 字段 | 类型 | 描述 |
|------|------|------|
| templateId | String | 模板唯一标识（大写字母+数字+下划线） |
| templateName | String | 模板中文名 |
| description | String | 模板描述 |
| operators | List\<OperatorDefinition\> | 跨分类扁平算子列表 |
| inputSchema | InputSchema | 入参 JSON Schema |
| scopeBehavior | ScopeBehavior | scope 行为配置 |
| outputSchema | OutputSchema | 输出结构配置 |
| config | Map\<String, Object\> | 模板级配置数据，支持双层结构 `{ global, operators }`：`global`=全模板共享（经 CognitionQueryContext.templateConfig 透传，兼容单层结构如 ORIENT 的 `levelAliases`）；`operators.{operatorId}`=算子级精确配置（经 CognitionQueryContext.operatorConfig 透传） |

**校验规则**：
- `templateId` 非空，仅含大写字母、数字、下划线
- `operators` 至少声明一个算子条目
- 所有 `operatorId` 在 `OperatorRegistry` 中可解析
- 所有 `archetypes` 值为 `AgentArchetype` 枚举子集
- 所有 `priority` 为非负整数

### OperatorDefinition

算子定义实体 —— 模板中单个算子条目的配置表示。

| 字段 | 类型 | 描述 |
|------|------|------|
| operatorId | String | 算子标识（如 `ontological.bundle-discovery`） |
| priority | int | 执行优先级（越大越优先，默认 0） |
| required | boolean | 是否强制保留（true=豁免裁剪） |
| timeoutMs | long | 算子执行超时（毫秒） |
| archetypes | Set\<AgentArchetype\> | 可用 Agent 原型白名单 |

**校验规则**：
- `operatorId` 在 `OperatorRegistry` 中可解析
- `priority` ≥ 0
- `archetypes` ⊆ `AgentArchetype` 枚举全集（4 值）
- 若 `archetypes` 为空，视为对所有原型可用

---

## 3. 值对象（Value Object）

### TemplateId

模板唯一标识值对象。

| 字段 | 类型 |
|------|------|
| value | String |

**校验**：符合正则 `[A-Z][A-Z0-9_]+`，不允许小写、不允许特殊符号（下划线除外）

### OperatorId

算子唯一标识值对象。

| 字段 | 类型 |
|------|------|
| value | String |

**格式**：`{分类前缀}.{能力名}`（如 `ontological.bundle-discovery`、`relational.direct-link`）

### DimensionCategory

认知分类枚举 —— 8 值封闭集合。

| 枚举值 | displayName | layer |
|--------|-------------|-------|
| ONTOLOGICAL | 本体论 | object |
| STRUCTURAL | 结构论 | object |
| RELATIONAL | 关系论 | object |
| PROCEDURAL | 流程论 | object |
| DEONTIC | 约束论 | action |
| CAPABILITY | 能力论 | action |
| EPISTEMIC | 认知论 | meta |
| GOVERNANCE | 治理 | meta |

### AgentArchetype

Agent 原型枚举 —— 4 值封闭集合。

| 枚举值 | 描述 |
|--------|------|
| EXECUTION | 执行型 Agent |
| EXPLORATION | 探索型 Agent |
| AUDIT | 审计型 Agent |
| ORCHESTRATION | 编排型 Agent |

### CognitionDepth

认知深度枚举 —— 3 值。

| 枚举值 | 保留比例 (required=false) |
|--------|--------------------------|
| L1 | 1/3（不低于 min-keep） |
| L2 | 2/3（不低于 min-keep） |
| L3 | 全量（不裁剪） |

### Scope

认知边界五字段值对象——定义于 `-api` 模块作为共享 DTO，贯穿 API 入参→校验→算子上下文→输出裁剪全管线。

| 字段 | 类型 | 描述 |
|------|------|------|
| bundles | List\<String\> | Bundle FQN 列表（如 `["order:1.0.0"]`） |
| packages | List\<String\> | Package FQN 列表 |
| domainGroups | List\<String\> | 域组 FQN 列表 |
| domains | List\<String\> | 域 FQN 列表 |
| entitySchemas | List\<String\> | EntitySchema FQN 列表 |

**特殊值**：`Scope.EMPTY` — 无边界约束（全量无过滤）

### ScopeBehavior

模板 scope 行为策略值对象 —— 驱动 ScopeResolutionService 的校验规则与 DELEGATE 模板的 scope 收窄/产出逻辑。

| 字段 | 类型 | 描述 |
|------|------|------|
| acceptsScope | boolean | 模板是否接受 scope 入参（false 则忽略 scope，按 Scope.EMPTY 全量执行） |
| scopeRequired | boolean | 模板是否强制要求 scope。true 时请求不带 scope → MISSING_SCOPE(34005) |
| producesUpdatedScope | boolean | 模板执行后是否产出收窄后的新 scope（DELEGATE 模板用） |
| scopeFields | List\<String\> | 该模板实际消费的五字段子集（如仅 [bundles, domains]） |

**校验规则**：`scopeRequired=true` 自动修正 `acceptsScope=true`（FR-008）；DELEGATE 模板两者均 true 且 `producesUpdatedScope=true`。

### DataVersionAnchor

数据版本锚值对象 —— 记录各 Bundle 的版本快照。

| 字段 | 类型 |
|------|------|
| bundleFqn | String |
| versionFqn | String |
| resolvedAt | Instant |

### TokenBudget

Token 预算值对象。

| 字段 | 类型 | 描述 |
|------|------|------|
| maxTokens | int | 最大 Token 数 |
| estimated | int | 当前估算 Token 数 |

**规则**：`maxTokens < 500` → 自动降为 L1 深度

### Priority

算子优先级值对象。

| 字段 | 类型 | 描述 |
|------|------|------|
| value | int | 优先级（≥ 0，越大越优先） |

### OutputFormat

输出格式枚举 —— 支持 SPI 扩展。

| 枚举值 | Source |
|--------|--------|
| JSON | 内置 |
| PROMPT | 内置 |

**扩展**：新增格式 = 实现 `OutputFormatter` SPI + 在 `OutputFormat` 枚举中注册新值

---

## 4. 注册表（Registry）

### TemplateRegistry

模板注册表 —— 内存缓存（`ConcurrentHashMap<String, TemplateDefinition>`）。

| 操作 | 描述 |
|------|------|
| `resolve(templateId): TemplateDefinition` | 按 ID 获取模板定义 |
| `register(template): void` | 注册模板（原子替换） |
| `unregister(templateId): void` | 注销模板 |
| `listAll(): List<TemplateDefinition>` | 列出所有已注册模板 |

### OperatorRegistry

算子注册表 —— 内存缓存（`Map<String, CognitionOperator>`）。

| 操作 | 描述 |
|------|------|
| `resolve(operatorId): CognitionOperator` | 按 ID 获取算子 Bean |
| `register(operator): void` | 注册算子（启动时一次性完成） |
| `validate(): ValidationResult` | 校验所有算子 category 声明的合法性与一致性 |

---

## 5. SPI 接口（Contract Layer）

### CognitionOperator

认知算子 SPI 接口（定义于 `-api` 模块）。

| 方法 | 返回类型 | 描述 |
|------|----------|------|
| `operatorId()` | String | 算子唯一标识 |
| `category()` | DimensionCategory | 所属 8 分类 |
| `execute(CognitionQueryContext)` | CognitionResult | 执行认知计算 |

### CognitionQueryContext

算子查询上下文 record（定义于 `-api` 模块）。

| 字段 | 类型 |
|------|------|
| templateId | String |
| operatorId | String |
| category | DimensionCategory |
| scope | Scope |
| bundleFqns | List\<String\> |
| entityFqn | String |
| templateParams | Map\<String, Object\> |
| agentArchetype | AgentArchetype |
| cognitionDepth | CognitionDepth |
| cursor | Integer |
| pageSize | int |
| templateConfig | Map\<String, Object\> |
| operatorConfig | Map\<String, Object\> |

### CognitionResult

算子执行结果 record（定义于 `-api` 模块）。

| 字段 | 类型 |
|------|------|
| operatorId | String |
| category | DimensionCategory |
| data | Object |
| success | boolean |
| error | String |

### OutputFormatter

输出格式化 SPI 接口（定义于 `-api` 模块）。

| 方法 | 返回类型 | 描述 |
|------|----------|------|
| `supports(OutputFormat)` | boolean | 是否支持该格式 |
| `format(TemplateDefinition, Map<DimensionCategory, List<CognitionResult>>, ContextMeta)` | Object | 格式化输出 |

---

## 6. 领域服务（Domain Service）

| Service | 职责 |
|---------|------|
| `ScopeResolutionService` | scope 校验与边界过滤；调用上游 Port 校验 bundle FQN 有效性 |
| `TemplateResolutionService` | 模板解析 —— 从 `TemplateRegistry` 解析模板 → 拆解算子清单 → 注入默认值 |
| `OperatorOrchestrationService` | 算子加载（`OperatorRegistry`）、archetype 白名单过滤、按 priority 排序调度执行 |
| `OutputAssemblyService` | 收集算子结果 → 调用 `OutputFormatter` SPI 组装 → 注入 `ContextMeta` |
| `ContextMetaService` | 生成 `version_anchors`（调用 `MetamodelReadPort`）、`scope_applied`、`token_estimate` |
| `DelegatedScopeService` | DELEGATE 模板的三层收窄与交集合并规则（父 scope ∩ 子任务范围） |
| `DepthTrimmingService` | 按 depth + priority + required 裁剪算子结果（比例制 + 最小保留数） |
| `ArchetypeFilterService` | 按请求 `agent_archetype` 过滤模板算子条目（archetypes 白名单） |

---

## 7. 上游 Port 接口（定义于 `-api` 模块）

### MetamodelReadPort

Provider: `-core` 的 `MetamodelReadPortAdapter`

| 方法 | 返回类型 | 描述 |
|------|----------|------|
| `getBundle(String fqn)` | `BundleDto` | 按 FQN 查询 Bundle（生效态） |
| `listBundles(PageRequest)` | `PageResult<BundleDto>` | 分页查询 Bundle 列表 |
| `getEntitySchema(String fqn)` | `EntitySchemaDto` | 按 FQN 查询 EntitySchema（生效态） |
| `listEntitySchemas(ElementQueryRequest)` | `PageResult<EntitySchemaDto>` | 按 FQN 前缀过滤查询 |
| `getRelationSchema(String fqn)` | `RelationSchemaDto` | 按 FQN 查询 RelationSchema（生效态） |
| `listRelationSchemas(ElementQueryRequest)` | `PageResult<RelationSchemaDto>` | 按 FQN 前缀过滤查询 |
| `listPackages(String bundleVersionFqn)` | `List<PackageDto>` | 列出版本下所有 Package |
| `getExport(String bundleVersionFqn)` | `ExportManifestDto` | 查询导出清单 |
| `isPackageExported(String, String)` | `boolean` | 判断 Package 是否导出 |
| `getDependencyGraph(String)` | `DependencyGraphDto` | 获取 Bundle 依赖图 |
| `listBundleVersions(String)` | `List<BundleVersionDto>` | 查询 Bundle 所有版本 |

### MetadataReadPort

Provider: `-core` 的 `MetadataReadPortAdapter`

| 方法 | 返回类型 | 描述 |
|------|----------|------|
| `getByFqn(String fqn)` | `MetadataEntityDto` | FQN 精准查询生效实体 |
| `listByFqnPrefixes(List<String>, PageRequest)` | `PageResult<MetadataEntityDto>` | FQN 前缀范围查询 |
| `listByEntitySchema(String, PageRequest)` | `PageResult<MetadataEntityDto>` | 按 EntitySchema 类型查询 |

### GraphReadPort

Provider: `-core` 的 `GraphReadPortAdapter`

| 方法 | 返回类型 | 描述 |
|------|----------|------|
| `getByFqn(String)` | `RelationInstanceDto` | FQN 精准查询生效关系 |
| `getOutboundRelations(String, String, String)` | `List<RelationInstanceDto>` | 查询实体出边关系 |
| `getInboundRelations(String, String, String)` | `List<RelationInstanceDto>` | 查询实体入边关系 |
| `multiFilter(RelationQueryRequest)` | `PageResult<RelationInstanceDto>` | 多维过滤查询 |
| `getRelationCount(String)` | `RelationCount` | 查询实体的出入边计数 |
| `listByConditions(String, String, PageRequest)` | `PageResult<RelationInstanceDto>` | 条件查询 |

### ComputeEngineReadPort

Provider: `-core` 的 `ComputeEngineReadPortAdapter`

**全部 compute-engine 对外能力收敛于此 Port**，禁止在 core 层直接注入 compute-engine api Service。

| 方法 | 返回类型 | 描述 |
|------|----------|------|
| `queryAdjacency(AdjacencyQueryRequest)` | `GraphQueryResult` | 多度邻接查询 |
| `queryCompositionTree(CompositionTreeQueryRequest)` | `GraphQueryResult` | 组合层级树查询 |
| `querySubgraph(SubgraphQueryRequest)` | `GraphQueryResult` | 子图提取查询 |
| `queryPatternMatch(PatternMatchRequest)` | `GraphQueryResult` | 图模式匹配查询 |
| `searchCompound(CompoundSearchRequest)` | `PageResult<EntitySummary>` | 多条件复合检索 |
| `queryBatch(BatchQueryRequest)` | `GraphQueryResult` | 批量语义查询 |
| `findPaths(PathQueryRequest)` | `PathResult` | 两点间路径查询 |
| `computeClosure(ClosureQueryRequest)` | `ClosureResult` | 传递闭包推理 |
| `multiHopReasoning(MultiHopQueryRequest)` | `PathResult` | 多跳语义推理 |
| `checkReachability(ReachabilityCheckRequest)` | `PathResult` | 路径可达性判定 |
| `diffuseForward(ImpactDiffusionRequest)` | `ImpactTraceResult` | 正向影响扩散 |
| `traceBackward(ImpactDiffusionRequest)` | `ImpactTraceResult` | 反向依赖溯源 |
| `getImpactPaths(String, String, List, int)` | `ImpactTraceResult` | 影响路径详情 |
