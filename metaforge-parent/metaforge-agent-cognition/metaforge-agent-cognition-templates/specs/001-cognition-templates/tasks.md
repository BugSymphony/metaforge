# Tasks: 认知模板配置层 (cognition-templates)

**Input**: Design documents from `specs/001-cognition-templates/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/template-contract.md

**Organization**: Tasks grouped by user story to enable independent validation of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

All paths relative to BC root: `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-templates/`

---

## Phase 1: Setup — Maven 模块基础设施

**Purpose**: 将 templates 目录升级为正式 Maven 子模块，集成到父 POM 和 starter 聚合中

- [x] T001 Create Maven module pom.xml with packaging jar (resources only) at `metaforge-agent-cognition-templates/pom.xml`. Parent: `com.metaforge:metaforge-agent-cognition`, artifactId: `metaforge-agent-cognition-templates`, no Java compile dependencies. Include `<name>` and `<description>` per plan.md.
- [x] T002 Register templates module in parent aggregator: add `<module>metaforge-agent-cognition-templates</module>` to `<modules>` in `metaforge-parent/metaforge-agent-cognition/pom.xml`
- [x] T003 Add templates dependency to starter aggregator: add `<dependency>` for `com.metaforge:metaforge-agent-cognition-templates` in `metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-starter/pom.xml`

**Checkpoint**: 模块可被 Maven 构建，参与 `mvn clean package -pl metaforge-parent/metaforge-agent-cognition -am` 统一构建

---

## Phase 2: User Story 2 — 模板文件被引擎自动发现注册 (Priority: P1) 🎯 MVP

**Goal**: 6 个内置认知模板 YAML 文件部署到 classpath，引擎核心 TemplateScanner 启动时自动扫描、校验、注册

**Independent Test**: 构建模块后将 jar 加入 classpath，启动引擎核心，通过日志确认 6 个模板全部注册成功，无校验错误

### Implementation for User Story 2

- [x] T004 [P] [US2] Create DISCOVER template: `src/main/resources/cognition/templates/discover-template.yml` — 4 operators (ontological.bundle-discovery, ontological.package-explorer, ontological.entity-schema-inventory, ontological.relation-schema-inventory), inputSchema: operators/parent_fqn/cursor/page_size, scopeRequired: false, producesUpdatedScope: true, scopeFields: [bundles, packages, entity_schemas], outputSchema type: DISCOVER_RESULT. Reference research.md Section 1 for exact field values.
- [x] T005 [P] [US2] Create ORIENT template: `src/main/resources/cognition/templates/orient-template.yml` — 2 operators (ontological.domain-drilldown, structural.domain-locator), inputSchema: parent_fqn/level/cursor/page_size, scopeRequired: false, producesUpdatedScope: true, scopeFields: [bundles, domain_groups, domains, entity_schemas], outputSchema type: ORIENT_RESULT. Reference research.md Section 1 for exact field values.
- [x] T006 [P] [US2] Create BRIEF template: `src/main/resources/cognition/templates/brief-template.yml` — 8 operators (ontological.entity-profile, procedural.flow-blueprint, procedural.adjacent-step, deontic.rule-listing, deontic.level-classifier, capability.tool-discovery, capability.call-method, relational.direct-link), inputSchema: entity_fqn/cursor/page_size, scopeRequired: false, producesUpdatedScope: false, scopeFields: [bundles, domains], contextMeta.includeSkippedEntities: true, outputSchema type: BRIEF_RESULT. Reference research.md Section 1 for exact field values.
- [x] T007 [P] [US2] Create GUIDE template: `src/main/resources/cognition/templates/guide-template.yml` — 10 operators (ontological.entity-profile, deontic.rule-listing, deontic.level-classifier, deontic.condition-action, capability.tool-discovery, capability.call-method, capability.protocol-detail, procedural.adjacent-step, procedural.decision-branch, relational.direct-link), inputSchema: entity_fqn, scopeRequired: false, producesUpdatedScope: false, scopeFields: [entity_schemas], outputSchema type: GUIDE_RESULT. Reference research.md Section 1 for exact field values.
- [x] T008 [P] [US2] Create FORECAST template: `src/main/resources/cognition/templates/forecast-template.yml` — 3 operators (relational.neighborhood, relational.impact-trace, deontic.rule-listing), inputSchema: entity_fqn/direction/max_depth, scopeRequired: false, producesUpdatedScope: false, scopeFields: [bundles], outputSchema type: FORECAST_RESULT. Reference research.md Section 1 for exact field values.
- [x] T009 [P] [US2] Create DELEGATE template: `src/main/resources/cognition/templates/delegate-template.yml` — 5 operators (governance.scope-narrowing, ontological.entity-profile, deontic.rule-listing, capability.tool-discovery, procedural.flow-blueprint), inputSchema: entity_fqn/task_fqn, scopeRequired: true, scopeBehavior.acceptsScopeAutoFix per FR-007, producesUpdatedScope: true, scopeFields: [bundles, packages, domains, entity_schemas], contextMeta.includeSkippedEntities: true, outputSchema type: DELEGATE_RESULT. Reference research.md Section 1 for exact field values.

**Checkpoint**: 6 个模板 YAML 文件就绪，classpath 扫描路径完整，引擎核心可发现并注册

---

## Phase 3: User Story 1 — Agent 通过模板消费结构化语义认知 (Priority: P1) 🎯 MVP

**Goal**: 验证已注册模板可被 Agent 消费调用，返回结构化语义上下文

**Independent Test**: 启动引擎核心后，调用 `POST /api/v1/cognition/DISCOVER { params: { parent_fqn: null } }`，返回 DISCOVER_RESULT 格式的结构化结果

### Implementation for User Story 1

- [x] T010 [US1] Rebuild cognition modules: `mvn clean package -pl metaforge-parent/metaforge-agent-cognition -am` to package templates jar and verify no build errors
- [x] T011 [US1] Validate template consumption: build `metaforge-boot` with templates on classpath, start application, execute quickstart.md Scenario 1 (all 6 registered) and Scenario 2 (DISCOVER returns Bundle list). Verify contextMeta.template field matches templateId.

**Checkpoint**: US1 和 US2 均验证通过 — 模板注册 + Agent 消费闭环完成 (MVP!)

---

## Phase 4: User Story 3 — 开发者新增自定义模板无需修改引擎代码 (Priority: P2)

**Goal**: 验证模板扩展仅需新增 YAML 文件，不触碰其他模块

**Independent Test**: 新建一个示例模板 YAML 文件（如 `audit-template.yml`），重启后验证新模板被注册，其余 6 个模板不受影响

### Implementation for User Story 3

- [x] T012 [P] [US3] Create extensibility example: `src/main/resources/cognition/templates/audit-template.yml` — minimal valid template with unique templateId `AUDIT`, 1 operator (deontic.rule-listing), archetypes: [audit]. Verify: (a) audit-template is registered alongside existing 6 templates, (b) no other module files were modified

**Checkpoint**: 模板扩展仅需新增 YAML 文件，零模块变更（符合 BC 宪法 V）

---

## Phase 5: User Story 4 — 模板校验失败不影响已注册模板 (Priority: P2)

**Goal**: 验证非法模板被拒绝注册时，不影响已注册模板的正常服务

**Independent Test**: 构造非法模板（operators 为空数组），验证其余模板正常工作，非法模板仅产生告警

### Implementation for User Story 4

- [x] T013 [P] [US4] Create invalid template for isolation test: `src/main/resources/cognition/templates/invalid-template.yml` — templateId `INVALID`, operators: [], all other fields filled. Verify: (a) INVALID template is rejected with warning log, (b) all 6 built-in templates remain registered and routable, (c) REGISTERED template count unaffected

**Checkpoint**: 模板校验失败隔离机制验证通过（符合 BC 宪法 III）

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 文档同步与最终验证

- [x] T014 [P] Validate quickstart.md all scenarios: execute Scenario 1 (registration), Scenario 2 (DISCOVER), Scenario 3 (DELEGATE scope error), Scenario 6 (isolation), Scenario 7 (archetype filtering). Remove test templates (audit-template.yml, invalid-template.yml) after validation
- [x] T015 [P] Verify all template YAML files pass manual review against contracts/template-contract.md — check: all required fields present, operatorId values in operator-catalog-contract, archetypes in valid set, scopeBehavior field consistency (FR-007 check)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **User Story 2 (Phase 2)**: Depends on Phase 1 (module infrastructure ready) — BLOCKS US1, US3, US4
- **User Story 1 (Phase 3)**: Depends on Phase 2 (templates must exist and be registered before consumption)
- **User Story 3 (Phase 4)**: Depends on Phase 2 (needs existing templates as baseline)
- **User Story 4 (Phase 5)**: Depends on Phase 2 (needs existing templates as baseline) — can run parallel with US3
- **Polish (Phase 6)**: Depends on all previous phases

### User Story Dependencies

```
Phase 1 (Setup) ──► Phase 2 (US2: Templates) ──┬──► Phase 3 (US1: Consumption)
                                                │
                                                ├──► Phase 4 (US3: Extensibility)
                                                │
                                                └──► Phase 5 (US4: Isolation)
                                                         │
                                                    Phase 6 (Polish)
```

- **US2** is the foundational story — all other stories verify behavior on the registered templates
- **US1** depends on US2 (templates must be registered before consumption)
- **US3** and **US4** depend on US2 (need baseline templates), but can run in parallel with each other
- **US3** and **US4** can run in parallel with US1 (verify different aspects independently)

### Within Each Phase

- Phase 1: T001 → T002 → T003 (sequential: pom.xml must exist before parent/starter reference it)
- Phase 2: T004–T009 all [P] — 6 template files are independent, can be created in parallel
- Phase 3: T010 → T011 (build before validate)
- Phase 4: T012 independent (no predecessors other than Phase 2)
- Phase 5: T013 independent (no predecessors other than Phase 2)
- Phase 6: T014 → T015 (validate before review cleanup)

### Parallel Opportunities

```bash
# Phase 2: Create all 6 template YAML files in parallel
Task: "Create DISCOVER template in src/main/resources/cognition/templates/discover-template.yml"
Task: "Create ORIENT template in src/main/resources/cognition/templates/orient-template.yml"
Task: "Create BRIEF template in src/main/resources/cognition/templates/brief-template.yml"
Task: "Create GUIDE template in src/main/resources/cognition/templates/guide-template.yml"
Task: "Create FORECAST template in src/main/resources/cognition/templates/forecast-template.yml"
Task: "Create DELEGATE template in src/main/resources/cognition/templates/delegate-template.yml"

# Phase 4 & 5: Run US3 and US4 validation in parallel
Task: "Create audit-template.yml for extensibility validation"
Task: "Create invalid-template.yml for isolation validation"
```

---

## Implementation Strategy

### MVP First (US2 + US1)

1. Complete Phase 1: Setup — Maven module infrastructure
2. Complete Phase 2: US2 — 6 template YAML files
3. Complete Phase 3: US1 — Validate consumption
4. **STOP and VALIDATE**: Templates registered + consumable = MVP delivered
5. Build: `mvn clean package -pl metaforge-parent/metaforge-agent-cognition -am` should produce `metaforge-agent-cognition-templates.jar`

### Incremental Delivery

1. Setup + US2 + US1 → MVP: templates registered and consumable
2. Add US3 → Validate extensibility: new templates added without code changes
3. Add US4 → Validate isolation: invalid templates don't break valid ones
4. Polish → Final validation and cleanup

### Single Developer Strategy

Phase 1 → Phase 2 (create 6 templates sequentially or in batches) → Phase 3 → Phase 4 → Phase 5 → Phase 6

---

## Notes

- All template YAML operatorId values MUST exactly match operator-catalog-contract (24 operators, 8 categories)
- Archetype values use lowercase: `execution`, `exploration`, `audit`, `orchestration`
- Template files MUST be UTF-8 encoded, extension `.yml` only
- No Java code, no test code — pure YAML configuration module
- BC constitution compliance: Principles I-VIII all PASS per plan.md constitution check
- Templates are "data" not "code" — engine core scans and consumes them via classpath
