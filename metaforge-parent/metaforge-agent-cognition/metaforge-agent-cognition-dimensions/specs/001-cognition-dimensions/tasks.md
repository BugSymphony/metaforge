# Tasks: 认知算子实现层

**Input**: Design documents from `specs/001-cognition-dimensions/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included — per spec acceptance scenarios.

**Organization**: Tasks grouped by user story (7 stories, P1→P2→P3) for independent implementation.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story (US1–US7)
- Include exact file paths

## Path Conventions

`$BC_PATH = metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-dimensions/`

```
src/main/java/com/metaforge/agent/cognition/operator/
├── common/                 # AbstractCognitionOperator
├── ontological/            # US1 (7 ops)
├── structural/             # US2 (3 ops)
├── relational/             # US3 (3 ops)
├── procedural/             # US4 (3 ops)
├── deontic/                # US5 (3 ops)
├── capability/             # US6 (3 ops)
├── epistemic/              # US7 (1 op)
└── governance/             # US7 (1 op)

src/test/java/com/metaforge/agent/cognition/operator/
└── [matching test packages]
```

---

## Phase 1: Setup (Module Registration)

**Purpose**: Register `-dimensions` module in Maven reactor, create pom.xml, configure dependencies.

- [x] T001 [P] Register `metaforge-agent-cognition-dimensions` in `$BC_PATH/../pom.xml` (`metaforge-agent-cognition/pom.xml`) `<modules>` section
- [x] T002 [P] Create `$BC_PATH/pom.xml` with parent `metaforge-agent-cognition`, artifactId `metaforge-agent-cognition-dimensions`, and single dependency on `metaforge-agent-cognition-api`
- [x] T003 [P] Add `metaforge-agent-cognition-dimensions` dependency to `$BC_PATH/../metaforge-agent-cognition-starter/pom.xml`

**Checkpoint**: Module builds successfully (`mvn compile -pl metaforge-parent/metaforge-agent-cognition -am`)

---

## Phase 2: Foundational (AbstractCognitionOperator)

**Purpose**: Abstract base class providing Port injection, scope cropping, failure templates, lazy node construction — shared by ALL 24 operators.

**⚠️ CRITICAL**: No operator implementation can begin until this phase is complete.

- [x] T004 Create `AbstractCognitionOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/common/AbstractCognitionOperator.java`:
  - `@Autowired` fields: `metamodelReadPort`, `metadataReadPort`, `graphReadPort`, `computeEngineReadPort`
  - `applyScope(data, scope)` — filter results by scope, annotate out-of-scope entities
  - `buildLazyNode(data, hasChildren, suggestedNextCall)` — return `Map<String, Object>` lazy node structure
  - `wrapFailure(error)` — delegate to `CognitionResult.failure(operatorId(), category(), error)`
  - `executeWithPort(Supplier<T>)` — try-catch wrapper, return `CognitionResult.failure` on exception

- [x] T005 [P] Add unit test for `AbstractCognitionOperator` in `$BC_PATH/src/test/java/com/metaforge/agent/cognition/operator/common/AbstractCognitionOperatorTest.java`:
  - Verify `wrapFailure` returns `CognitionResult` with `success=false`
  - Verify `executeWithPort` catches exception and returns failure
  - Verify `buildLazyNode` produces correct map structure
  - Verify `applyScope` correctly filters entities within scope boundaries

**Checkpoint**: Base class tested and ready — operator implementation can now begin in parallel

---

## Phase 3: User Story 1 - 本体论认知算子链 (ONTOLOGICAL, P1) 🎯 MVP

**Goal**: 7 ONTOLOGICAL operators enabling Agent domain discovery from Bundle→Package→EntitySchema→Instance→Entity.

**Independent Test**: Run `OntologicalOperatorsTest` — verify full discovery chain with mocked Ports.

### Tests for User Story 1

- [x] T006 [P] [US1] Create `OntologicalOperatorsTest` in `$BC_PATH/src/test/java/com/metaforge/agent/cognition/operator/ontological/OntologicalOperatorsTest.java`:
  - Mock `MetamodelReadPort`, `MetadataReadPort`, `GraphReadPort`
  - Test bundle-discovery → package-explorer → entity-schema-inventory → relation-schema-inventory full chain
  - Test scope filtering: `scope.bundles=["order:1.0.0"]` excludes other Bundles
  - Test lazy node mode: `has_children`, `suggested_next_call` correctness
  - Test `entity-profile` returns full attributes + EntitySchema structure + domain_location
  - Test edge cases: unknown Bundle FQN returns empty, unknown entity_fqn returns failure

### Implementation for User Story 1

- [x] T007 [P] [US1] Implement `OntologicalBundleDiscoveryOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/ontological/OntologicalBundleDiscoveryOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = ONTOLOGICAL`, `operatorId = "ontological.bundle-discovery"`
  - Call `metamodelReadPort.listBundles(pageRequest)`, apply `scope.bundles` filter
  - Return lazy nodes: `buildLazyNode(bundle, hasChildren(packages), "ontological.package-explorer")`
  - Produce `updated_scope.bundles` in result data

- [x] T008 [P] [US1] Implement `OntologicalPackageExplorerOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/ontological/OntologicalPackageExplorerOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = ONTOLOGICAL`, `operatorId = "ontological.package-explorer"`
  - Call `metamodelReadPort.listPackages(bundleVersionFqn)` + `graphReadPort.getOutboundRelations(entityFqn, "COMPOSITION", null)`
  - Apply `scope.packages` filter, return lazy nodes

- [x] T009 [P] [US1] Implement `OntologicalEntitySchemaInventoryOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/ontological/OntologicalEntitySchemaInventoryOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = ONTOLOGICAL`, `operatorId = "ontological.entity-schema-inventory"`
  - Call `metamodelReadPort.listEntitySchemas(query)`, then `metadataReadPort.listByEntitySchema(entitySchemaFqn, pageRequest)` for `instance_count`
  - Return lazy nodes with `instance_count` + `key_attributes`, produce `updated_scope.entity_schemas`

- [x] T010 [P] [US1] Implement `OntologicalRelationSchemaInventoryOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/ontological/OntologicalRelationSchemaInventoryOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = ONTOLOGICAL`, `operatorId = "ontological.relation-schema-inventory"`
  - Call `metamodelReadPort.listRelationSchemas(query)`, return lazy nodes

- [x] T011 [US1] Implement `OntologicalDomainDrillDownOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/ontological/OntologicalDomainDrillDownOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = ONTOLOGICAL`, `operatorId = "ontological.domain-drilldown"`
  - Support `level=null` auto-discovery (group by entity_type) and specified level filter
  - Call `metadataReadPort.listByFqnPrefixes(prefixes, pageRequest)` + `graphReadPort` for COMPOSITION edges
  - Produce `updated_scope.domains`, return lazy nodes per level

- [x] T012 [P] [US1] Implement `OntologicalInstanceCatalogOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/ontological/OntologicalInstanceCatalogOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = ONTOLOGICAL`, `operatorId = "ontological.instance-catalog"`
  - Call `metadataReadPort.listByEntitySchema(entitySchemaFqn, pageRequest)`, support `pageSize`/`cursor` pagination

- [x] T013 [P] [US1] Implement `OntologicalEntityProfileOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/ontological/OntologicalEntityProfileOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = ONTOLOGICAL`, `operatorId = "ontological.entity-profile"`
  - Call `metadataReadPort.getByFqn(fqn)` + `metamodelReadPort.getEntitySchema(fqn)` for structure + `structural.domain-locator` pattern for domain_location

**Checkpoint**: ONTOLOGICAL chain test passes — 7 operators functional, scope filtering works, lazy mode correct.

---

## Phase 4: User Story 2 - 结构论认知算子 (STRUCTURAL, P1)

**Goal**: 3 STRUCTURAL operators (decomposition, belonging, domain-locator) providing entity composition understanding.

**Independent Test**: Run `StructuralOperatorsTest` — call decomposition+b+belonging+domain-locator on same entity, verify consistency.

### Tests for User Story 2

- [x] T014 [P] [US2] Create `StructuralOperatorsTest` in `$BC_PATH/src/test/java/com/metaforge/agent/cognition/operator/structural/StructuralOperatorsTest.java`:
  - Mock `ComputeEngineReadPort`, `GraphReadPort`, `MetadataReadPort`
  - Test `decomposition` FORWARD returns full COMPOSITION subtree
  - Test `belonging` BACKWARD returns parent chain to root
  - Test `domain-locator` returns L1→L5 path coordinates
  - Test edge cases: no COMPOSITION children → NO_CHILDREN, entity not in tree → graceful empty

### Implementation for User Story 2

- [x] T015 [P] [US2] Implement `StructuralDecompositionOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/structural/StructuralDecompositionOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = STRUCTURAL`, `operatorId = "structural.decomposition"`
  - Call `computeEngineReadPort.queryCompositionTree(request)` with FORWARD direction

- [x] T016 [P] [US2] Implement `StructuralBelongingOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/structural/StructuralBelongingOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = STRUCTURAL`, `operatorId = "structural.belonging"`
  - Call `computeEngineReadPort.queryCompositionTree(request)` with BACKWARD direction

- [x] T017 [P] [US2] Implement `StructuralDomainLocatorOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/structural/StructuralDomainLocatorOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = STRUCTURAL`, `operatorId = "structural.domain-locator"`
  - Call `graphReadPort.getInboundRelations(entityFqn, "COMPOSITION", null)` recursively until L1 root, build path coordinates

**Checkpoint**: STRUCTURAL operators test passes — decomposition, belonging, domain-locator all functional.

---

## Phase 5: User Story 3 - 关系论认知算子 (RELATIONAL, P2)

**Goal**: 3 RELATIONAL operators (direct-link, neighborhood, impact-trace) providing entity relationship understanding.

**Independent Test**: Run `RelationalOperatorsTest` — verify neighborhood superset relationship, impact diffusion consistency.

### Tests for User Story 3

- [x] T018 [P] [US3] Create `RelationalOperatorsTest` in `$BC_PATH/src/test/java/com/metaforge/agent/cognition/operator/relational/RelationalOperatorsTest.java`:
  - Mock `GraphReadPort`, `ComputeEngineReadPort`
  - Test `direct-link` groups by AssociationType, returns inbound + outbound
  - Test `neighborhood` returns 2-degree entities including indirect
  - Test `impact-trace` forward diffusion returns cascading chain
  - Test `impact-trace` backward trace returns upstream dependencies
  - Test edge cases: scope boundary cutoff in diffusion, `getImpactPaths` returns full path details

### Implementation for User Story 3

- [x] T019 [P] [US3] Implement `RelationalDirectLinkOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/relational/RelationalDirectLinkOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = RELATIONAL`, `operatorId = "relational.direct-link"`
  - Call `graphReadPort.getOutboundRelations(entityFqn, null, null)` + `getInboundRelations(entityFqn, null, null)`, group by `AssociationType`

- [x] T020 [P] [US3] Implement `RelationalNeighborhoodOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/relational/RelationalNeighborhoodOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = RELATIONAL`, `operatorId = "relational.neighborhood"`
  - Call `computeEngineReadPort.queryAdjacency(request)` with `maxDepth` from `templateParams` (default 2, max 3)

- [x] T021 [US3] Implement `RelationalImpactTraceOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/relational/RelationalImpactTraceOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = RELATIONAL`, `operatorId = "relational.impact-trace"`
  - Call `computeEngineReadPort.diffuseForward(request)` for forward diffusion
  - Call `computeEngineReadPort.traceBackward(request)` for backward trace
  - Call `computeEngineReadPort.getImpactPaths(source, target, types, maxDepth)` for path details

**Checkpoint**: RELATIONAL operators test passes — direct-link, neighborhood, impact-trace all functional.

---

## Phase 6: User Story 4 - 流程论认知算子 (PROCEDURAL, P2)

**Goal**: 3 PROCEDURAL operators (flow-blueprint, adjacent-step, decision-branch) providing process understanding.

**Independent Test**: Run `ProceduralOperatorsTest` — verify blueprint sequence, adjacent-step consistency with blueprint, branch identification.

### Tests for User Story 4

- [x] T022 [P] [US4] Create `ProceduralOperatorsTest` in `$BC_PATH/src/test/java/com/metaforge/agent/cognition/operator/procedural/ProceduralOperatorsTest.java`:
  - Mock `GraphReadPort`, `ComputeEngineReadPort`
  - Test `flow-blueprint` returns longest path with ENTRY/DECISION/EXIT markers
  - Test `adjacent-step` prev/next match blueprint sequence
  - Test `decision-branch` identifies >1 outbound PROCESS_SEQUENCE edges, returns PRIMARY/ALTERNATIVE
  - Test edge cases: no M1 blueprint → NO_BLUEPRINT, single outbound → not a decision branch

### Implementation for User Story 4

- [x] T023 [P] [US4] Implement `ProceduralFlowBlueprintOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/procedural/ProceduralFlowBlueprintOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = PROCEDURAL`, `operatorId = "procedural.flow-blueprint"`
  - Call `computeEngineReadPort.findPaths(request)` for longest path, annotate steps as ENTRY/DECISION/EXIT

- [x] T024 [P] [US4] Implement `ProceduralAdjacentStepOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/procedural/ProceduralAdjacentStepOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = PROCEDURAL`, `operatorId = "procedural.adjacent-step"`
  - Call `graphReadPort.getOutboundRelations(entityFqn, "PROCESS_SEQUENCE", null)` + `getInboundRelations(...)`

- [x] T025 [P] [US4] Implement `ProceduralDecisionBranchOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/procedural/ProceduralDecisionBranchOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = PROCEDURAL`, `operatorId = "procedural.decision-branch"`
  - Call `graphReadPort.getOutboundRelations(entityFqn, "PROCESS_SEQUENCE", null)`, if >1: for each call `computeEngineReadPort.diffuseForward(request)` for downstream impact, annotate PRIMARY/ALTERNATIVE

**Checkpoint**: PROCEDURAL operators test passes — flow-blueprint, adjacent-step, decision-branch all functional.

---

## Phase 7: User Story 5 - 约束论认知算子 (DEONTIC, P2)

**Goal**: 3 DEONTIC operators (rule-listing, level-classifier, condition-action) providing constraint understanding.

**Independent Test**: Run `DeonticOperatorsTest` — verify rule chain: listing → level classification → condition/action extraction.

### Tests for User Story 5

- [x] T026 [P] [US5] Create `DeonticOperatorsTest` in `$BC_PATH/src/test/java/com/metaforge/agent/cognition/operator/deontic/DeonticOperatorsTest.java`:
  - Mock `GraphReadPort`, `MetadataReadPort`
  - Test `rule-listing` returns rules from DEPENDENCY_INFLUENCE inbound + ASSOCIATION_REFERENCE edges
  - Test `level-classifier` returns MANDATORY/RECOMMENDED/REFERENCE classification
  - Test `condition-action` extracts condition + action fields correctly
  - Test edge cases: no rules → empty list, missing constraint_level → default classification

### Implementation for User Story 5

- [x] T027 [P] [US5] Implement `DeonticRuleListingOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/deontic/DeonticRuleListingOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = DEONTIC`, `operatorId = "deontic.rule-listing"`
  - Call `graphReadPort.getInboundRelations(entityFqn, "DEPENDENCY_INFLUENCE", null)` + `getInboundRelations(entityFqn, "ASSOCIATION_REFERENCE", null)` + `metadataReadPort.getByFqn(fqn)` for details

- [x] T028 [P] [US5] Implement `DeonticLevelClassifierOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/deontic/DeonticLevelClassifierOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = DEONTIC`, `operatorId = "deontic.level-classifier"`
  - Call `metadataReadPort.getByFqn(fqn)`, read `constraint_level`/`level` attribute, classify as MANDATORY/RECOMMENDED/REFERENCE

- [x] T029 [P] [US5] Implement `DeonticConditionActionOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/deontic/DeonticConditionActionOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = DEONTIC`, `operatorId = "deontic.condition-action"`
  - Call `metadataReadPort.getByFqn(fqn)`, extract `condition` + `action` fields

**Checkpoint**: DEONTIC operators test passes — rule-listing, level-classifier, condition-action all functional.

---

## Phase 8: User Story 6 - 能力论认知算子 (CAPABILITY, P3)

**Goal**: 3 CAPABILITY operators (tool-discovery, call-method, protocol-detail) bridging cognition to action.

**Independent Test**: Run `CapabilityOperatorsTest` — verify tool discovery → call method identification → protocol detail expansion.

### Tests for User Story 6

- [x] T030 [P] [US6] Create `CapabilityOperatorsTest` in `$BC_PATH/src/test/java/com/metaforge/agent/cognition/operator/capability/CapabilityOperatorsTest.java`:
  - Mock `GraphReadPort`, `MetadataReadPort`
  - Test `tool-discovery` returns capabilities from ASSOCIATION_REFERENCE edges
  - Test `call-method` identifies REST/MCP/CLI/LocalMethod
  - Test `protocol-detail` expands interface_spec (Http endpoint/method/headers or McpTool server/arguments)
  - Test edge cases: no capabilities → empty list, no interface_spec → empty protocol detail

### Implementation for User Story 6

- [x] T031 [P] [US6] Implement `CapabilityToolDiscoveryOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/capability/CapabilityToolDiscoveryOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = CAPABILITY`, `operatorId = "capability.tool-discovery"`
  - Call `graphReadPort.getOutboundRelations(entityFqn, "ASSOCIATION_REFERENCE", null)` + `getInboundRelations(...)`

- [x] T032 [P] [US6] Implement `CapabilityCallMethodOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/capability/CapabilityCallMethodOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = CAPABILITY`, `operatorId = "capability.call-method"`
  - Call `metadataReadPort.getByFqn(fqn)`, read `call_method` field, identify as REST/MCP/CLI/LocalMethod

- [x] T033 [US6] Implement `CapabilityProtocolDetailOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/capability/CapabilityProtocolDetailOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = CAPABILITY`, `operatorId = "capability.protocol-detail"`
  - Call `metadataReadPort.getByFqn(fqn)` for `interface_spec` + `graphReadPort.getOutboundRelations(entityFqn, "COMPOSITION", null)` for protocol subtypes

**Checkpoint**: CAPABILITY operators test passes — tool-discovery, call-method, protocol-detail all functional.

---

## Phase 9: User Story 7 - 认知论与治理算子 (EPISTEMIC + GOVERNANCE, P3)

**Goal**: 2 operators (freshness-check, scope-narrowing) providing version awareness and scope management.

**Independent Test**: Run `EpistemicOperatorsTest` for version_anchors, `GovernanceOperatorsTest` for three-layer narrowing.

### Tests for User Story 7

- [x] T034 [P] [US7] Create `EpistemicFreshnessCheckTest` in `$BC_PATH/src/test/java/com/metaforge/agent/cognition/operator/epistemic/EpistemicFreshnessCheckTest.java`:
  - Test `freshness-check` compares `context_meta.version_anchors` with cached versions, returns staleness flag

- [x] T035 [P] [US7] Create `GovernanceScopeNarrowingTest` in `$BC_PATH/src/test/java/com/metaforge/agent/cognition/operator/governance/GovernanceScopeNarrowingTest.java`:
  - Mock `GraphReadPort`, `MetadataReadPort`, `MetamodelReadPort`
  - Test three-layer narrowing: blueprint (1-2 steps PROCESS_SEQUENCE), entity collection, Schema dedup
  - Verify result is subset of original scope

### Implementation for User Story 7

- [x] T036 [P] [US7] Implement `EpistemicFreshnessCheckOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/epistemic/EpistemicFreshnessCheckOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = EPISTEMIC`, `operatorId = "epistemic.freshness-check"`
  - Pure `context_meta` operation — compare `version_anchors` from context with cached Bundle versions

- [x] T037 [P] [US7] Implement `GovernanceScopeNarrowingOperator` in `$BC_PATH/src/main/java/com/metaforge/agent/cognition/operator/governance/GovernanceScopeNarrowingOperator.java`:
  - Extends `AbstractCognitionOperator`, `@Component`, `category = GOVERNANCE`, `operatorId = "governance.scope-narrowing"`
  - Layer 1: `graphReadPort` PROCESS_SEQUENCE outbound/inbound 1-2 steps → sub-blueprint
  - Layer 2: `graphReadPort` related edges → entity FQN collection
  - Layer 3: `metadataReadPort.getByFqn(fqn)` → reverse-lookup `entity_schema_fqn`, dedup → Schema set

**Checkpoint**: EPISTEMIC + GOVERNANCE operators test passes — freshness-check, scope-narrowing both functional.

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Full test suite validation, documentation, module integration verification.

- [x] T038 [P] Update `quickstart.md` validation scenarios — execute all 8 verification commands and confirm output
- [x] T039 [P] Run full operator test suite: `mvn test -pl metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-dimensions` — confirm 24 operators all passing
- [x] T040 [P] Verify module registration — `mvn compile -pl metaforge-parent/metaforge-agent-cognition -am` passes with `-dimensions` in classpath
- [x] T041 [P] Verify Starter integration — `metaforge-agent-cognition-starter` transitive dependencies include `-dimensions` module
- [x] T042 Operator catalog contract consistency — verify all 25 `operatorId` values in `contracts/operator-catalog.md` match implementation
- [x] T043 [P] Create determinism test (SC-006) in `$BC_PATH/src/test/java/com/metaforge/agent/cognition/operator/common/OperatorDeterminismTest.java`:
  - Select 3 representative operators (ONTOLOGICAL, STRUCTURAL, RELATIONAL)
  - Execute each operator 10 times with identical `CognitionQueryContext` and identical mocked Port responses
  - Assert all 10 `CognitionResult.data` objects are deep-equal
  - Covers SC-006: "同一输入同一数据状态下执行10次，返回数据完全一致"
- [x] T044 [P] Create timeout test (FR-010) in `$BC_PATH/src/test/java/com/metaforge/agent/cognition/operator/common/OperatorTimeoutTest.java`:
  - Create test operator that calls a Port method with simulated delay exceeding `timeoutMs`
  - Verify `executeWithPort()` returns `CognitionResult.failure` with error code `OPERATOR_TIMEOUT`
  - Verify non-timeout Port calls (within `timeoutMs`) return success
  - Covers FR-010: "每个算子必须遵守 timeoutMs 超时约束，超时返回失败标注 OPERATOR_TIMEOUT"

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup (needs pom.xml) — BLOCKS all user stories
- **User Stories (Phase 3–9)**: All depend on Foundational (Phase 2) completion
  - US1 (ONTOLOGICAL, P1) → US2 (STRUCTURAL, P1) → US3 (RELATIONAL, P2) → US4 (PROCEDURAL, P2) → US5 (DEONTIC, P2) → US6 (CAPABILITY, P3) → US7 (EPISTEMIC+GOVERNANCE, P3)
  - OR: all can proceed in parallel after Phase 2 (different packages, no code dependencies)
- **Polish (Phase 10)**: Depends on all user stories being complete

### Within Each User Story

- Tests → Implementation (tests first, verify fail before coding)
- All operators within a story marked [P] can run in parallel
- Stories themselves are independent (different categories, no cross-category dependencies)

### Parallel Opportunities

- T001, T002, T003 (Setup) — all [P], different files
- T007–T013 (US1 7 operators) — all [P] except T011 which may depend on domain understanding
- T015–T017 (US2 3 operators) — all [P]
- T019–T021 (US3 3 operators) — all [P]
- T023–T025 (US4 3 operators) — all [P]
- T027–T029 (US5 3 operators) — all [P]
- T031–T033 (US6 3 operators) — all [P] except T033
- T036, T037 (US7 2 operators) — all [P]
- All user story test files (T006, T014, T018, T022, T026, T030, T034, T035) — all [P] across stories
- T043, T044 (Polish cross-cutting tests) — both [P], independent files

## Parallel Example: User Story 1 (ONTOLOGICAL)

```bash
# All 7 ONTOLOGICAL operators can be implemented in parallel:
Task: "Implement bundle-discovery in .../ontological/OntologicalBundleDiscoveryOperator.java"
Task: "Implement package-explorer in .../ontological/OntologicalPackageExplorerOperator.java"
Task: "Implement entity-schema-inventory in .../ontological/OntologicalEntitySchemaInventoryOperator.java"
Task: "Implement relation-schema-inventory in .../ontological/OntologicalRelationSchemaInventoryOperator.java"
Task: "Implement domain-drilldown in .../ontological/OntologicalDomainDrillDownOperator.java"
Task: "Implement instance-catalog in .../ontological/OntologicalInstanceCatalogOperator.java"
Task: "Implement entity-profile in .../ontological/OntologicalEntityProfileOperator.java"
```

---

## Implementation Strategy

### MVP First (US1 + US2 Only)

1. Complete Phase 1: Setup (module registration)
2. Complete Phase 2: Foundational (AbstractCognitionOperator)
3. Complete Phase 3: User Story 1 (ONTOLOGICAL — 7 operators)
4. Complete Phase 4: User Story 2 (STRUCTURAL — 3 operators)
5. **STOP and VALIDATE**: Full discovery chain + structure chain work end-to-end
6. Deploy/demo — 10 operators covering the most critical Agent discovery scenarios

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. + US1 + US2 (P1) → Agent can discover domains and understand structure → MVP!
3. + US3 + US4 + US5 (P2) → Agent can explore relationships, processes, constraints
4. + US6 + US7 (P3) → Agent can discover capabilities, check freshness, narrow scope
5. Polish → Full 24 operators, all tests passing

### Parallel Team Strategy

With multiple developers:
1. Team completes Setup + Foundational together
2. Once Phase 2 is done, split by category:
   - Dev A: US1 (ONTOLOGICAL — 7 ops)
   - Dev B: US2 + US3 (STRUCTURAL + RELATIONAL — 6 ops)
   - Dev C: US4 + US5 (PROCEDURAL + DEONTIC — 6 ops)
   - Dev D: US6 + US7 (CAPABILITY + EPISTEMIC/GOVERNANCE — 5 ops)
3. All stories are independent packages — no merge conflicts expected

---

## Notes

- [P] tasks = different files, no dependencies within the same story
- [Story] label maps task to specific user story for traceability
- All operators extend `AbstractCognitionOperator`, no need to redefine Port injection
- All operators use `executeWithPort()` for upstream calls — automatic exception → failure conversion
- All operators apply `applyScope()` before returning results
- Lazy node operators use `buildLazyNode()`, full result operators return data directly
- Tests mock all 4 Port interfaces — no real upstream BC needed for unit testing
- After each story checkpoint, run `mvn test` for that story's package to validate independence
