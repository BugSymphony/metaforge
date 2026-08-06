# Tasks: 语义查询与推理引擎

**Input**: Design documents from `/specs/001-semantic-query-reasoning/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Maven Module Scaffolding)

**Purpose**: Create BC Maven module structure, register in build system, declare dependencies

- [x] T001 Create BC root pom.xml at `metaforge-parent/metaforge-compute-engine/pom.xml` (parent=`metaforge-parent`, packaging=`pom`, modules=[api, core])
- [x] T002 Create api module pom.xml at `metaforge-parent/metaforge-compute-engine/metaforge-compute-engine-api/pom.xml` (parent=BC root, depends on `metaforge-framework`)
- [x] T003 Create core module pom.xml at `metaforge-parent/metaforge-compute-engine/metaforge-compute-engine-core/pom.xml` (parent=BC root, depends on api + `metaforge-metamodel-api` + `metaforge-metadata-api` + `metaforge-graph-api` + `metaforge-framework` + jOOQ + MapStruct)
- [x] T004 Register `metaforge-compute-engine` in `metaforge-parent/pom.xml` `<modules>` section
- [x] T005 Register `metaforge-compute-engine-api` and `metaforge-compute-engine-core` in `metaforge-parent/pom.xml` `<dependencyManagement>`
- [x] T006 Register `metaforge-compute-engine-core` as dependency in `metaforge-boot/pom.xml` `<dependencies>`
- [x] T007 [P] Create package directory structure for api module under `metaforge-compute-engine/metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/{constant,dto/{request,response,common},enums,service,event}`
- [x] T008 [P] Create package directory structure for core module (DDD layers) under `metaforge-compute-engine/metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/{application/service,domain/{model/{aggregate,entity,valueobject},port,service,exception},infrastructure/{config,persistence/jooq/converter,gateway,mapper,spi},interfaces/{rest,mcp}}`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### API Module — Enums & Constants

- [x] T009 [P] Create `AssociationType` enum in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/enums/AssociationType.java` (COMPOSITION, DEPENDENCY_INFLUENCE, PROCESS_SEQUENCE, ASSOCIATION_REFERENCE, MAPPING_CORRESPONDENCE)
- [x] T010 [P] Create `TraversalDirection` enum in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/enums/TraversalDirection.java` (FORWARD, BACKWARD, DIRECTED, BIDIRECTIONAL)
- [x] T011 [P] Create `MatchMode` enum in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/enums/MatchMode.java` (PREFIX, EXACT, PATTERN)
- [x] T012 [P] Create `TruncatedReason` enum in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/enums/TruncatedReason.java` (DEPTH_EXCEEDED, COUNT_EXCEEDED, TIMEOUT)
- [x] T013 [P] Create `WeightStrategy` enum in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/enums/WeightStrategy.java` (MULTIPLY, ADD, MAX, NONE)
- [x] T014 [P] Create `LogicOperator` enum in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/enums/LogicOperator.java` (AND, OR)
- [x] T015 Create `ComputeEngineErrorCodes` constant class in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/constant/ComputeEngineErrorCodes.java` (error codes 33001-33011 with code, message, HTTP status)

### API Module — Common DTOs

- [x] T016 [P] Create `EntitySummary` record in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/common/EntitySummary.java`
- [x] T017 [P] Create `RelationSummary` record in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/common/RelationSummary.java`
- [x] T018 [P] Create `FilterCriteria` record and `FqnFilterGroup` / `PropertyFilter` records in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/common/FilterCriteria.java`
- [x] T019 [P] Create `PatternWildcard` constant class in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/enums/PatternWildcard.java` (ENTITY_WILDCARD='*', RELATION_WILDCARD='?')

### API Module — Result DTOs

- [x] T020 [P] Create `GraphQueryResult` record in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/response/GraphQueryResult.java`
- [x] T021 [P] Create `PathResult` + `PathDetail` + `PathStep` records in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/response/PathResult.java`
- [x] T022 [P] Create `ClosureResult` + `ClosuredEntityDetail` records in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/response/ClosureResult.java`
- [x] T023 [P] Create `ImpactTraceResult` + `ImpactEntityDetail` records in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/response/ImpactTraceResult.java`

### API Module — Request DTOs

- [x] T024 [P] Create `AdjacencyQueryRequest` record in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/request/AdjacencyQueryRequest.java`
- [x] T025 [P] Create `CompositionTreeQueryRequest` record in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/request/CompositionTreeQueryRequest.java`
- [x] T026 [P] Create `SubgraphQueryRequest` record in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/request/SubgraphQueryRequest.java`
- [x] T027 [P] Create `PatternMatchRequest` record in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/request/PatternMatchRequest.java`
- [x] T028 [P] Create `CompoundSearchRequest` + `AttributeCondition` records in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/request/CompoundSearchRequest.java`
- [x] T029 [P] Create `BatchQueryRequest` record in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/request/BatchQueryRequest.java`
- [x] T030 [P] Create `PathQueryRequest` record in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/request/PathQueryRequest.java`
- [x] T031 [P] Create `ClosureQueryRequest` record in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/request/ClosureQueryRequest.java`
- [x] T032 [P] Create `MultiHopQueryRequest` + `HopStep` records in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/request/MultiHopQueryRequest.java`
- [x] T033 [P] Create `ReachabilityCheckRequest` record in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/request/ReachabilityCheckRequest.java`
- [x] T034 [P] Create `ImpactDiffusionRequest` record in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/request/ImpactDiffusionRequest.java`
- [x] T035 [P] Create `ImpactPathRequest` record in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/dto/request/ImpactPathRequest.java`

### API Module — Service Interfaces

- [x] T036 Create `GraphQueryService` interface in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/service/GraphQueryService.java` with `@OpenHostService` semantic marker, 6 methods with full Javadoc (adjacency, compositionTree, subgraph, patternMatch, searchCompound, queryBatch)
- [x] T037 Create `PathReasoningService` interface in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/service/PathReasoningService.java` with `@OpenHostService` semantic marker, 4 methods with full Javadoc (findPaths, computeClosure, multiHopReasoning, checkReachability)
- [x] T038 Create `ImpactTracingService` interface in `metaforge-compute-engine-api/src/main/java/com/metaforge/computeengine/api/service/ImpactTracingService.java` with `@OpenHostService` semantic marker, 3 methods with full Javadoc (diffuseForward, traceBackward, getImpactPaths)

### Core Module — Domain Value Objects

- [x] T039 [P] Create `FQN` value object in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/model/valueobject/FQN.java` (immutable, FQN parsing with derived fields: bundleCode, version, packageFqn, segment; use FQN Generator utility for parse/split operations)
- [x] T040 [P] Create `GraphPattern` + `PatternSegment` value objects in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/model/valueobject/GraphPattern.java` (pattern parsing, segment list, isValid validation, max 4 segments)
- [x] T041 [P] Create `TraversalDepth` value object in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/model/valueobject/TraversalDepth.java` (globalMaxDepth, perTypeMaxDepths map, effectiveDepth(type) method returning min(global, per-type))
- [x] T042 [P] Create `EntitySnapshot` value object in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/model/valueobject/EntitySnapshot.java` (fqn, name, entitySchemaFqn, content map, depth)
- [x] T043 [P] Create `RelationSnapshot` value object in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/model/valueobject/RelationSnapshot.java` (fqn, sourceFqn, targetFqn, relationSchemaFqn, associationType, content)
- [x] T044 [P] Create `TransitivityRule` value object in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/model/valueobject/TransitivityRule.java` (type, transitive, direction, weightStrategy, maxDepth, description)
- [x] T045 [P] Create `PathSegmentVO` value object in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/model/valueobject/PathSegmentVO.java` (fromEntity, toEntity, relation, relationType, direction, weight)
- [x] T046 [P] Create `InfluenceScope` value object in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/model/valueobject/InfluenceScope.java` (centerFqn, direction, maxDepth, relationTypes)
- [x] T047 [P] Create `FilterCriteriaVO` value object in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/model/valueobject/FilterCriteriaVO.java` (7 dimensions, AND/OR logic evaluation method)

### Core Module — Domain Ports

- [x] T048 [P] Create `EntityDataPort` interface in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/port/EntityDataPort.java` (findByFqn, findByFqnPrefixes, findByEntitySchemaFqn, batchFindByFqns — all return EntitySnapshot)
- [x] T049 [P] Create `RelationDataPort` interface in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/port/RelationDataPort.java` (findOutboundRelations, findInboundRelations, findRelations by direction, findByFqn — all return RelationSnapshot)
- [x] T050 Create `MetamodelSemanticPort` interface in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/port/MetamodelSemanticPort.java` (getEntitySchema, getRelationSchema, isEntitySchemaExists, isRelationSchemaExists)

### Core Module — Domain Exceptions

- [x] T051 [P] Create `ComputeEngineException` base exception in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/exception/ComputeEngineException.java` (abstract, errorCode, httpStatus)
- [x] T052 [P] Create concrete exception classes in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/exception/` (EntityNotFoundException, TraversalDepthExceededException, QueryTimeoutException, ResultCountExceededException, InvalidPatternException, InvalidFilterException, NoLegalConductionPathException, BatchSizeExceededException, UpstreamServiceUnavailableException, CircularReferenceException)

### Core Module — Infrastructure Configuration

- [x] T053 Create `ComputeEngineProperties` configuration class in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/infrastructure/config/ComputeEngineProperties.java` (@ConfigurationProperties prefix=`metaforge.compute-engine`, bind traversal/max-depth/timeout-ms/result-count and transitivity-rules list)
- [x] T054 Create jOOQ `DSLContext` bean configuration in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/infrastructure/config/JooqConfig.java` (inject DataSource, DSL.using with SQLDialect.POSTGRES)
- [x] T055 Create `TransitivityRuleService` in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/service/TransitivityRuleService.java` (load rules from ComputeEngineProperties on startup, provide query methods: getRule(type), isTransitive(type), getEffectiveMaxDepth(type, globalMax), getDirection(type), getWeightStrategy(type))

### Core Module — Infrastructure Adapters

- [x] T056 Create `EntityDataPortImpl` jOOQ adapter in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/infrastructure/persistence/jooq/EntityDataPortImpl.java` (implements EntityDataPort, jOOQ queries on metadata_management.metadata_entity STATUS='ACTIVE')
- [x] T057 Create `RelationDataPortImpl` jOOQ adapter in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/infrastructure/persistence/jooq/RelationDataPortImpl.java` (implements RelationDataPort, jOOQ queries on semantic_relation_network.relation_instance STATUS='ACTIVE' + entity_relation_index)
- [x] T058 Create `EntityConverter` MapStruct mapper in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/infrastructure/persistence/jooq/converter/EntityConverter.java` (jOOQ Record → EntitySnapshot)
- [x] T059 Create `RelationConverter` MapStruct mapper in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/infrastructure/persistence/jooq/converter/RelationConverter.java` (jOOQ Record → RelationSnapshot)
- [x] T060 Create `MetamodelGatewayAdapter` in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/infrastructure/gateway/MetamodelGatewayAdapter.java` (implements MetamodelSemanticPort, delegates to `metaforge-metamodel-api` ElementDefinitionService)

### Core Module — SPI Extensions

- [x] T061 [P] Create `ComputeEngineExceptionHandler` in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/infrastructure/spi/ComputeEngineExceptionHandler.java` (implements ExceptionHandlerSpi, maps BC exceptions 33001-33011 to ApiResponse with error codes)
- [x] T062 [P] Create `ComputeEngineHealthCheck` in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/infrastructure/spi/ComputeEngineHealthCheck.java` (implements HealthCheckSpi, checks upstream BC table accessibility via jOOQ)

### Core Module — BC Configuration File

- [x] T063 Create BC configuration file `metaforge-boot/src/main/resources/application-metaforge-compute-engine.yml` with `metaforge.compute-engine.traversal.*` properties and full `transitivity-rules` list (5 AssociationType entries)

**Checkpoint**: Foundation ready — domain model, ports, adapters, config, API interfaces all in place. User story implementation can now begin.

---

## Phase 3: User Story 1 — 多维图遍历与检索 (Priority: P1) 🎯 MVP

**Goal**: 提供六种核心图查询能力：多度邻接查询、组合层级树查询（含上溯父链）、子图提取、图模式匹配、多条件复合检索、批量语义查询。所有查询结果包含统一截断标记与内联摘要。

**Independent Test**: 对包含 100+ 实体、50+ 关系边的语义图谱执行 3 度邻接查询、组合层级树查询、子图提取查询、图模式匹配查询、多条件复合检索、批量语义查询六类查询，全部返回正确的结构化结果且附带实体与关系内联摘要。

**Dependencies**: FR-001~007, FR-015, FR-017, FR-019, FR-020, FR-022, FR-023, FR-024

### Domain Layer — GraphQuery Aggregate

- [x] T064 [US1] Create `GraphQuery` aggregate root in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/model/aggregate/GraphQuery.java` (fields: sourceFqn, direction, maxDepth, relationTypes, filterCriteria, traversalDepth; methods: execute, applyFilters, collectEntities, collectRelations, computeAdjacency, assembleResult — enforces depth constraints, dedup, filter)
- [x] T065 [US1] Create `TraversalPath` domain entity in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/model/entity/TraversalPath.java` (pathId, segments list, totalWeight; addSegment, containsEntity methods)

### Domain Layer — Services

- [x] T066 [US1] Create `GraphTraversalService` domain service in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/service/GraphTraversalService.java` (executeBfs/executeDfs methods: build recursive CTE jOOQ query with depth/path tracking, cycle detection, early termination on maxDepth/resultCount exceeded; returns raw path list)
- [x] T067 [US1] Create `FilterPredicateService` domain service in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/service/FilterPredicateService.java` (buildFilterConditions: converts FilterCriteriaVO → jOOQ Condition list for CTE WHERE clause, handles 7 dimensions AND/OR logic, PREFIX→LIKE, EXACT→=, PATTERN→LIKE, propertyFilters→JSONB @>)

### Application Layer

- [x] T068 [US1] Implement `GraphQueryServiceImpl` in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/application/service/GraphQueryServiceImpl.java` (implements GraphQueryService, orchestrates GraphQuery aggregate + GraphTraversalService + FilterPredicateService; each method: validate input → build GraphQuery → execute traversal → assemble result with truncated flag; handle EntityNotFoundException, timeout, depth/count exceeded)

### Infrastructure — MapStruct Mapper

- [x] T069 [US1] Create `GraphQueryMapper` MapStruct interface in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/infrastructure/mapper/GraphQueryMapper.java` (GraphQuery aggregate → GraphQueryResult DTO; TraversalPath → PathDetail; EntitySnapshot → EntitySummary; RelationSnapshot → RelationSummary)

### Interfaces — REST

- [x] T070 [US1] Create `GraphQueryController` in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/interfaces/rest/GraphQueryController.java` (@Tag(name="compute-engine"), 6 POST/GET endpoints: /adjacency, /composition-tree, /subgraph, /pattern-match, /search, /batch; validates input → calls service → returns result DTO; auto-wrapped in ApiResponse<T> by foundation-core)

### Interfaces — MCP

- [x] T071 [US1] Create `ComputeEngineMcpTools` class with graph query `@Tool` methods in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/interfaces/mcp/ComputeEngineMcpTools.java` (@Tool adjacencyQuery, compositionTree, subgraphExtract, patternMatch, compoundSearch, batchQuery — each delegates to GraphQueryService)

**Checkpoint**: User Story 1 fully functional — 6 graph queries with 7-dimension filtering, truncation markers, inline summaries. Independently testable via REST API.

---

## Phase 4: User Story 2 — 路径推理与语义关联分析 (Priority: P1)

**Goal**: 提供四种推理能力：两点间路径查询（含最短路径）、传递闭包推理、多跳语义推理、路径可达性判定。基于传导规则配置执行传递推理。

**Independent Test**: 对包含多种 AssociationType 关系类型的语义图谱，执行两点间路径查询（含最短路径）、传递闭包推理、多跳语义推理、路径可达性判定四种操作，全部返回正确的推理路径与可达实体集合。

**Dependencies**: FR-008~011

### Domain Layer — PathQuery Aggregate

- [x] T072 [US2] Create `PathQuery` aggregate root in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/model/aggregate/PathQuery.java` (fields: sourceFqn, targetFqn, direction, relationTypes, maxDepth, traversalDepth, transitivityRules; methods: findPaths, computeClosure, multiHopTraverse, checkReachability — enforces transitive-only filtering for closure, conduction matrix validation for multi-hop)
- [x] T073 [US2] Create `ClosuredEntity` domain entity in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/model/entity/ClosuredEntity.java` (fqn, depth, arrivedByTypes; equals/hashCode by fqn for dedup)

### Domain Layer — Services

- [x] T074 [US2] Create `PathInferenceService` domain service in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/service/PathInferenceService.java` (findAllPaths: recursive CTE BFS with path tracking; findShortestPath: LIMIT 1 with ORDER BY depth; computeClosure: recursive CTE filtered by TransitivityRuleService.isTransitive; multiHopTraverse: iterative CTE with per-hop relationType validation; checkReachability: LIMIT 1 early termination)

### Application Layer

- [x] T075 [US2] Implement `PathReasoningServiceImpl` in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/application/service/PathReasoningServiceImpl.java` (implements PathReasoningService; findPaths → PathQuery.findPaths; computeClosure → PathQuery.computeClosure with per-type maxDepth from TransitivityRule; multiHopReasoning → validate hop sequence in TransitivityRule then PathQuery.multiHopTraverse; checkReachability → PathQuery.checkReachability with LIMIT 1)

### Infrastructure — MapStruct Mapper

- [x] T076 [US2] Create `PathResultMapper` MapStruct interface in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/infrastructure/mapper/PathResultMapper.java` (PathQuery aggregate → PathResult DTO; ClosuredEntity → ClosuredEntityDetail; map layer groups)

### Interfaces — REST

- [x] T077 [US2] Create `PathReasoningController` in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/interfaces/rest/PathReasoningController.java` (@Tag(name="compute-engine"), 4 POST endpoints: /paths, /closure, /multi-hop, /reachability)

### Interfaces — MCP

- [x] T078 [US2] Add path reasoning `@Tool` methods to `ComputeEngineMcpTools` in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/interfaces/mcp/ComputeEngineMcpTools.java` (@Tool findPaths, computeClosure, multiHopReasoning, checkReachability)

**Checkpoint**: User Stories 1 AND 2 both functional — graph queries + path reasoning complete.

---

## Phase 5: User Story 3 — 影响溯源与变更评估 (Priority: P2)

**Goal**: 提供三种影响分析能力：正向影响扩散、反向依赖溯源、影响路径详情查询。

**Independent Test**: 对包含多度关联关系的语义图谱，对指定起点执行正向影响扩散、反向依赖溯源、影响路径详情查询三种操作，全部返回正确的影响范围统计与路径明细。

**Dependencies**: FR-012~014

### Domain Layer — ImpactQuery Aggregate

- [x] T079 [US3] Create `ImpactQuery` aggregate root in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/model/aggregate/ImpactQuery.java` (fields: centerFqn, direction, relationTypes, maxDepth, traversalDepth; methods: diffuseForward, traceBackward, getImpactPaths — BFS extension with dedup and depth-tracking)
- [x] T080 [US3] Create `ImpactEntity` domain entity in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/model/entity/ImpactEntity.java` (fqn, depth, impactPaths, affectedByTypes)

### Domain Layer — Services

- [x] T081 [US3] Create `ImpactAnalysisService` domain service in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/domain/service/ImpactAnalysisService.java` (diffuse: recursive CTE with forward BFS; trace: recursive CTE with reverse BFS along inbound edges; impactPaths: iterative path enumeration between two FQNs with relationType filtering)

### Application Layer

- [x] T082 [US3] Implement `ImpactTracingServiceImpl` in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/application/service/ImpactTracingServiceImpl.java` (implements ImpactTracingService; diffuseForward → ImpactQuery.diffuseForward; traceBackward → ImpactQuery.traceBackward; getImpactPaths → ImpactQuery.getImpactPaths; layer stats aggregation)

### Infrastructure — MapStruct Mapper

- [x] T083 [US3] Create `ImpactTraceMapper` MapStruct interface in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/infrastructure/mapper/ImpactTraceMapper.java` (ImpactQuery aggregate → ImpactTraceResult DTO; ImpactEntity → ImpactEntityDetail; typeStats map)

### Interfaces — REST

- [x] T084 [US3] Create `ImpactTracingController` in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/interfaces/rest/ImpactTracingController.java` (@Tag(name="compute-engine"), 3 endpoints: POST /impact/diffuse, POST /impact/trace, GET /impact/paths)

### Interfaces — MCP

- [x] T085 [US3] Add impact tracing `@Tool` methods to `ComputeEngineMcpTools` in `metaforge-compute-engine-core/src/main/java/com/metaforge/computeengine/interfaces/mcp/ComputeEngineMcpTools.java` (@Tool diffuseForward, traceBackward, getImpactPaths)

**Checkpoint**: All 3 user stories functional — complete semantic query & reasoning engine.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validation, hardening, and cross-cutting improvements

- [x] T086 Verify Maven compilation: `mvn clean compile -pl metaforge-compute-engine -am` passes without errors
- [x] T087 Verify Spring Boot application starts with compute-engine BC loaded: `mvn spring-boot:run -pl metaforge-boot` succeeds
- [x] T088 [P] Verify REST API endpoints accessible via `/swagger-ui.html` under `compute-engine` tag group
- [x] T089 [P] Run quickstart.md validation scenarios (8 scenarios) and confirm all pass
- [x] T090 Verify 7-dimension filtering AND/OR logic correctness with integration test data
- [x] T091 Verify per-type maxDepth differentiated traversal (COMPOSITION=5, ASSOCIATION_REFERENCE=1) with test graph
- [x] T092 Verify truncation markers (DEPTH_EXCEEDED, COUNT_EXCEEDED, TIMEOUT) appear correctly in result carriers
- [x] T093 Verify timeout mechanism triggers at 2000ms with deep traversal on complex graph
- [x] T094 Verify circular reference detection and dedup in closure computation
- [x] T095 Verify upward ancestry chain (FR-002 BACKWARD direction) returns complete parent chain
- [x] T096 Verify FR-004 wildcard `*`/`?` matches entire EntitySchema/RelationSchema FQN (not name segment)
- [x] T097 Verify upstream BC unavailability returns error code 33010 with clear message
- [x] T098 [P] Add i18n messages file `metaforge-boot/src/main/resources/i18n/messages_compute-engine_zh_CN.properties` with BC error messages
- [x] T099 [P] Run complete integration test suite: `mvn verify -pl metaforge-compute-engine -am -P integration-test`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Phase 2 completion
- **User Story 2 (Phase 4)**: Depends on Phase 2 completion (US1 not strictly required but US2 reuses GraphTraversalService + FilterPredicateService from US1 for efficiency)
- **User Story 3 (Phase 5)**: Depends on Phase 2 completion (P2 priority, lower than US1/US2)
- **Polish (Phase 6)**: Depends on all desired user stories

### User Story Dependencies

- **US1 (P1)**: Independent — can complete with only Foundational phase
- **US2 (P1)**: Reuses `GraphTraversalService` and `FilterPredicateService` from US1 phase, but the domain services are shared and already in foundational; `PathQuery` aggregate and `PathInferenceService` are independent. Can start in parallel with US1 if different team members.
- **US3 (P2)**: Depends on US2's `PathInferenceService` for path computation. Should follow US2.

### Within Each Phase

- Phase 2: Enums → Common DTOs → Request/Result DTOs → Service Interfaces → Value Objects → Ports → Exceptions → Config → Adapters → SPI
- Phase 3: Aggregate → Domain Services → Application Service → Mapper → REST → MCP
- Phase 4: Aggregate → Domain Services → Application Service → Mapper → REST → MCP
- Phase 5: Aggregate → Domain Services → Application Service → Mapper → REST → MCP

### Parallel Opportunities

- Phase 2 has 43 tasks, ~30 marked [P] for parallel execution across different package directories
- US1, US2, US3 phases can be executed by different developers after Phase 2 completion
- Within each US phase: Aggregate + Domain Service are sequential; Mapper + REST + MCP can be started once Application Service is defined

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Parallel batch 1 — all enums (different files):
Task T009: "Create AssociationType enum"
Task T010: "Create TraversalDirection enum"
Task T011: "Create MatchMode enum"
Task T012: "Create TruncatedReason enum"
Task T013: "Create WeightStrategy enum"
Task T014: "Create LogicOperator enum"

# Parallel batch 2 — all common DTOs + result DTOs + request DTOs (different files):
Task T016-T023: "Create all DTO records"

# Parallel batch 3 — all value objects (different files):
Task T039-T047: "Create all domain value objects"

# Parallel batch 4 — all domain ports (different files):
Task T048-T050: "Create domain ports"

# Parallel batch 5 — all domain exceptions (different files):
Task T051-T052: "Create domain exceptions"

# Parallel batch 6 — infrastructure components (different files):
Task T053-T054: "Config classes"
Task T056-T060: "Adapters"
Task T061-T062: "SPI extensions"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (8 tasks)
2. Complete Phase 2: Foundational (55 tasks, critical blocker)
3. Complete Phase 3: US1 — 多维图遍历与检索 (8 tasks)
4. **STOP and VALIDATE**: Test 6 graph query types independently via REST API
5. Deploy/demo if ready — users can already query semantic graphs

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 → 6 graph queries operational → **MVP!**
3. Add US2 → Path reasoning + closure + multi-hop → Complete query engine
4. Add US3 → Impact tracing → Full reasoning capabilities
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With 2 developers after Phase 2:
- Developer A: US1 (GraphQuery aggregate + services + REST/MCP)
- Developer B: US2 (PathQuery aggregate + services + REST/MCP)

US3 follows after US2 is complete.

---

## Notes

- [P] tasks = different files, no dependencies on parallel tasks in same batch
- [Story] label maps task to specific user story for traceability
- All file paths relative to `metaforge-parent/metaforge-compute-engine/`
- Foundation platform capabilities (ApiResponse, PageRequest, virtual threads, Jackson, SpringDoc, Caffeine, Actuator, Security, MessageSource, Flyway) are inherited — no BC-level implementation needed
- jOOQ cross-schema queries are SELECT-only; no INSERT/UPDATE/DELETE allowed
- MapStruct mappers in infrastructure/mapper/ only; domain layer stays pure
- FQN manipulation must use FQN Generator utility from metamodel-api; no manual string concatenation
- Error codes 33000-33999 registered via ExceptionHandlerSpi; all error constants in ComputeEngineErrorCodes
- BC configuration in metaforge-boot application-metaforge-compute-engine.yml; property prefix metaforge.compute-engine
