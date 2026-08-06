# Tasks: 元认知指导层

**Input**: Design documents from `/specs/001-agent-cognition-guidance/`

**Prerequisites**: plan.md (required), spec.md (required), data-model.md, contracts/, research.md, foundation-adaptation.md

**Tests**: Tests are NOT explicitly requested in the feature specification — tasks focus on implementation only.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

BC root: `metaforge-agent-cognition/` (under `metaforge-parent/` in monorepo)

- Contract (api) module: `metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/`
- Implementation (core) module: `metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/`
- Resources: `metaforge-boot/src/main/resources/cognition/`（YAML 配置文件部署于 boot 模块，非 core 模块）

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: BC module scaffolding, Maven build configuration, and platform registration

- [x] T001 Create BC root aggregation POM `metaforge-agent-cognition/pom.xml` with parent `metaforge-parent`, `<modules>` declaring `metaforge-agent-cognition-api` and `metaforge-agent-cognition-core`
- [x] T002 [P] Create api module POM `metaforge-agent-cognition/metaforge-agent-cognition-api/pom.xml` with parent `metaforge-agent-cognition`, no framework dependencies, Java 21
- [x] T003 [P] Create core module POM `metaforge-agent-cognition/metaforge-agent-cognition-core/pom.xml` with parent `metaforge-agent-cognition`, dependencies on `metaforge-framework`, `metaforge-agent-cognition-api`, `metaforge-metamodel-api`, `metaforge-metadata-api`, `metaforge-graph-api`, `metaforge-compute-engine-api`, MapStruct
- [x] T004 Create core module Spring Boot entrypoint `CognitionApplication.java` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/` with `@SpringBootApplication` and `@ComponentScan("com.metaforge.agent.cognition")`
- [x] T005 Register BC module in root reactor: add `<module>metaforge-agent-cognition</module>` entry in `metaforge-parent/pom.xml` `<modules>` section
- [x] T006 Register BC in boot assembly: add `<dependency>` for `metaforge-agent-cognition-core` in `metaforge-boot/pom.xml` `<dependencies>` section

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Configuration & Properties

- [x] T007 [P] Create `AgentCognitionProperties.java` with `@ConfigurationProperties(prefix = "metaforge.agent-cognition")` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/config/` — fields: templatePath, perspectivesPath, cacheEnabled, cacheExpireMinutes, perspectiveTimeoutMs, defaultMaxTokens, defaultDepth, defaultArchetype
- [x] T008 [P] Create application configuration block in `metaforge-boot/src/main/resources/application.yml` under `metaforge.agent-cognition:` prefix — template-path: "classpath:cognition/", perspectives-path: "classpath:cognition/", perspective-timeout-ms: 200, default-max-tokens: 8000, default-depth: L2, default-archetype: execution, cache-enabled: true, cache-expire-minutes: 30

### YAML Template Configuration

- [x] T009 Create `cognition-perspectives.yml` in `metaforge-boot/src/main/resources/cognition/` — define all 14 perspectives with perspectiveId, scope (ENTITY/BUNDLE/BOTH), description; implement YAML schema validation at startup with clear error messages on parse failure
- [x] T010 Create `cognition-templates.yml` in `metaforge-boot/src/main/resources/cognition/` — define 6 built-in templates: bundle-catalog, cognition-guidance, task-brief, step-guide, navigate, sub-task-brief with perspectives[], depthTrim, archetypeAdapt, outputFormat, contextMode; implement YAML schema validation at startup with clear error messages on parse failure

### SPI Extensions

- [x] T011 [P] Create `AgentCognitionExceptionHandler.java` implementing `ExceptionHandlerSpi` with `@Order(10)` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/spi/` — map 6 custom exceptions (TemplateNotFoundException→34001, InvalidBundleFqnException→34002, EmptyBundleFqnsException→34003, InvalidEntityFqnException→34004, PerspectiveTimeoutException→34005, UpstreamUnavailableException→34006) to `ApiResponse.error()`
- [x] T012 [P] Create `CognitionHealthCheck.java` implementing `HealthCheckSpi` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/spi/` — verify YAML template configs loaded successfully, return healthy/unhealthy with detail

### API Module — Contracts & DTOs

- [x] T013 [P] Create `AgentCognitionErrorCodes.java` constant class in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/constant/` — define all 6 error code constants (34001-34006) with descriptions
- [x] T014 [P] Create enums package with `CognitionDepth.java` (L1/L2/L3), `AgentArchetype.java` (execution/exploration/audit/orchestration), `PerspectiveCode.java` (14 perspective enum values), `ContextMode.java` (BUNDLE_LEVEL/ENTITY_LEVEL), `ScopeMode.java` (INHERITED/PURE), `OutputFormat.java` (JSON/PROMPT), `PerspectiveScope.java` (ENTITY/BUNDLE/BOTH), `ConstraintLevel.java` (MANDATORY/RECOMMENDED/REFERENCE) in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/enums/`

### API Module — Core Service Interfaces

- [x] T015 Create `CognitionQueryService.java` interface with `@OpenHostService` marker in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/service/` — single `execute(String templateId, CognitionRequest request)` method with full Javadoc
- [x] T016 [P] Create `TemplateRegistryService.java` interface in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/service/` — methods: `getTemplate(String templateId)`, `listTemplates()`, `validateTemplate(String templateId)`
- [x] T017 [P] Create `CognitionOutputService.java` interface in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/service/` — methods: `formatJson(Result)`, `formatPrompt(Result)`

### API Module — Request/Response DTOs

- [x] T018 [P] Create `CognitionRequest.java` record in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/request/` — fields: bundleFqns (List\<String\>), entityFqn, entityTypes, subjectDomainFqn, scopeMode, cognitionDepth, agentArchetype, maxTokens, expand, format, cursor, pageSize, contextParameters (Map\<String,String\>); @NotBlank on bundleFqns
- [x] T019 [P] Create `TaskBriefRequest.java` record in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/request/` — fields: bundleFqns, cognitionDepth, agentArchetype, maxTokens, contextParameters, format
- [x] T020 [P] Create `StepGuideRequest.java` record in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/request/` — fields: entityFqn, cognitionDepth, agentArchetype, maxTokens, format
- [x] T021 [P] Create `NavigateRequest.java` record in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/request/` — fields: anchorFqn, level, cursor, pageSize, expand, format
- [x] T022 [P] Create `SubTaskBriefRequest.java` record in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/request/` — fields: entryEntityFqn, scopeMode, cognitionDepth, agentArchetype, maxTokens, perspectives, format
- [x] T023 [P] Create `GuidanceResult.java` response DTO in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/response/` — fields: templateId, contextMeta (ContextMeta), perspectives (Map\<String, PerspectiveResult\>)
- [x] T024 [P] Create `ContextMeta.java` response DTO in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/response/` — fields: bundleFqns, entityFqn, contextMode, scopeMode, cognitionDepth, agentArchetype, appliedPerspectives, skippedPerspectives, skipReasons, dataVersionAnchors, totalTokenCount, tokenTrimmed, truncated, truncations, queriedAt
- [x] T025 [P] Create `DataVersionAnchor.java` record in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/` — fields: bundleFqn, publishedVersionFqn, latestVersionNumber, queriedAt
- [x] T026 [P] Create `AdjacentContext.java` record in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/response/` — fields: previousSteps, nextSteps, upstreamEntities, downstreamEntities
- [x] T027 [P] Create `StepGuidanceResult.java` record in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/response/` — fields: entityProfile, constraintSet, capabilityCatalog, decisionMatrix, impactTrace, relationshipGraph, adjacentContext
- [x] T028 [P] Create 14 perspective output DTOs in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/response/` — `EntityProfile.java`, `DomainLocation.java`, `CompositionTree.java`, `RelationshipGraph.java`, `ConstraintSet.java`, `CapabilityCatalog.java`, `FlowBlueprint.java`, `DecisionMatrix.java`, `ImpactTrace.java`, `PrerequisiteChain.java`, `BundleDirectory.java`, `DomainNavigation.java`, `InstanceCatalog.java`, `SchemaInventory.java` — each as Java record with fields per data-model.md definitions, including nested static inner classes
- [x] T029 [P] Create endpoint-specific response DTOs in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/response/` — `TaskBriefResponse.java`, `StepGuideResponse.java`, `NavigateResponse.java`, `BundleCatalogResponse.java`

### API Module — Perspective Executor SPI (Plugin Extension Point)

- [x] T030 Create `PerspectiveExecutor.java` SPI interface in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/perspective/` — single method `Object execute(PerspectiveExecutionContext ctx)`; method `PerspectiveCode supportedPerspective()`; annotated as plugin extension point for third-party developers
- [x] T031 Create `PerspectiveExecutionContext.java` record in `metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/perspective/` — fields: contextMode, bundleFqns, entityFqn, entityTypes, subjectDomainFqn, contextParameters, cursor, pageSize, expand

### Domain Layer — Core Model

- [x] T032 Create `GuidanceResult.java` aggregate root in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/aggregate/` — fields: contextMeta (ContextMeta), perspective chapters (Map of perspectiveId → Object); static factory method `create(ContextMeta)`
- [x] T033 [P] Create `CognitionQuery.java` aggregate root in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/aggregate/` — fields: templateId (TemplateId), queryParameters (QueryParameters), perspectiveResults (List\<PerspectiveResult\>), contextMeta (ContextMeta); business methods: execute(), applyDepthTrim(), applyTokenBudget()
- [x] T034 [P] Create 15 domain entities in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/entity/` — `EntityProfile.java`, `DomainLocation.java`, `CompositionTree.java`, `RelationshipGraph.java`, `ConstraintSet.java`, `CapabilityCatalog.java`, `FlowBlueprint.java`, `DecisionMatrix.java`, `ImpactTrace.java`, `PrerequisiteChain.java`, `BundleDirectory.java`, `DomainNavigation.java`, `InstanceCatalog.java`, `SchemaInventory.java`, `PerspectiveResult.java` — all with full field definitions and nested inner classes per data-model.md
- [x] T035 [P] Create value objects in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/valueobject/` — `TemplateId.java` (record, with FQN validation), `PerspectiveCode.java` (record, 14 codes), `CognitionDepth.java` (enum, L1/L2/L3 with maxPerspectives), `AgentArchetype.java` (enum, 4 types with fromString), `ContextMode.java` (enum), `ScopeMode.java` (enum), `OutputFormat.java` (enum), `PerspectiveScope.java` (enum), `ConstraintLevel.java` (enum), `DataVersionAnchor.java` (record), `ContextMeta.java` (class, with TruncationNote inner record), `AdjacentContext.java` (record)
- [x] T036 [P] Create `QueryParameters.java` value object in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/valueobject/` — fields: bundleFqns, entityFqn, entityTypes, subjectDomainFqn, scopeMode, cognitionDepth, agentArchetype, maxTokens, expand, format, cursor, pageSize, contextParameters; with validation logic for FQN format

### Domain Layer — Upstream Client Ports

- [x] T037 [P] Create `MetamodelClientPort.java` interface in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/port/` — methods: getBundle(String fqn), listBundles(int page, int size), getLatestPublishedVersion(String bundleFqn), getEntitySchema(String fqn), listEntitySchemasByPrefixes(List\<String\> fqnPrefixes), resolveBundleFqnByPrefix(String fqnPrefix)
- [x] T038 [P] Create `MetadataClientPort.java` interface in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/port/` — methods: getByFqn(String fqn), listByFqnPrefixes(List\<String\> fqnPrefixes, int page, int size), listByEntitySchema(String entitySchemaFqn, int page, int size), queryByAttributes(List\<AttributeCondition\> conditions, String matchMode)
- [x] T039 [P] Create `GraphClientPort.java` interface in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/port/` — methods: getOutboundRelations(String entityFqn, List\<AssociationType\> relationTypes, List\<String\> targetEntityTypes), getInboundRelations(String entityFqn, List\<AssociationType\> relationTypes, List\<String\> sourceEntityTypes), multiFilter(RelationQueryCriteria criteria), getDependentRelations(String entityFqn), getRelationCount(String entityFqn)
- [x] T040 [P] Create `ComputeEngineClientPort.java` interface in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/port/` — methods: queryAdjacency(String sourceFqn, String direction, int maxDepth, List\<AssociationType\> relationTypes), queryCompositionTree(String rootFqn, String direction, int maxDepth), querySubgraph(List\<String\> centerFqns, int expandDepth, List\<AssociationType\> relationTypes), diffuseForward(String sourceFqn, List\<AssociationType\> relationTypes, int maxDepth), traceBackward(String sourceFqn, List\<AssociationType\> relationTypes, int maxDepth), getImpactPaths(String sourceFqn, String targetFqn, List\<AssociationType\> relationTypes, int maxDepth), computeClosure(String sourceFqn, List\<AssociationType\> relationTypes), queryBatch(List\<String\> fqns, int page, int size), searchCompound(List\<String\> entityTypes, List\<AttributeCondition\> conditions, int page, int size)

### Domain Layer — Domain Service Interfaces

- [x] T041 [P] Create `TemplateResolutionService.java` interface in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — methods: resolve(TemplateId templateId, RequestOverrides overrides), resolveFromRequest(...); inner records: ExecutionPlan, RequestOverrides
- [x] T042 [P] Create `PerspectiveOrchestrationService.java` interface in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — method: orchestrate(ExecutionPlan, OrchestrationContext); inner record: OrchestrationContext
- [x] T043 [P] Create `TokenBudgetService.java` interface in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — methods: trim(GuidanceResult result, int maxTokens), estimateTokens(GuidanceResult result)
- [x] T044 [P] Create `VersionAnchorService.java` interface in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — method: resolveAnchors(List\<String\> bundleFqns)
- [x] T045 [P] Create `FqnValidationService.java` interface in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — methods: validateBundleFqns(List\<String\> bundleFqns), resolveBundleFromEntityFqn(String entityFqn)
- [x] T046 [P] Create `ScopeNarrowingService.java` interface in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — method: narrow(String entryEntityFqn); inner record: NarrowedScope
- [x] T047 [P] Create `ChangeWatchService.java` interface in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — methods: handleMetadataChange(String changedEntityFqn), handleRelationChange(String changedRelationFqn)
- [x] T048 [P] Create `OutputFormattingService.java` interface in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — method: format(GuidanceResult result, OutputFormat format)

### Infrastructure — Upstream Adapters

- [x] T049 [P] Create `MetamodelClientAdapter.java` implementing `MetamodelClientPort` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/adapter/` — inject upstream services from `metaforge-metamodel-api`; delegate all methods, translate upstream DTOs to domain objects
- [x] T050 [P] Create `MetadataClientAdapter.java` implementing `MetadataClientPort` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/adapter/` — inject upstream services from `metaforge-metadata-api`; delegate all methods
- [x] T051 [P] Create `GraphClientAdapter.java` implementing `GraphClientPort` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/adapter/` — inject upstream services from `metaforge-graph-api`; delegate all methods
- [x] T052 [P] Create `ComputeEngineClientAdapter.java` implementing `ComputeEngineClientPort` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/adapter/` — inject upstream services from `metaforge-compute-engine-api`; delegate all methods

### Infrastructure — Configuration & Mapper

- [x] T053 Create `TemplateConfig.java` startup-loaded POJO in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/config/` — parse `cognition-templates.yml` from classpath (metaforge-boot/src/main/resources/cognition/); via `@Value("${metaforge.agent-cognition.template-path}")` resolve path; provide `getTemplate(String)`, `getAllTemplates()`
- [x] T054 Create `PerspectiveConfig.java` startup-loaded POJO in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/config/` — parse `cognition-perspectives.yml` from classpath (metaforge-boot/src/main/resources/cognition/); via `@Value("${metaforge.agent-cognition.perspectives-path}")` resolve path; provide `resolveActivePerspectives(ContextMode, List\<PerspectiveCode\>, CognitionDepth, AgentArchetype)`
- [x] T055 [P] Create MapStruct mappers in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/mapper/` — `CognitionMapper.java` (CognitionRequest ↔ QueryParameters, domain GuidanceResult ↔ api GuidanceResult), `UpstreamDtoMapper.java` (upstream DTOs ↔ domain objects per data-model.md §5.5)

**Checkpoint**: Foundation ready — all domain ports, adapters, DTOs, config, SPI extensions, and PerspectiveExecutor SPI in api module. User story implementation can now begin.

---

## Phase 3: User Story 1 — 跨 Bundle 通用认知查询 (Priority: P1) 🎯 MVP

**Goal**: Consumers can query any Bundle for structured cognitive views via template-driven perspective orchestration. Engine auto-discovers applicable entity types via graph topology and field-name heuristics.

**Independent Test**: Call `cognitionGuidance` with a codebase Bundle and perspectives ["composition_tree", "constraint_set"] → verify correct composition tree and constraint set returned, no unrelated entities.

### Implementation for User Story 1

- [x] T056 [US1] Create `CognitionQueryServiceImpl.java` implementing `CognitionQueryService` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/application/service/` — core orchestration: resolve templateId via TemplateConfig, validate bundle_fqns via FqnValidationService, derive contextMode from entity_fqn presence, iterate perspectives[], delegate to PerspectiveExecutor via PerspectiveRegistry, aggregate results via OutputAssembler
- [x] T057 [US1] Create `TemplateRegistryServiceImpl.java` implementing `TemplateRegistryService` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/application/service/` — wrap TemplateConfig: getTemplate(), listTemplates(), validateTemplate()
- [x] T058 [US1] Create `PerspectiveRegistry.java` Spring bean in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/application/executor/` — auto-inject all beans implementing `api.perspective.PerspectiveExecutor`; provide `getExecutor(PerspectiveCode)`, `getAllExecutors()`; auto-discover third-party plugin implementations
- [x] T059 [US1] Create `PerspectiveOrchestrationServiceImpl.java` implementing `PerspectiveOrchestrationService` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — determine applicable perspectives for contextMode (BUNDLE_LEVEL → BUNDLE+BOTH scope; ENTITY_LEVEL → ENTITY+BOTH scope, skip BUNDLE); scope conflict handling and skipped perspective annotation; autoDiscover: check entityFqn exists before forcing ENTITY_LEVEL perspectives. **CRITICAL**: perspective executors are stateless and atomic — they MUST NOT call each other directly. All cross-perspective coordination happens exclusively in this service.
- [x] T060 [US1] Create `OutputAssembler.java` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/application/assembler/` — assemble perspective results into GuidanceResult; build contextMeta with dataVersionAnchors, truncatedPerspectives, skippedPerspectives; apply archetype ordering; apply depth trimming
- [x] T061 [P] [US1] Create `EntityProfileExecutor.java` implementing `api.perspective.PerspectiveExecutor` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/application/executor/impl/` — BOTH scope; if entityFqn present: call MetadataClientPort.getByFqn(entityFqn), if not found throw InvalidEntityFqnException with candidate FQN list; call MetamodelClientPort.getEntitySchema to enrich with schema definition; if entityFqn absent: call MetadataClientPort.listByFqnPrefixes(bundleFqns) for all entities; handle empty result sets with explicit annotation
- [x] T062 [P] [US1] Create `CompositionTreeExecutor.java` implementing `api.perspective.PerspectiveExecutor` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/application/executor/impl/` — ENTITY scope; call ComputeEngineClientPort.queryCompositionTree(rootFqn=entityFqn, direction=BOTH, maxDepth); auto-discover entities with COMPOSITION edges; return empty CompositionTree with `empty=true` and descriptive `emptyNote` if no composition edges found; handle truncated response from upstream with pass-through of truncated/truncatedReason
- [x] T063 [P] [US1] Create `RelationshipGraphExecutor.java` implementing `api.perspective.PerspectiveExecutor` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/application/executor/impl/` — ENTITY scope; call ComputeEngineClientPort.queryAdjacency(sourceFqn=entityFqn, direction=BOTH, maxDepth=3); group results by AssociationType; return empty with annotation if no edges
- [x] T064 [P] [US1] Create `ConstraintSetExecutor.java` implementing `api.perspective.PerspectiveExecutor` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/application/executor/impl/` — BOTH scope; query IN direction DEPENDENCY_INFLUENCE edges via GraphClientPort.getInboundRelations(entityFqn, "DEPENDENCY_INFLUENCE"); query ASSOCIATION_REFERENCE edges; fetch full constraint entity content via MetadataClientPort; extract hard boundaries from EntitySchema JSON Schema; classify by constraint_level field (default REFERENCE); return empty if no DEPENDENCY_INFLUENCE edges
- [x] T065 [P] [US1] Create `FlowBlueprintExecutor.java` implementing `api.perspective.PerspectiveExecutor` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/application/executor/impl/` — BUNDLE scope; query PROCESS_SEQUENCE edges across whole bundle via GraphClientPort.multiFilter; build ordered step sequence; detect entryStep/exitSteps/branchPoints; return empty with annotation if no blueprint instances; detect circular reference (PROCESS_SEQUENCE loop)
- [x] T066 [US1] Create `CognitionController.java` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/interfaces/rest/` — single endpoint: `@PostMapping("/api/v1/cognition/{templateId}")` accepting `@Valid @RequestBody CognitionRequest`; delegate to CognitionQueryService.execute(); return `ApiResponse<GuidanceResult>`; add `@Tag(name = "agent-cognition")` and `@Operation` annotations; handle format parameter for json/prompt output

**Checkpoint**: US1 MVP complete — template-driven perspective orchestration with 5 core perspectives working end-to-end, REST endpoint functional, PerspectiveExecutor SPI in api module ready for third-party plugins

---

## Phase 4: User Story 2 — 实体上下文感知的过滤 (Priority: P1)

**Goal**: When entity_fqn is provided, engine auto-switches to ENTITY_LEVEL mode: BUNDLE-scope perspectives skip, BOTH-scope filtered by graph edges from entity_fqn. Bundle scope recovered from FQN prefix.

**Independent Test**: Call stepGuide with a specific entity_fqn → verify only entity-relevant content returned, no full-bundle data.

### Implementation for User Story 2

- [x] T067 [US2] Implement contextMode auto-derivation in `CognitionQueryServiceImpl.java` — if entity_fqn present → ENTITY_LEVEL else BUNDLE_LEVEL; extract bundle_fqns from entity_fqn FQN prefix if not explicitly passed; integrate FqnValidationService
- [x] T068 [US2] Create `FqnValidationServiceImpl.java` implementing `FqnValidationService` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — FQN format validation via regex; prefix extraction to derive bundle_fqns; FQN-to-Bundle membership check via MetamodelClientPort.resolveBundleFqnByPrefix(); throw InvalidEntityFqnException with candidates if no match; throw InvalidBundleFqnException/EmptyBundleFqnsException for bundle validation
- [x] T069 [US2] Create `ScopeResolutionServiceImpl.java` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — for ENTITY_LEVEL: filter BOTH-scope perspectives by entity_fqn via graph edge queries; build adjacent_context from PROCESS_SEQUENCE edges (next steps via outbound, previous step via inbound)
- [x] T070 [US2] Implement ENTITY_LEVEL mode in each BOTH-scope executor (ConstraintSetExecutor, CapabilityCatalogExecutor, DecisionMatrixExecutor, InstanceCatalogExecutor, PrerequisiteChainExecutor) — add entity_fqn graph edge filtering when contextMode==ENTITY_LEVEL; for ENTITY scope executors pass entity_fqn as root; BUNDLE scope executors skip entirely
- [x] T071 [US2] Implement step-guide template full logic — register template "step-guide" (already defined in T010) with contextMode=ENTITY_LEVEL; integrate adjacent_context into step-guide output; verify stepGuide response matches StepGuidanceResult DTO structure

**Checkpoint**: US2 complete — entity-level filtering operational, step-guide template works end-to-end

---

## Phase 5: User Story 3 — 无状态 FQN 寻址与版本锚一致感知 (Priority: P1)

**Goal**: All queries stateless, idempotent, repeatable. Each output includes data_version_anchors for version traceability. changeWatch monitors metadata changes and computes impact reports.

**Independent Test**: Identical taskBrief calls return consistent content (only timestamp differs); version anchors match published bundle versions.

### Implementation for User Story 3

- [x] T072 [US3] Create `VersionAnchorServiceImpl.java` implementing `VersionAnchorService` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — for each bundle_fqn: query latest published version via MetamodelClientPort.getLatestPublishedVersion; compose DataVersionAnchor with queriedAt timestamp; aggregate into List\<DataVersionAnchor\>
- [x] T073 [US3] Integrate VersionAnchorService into `CognitionQueryServiceImpl.java` — call before perspective execution, embed anchors into contextMeta of GuidanceResult
- [x] T074 [US3] Create `ChangeWatchServiceImpl.java` implementing `ChangeWatchService` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/application/service/` — listen to Spring ApplicationEvents; handleMetadataChange → delegate to ComputeEngineClientPort.diffuseForward() for affected entity set; handleRelationChange → delegate to ComputeEngineClientPort.diffuseForward(); generate ChangeImpactReport with best-effort semantics (no event persistence, no replay)
- [x] T075 [US3] Create event listener `ChangeEventListener.java` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/event/` — `@EventListener` annotated methods for upstream BC's MetadataChangeEvent and RelationChangeEvent; delegate to ChangeWatchService; WARN-level logging for missed events
- [x] T076 [US3] Add version anchor to all perspective executor responses — each executor appends its own data sources' version info to a shared context collected by OutputAssembler; ensure dataVersionAnchors in contextMeta are complete

**Checkpoint**: US3 complete — stateless queries with version anchoring, changeWatch event listener operational

---

## Phase 6: User Story 4 — 多级认知深度与代理原型适配 (Priority: P2)

**Goal**: cognition_depth (L1/L2/L3) controls output granularity; agent_archetype adjusts perspective ordering priority; max_tokens triggers automatic truncation.

**Independent Test**: L1 mode returns ≤3 perspectives; exploration archetype puts composition_tree first; low max_tokens triggers L1 fallback.

### Implementation for User Story 4

- [x] T077 [US4] Create `TokenBudgetServiceImpl.java` implementing `TokenBudgetService` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — accept maxTokens (default 8000); estimate token count for each perspective result (rough char count ÷ 4); if total exceeds: trim progressively (L3→L2→L1) preserving full perspective coverage; if max_tokens < 500: force L1 mode; apply per-perspective result truncation at field level
- [x] T078 [US4] Create `DepthTrimmingService.java` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — L1: max 3 perspectives (highest archetype priority); L2: max 7 perspectives; L3: all 14 perspectives; unknown depth falls back to L2; validate cognition_depth against valid enum values and log WARN on unknown input; integrate with output ordering
- [x] T079 [US4] Create `ArchetypeOrderingService.java` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — define priority orders: execution=[constraint_set, capability_catalog, flow_blueprint, entity_profile, decision_matrix, impact_trace, prerequisite_chain, ...]; exploration=[composition_tree, relationship_graph, domain_location, domain_navigation, entity_profile, ...]; audit=[constraint_set, impact_trace, prerequisite_chain, entity_profile, ...]; orchestration=[flow_blueprint, decision_matrix, capability_catalog, entity_profile, ...]; unknown archetype falls back to execution with WARN-level log; validate agentArchetype against valid enum values
- [x] T080 [US4] Integrate DepthTrimmingService and ArchetypeOrderingService into `OutputAssembler.java` — apply ordering first, then depth trimming, then token budget; report which perspectives were trimmed in contextMeta.truncations
- [x] T081 [US4] Add L1/L2/L3 depth configuration to each template definition in cognition-templates.yml — L1 templates: max 3 perspectives; L2: max 7; L3: all 14
- [x] T082 [P] [US4] Create remaining 9 perspective executor implementations implementing `api.perspective.PerspectiveExecutor` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/application/executor/impl/`:
  - `DomainLocationExecutor.java` — ENTITY scope: ComputeEngineClientPort.queryCompositionTree(direction=BACKWARD); return empty path if no classification tree
  - `CapabilityCatalogExecutor.java` — BOTH scope: GraphClientPort.getOutboundRelations with ASSOCIATION_REFERENCE; fetch capability details via MetadataClientPort; expand protocol subtypes via COMPOSITION edges
  - `DecisionMatrixExecutor.java` — BOTH scope: GraphClientPort.getOutboundRelations(entityFqn, PROCESS_SEQUENCE); if outbound > 1 → decision point; compute downstream impact via ComputeEngineClientPort.diffuseForward()
  - `ImpactTraceExecutor.java` — ENTITY scope: ComputeEngineClientPort.diffuseForward(depth=3) for forward impact; ComputeEngineClientPort.traceBackward() for backward dependency; ComputeEngineClientPort.getImpactPaths() for detailed paths
  - `PrerequisiteChainExecutor.java` — BOTH scope: ComputeEngineClientPort.traceBackward(relationTypes=[DEPENDENCY_INFLUENCE]); build dependency tree with level, blocking status, entity state
  - `DomainNavigationExecutor.java` — BUNDLE scope: traverse L1→L2→Task via GraphClientPort (COMPOSITION relations) with entityTypes filter; lazy loading: children summary + has_more; expand=all: full tree
  - `InstanceCatalogExecutor.java` — BOTH scope: MetadataClientPort.listByFqnPrefixes(bundleFqns) grouped by entitySchemaFqn; ENTITY mode: filter by entity_fqn edges
  - `BundleDirectoryExecutor.java` — BUNDLE scope: MetamodelClientPort.listBundles() + MetadataClientPort for populated subject domain trees
  - `SchemaInventoryExecutor.java` — BUNDLE scope: MetamodelClientPort.listEntitySchemasByPrefixes(bundleFqns); count M1 instances per schema via MetadataClientPort; retain entries with count=0

**Checkpoint**: US4 complete — all 14 perspectives implemented, depth/archetype adaptation functional

---

## Phase 7: User Story 5 — 自包含标准化多格式输出 (Priority: P2)

**Goal**: All outputs follow unified root JSON structure (context_meta + perspective chapters). json and prompt dual format, semantically identical. Consumer needs no secondary queries to any BC.

**Independent Test**: JSON and prompt outputs are semantically identical; empty perspectives retain chapter headers with annotations; all entity references inline full semantics.

### Implementation for User Story 5

- [x] T083 [US5] Create `CognitionOutputServiceImpl.java` implementing `CognitionOutputService` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/application/service/` — formatJson(): return GuidanceResult as structured JSON via Jackson; formatPrompt(): convert same GuidanceResult to Markdown blocks (## perspective name, table/structured text, annotations for empty/truncated)
- [x] T084 [US5] Create `OutputFormattingServiceImpl.java` implementing `OutputFormattingService` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — format Prompt format with Markdown headings for each perspective chapter; inline FQN-based entity summaries; annotate truncated/empty perspectives with blockquote notes; maintain full semantic parity with JSON
- [x] T085 [US5] Ensure all perspective executors inline full entity semantics — each EntitySummary includes fqn, name, entitySchemaFqn, plus relevant context (for entity_profile: full content; for relationships: source/target FQN with type annotations)
- [x] T086 [US5] Add format parameter handling to `CognitionQueryServiceImpl` — if format="prompt", delegate to OutputFormattingService.format() after building GuidanceResult; integrate with Controller Accept header
- [x] T087 [US5] Add truncated/truncatedReason pass-through from upstream compute-engine results — each executor propagates upstream truncated/truncatedReason into PerspectiveResult; OutputAssembler aggregates into contextMeta.truncations; ensure empty perspectives (empty=true) retain chapter headers with annotations; verify truncated entries include perspectiveId and reason (TIMEOUT/DEPTH_EXCEEDED/COUNT_EXCEEDED)

**Checkpoint**: US5 complete — dual format output operational, self-contained semantics verified

---

## Phase 8: User Story 6 — 层级化子任务元认知指导 (Priority: P2)

**Goal**: Parent agent delegates entity-specific subtasks to child agents. Each child agent gets narrowed metacognition brief via INHERITED scope_mode (blueprint narrowing → entity collection → schema narrowing). Sibling subtask contexts naturally isolated.

**Independent Test**: Different entity subtasks with INHERITED narrowing produce mutually isolated briefs, no cross-contamination.

### Implementation for User Story 6

- [x] T088 [US6] Create `ScopeNarrowingServiceImpl.java` implementing `ScopeNarrowingService` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/` — INHERITED mode three-layer narrowing: (a) blueprint narrow → ComputeEngineClientPort.queryAdjacency(relationTypes=[PROCESS_SEQUENCE], direction=BOTH, from entry_entity_fqn, maxDepth=1) to limit blueprint scope; (b) entity collection → GraphClientPort.getOutboundRelations+getInboundRelations from narrowed blueprint entities to collect related FQN set; (c) schema narrowing → deduplicate entitySchemaFqns from collected entities
- [x] T089 [US6] Implement PURE mode: return only entity_profile perspective for entry_entity_fqn, no narrowing, no inheritance; scope_mode validation (required field)
- [x] T090 [US6] Integrate scope_mode into `CognitionQueryServiceImpl` — if scope_mode==INHERITED: invoke ScopeNarrowingService before perspective execution, pass narrowed NarrowedScope context to executors; if scope_mode==PURE: limit to entity_profile only
- [x] T091 [US6] Implement sub-task-brief template logic — register template "sub-task-brief" (already defined in T010) with entry_entity_fqn and scope_mode parameters; perspectives depend on scope_mode (INHERITED: full but narrowed; PURE: entity_profile only)
- [x] T092 [US6] Add isolation verification — ensure each scope narrowing query is stateless and self-contained (no shared mutable state at service level); sibling isolation guaranteed by request-scoped QueryContext with no cross-request caching; verify via concurrent sub-task brief requests

**Checkpoint**: US6 complete — hierarchical narrowing operational, isolation verified

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T093 [P] Create `CognitionCacheConfig.java` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/config/` — Caffeine cache for GuidanceResult with key = templateId + bundleFqns hash + entityFqn; TTL 30 minutes; evict on version anchor mismatch
- [x] T094 [P] Create `CognitionMcpTools.java` in `metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/interfaces/mcp/` — define `cognition_execute` MCP tool function with parameters matching contracts/mcp-tools.md; delegate to CognitionQueryService.execute(); annotate with `@Tool` and `@ToolParam`
- [x] T095 Add comprehensive error handling — wrap all upstream BC calls with try/catch; translate upstream exceptions to AgentCognitionErrorCodes (34001-34006); add WARN-level logging for upstream unavailability (`@Slf4j`); add INFO-level logging for each perspective execution (perspectiveId, duration, truncated status)
- [x] T096 Add PerspectiveResult timeout enforcement — in PerspectiveOrchestrationServiceImpl: wrap each executor invocation with `CompletableFuture.orTimeout(200ms)`; catch TimeoutException → mark PerspectiveResult as truncated=true, truncatedReason=TIMEOUT; do NOT cancel other perspective executions; add @Timeout annotation on PerspectiveExecutor.execute() as secondary guard
- [x] T097 Configure SpringDoc OpenAPI grouping — ensure `@Tag(name = "agent-cognition")` on CognitionController groups all cognition endpoints in Swagger UI; add `@Operation(summary=..., description=...)` on each endpoint
- [x] T098 Verify foundation compliance — confirm no custom `MessageSource`, no custom `ObjectMapper`, no custom pagination DTOs; `ApiResponse<T>` used exclusively for REST responses; error codes 34001-34006 confirmed in foundation's error code registry; `@Tag("agent-cognition")` on controller
- [x] T099 Validate all 6 user stories via quickstart.md scenarios — verify bundle-catalog, task-brief ≤500ms, step-guide ≤150ms, L1 max 3 perspectives, prompt format parity, error codes (34001-34006), scope narrowing isolation, archetype ordering, navigate lazy loading, sub-task-brief INHERITED/PURE

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — COMPLETED ✓
- **Phase 2 (Foundational)**: Depends on Phase 1 completion — BLOCKS all user stories
- **Phase 3 (US1 MVP)**: Depends on Phase 2. Core orchestration and 5 P1 perspectives, PerspectiveExecutor SPI in api module
- **Phase 4 (US2)**: Depends on Phase 3 (needs CognitionQueryServiceImpl for contextMode integration)
- **Phase 5 (US3)**: Depends on Phase 3 (needs OutputAssembler for version anchor integration)
- **Phase 6 (US4)**: Depends on Phase 3 (needs OutputAssembler for depth/archetype integration). Can parallel with US2/US3
- **Phase 7 (US5)**: Depends on Phase 3 (needs GuidanceResult structure). Can parallel with US2/US3/US4
- **Phase 8 (US6)**: Depends on Phase 3 (needs QueryContext). Can parallel with US4/US5
- **Phase 9 (Polish)**: Depends on all desired user stories being complete

### Within Each User Story

- API DTOs → Domain models → Services → Controller/MCP
- Core orchestrator (US1) is foundation for US2-US6
- Perspective executors marked [P] within each phase can run in parallel
- All executors implement `api.perspective.PerspectiveExecutor` (SPI in api module)

### Parallel Opportunities

- **Phase 2**: All domain ports (T037-T040) can run in parallel; all infrastructure adapters (T049-T052) can run in parallel; all DTOs (T018-T029) can run in parallel; all enums (T014) can run in parallel; all domain service interfaces (T041-T048) can run in parallel; API service interfaces (T015-T017) can run in parallel
- **Phase 3**: All 5 initial perspective executors (T061-T065) can run in parallel; PerspectiveRegistry (T058) and PerspectiveOrchestrationServiceImpl (T059) can run in parallel
- **Phase 6**: All 9 remaining perspective executors (T082 a-i) can run in parallel
- **Phase 6/7/8**: US4, US5, US6 can start in parallel once US3 foundation is done

---

## Parallel Example: User Story 1

```bash
# Phase 2: Launch all DTOs and perspective infra in parallel:
Task: "Create all enums in api/enums/" (T014)
Task: "Create all request DTOs in api/dto/request/" (T018-T022)
Task: "Create all response DTOs in api/dto/response/" (T023-T029)
Task: "Create PerspectiveExecutor SPI in api/perspective/" (T030-T031)

# Phase 3: Launch all perspective executors in parallel:
Task: "Create EntityProfileExecutor" (T061)
Task: "Create CompositionTreeExecutor" (T062)
Task: "Create RelationshipGraphExecutor" (T063)
Task: "Create ConstraintSetExecutor" (T064)
Task: "Create FlowBlueprintExecutor" (T065)

# Then sequentially:
Task: "Create OutputAssembler" (T060, depends on perspective executors)
Task: "Create CognitionQueryServiceImpl" (T056, depends on assembler + registry)
Task: "Create CognitionController" (T066, depends on service)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T006) ✓ DONE
2. Complete Phase 2: Foundational (T007-T055)
3. Complete Phase 3: User Story 1 (T056-T066)
4. **STOP and VALIDATE**: Call `/api/v1/cognition/cognition-guidance` with a test bundle, verify 5 core perspectives return correctly, verify PerspectiveExecutor SPI is extensible
5. Deploy/demo if ready

### Incremental Delivery

1. Phase 1+2 → Foundation ready (POM, config, ports, adapters, PerspectiveExecutor SPI in api module)
2. Phase 3 → US1: Template-driven perspective orchestration with 5 core perspectives (MVP!)
3. Phase 4 → US2: Entity-level filtering with step-guide template
4. Phase 5 → US3: Version anchoring + changeWatch
5. Phase 6 → US4: All 14 perspectives + depth/archetype adaptation
6. Phase 7 → US5: Dual format output (json + prompt)
7. Phase 8 → US6: Hierarchical scope narrowing for subtask delegation
8. Phase 9 → Cache, MCP tools, validation, compliance

### Parallel Team Strategy (if multiple developers)

1. Team completes Setup + Foundational together (T001-T055)
2. Once Foundational done:
   - Developer A: US1 core (T056-T066) → unblocks US2-US6
   - After US1 done:
     - Developer A: US2 (T067-T071)
     - Developer B: US3 (T072-T076)
     - Developer C: US4 perspective executors (T082) in parallel
3. US5 (T083-T087) and US6 (T088-T092) can proceed after respective dependencies
4. Final Polish (T093-T099) as a team

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability (US1-US6)
- Each user story should be independently completable and testable
- This BC has NO database, NO Flyway, NO JPA — all tasks follow stateless compute/orchestration patterns
- No test tasks included (not explicitly requested in spec)
- All upstream BC access via api module interfaces, never via direct repository/DAO access
- Domain layer must remain framework-free — no Spring MVC, JPA, WebFlux dependencies in `domain/` package
- Foundation capabilities (ApiResponse, PageRequest, CacheManager, etc.) are already configured by foundation-core — no implementation needed
- **PerspectiveExecutor SPI is in api module** (`api/perspective/`) — third-party developers can implement custom perspectives by depending only on `metaforge-agent-cognition-api`
- All 14 built-in executors implement `api.perspective.PerspectiveExecutor` and live in `core/application/executor/impl/`
- `PerspectiveRegistry` in core auto-discovers all `PerspectiveExecutor` beans (including third-party plugins) via Spring DI
