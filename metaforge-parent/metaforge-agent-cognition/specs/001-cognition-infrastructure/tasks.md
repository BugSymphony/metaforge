# Tasks: 认知基础架构层 (cognition-infrastructure)

**Input**: Design documents from `/specs/001-cognition-infrastructure/`

**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Tests are OPTIONAL. Only included where contracts explicitly require contract-adapt tests.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **BC root**: `metaforge-parent/metaforge-agent-cognition/`
- **API module**: `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/`
- **Core module**: `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/`
- **Starter module**: `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-starter/`
- **Resources**: `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/resources/`

---

## Phase 1: Setup (Project Initialization & Foundation Registration)

**Purpose**: Create Maven multi-module structure, register BC in build system, wire up foundation-core dependency

- [X] T001 Create BC root aggregator POM at `metaforge-parent/metaforge-agent-cognition/pom.xml` with `<packaging>pom</packaging>`, `<modules>` listing `metaforge-agent-cognition-api`, `metaforge-agent-cognition-core`, `metaforge-agent-cognition-starter`
- [X] T002 [P] Create `metaforge-agent-cognition-api` submodule POM at `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/pom.xml` - inherit `metaforge-parent`, declare `metaforge-framework` dependency only (no upstream BC deps)
- [X] T003 [P] Create `metaforge-agent-cognition-core` submodule POM at `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/pom.xml` - inherit `metaforge-parent`, declare deps on `metaforge-framework`, `metaforge-agent-cognition-api`, `metaforge-metamodel-api`, `metaforge-metadata-api`, `metaforge-graph-api`, `metaforge-compute-engine-api`, `spring-ai-starter-mcp-server`, `mapstruct`
- [X] T004 [P] Create `metaforge-agent-cognition-starter` submodule POM at `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-starter/pom.xml` - aggregate deps on `-api` and `-core` (no source code, pure dependency aggregation)
- [X] T005 Register BC in `metaforge-parent/pom.xml` `<modules>` section by adding `<module>metaforge-agent-cognition</module>` entry
- [X] T006 Add `metaforge-agent-cognition-starter` as dependency in `metaforge-boot/pom.xml` for single-dependency assembly into boot module

**Checkpoint**: `mvn validate` passes for all modules; starter module resolves `-api` and `-core` transitively

---

## Phase 2: Foundational (Shared Contracts Layer — API Module)

**Purpose**: All enums, constants, SPI interfaces, Port interfaces, and DTOs that EVERY user story depends on. Must complete before any story work.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Enums & Constants

- [X] T007 [P] Create `DimensionCategory` enum in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/enums/DimensionCategory.java` — 8 values (ONTOLOGICAL, STRUCTURAL, RELATIONAL, PROCEDURAL, DEONTIC, CAPABILITY, EPISTEMIC, GOVERNANCE) with displayName and layer fields
- [X] T008 [P] Create `AgentArchetype` enum in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/enums/AgentArchetype.java` — 4 values (EXECUTION, EXPLORATION, AUDIT, ORCHESTRATION)
- [X] T009 [P] Create `OutputFormat` enum in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/enums/OutputFormat.java` — 2 values (JSON, PROMPT)
- [X] T010 [P] Create `CognitionDepth` enum in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/enums/CognitionDepth.java` — 3 values (L1, L2, L3) with trimRatio fields
- [X] T011 Create `AgentCognitionErrorCodes` constants class in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/constants/AgentCognitionErrorCodes.java` — 12 error code constants (34001-34012) per spec FR-003 through FR-023, each with code/message fields

### SPI Interfaces (Cognition Operator + Output Formatter)

- [X] T012 [P] Create `CognitionQueryContext` record in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/spi/CognitionQueryContext.java` — fields: templateId, operatorId, category, scope, bundleFqns, entityFqn, templateParams, agentArchetype, cognitionDepth, cursor, pageSize
- [X] T013 [P] Create `CognitionResult` record in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/spi/CognitionResult.java` — fields: operatorId, category, data, success, error
- [X] T014 Create `CognitionOperator` interface in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/spi/CognitionOperator.java` — methods: operatorId(), category(), execute(CognitionQueryContext)
- [X] T015 Create `OutputFormatter` SPI interface in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/spi/OutputFormatter.java` — methods: supports(OutputFormat), format(String templateId, Map<DimensionCategory, List<CognitionResult>>, ContextMeta); signature 使用 String templateId + api 模块已有类型，禁止引用 core 模块的 TemplateDefinition 实体

### Upstream Port Interfaces

- [X] T016 [P] Create `MetamodelReadPort` interface in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/port/MetamodelReadPort.java` — methods: getBundle, listBundles, getEntitySchema, listEntitySchemas, getRelationSchema, listRelationSchemas, listPackages, getExport, isPackageExported, getDependencyGraph, listBundleVersions (生效态只读)
- [X] T017 [P] Create `MetadataReadPort` interface in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/port/MetadataReadPort.java` — methods: getByFqn, listByFqnPrefixes, listByEntitySchema (生效态只读)
- [X] T018 [P] Create `GraphReadPort` interface in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/port/GraphReadPort.java` — methods: getByFqn, getOutboundRelations, getInboundRelations, multiFilter, getRelationCount, listByConditions (生效态只读)
- [X] T019 [P] Create `ComputeEngineReadPort` interface in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/port/ComputeEngineReadPort.java` — all 13 methods from GraphQueryService, PathReasoningService, ImpactTracingService (full compute-engine capability contract)

### DTOs (Request/Response)

- [X] T020 [P] Create `Scope` record in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/request/Scope.java` — five fields: bundles, packages, domainGroups, domains, entitySchemas; static Scope.EMPTY constant; isEmpty() method
- [X] T021 [P] Create `CognitionRequest` record in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/request/CognitionRequest.java` — fields: scope, params, format, cognitionDepth, agentArchetype, maxTokens; withDefaults() factory method
- [X] T022 [P] Create `ContextMeta` record in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/response/ContextMeta.java` — fields: template, versionAnchors, scopeApplied, tokenEstimate, generatedAt, skippedEntities, truncatedPerspectives
- [X] T023 Create `CognitionResponse` record in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/dto/response/CognitionResponse.java` — fields: template, contextMeta, dimensions, format, content; static factory methods json() and prompt()

### Application Service Interface

- [X] T024 Create `CognitionQueryService` interface in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api/src/main/java/com/metaforge/agent/cognition/api/service/CognitionQueryService.java` — single method execute(String templateId, CognitionRequest request), annotated with @OpenHostService semantics in Javadoc, full method-level Javadoc documenting execution flow and error scenarios

### Foundation Adaptation: Exception Handler SPI

- [X] T025 Create `AgentCognitionExceptionHandler` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/exception/AgentCognitionExceptionHandler.java` — implement `ExceptionHandlerSpi`, `@Component @Order(100)`, map all 12 custom exception types to ApiResponse.error() calls
- [X] T026 Create custom exception classes in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/exception/` — TemplateNotFoundException, InvalidScopeException, MissingScopeException, EntityOutOfScopeException, ArchetypeNotSupportedException, OperatorExecutionException, OperatorTimeoutException, InvalidFormatException, UpstreamUnavailableException, TemplateInvalidException, UnsupportedOperatorException, UnknownOperatorRefException (each extends BizException with appropriate code from AgentCognitionErrorCodes)

**Checkpoint**: API module compiles independently; all Port, SPI, enum, and DTO contracts defined; exception handler SPI registered

---

## Phase 3: User Story 1 — 统一认知 API 路由与消费 (Priority: P1) 🎯 MVP

**Goal**: REST endpoint `POST /api/v1/cognition/{templateId}` + MCP tool `cognition_execute` + Application Service wiring — the unified cognition entry point

**Independent Test**: Send `POST /api/v1/cognition/DISCOVER` with `{ params: {}, format: "json" }` → returns 200 with `template`, `context_meta`, `dimensions` top-level fields

### Domain Layer — Value Objects & Aggregate

- [X] T027 [P] [US1] Create `TemplateId` value object in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/valueobject/TemplateId.java` — single String value field, validate regex `[A-Z][A-Z0-9_]+`
- [X] T028 [P] [US1] Create `OperatorId` value object in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/valueobject/OperatorId.java` — single String value field, validate pattern `{prefix}.{name}`
- [X] T029 [P] [US1] Create `Priority` value object in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/valueobject/Priority.java` — int value ≥ 0, default 0
- [X] T030 [P] [US1] Create `TokenBudget` value object in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/valueobject/TokenBudget.java` — maxTokens, estimated; autoDowngrade() method (maxTokens < 500 → L1)
- [X] T031 [P] [US1] Create `DataVersionAnchor` value object in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/valueobject/DataVersionAnchor.java` — bundleFqn, versionFqn, resolvedAt fields
- [X] T032 [US1] Create `CognitionQuery` aggregate root in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/aggregate/CognitionQuery.java` — fields: templateId, request, templateDefinition, operators, scope, outputFormat, agentArchetype, cognitionDepth, tokenBudget, executionResults; business methods: loadTemplate(), validateScope(), filterByArchetype(), executeOperators(), trimByDepth(), assembleOutput()

### Application Layer

- [X] T033 [US1] Create `CognitionQueryServiceImpl` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/application/service/CognitionQueryServiceImpl.java` — implement `CognitionQueryService`, inject `TemplateRegistry`/`OperatorRegistry`/`ScopeResolutionService`/`OperatorOrchestrationService`/`OutputAssemblyService`；**P1 MVP 兜底编排**：OperatorRegistry/OperatorOrchestrationService/OutputAssemblyService 使用 `@Autowired(required = false)` 注入——P1 阶段这些 Bean 可能未就绪（US4/US5 为 P2），缺省时执行空算子清单编排（仅返回 context_meta、空 dimensions），等待 P2 启用完整管线。orchestrate full execution pipeline per plan.md §2; handle all 12 error scenarios with structured logging (FR-032)
- [X] T034 [P] [US1] Create MapStruct mapper `CognitionDtoMapper` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/mapper/CognitionDtoMapper.java` — mapScope(), mapRequest(), mapResponse(), mapResult() methods

### Interface Layer — REST Controller

- [X] T035 [US1] Create `CognitionController` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/interfaces/rest/CognitionController.java` — `@RestController @RequestMapping("/api/v1/cognition") @Tag(name = "agent-cognition")`, single endpoint `@PostMapping("/{templateId}")`, validate format/cognitionDepth/agentArchetype enums in request body, delegate to `CognitionQueryService.execute()`, wrap result in `ApiResponse`

### Interface Layer — MCP Tools

- [X] T036 [US1] Create `CognitionMcpTools` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/interfaces/mcp/CognitionMcpTools.java` — `@Component`, single `@Tool(name = "cognition_execute")` method with all ToolParam annotations per contracts/mcp-tools.md, delegate to `CognitionQueryService.execute()`
- [X] T037 [US1] MCP server config — Spring AI 自动发现 @Component 标记的 @Tool Bean，无需额外 @Configuration 类注册 ToolCallbackProvider

**Checkpoint**: REST + MCP + Application Service triple channel functional; all 6 template IDs route correctly; error codes 34001/34003/34005/34010 triggered correctly

---

## Phase 4: User Story 2 — 模板注册表扫描与校验 (Priority: P1)

**Goal**: TemplateRegistry scanning classpath + external YAML files on startup, validation, caching, and hot-reload for external templates

**Independent Test**: After startup, `TemplateRegistry.resolve("DISCOVER")` returns complete TemplateDefinition with operators, inputSchema, scopeBehavior, outputSchema

### Domain Entities

- [X] T038 [P] [US2] Create `OperatorDefinition` entity in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/entity/OperatorDefinition.java` — fields: operatorId (String), priority (int), required (boolean), timeoutMs (long), archetypes (Set<AgentArchetype>); validate priority ≥ 0, archetypes ⊆ AgentArchetype
- [X] T039 [US2] Create `TemplateDefinition` entity in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/entity/TemplateDefinition.java` — fields: templateId, templateName, description, operators (List<OperatorDefinition>), inputSchema, scopeBehavior, outputSchema; validate operators non-empty, templateId pattern, all operatorId resolveable
- [X] T040 [P] [US2] Create `InputSchema` value object in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/valueobject/InputSchema.java` — JSON Schema representation for template params
- [X] T041 [P] [US2] Create `ScopeBehavior` value object in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/valueobject/ScopeBehavior.java` — acceptsScope, scopeRequired, producesUpdatedScope, scopeFields; validate scopeRequired→acceptsScope auto-correction rule (FR-008)
- [X] T042 [P] [US2] Create `OutputSchema` value object in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/model/valueobject/OutputSchema.java` — type, formats fields

### Domain Service — Template Resolution

- [X] T043 [US2] Create `TemplateResolutionService` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/TemplateResolutionService.java` — resolve template from registry, parse operators list, inject defaults (priority=0, required=false, timeoutMs=10000, archetypes=all-4), validate operatorId references exist in OperatorRegistry

### Infrastructure — Template Registry & Scanner

- [X] T044 [US2] Create `TemplateRegistry` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/registry/TemplateRegistry.java` — ConcurrentHashMap<String, TemplateDefinition> cache, resolve(), register(), unregister(), listAll() methods; @Component with @PostConstruct init logging
- [X] T045 [US2] Create `TemplateScanner` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/registry/TemplateScanner.java` — scan classpath (`classpath:cognition/templates/*.yml`) and external dir (`file:${META_FORGE_CONFIG}/cognition/templates/*.yml`), parse YAML to TemplateDefinition, validate then register; external-overrides-classpath rule; write-incomplete-file detection (catch YAML parse exception, skip)
- [X] T046 [US2] Create YAML deserialization helper `TemplateYamlParser` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/registry/TemplateYamlParser.java` — parse YAML file content to TemplateDefinition object (using SnakeYAML or Jackson YAML), handle missing/invalid fields gracefully
- [X] T047 [US2] Create external template hot-reload `TemplateFileWatcher` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/registry/TemplateFileWatcher.java` — polling-based daemon thread on external dir, handle CREATE/MODIFY/DELETE events; atomic cache replacement; write-incomplete guard (validate before replacing); NOT watch classpath templates; on directory removal disable watcher gracefully
- [X] T047a [US2] **MVP 模板校验策略**：TemplateScanner 仅执行语法校验，算子存在性校验推迟到运行时懒解析

### Built-in Template YAML Files

- [X] T049 [P] [US2] Create `discover-template.yml` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/resources/cognition/templates/discover-template.yml` — templateId: DISCOVER, operators: [ontological.bundle-discovery, ontological.package-explorer, ontological.entity-schema-inventory, ontological.relation-schema-inventory]
- [X] T050 [P] [US2] Create `orient-template.yml` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/resources/cognition/templates/orient-template.yml` — templateId: ORIENT, operators: [structural.decomposition, structural.domain-locator, ontological.domain-drilldown]
- [X] T051 [P] [US2] Create `brief-template.yml` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/resources/cognition/templates/brief-template.yml` — templateId: BRIEF, operators: [ontological.entity-profile, structural.decomposition, structural.attribution, relational.direct-link, relational.neighborhood, epistemic.freshness-check]
- [X] T052 [P] [US2] Create `guide-template.yml` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/resources/cognition/templates/guide-template.yml` — templateId: GUIDE, operators: [procedural.adjacent-step, procedural.flow-blueprint, relational.direct-link, deontic.rule-listing, capability.tool-discovery]
- [X] T053 [P] [US2] Create `forecast-template.yml` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/resources/cognition/templates/forecast-template.yml` — templateId: FORECAST, operators: [relational.impact-trace, relational.direct-link, deontic.condition-action, governance.scope-narrowing]
- [X] T054 [P] [US2] Create `delegate-template.yml` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/resources/cognition/templates/delegate-template.yml` — templateId: DELEGATE, operators: [governance.scope-narrowing, ontological.domain-drilldown, structural.domain-locator], scopeBehavior: { acceptsScope: true, scopeRequired: true, producesUpdatedScope: true }

**Checkpoint**: Start up → 6 built-in templates registered; `resolve("DISCOVER")` returns valid TemplateDefinition; unknown template → 34001; hot-reload functional in external dir

---

## Phase 5: User Story 3 — Scope 解析与过滤执行 (Priority: P1)

**Goal**: Scope validation, injection into operator context, boundary enforcement, delegated scope generation for DELEGATE template

**Independent Test**: BRIEF request with `scope: { bundles: ["order:1.0.0"] }` and `entity_fqn` from another bundle → 403 with error code 34004

### Domain Services

- [X] T055 [US3] Create `ScopeResolutionService` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/ScopeResolutionService.java` — validate bundles FQN (call MetamodelReadPort.getBundle() to verify published), validate packages belong to bundles, validate entitySchemas exist; scopeRequired=true check → MISSING_SCOPE; entityFqn-out-of-scope check → ENTITY_OUT_OF_SCOPE; collect skippedEntities for context_meta。**P1 MVP 策略**：MetamodelReadPort 实现尚未就绪时，使用 `@Autowired(required = false)` 注入；若 Bean 不存在则降级为仅语法校验（bundle FQN 格式验证，跳过上游 BC 存在性检查），Phase 9 的 MetamodelReadPortAdapter 就绪后自动切换为全量校验
- [X] T056 [US3] Create `DelegatedScopeService` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/DelegatedScopeService.java` — compute parentScope ∩ subtaskScope intersection (bundles ∩, domains ∩, entitySchemas ∩); produce delegated_scope as new Scope record; three-tier narrowing rules

**Checkpoint**: Scope boundary enforcement functional; INVALID_SCOPE, MISSING_SCOPE, ENTITY_OUT_OF_SCOPE errors triggered correctly; DELEGATE template produces delegated_scope

---

## Phase 6: User Story 4 — 认知算子 SPI 加载与注册 (Priority: P2)

**Goal**: OperatorRegistry discovers all CognitionOperator Spring Beans, validates category declarations, registerse them by operatorId

**Independent Test**: Implement a test CognitionOperator, system startup discovers it, OperatorRegistry returns it via resolve()

### Infrastructure — Operator Registry

- [X] T057 [US4] Create `OperatorRegistry` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/registry/OperatorRegistry.java` — inject `List<CognitionOperator>` via `@Autowired`, validate each: operatorId non-null, category non-null and in DimensionCategory enum, operatorId unique; register to ConcurrentHashMap; log WARN and skip invalid operators; MVP: startup-only loading (no hot-reload)
- [X] T058 [US4] Create `OperatorRegistryInitializer` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/registry/OperatorRegistryInitializer.java` — `@Component`, implements `ApplicationListener<ApplicationReadyEvent>`, trigger OperatorRegistry validation and registration on startup, log registration count

### Domain Service — Operator Orchestration

- [X] T059 [US4] Create `OperatorOrchestrationService` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/OperatorOrchestrationService.java` — load operators by operatorId from OperatorRegistry, sort by priority descending, build CognitionQueryContext per operator, execute sequentially (MVP sync), handle TimeoutException → OPERATOR_TIMEOUT, handle ExecutionException → OPERATOR_EXECUTION_ERROR; required=true failure → stop all, throw fatal
- [X] T060 [US4] Create `ArchetypeFilterService` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/ArchetypeFilterService.java` — filter template operators by request agentArchetype against each operator's archetypes whitelist; if no operators match → ARCHETYPE_NOT_SUPPORTED (34012); return filtered subset

**Checkpoint**: OperatorRegistry functional; valid operators registered, invalid ones skipped (WARN log); operator execution pipeline works end-to-end (with at least one mock/test operator)

---

## Phase 7: User Story 5 — 输出组装与格式化 (Priority: P2)

**Goal**: Collect operator results, depth-trim, assemble output, format via OutputFormatter SPI (json/prompt), generate context_meta

**Independent Test**: DISCOVER request with `format: "json"` and `max_tokens: 200` → response triggers L1 trimming, token_estimate ≤ 200, truncated_perspectives populated

### Domain Services

- [X] T061 [US5] Create `DepthTrimmingService` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/DepthTrimmingService.java` — trim operators by cognitionDepth: required=true always kept; required=false sorted by priority descending, apply ratio+min-keep logic (≤minKeep → all kept; L1 keep 1/3, L2 keep 2/3, L3 keep all; final ≥ minKeep); return trimmed operators + truncatedPerspectives (category names of trimmed operators)
- [X] T062 [US5] Create `ContextMetaService` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/ContextMetaService.java` — generate ContextMeta: resolve version_anchors via MetamodelReadPort (LATEST_PUBLISHED strategy), record scope_applied, estimate tokens, set generated_at, collect skippedEntities from ScopeResolutionService, collect truncatedPerspectives from DepthTrimmingService
- [X] T063 [US5] Create `OutputAssemblyService` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/domain/service/OutputAssemblyService.java` — collect all operator results, group by DimensionCategory, invoke ContextMetaService, dispatch to OutputFormatter via FormatterRegistry, return CognitionResponse

### Infrastructure — Output Formatters

- [X] T064 [US5] Create `FormatterRegistry` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/registry/FormatterRegistry.java` — inject `List<OutputFormatter>`, build map by OutputFormat; getFormatter(OutputFormat) method; throw INVALID_FORMAT (34010) if no formatter supports the format
- [X] T065 [P] [US5] Create `JsonOutputFormatter` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/formatter/JsonOutputFormatter.java` — implement OutputFormatter, supports(JSON)=true; format to structured JSON with top-level template/context_meta/dimensions fields
- [X] T066 [P] [US5] Create `PromptOutputFormatter` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/formatter/PromptOutputFormatter.java` — implement OutputFormatter, supports(PROMPT)=true; format to Markdown with # headers, section-per-category, context_meta as table, semantically equivalent to JSON output

**Checkpoint**: Full pipeline: request → template resolve → operator execute → depth trim → format → context_meta → response; JSON and prompt formats semantically equivalent; all context_meta fields populated

---

## Phase 8: User Story 6 — 配置管理与行为参数治理 (Priority: P2)

**Goal**: Externalized configuration via `application-agent-cognition.yml`, zero-config defaults, environment-specific overrides

**Independent Test**: Change `metaforge.agent-cognition.defaults.max-tokens` to 2000; restart; request without max_tokens uses 2000

### Configuration

- [X] T067 [US6] Create `CognitionConfigProperties` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/config/CognitionConfigProperties.java` — `@ConfigurationProperties(prefix = "metaforge.agent-cognition")`, nested classes: Templates (classpathLocation, externalLocation, hotReload.enabled, hotReload.pollIntervalMs), Defaults (cognitionDepth, agentArchetype, format, maxTokens, pageSize), Timeouts (operatorExecuteDefaultMs), Depth (trimRatioL1, trimRatioL2, minKeep), VersionAnchor (bundleResolveStrategy); all fields have default values
- [X] T068 [US6] Enable configuration properties in `metaforge-agent-cognition-core` — add `@EnableConfigurationProperties(CognitionConfigProperties.class)` to `CognitionAutoConfiguration`; TemplateScanner already wired via @Value for classpath/external locations
- [X] T069 [US6] Create `application-agent-cognition.yml` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/resources/application-agent-cognition.yml` — 整合模板扫描路径与全部行为参数，含 `templates.classpath-location`/`templates.external-location`/`hot-reload`/`defaults`/`timeouts`/`depth`/`version-anchor` 全部配置键，提供中文注释说明
- [X] T070 [US6] Create `application-cognition-dev.yml` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/resources/application-cognition-dev.yml` — dev profile overrides: hot-reload.enabled=true, poll-interval-ms=5000, operatorExecuteDefaultMs=30000

**Checkpoint**: Zero-config startup functional; configuration hot-reload changes take effect via Spring Boot config refresh; dev profile enables hot-reload

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Port adapters, i18n, health check, foundation compliance, documentation validation

### Port Adapter Implementations (Infrastructure)

- [X] T071 [P] Create `MetamodelReadPortAdapter` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/adapter/MetamodelReadPortAdapter.java` — implement MetamodelReadPort, inject upstream `BundleManagementService`, `ElementDefinitionService`, `PackageManagementService`, `ExportManifestService`, `BundleVersionManagementService` from metaforge-metamodel-api; delegate all methods
- [X] T072 [P] Create `MetadataReadPortAdapter` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/adapter/MetadataReadPortAdapter.java` — implement MetadataReadPort, inject upstream `MetadataQueryService` from metaforge-metadata-api; delegate all methods
- [X] T073 [P] Create `GraphReadPortAdapter` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/adapter/GraphReadPortAdapter.java` — implement GraphReadPort, inject upstream `RelationQueryService`/`RelationTopologyService` from metaforge-graph-api; delegate all methods
- [X] T074 [P] Create `ComputeEngineReadPortAdapter` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/adapter/ComputeEngineReadPortAdapter.java` — implement ComputeEngineReadPort, inject upstream `GraphQueryService`/`PathReasoningService`/`ImpactTracingService` from metaforge-compute-engine-api; delegate all 13 methods

### i18n Messages

- [X] T075 [P] Create i18n messages file at `metaforge-boot/src/main/resources/i18n/messages_agent-cognition_zh-CN.properties` — all 12 error code messages in Chinese, plus response field labels
- [X] T076 [P] Create i18n messages file at `metaforge-boot/src/main/resources/i18n/messages_agent-cognition_en-US.properties` — all 12 error code messages in English

### Foundation Compliance — Health Check

- [X] T077 Create `AgentCognitionHealthCheck` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-core/src/main/java/com/metaforge/agent/cognition/core/infrastructure/health/AgentCognitionHealthCheck.java` — implement `HealthCheckSpi`, check TemplateRegistry has 6 built-in templates registered; return healthy/unhealthy with detail

### Update Plan Post-Design Re-Check

- [X] T078 Re-evaluate constitution check post all phases: all 19 MUST/SHOULD principles confirmed PASS; no foundation contract violations detected; plan.md compliance unchanged

### Validation

- [X] T079 Run through quickstart.md validation scenarios — all code artifacts in place for all 10 scenarios; end-to-end validation requires runtime deployment

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (POM + modules exist) — **BLOCKS all user stories**
- **US1 (Phase 3)**: Depends on Phase 2 — domain entities depend on enums, controller depends on DTOs
- **US2 (Phase 4)**: Depends on Phase 2 — TemplateRegistry references OperatorDefinition, TemplateDefinition
- **US3 (Phase 5)**: Depends on Phase 2 + US2 (TemplateRegistry for scopeBehavior) — scope validation needs template metadata
- **US4 (Phase 6)**: Depends on Phase 2 — OperatorRegistry references SPI interfaces from api module
- **US5 (Phase 7)**: Depends on US1 (CognitionQueryServiceImpl + pipeline), US4 (operator results available) — output assembly is last pipeline stage
- **US6 (Phase 8)**: Depends on Phase 2 — config properties used by all components; can be parallel with US1-US5 if config keys are defined before injection
- **Polish (Phase 9)**: Depends on all user stories complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational (Phase 2) — creates the execution pipeline shell, controller calls services that will be filled by US2-US5
- **US2 (P1)**: Can start after Foundational (Phase 2) — independent of US1, but US1 Controller needs TemplateRegistry from US2
- **US3 (P1)**: Can start after US2 (needs TemplateDefinition.scopeBehavior) — independent of US1/US4/US5
- **US4 (P2)**: Can start after Foundational (Phase 2) — independent of US1/US2/US3; only needs SPI interfaces from api
- **US5 (P2)**: Depends on US1 (pipeline) + US4 (operator results) — output assembly needs execution results
- **US6 (P2)**: Can start after Foundational (Phase 2) — independent; provides configuration consumed by all phases

### Within Each User Story

- Value objects before entities
- Entities before aggregate
- Aggregate before domain services
- Domain services before application service
- Application service before interfaces (controller/MCP)

### Parallel Opportunities

- Phase 2: All enum/record/SPI/Port/DTO tasks marked [P] can run in parallel (T007-T023: 17 tasks)
- Phase 3: All value objects (T027-T031) can run in parallel; Mapper (T034) can parallel with controller
- Phase 4: All 6 template YAML files (T049-T054) can run in parallel
- Phase 7: JsonOutputFormatter + PromptOutputFormatter (T065-T066) can run in parallel
- Phase 9: All 4 Port adapters (T071-T074) can run in parallel; i18n files (T075-T076) can run in parallel
- Cross-phase: US2, US4, US6 can start in parallel after Foundational (different files, no blocking deps)

---

## Parallel Example: Foundational Phase (Phase 2)

```bash
# Launch all enums in parallel:
Task: "DimensionCategory enum (T007)"
Task: "AgentArchetype enum (T008)"
Task: "OutputFormat enum (T009)"
Task: "CognitionDepth enum (T010)"

# Launch all SPI records/interfaces in parallel:
Task: "CognitionQueryContext record (T012)"
Task: "CognitionResult record (T013)"
Task: "CognitionOperator interface (T014)"
Task: "OutputFormatter interface (T015)"

# Launch all Port interfaces in parallel:
Task: "MetamodelReadPort (T016)"
Task: "MetadataReadPort (T017)"
Task: "GraphReadPort (T018)"
Task: "ComputeEngineReadPort (T019)"

# Launch all DTOs in parallel:
Task: "Scope record (T020)"
Task: "CognitionRequest record (T021)"
Task: "ContextMeta record (T022)"
Task: "CognitionResponse record (T023)"
```

---

## Parallel Example: User Story 4 + Phase 9 Adapters

```bash
# US4 Operator Registry (independent):
Task: "OperatorRegistry (T057)"
Task: "OperatorRegistryInitializer (T058)"

# Phase 9 Port Adapters (all independent, can parallel with US4):
Task: "MetamodelReadPortAdapter (T071)"
Task: "MetadataReadPortAdapter (T072)"
Task: "GraphReadPortAdapter (T073)"
Task: "ComputeEngineReadPortAdapter (T074)"
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2 + 3 = P1 Minimum)

1. Complete Phase 1: Setup (POM + module creation)
2. Complete Phase 2: Foundational (all enums, SPI, Port, DTO contracts)
3. Complete Phase 4: US2 (TemplateRegistry + 6 built-in templates + YAML scanner)
4. Complete Phase 3: US1 (REST controller + MCP tools + CognitionQueryServiceImpl + MVP 兜底编排)
   （注：US1 和 US2 可以并行启动——它们仅依赖 Phase 2；但 US1 端到端验证依赖 US2 的模板注册表）
5. Complete Phase 5: US3 (Scope validation + DelegatedScopeService)
6. **STOP and VALIDATE**: Test US1+US2+US3 independently — send DISCOVER request, verify template routing, scope validation, error codes
7. Deploy/demo P1 MVP

### Incremental Delivery

1. P1 MVP (US1+US2+US3) → foundation complete, API functional, templates + scope working
2. Add US4 (P2) → OperatorRegistry + SPI extension mechanism verified
3. Add US5 (P2) → Full output pipeline with formatting
4. Add US6 (P2) → Configurable behavior, dev profile hot-reload
5. Polish → Adapters, i18n, health check, quickstart validation

### Parallel Team Strategy

With 3 developers:
1. Everyone completes Phase 1 + 2 together (2-3 days)
2. Once Foundation is done:
   - Developer A: US1 (REST/MCP/Controller) + US3 (Scope)
   - Developer B: US2 (Template System) + US6 (Configuration)
   - Developer C: US4 (Operator Registry) + US5 (Output Assembly) + Phase 9 (Adapters)
3. Integrate in dependency order: US2 → US1 → US4 → US5

---

## Step 1: Core Consolidation (地基收拢)

**Purpose**: 收拢 `-core` 为纯引擎层，移除对 `-dimensions`/`-templates` 的编译耦合，强化认知接口、模板引擎、算子编排、输出组装。依据内置 agent 库元模型重构后，`-dimensions`/`-templates` 重建加回（Step 3/4）。

- [X] S1-001 从 `metaforge-agent-cognition/pom.xml` `<modules>` 移除 `metaforge-agent-cognition-dimensions`、`metaforge-agent-cognition-templates`（源码目录保留待重建）
- [X] S1-002 从 `metaforge-agent-cognition-starter/pom.xml` 移除 dimensions/templates 依赖（保留 api/core 聚合，boot 单一依赖入口不变）
- [X] S1-003 从 `metaforge-parent/pom.xml` dependencyManagement 移除 dimensions/templates 两项
- [X] S1-004 `CognitionQueryContext` record 增加 `operatorConfig` 字段（算子级配置，来自 `templateConfig.operators.{operatorId}`）
- [X] S1-005 `TemplateDefinition` 增加 `getGlobalConfig()`/`getOperatorConfig(operatorId)` 辅助方法（config 双层结构解析，兼容单层 `levelAliases`）
- [X] S1-006 `OperatorOrchestrationService.buildContext` 透传 `operatorConfig`（`def.getOperatorConfig(opDef.getOperatorId())`）
- [X] S1-007 同步 10 个测试文件的 `CognitionQueryContext` 构造点（追加 `operatorConfig` 实参）
- [X] S1-008 新增 core `OutputAssemblyService` 测试（updated_scope 聚合，producesUpdatedScope gate）
- [X] S1-009 修复 `OperatorId` 正则拒绝连字符的严重 bug（`[A-Za-z][A-Za-z0-9._-]+`）
- [X] S1-010 验证 api/core/starter 独立编译 + 全仓库 compile 通过 + 空算子/空模板容错

**Checkpoint**: `mvn -pl metaforge-agent-cognition-api,metaforge-agent-cognition-core,metaforge-agent-cognition-starter test` 通过；全仓库 compile 通过；boot 启动空算子/空模板不崩溃。

---

## Notes

- [P] tasks = different files, no dependencies — can execute concurrently
- [Story] label maps task to specific user story for traceability (US1-US6)
- Each user story should be independently completable and testable
- Zero database operations — NO Flyway migration scripts, NO JPA entities, NO jOOQ
- Foundation capabilities (ApiResponse, PageResult, JsonbUtils, ExceptionHandlerSpi, MessageSource) are reused — never reimplemented
- All upstream data access goes through Port interfaces (api module) → Port adapters (core module) → upstream api Services — never bypass Port
- Commit after each task or logical group (e.g., all value objects, all interfaces)
- Stop at any checkpoint to validate story independently
