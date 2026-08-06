# Tasks: 元模型治理核心能力 MVP

**Input**: Design documents from `/specs/001-metamodel-mvp/`

**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Not explicitly requested in spec; excluded from task list.

**Organization**: Tasks grouped by user story for independent implementation and testing.

**Base Path**: `$BC_PATH` = `metaforge-parent/metaforge-metamodel/`

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story label (US1–US6)
- File paths relative to `$BC_PATH`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Maven module structure, foundation-core registration, build integration

- [X] T001 Create BC aggregator POM `pom.xml` under metaforge-parent/metaforge-metamodel/ inheriting from metaforge-parent, declaring sub-modules `metaforge-metamodel-api` and `metaforge-metamodel-core`
- [X] T002 [P] Create `metaforge-metamodel-api/pom.xml` with dependency on `metaforge-framework` (compile scope)
- [X] T003 [P] Create `metaforge-metamodel-core/pom.xml` with dependency on `metaforge-metamodel-api` and `metaforge-framework`
- [X] T004 Register BC module in `metaforge-parent/pom.xml` `<modules>` block and in `metaforge-boot/pom.xml` `<dependencies>` block

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Value objects, constants, FQN generator, exception hierarchy, DB DDL, all entities depend on these

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### API 模块常量与枚举

- [X] T005 [P] Define all error codes in `metaforge-metamodel-api/src/main/java/com/metaforge/metamodel/api/constants/ErrorCodes.java` (30101–30112)
- [X] T006 [P] Define public enums in `metaforge-metamodel-api/src/main/java/com/metaforge/metamodel/api/enums/` — `AssociationType`, `Cardinality`, `UpgradeLevel`, `ElementType`, `EntityType`, `VersionStatus`

### 值对象

- [X] T007 [P] Implement `Fqn.java` value object in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/model/valueobject/Fqn.java`
- [X] T008 [P] Implement `BundleCode.java` value object in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/model/valueobject/BundleCode.java` with regex `[a-z][a-z0-9_-]{2,63}` and `:` / `.` exclusion
- [X] T008a [P] Implement `SemanticVersion.java` value object in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/model/valueobject/SemanticVersion.java` — SemVer 2.0 parse, compare, bump(MAJOR/MINOR/PATCH)
- [X] T008b [P] Implement `NativeAttribute.java` value object in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/model/valueobject/NativeAttribute.java` — name, type(5 types), required, default, description
- [X] T008c [P] Implement `AttributeDefinition.java` value object in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/model/valueobject/AttributeDefinition.java` — name, type, constraints(enum/pattern/range/etc.)
- [X] T008d [P] Implement `ValidationResult.java` value object in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/model/valueobject/ValidationResult.java` — pass/fail, errors list with elementFqn + fieldName + message

### FQN 生成器

- [X] T009 Implement `FqnGenerator.java` interface in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/service/FqnGenerator.java` — methods: `bundle()`, `bundleVersion()`, `package_()`, `entitySchema()`, `relationSchema()`, `attributeTemplate()`, `parse()`, `toParentFqn()`, `toShortName()`, `toBundleCode()`, `toVersion()`, `toFilePath()`, `stripTypePrefix()`, `detectType()`
- [X] T010 Implement `FqnGeneratorImpl.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/service/FqnGeneratorImpl.java` — pure string transform, stateless `@Service`, no DB dependency, best-effort parse, no validation
- [X] T011 [P] Implement `FqnParts.java` record in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/model/valueobject/FqnParts.java`

### 异常层级

- [X] T012 [P] Implement `BaseMetamodelException.java` abstract class in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/exception/BaseMetamodelException.java` extending `BizException` with error code from `ErrorCodes`
- [X] T013 [P] Implement concrete exception classes in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/exception/` — `FqnDuplicateException`, `FqnNotFoundException`, `VersionNotDraftException`, `UpgradeLevelMismatchException`, `CircularDependencyException`, `AttributeNameConflictException`, `PackageDepthExceededException`, `ExportValidationException`, `PublishedImmutableException`, `PredefinedBundleProtectedException`, `DependencyTargetNotFoundException`, `ImportParseException`

### SPI 扩展

- [X] T014 [P] Implement `MetamodelExceptionHandler.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/spi/MetamodelExceptionHandler.java` — implements `ExceptionHandlerSpi`, maps `BaseMetamodelException` subclasses to `ApiResponse.error()`
- [X] T015 [P] Implement `MetamodelHealthCheck.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/spi/MetamodelHealthCheck.java` — implements `HealthCheckSpi`

### 数据库 DDL

- [X] T016 Implement Flyway migration `V1__metamodel_governance_ddl.sql` in `metaforge-boot/src/main/resources/db/migration/` — create `metamodel_governance` schema and all tables: `bundle`, `bundle_version`, `package`, `entity_schema`, `relation_schema`, `attribute_template`, `bundle_dependency`, `export_manifest`; all FK columns use `VARCHAR(512)` FQN references, not BIGINT IDs

### 基础设施配置

- [X] T017 [P] Implement `MetamodelProperties.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/config/MetamodelProperties.java` — BC-level `@ConfigurationProperties`
- [X] T018 [P] Add BC i18n resource files in `metaforge-boot/src/main/resources/i18n/messages_metamodel_zh_CN.properties` and `messages_metamodel_en_US.properties`

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 2 — Bundle 版本生命周期与依赖治理 (Priority: P1)

**Goal**: 创建 Bundle、管理版本两态生命周期（草稿→已发布）、声明升级等级并校验匹配、声明跨 Bundle 精确版本依赖并检测循环依赖

**Independent Test**: 创建 Bundle，从 v0.0.1 发布后新建 v0.1.0 草稿声明 MINOR 升级，发布通过。声明对其他已发布 Bundle 的依赖，循环依赖检测正确拦截。

### JPA 持久化层

- [X] T019 [P] [US2] Implement `BundleJpo.java` JPA entity in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/jpa/BundleJpo.java` — `@Table(schema = "metamodel_governance", name = "bundle")`, fields: id, fqn, name, description, owner, isSystem, embedding, createdTime, updatedTime
- [X] T020 [P] [US2] Implement `BundleVersionJpo.java` JPA entity in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/jpa/BundleVersionJpo.java` — fields: id, fqn, bundleFqn, status, sourceVersionFqn, upgradeLevel, createdTime, updatedTime
- [X] T021 [P] [US2] Implement `BundleJpaRepository.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/jpa/BundleJpaRepository.java` — `findByFqn()`, `existsByFqn()`
- [X] T022 [P] [US2] Implement `BundleVersionJpaRepository.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/jpa/BundleVersionJpaRepository.java` — `findByFqn()`, `findByBundleFqnAndStatus()`, `findTopByBundleFqnAndStatusOrderByCreatedTimeDesc()`, `existsByBundleFqnAndStatus()`

### 领域层

- [X] T023 [US2] Implement `Bundle.java` aggregate root in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/model/aggregate/Bundle.java` — create Bundle, validate FQN via BundleCode, validate description completeness
- [X] T024 [US2] Implement `BundleVersion.java` aggregate root in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/model/aggregate/BundleVersion.java` — create draft, publish, status transition DRAFT→PUBLISHED (irreversible), copy from source version (atomic clone)
- [X] T025 [US2] Implement `BundleRepository.java` port interface in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/repository/BundleRepository.java`
- [X] T026 [US2] Implement `BundleVersionRepository.java` port interface in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/repository/BundleVersionRepository.java`

### 基础设施适配层

- [X] T027 [US2] Implement `BundleRepositoryAdapter.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/adapter/BundleRepositoryAdapter.java` — Spring Data JPA implementation
- [X] T028 [US2] Implement `BundleVersionRepositoryAdapter.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/adapter/BundleVersionRepositoryAdapter.java`
- [X] T029 [P] [US2] Implement `BundleMapper.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/mapper/BundleMapper.java`
- [X] T030 [P] [US2] Implement `BundleVersionMapper.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/mapper/BundleVersionMapper.java`

### 应用层

- [X] T031 [US2] Implement `BundleManagementServiceImpl.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/application/service/BundleManagementServiceImpl.java` — create Bundle, get by FQN, list paged (via `ElementQueryRequest` with fqnPrefixes)
- [X] T032 [US2] Implement `BundleVersionManagementServiceImpl.java` — create draft from latest published, publish (atomic), get by FQN, list versions. **Guard: only one DRAFT per Bundle** (FR-023). **Derive `enabled` from BundleVersion.status: DRAFT→false, PUBLISHED→true** (FR-018/FR-019)

### API 模块（契约层）

- [X] T033 [P] [US2] Define `BundleManagementService.java` interface in `metaforge-metamodel-api/src/main/java/com/metaforge/metamodel/api/service/BundleManagementService.java`
- [X] T034 [P] [US2] Define `BundleVersionManagementService.java` interface in `metaforge-metamodel-api/src/main/java/com/metaforge/metamodel/api/service/BundleVersionManagementService.java`
- [X] T035 [P] [US2] Define DTOs in `metaforge-metamodel-api/src/main/java/com/metaforge/metamodel/api/dto/request/` — `CreateBundleRequest`, `CreateDraftRequest`
- [X] T036 [P] [US2] Define DTOs in `metaforge-metamodel-api/src/main/java/com/metaforge/metamodel/api/dto/response/` — `BundleDto`, `BundleVersionDto`

### REST 入口

- [X] T037 [US2] Implement `BundleController.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/interfaces/rest/BundleController.java` — `POST /bundles`, `GET /bundles`, `GET /bundles/{fqn}`, `POST /bundles/{fqn}/versions`, `POST /versions/{fqn}/publish`

### 领域服务 — 升级等级校验与循环依赖检测

- [X] T038 [US2] Implement `UpgradeLevelValidator.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/service/UpgradeLevelValidator.java` — `ChangeReport` diff between source and draft, PATCH/MINOR/MAJOR mismatch detection
- [X] T039 [US2] Implement `CircularDependencyDetector.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/service/CircularDependencyDetector.java` — Kahn topological sort + DFS cycle detection, report full cycle path

### 跨 Bundle 依赖

- [X] T040 [P] [US2] Implement `BundleDependencyJpo.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/jpa/BundleDependencyJpo.java` — fields: id, sourceVersionFqn, targetVersionFqn, createdTime
- [X] T041 [P] [US2] Implement `BundleDependencyJpaRepository.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/jpa/BundleDependencyJpaRepository.java`
- [X] T042 [US2] Implement `BundleDependencyRepository.java` port interface in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/repository/BundleDependencyRepository.java`
- [X] T043 [US2] Implement `BundleDependencyRepositoryAdapter.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/adapter/BundleDependencyRepositoryAdapter.java`
- [X] T044 [US2] Implement `BundleDependencyService.java` domain service — declare dependency (exact version, target must exist and be published), validate before publish. **Validate transitive dependency constraints: scope not amplified, version not auto-upgraded** (FR-026)

**Checkpoint**: Bundle 创建、版本管理、依赖声明可独立验证

---

## Phase 4: User Story 3 — Package 命名空间与分类管理 (Priority: P2)

**Goal**: 在 Bundle 草稿版本中创建多级嵌套 Package（上限 5 层），管理分类树

**Independent Test**: 创建 3 层嵌套 Package 树，在各层下创建 EntitySchema，验证 FQN 全局唯一且跨包引用正常

### JPA 持久化层

- [X] T045 [P] [US3] Implement `PackageJpo.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/jpa/PackageJpo.java` — fields: id, fqn, bundleVersionFqn, parentPackageFqn, description, depth, embedding
- [X] T046 [P] [US3] Implement `PackageJpaRepository.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/jpa/PackageJpaRepository.java` — `findByFqn()`, `findByBundleVersionFqn()`, `findByParentPackageFqn()`, `existsByFqnPrefix()`
- [X] T047 [P] [US3] Implement `PackageMapper.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/mapper/PackageMapper.java`

### 领域层

- [X] T048 [US3] Implement `Package.java` domain entity in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/model/entity/Package.java` — create, validate depth ≤ 4 (max 5 layers), validate description
- [X] T049 [US3] Implement `PackageRepository.java` port in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/repository/PackageRepository.java`
- [X] T050 [US3] Implement `PackageRepositoryAdapter.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/adapter/PackageRepositoryAdapter.java`

### 应用层

- [X] T051 [US3] Implement `PackageManagementServiceImpl.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/application/service/PackageManagementServiceImpl.java`

### API 模块

- [X] T052 [P] [US3] Define `PackageManagementService.java` interface in `metaforge-metamodel-api/src/main/java/com/metaforge/metamodel/api/service/PackageManagementService.java`
- [X] T053 [P] [US3] Define DTOs — `CreatePackageRequest`, `PackageDto`

### REST 入口

- [X] T054 [US3] Implement `PackageController.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/interfaces/rest/PackageController.java` — `POST /packages`, `GET /packages/{fqn}`, `DELETE /packages/{fqn}`, `GET /packages?bundleVersionFqn=`

**Checkpoint**: Package 命名空间树可独立验证

---

## Phase 5: User Story 1 — 核心语义元素建模 (Priority: P1)

**Goal**: EntitySchema + RelationSchema + AttributeTemplate 的 CRUD，属性平铺合并生成 JSON Schema，FQN 前缀集合查询

**Independent Test**: 创建 3 个 EntitySchema、2 个 RelationSchema、挂载 AttributeTemplate，发布后查询每个元素的平铺 JSON Schema

### JPA 持久化层

- [X] T055 [P] [US1] Implement `EntitySchemaJpo.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/jpa/EntitySchemaJpo.java` — fields: id, fqn, packageFqn, bundleVersionFqn, name, description, nativeAttributes(JSONB), mountedTemplateFqns(JSONB), jsonSchema(JSONB), embedding(JSONB)
- [X] T056 [P] [US1] Implement `RelationSchemaJpo.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/jpa/RelationSchemaJpo.java` — fields: id, fqn, packageFqn, bundleVersionFqn, name, description, sourceFqn, targetFqn, associationType, cardinalitySource, cardinalityTarget, nativeAttributes, mountedTemplateFqns, jsonSchema, embedding
- [X] T057 [P] [US1] Implement `AttributeTemplateJpo.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/jpa/AttributeTemplateJpo.java` — fields: id, fqn, bundleVersionFqn, name, description, attributeDefinitions(JSONB)
- [X] T058 [P] [US1] Implement `EntitySchemaJpaRepository.java` — `findByFqn()`, `findByFqnStartingWith()` (for fqnPrefixes), findAll with Specification for fqnPrefixes OR logic + paging
- [X] T059 [P] [US1] Implement `RelationSchemaJpaRepository.java` — same query pattern as EntitySchema
- [X] T060 [P] [US1] Implement `AttributeTemplateJpaRepository.java` — `findByFqn()`, `findByBundleVersionFqn()`
- [X] T061 [P] [US1] Implement `EntitySchemaMapper.java`, `RelationSchemaMapper.java`, `AttributeTemplateMapper.java` MapStruct interfaces in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/mapper/`

### 领域层

- [X] T062 [US1] Implement `EntitySchema.java` domain entity in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/model/entity/EntitySchema.java` — FQN segment validates no `:` / `.`; validate nativeAttributes compliant with JSON Schema Draft 2020-12 subset
- [X] T063 [US1] Implement `RelationSchema.java` domain entity in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/model/entity/RelationSchema.java` — validate sourceFqn/targetFqn existence and visibility, associationType enum
- [X] T064 [US1] Implement `AttributeTemplate.java` domain entity in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/model/entity/AttributeTemplate.java` — FQN format excludes Package path; attributeDefinitions comply with JSON Schema subset
- [X] T065 [US1] Implement repository port interfaces — `EntitySchemaRepository.java`, `RelationSchemaRepository.java`, `AttributeTemplateRepository.java`
- [X] T066 [US1] Implement repository adapters — `EntitySchemaRepositoryAdapter.java`, `RelationSchemaRepositoryAdapter.java`, `AttributeTemplateRepositoryAdapter.java`

### FQN Segment 字符校验

- [X] T067 [US1] Implement FQN segment character validation — segment 中禁止 `:` 和 `.`（保留分隔符），写入阶段校验拦截，错误码 30101

### 属性平铺合并与 JSON Schema 编译

- [X] T068 [US1] Implement `AttributeMergeService.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/service/AttributeMergeService.java` — load nativeAttributes + mountedTemplateFqns, flatten with name conflict detection, order-preserving
- [X] T069 [US1] Implement `JsonSchemaCompiler.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/service/JsonSchemaCompiler.java` — compile merged attributes to JSON Schema Draft 2020-12 flat output, validate types (string/number/integer/boolean/array only, no object nesting)

### 应用层

- [X] T070 [US1] Implement `ElementDefinitionServiceImpl.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/application/service/ElementDefinitionServiceImpl.java` — CRUD for EntitySchema, RelationSchema, AttributeTemplate; `listEntitySchemas(ElementQueryRequest)` and `listRelationSchemas(ElementQueryRequest)` for fqnPrefixes batch filtering; publish triggers attribute merge → jsonSchema write

### API 模块

- [X] T071 [P] [US1] Define `ElementDefinitionService.java` interface in `metaforge-metamodel-api/src/main/java/com/metaforge/metamodel/api/service/ElementDefinitionService.java` — includes `listEntitySchemas()`, `listRelationSchemas()`
- [X] T072 [P] [US1] Define DTOs — `CreateEntitySchemaRequest`, `UpdateEntitySchemaRequest`, `EntitySchemaDto`, `CreateRelationSchemaRequest`, `RelationSchemaDto`, `CreateAttributeTemplateRequest`, `AttributeTemplateDto`, `ElementQueryRequest` (with `List<String> fqnPrefixes`, `PageRequest`)
- [X] T073 [P] [US1] Define `NativeAttributeDto.java`, `AttributeDefinitionDto.java` in `metaforge-metamodel-api/src/main/java/com/metaforge/metamodel/api/dto/`

### REST 入口

- [X] T074 [US1] Implement `EntitySchemaController.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/interfaces/rest/EntitySchemaController.java` — `POST /entity-schemas`, `GET /entity-schemas/{fqn}`, `PUT /entity-schemas/{fqn}`, `DELETE /entity-schemas/{fqn}`, `GET /entity-schemas?fqnPrefix=&page=&size=`
- [X] T075 [US1] Implement `RelationSchemaController.java` — `POST /relation-schemas`, `GET /relation-schemas/{fqn}`, `PUT /relation-schemas/{fqn}`, `DELETE /relation-schemas/{fqn}`, `GET /relation-schemas?fqnPrefix=&page=&size=`
- [X] T076 [US1] Implement `AttributeTemplateController.java` — `POST /attribute-templates`, `GET /attribute-templates/{fqn}`, `PUT /attribute-templates/{fqn}`, `DELETE /attribute-templates/{fqn}`

**Checkpoint**: EntitySchema + RelationSchema + AttributeTemplate 完整 CRUD + 发布平铺合并可独立验证

---

## Phase 6: User Story 4 — 导出清单与跨 Bundle 可见性管控 (Priority: P2)

**Goal**: 配置导出清单（Package FQN 列表），发布时校验 RelationSchema 端点可达性，下游引用时校验导出范围

**Independent Test**: 发布 Bundle 时导出 `pkg_order` 和 `pkg_common`，下游仅能引用这两个包内元素

- [X] T077 [P] [US4] Implement `ExportManifestJpo.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/persistence/jpa/ExportManifestJpo.java` — fields: id, bundleVersionFqn, exportedPackageFqns(JSONB)
- [X] T078 [P] [US4] Implement `ExportManifestJpaRepository.java`
- [X] T079 [US4] Implement `ExportManifestRepository.java` port and `ExportManifestRepositoryAdapter.java`
- [X] T080 [US4] Implement `ExportValidationService.java` domain service in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/service/ExportValidationService.java` — validate RelationSchema endpoints in exported packages, validate exported packages exist in BundleVersion
- [X] T081 [US4] Implement `ExportManifestServiceImpl.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/application/service/ExportManifestServiceImpl.java`
- [X] T082 [P] [US4] Define `ExportManifestService.java` interface + DTOs in api module
- [X] T083 [US4] Implement `ExportManifestController.java` — `PUT /versions/{fqn}/export-manifest`, `GET /versions/{fqn}/export-manifest`

**Checkpoint**: 导出清单配置与校验可独立验证

---

## Phase 7: User Story 5 — 声明式批量导入导出 (Priority: P3)

**Goal**: YAML/JSON 格式的 Bundle/Package 级元模型导入导出，导入按依赖顺序解析，幂等性支持

- [X] T084 [US5] Implement `ExportService.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/service/ExportService.java` — Bundle full export and Package-level export to YAML/JSON, include all dependencies and attribute templates
- [X] T085 [US5] Implement `ImportService.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/domain/service/ImportService.java` — parse in order: Bundle → Package → AttributeTemplate → EntitySchema → RelationSchema; skip/error strategies; reject auto-publish; reject overwrite published version
- [X] T086 [US5] Implement `ImportExportServiceImpl.java` application service
- [X] T087 [P] [US5] Define `ImportExportService.java` interface + DTOs (`ImportResultDto`, `ExportRequest`) in api module
- [X] T088 [US5] Implement `ImportExportController.java` — `GET /export/bundle/{fqn}`, `POST /import`

**Checkpoint**: 导出文件可直接重新导入、结构一致

---

## Phase 8: User Story 6 — 预置系统 Bundle (Priority: P3)

**Goal**: 系统初始化时自动预置 `metaforge` Bundle v1.0.0，包含 `agent` 和 `common` 包

- [X] T089 [US6] Implement `PredefinedBundleInitializer.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/infrastructure/config/PredefinedBundleInitializer.java` — `ApplicationRunner`, check if `metaforge` Bundle exists, create if not
- [X] T090 [US6] Implement Flyway migration `V2__metamodel_governance_init.sql` with `metaforge` Bundle v1.0.0 data (Bundle + BundleVersion + Package `agent` + Package `common` + core EntitySchemas, RelationSchemas, AttributeTemplates)
- [X] T091 [US6] Implement protection logic — reject DELETE on `isSystem=true` Bundle, reject UPDATE on published `metaforge` core elements

**Checkpoint**: 系统启动后 `metaforge:v1.0.0` 预置元素可查询，删除被拒绝

---

## Phase 9: Polish — 校验服务与 MCP 工具

**Purpose**: Cross-cutting validation, MCP tool exposure, endpoint completion

- [X] T092 [P] Implement `ValidationService.java` domain service — `validateSave()` (FR-049 lightweight: FQN uniqueness, reference integrity, cycle detect, package depth, name conflict, segment char validation, naming compliance), `validatePublish()` (FR-050 full global: cross-Bundle reachability, export consistency, association endpoint validity, dependency self-consistency, upgrade level match, full name conflict, JSON Schema compliance), preview mode
- [X] T093 Implement `ValidationController.java` — `POST /versions/{fqn}/validate/save`, `POST /versions/{fqn}/validate/publish`, `POST /versions/{fqn}/validate/preview`
- [X] T094 [P] Implement `MetamodelMcpTools.java` in `metaforge-metamodel-core/src/main/java/com/metaforge/metamodel/interfaces/mcp/MetamodelMcpTools.java` — `@Tool` methods: `getElementSchema()`, `queryElements()` (with `List<String> fqnPrefixes`), `getRelationSchema()`, `listBundleVersions()`, `getExportManifest()`, `resolveFqn()`, `getAttributeTemplate()`, `validateVersion()`
- [X] T095 Implement validation completeness check — run quickstart.md scenarios end-to-end

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on Phase 1 — **BLOCKS all user stories**
- **User Story 2 (Phase 3)**: Depends on Phase 2 — Bundle/Version is container for all elements
- **User Story 3 (Phase 4)**: Depends on Phase 2 — Package depends on BundleVersion
- **User Story 1 (Phase 5)**: Depends on Phase 2+3+4 — EntitySchema/RelationSchema need BundleVersion + Package
- **User Story 4 (Phase 6)**: Depends on Phase 3+4 — ExportManifest needs BundleVersion + Package
- **User Story 5 (Phase 7)**: Depends on Phase 5+6 — Import/Export needs all entity types + export manifest
- **User Story 6 (Phase 8)**: Depends on Phase 5 — Preset Bundle populates EntitySchema/RelationSchema/AttributeTemplate
- **Polish (Phase 9)**: Depends on all stories

### Cross-Story Dependencies

| Story | Depends On | Reasoning |
|-------|-----------|-----------|
| US1 | US2, US3 | EntitySchema/RelationSchema 需要 BundleVersion + Package 容器 |
| US2 | (none beyond Phase 2) | Bundle 是顶层容器 |
| US3 | US2 | Package 归属 BundleVersion |
| US4 | US2, US3 | ExportManifest 绑定 BundleVersion，引用 Package FQN |
| US5 | US1, US4 | Import/Export 涉及所有元素类型 + 导出清单 |
| US6 | US1 | 预置 Bundle 包含 EntitySchema/RelationSchema/AttributeTemplate 数据 |

### Parallel Opportunities

- **Phase 2**: T005-T006 (constants), T007-T011 (value objects + FQN generator), T012-T013 (exceptions), T014-T015 (SPI) all [P] — can run in parallel
- **Phase 3**: T019-T022 (JPA), T029-T030 (Mapper), T033-T036 (API DTOs) all [P]
- **Phase 5**: T055-T061 (JPA + Mapper), T071-T073 (API DTOs) all [P]
- **Phases 6, 7, 8, 9**: all tasks marked [P] within each phase

---

## Parallel Example: User Story 1

```bash
# Batch 1 — JPA entities + repositories (parallel):
Task: "T055 Implement EntitySchemaJpo"
Task: "T056 Implement RelationSchemaJpo"
Task: "T057 Implement AttributeTemplateJpo"
Task: "T058 Implement EntitySchemaJpaRepository"
Task: "T059 Implement RelationSchemaJpaRepository"
Task: "T060 Implement AttributeTemplateJpaRepository"
Task: "T061 Implement Mappers"

# Batch 2 — Domain entities (parallel):
Task: "T062 Implement EntitySchema"
Task: "T063 Implement RelationSchema"
Task: "T064 Implement AttributeTemplate"

# Batch 3 — Services + Adapters:
Task: "T065-T066 Implement Repositories + Adapters"
Task: "T068 Implement AttributeMergeService"
Task: "T069 Implement JsonSchemaCompiler"

# Batch 4 — Application + REST:
Task: "T070 Implement ElementDefinitionServiceImpl"
Task: "T074 Implement EntitySchemaController"
Task: "T075 Implement RelationSchemaController"
Task: "T076 Implement AttributeTemplateController"
```

---

## Implementation Strategy

### MVP First (US2 + US3 + US1)

1. Phase 1: Setup
2. Phase 2: Foundational (CRITICAL)
3. Phase 3: US2 — Bundle & Version lifecycle → **test**
4. Phase 4: US3 — Package management → **test**
5. Phase 5: US1 — EntitySchema/RelationSchema/AttributeTemplate + Attribute merge + JSON Schema → **test**
6. **STOP**: MVP core delivered — 完整元模型定义链路可执行

### Incremental Delivery

1. Setup + Foundational → 基础设施就绪
2. US2 → Bundle 版本管理 → Demo
3. US3 → Package 命名空间 → Demo
4. US1 → 核心语义元素建模 + JSON Schema 生成 → **MVP Demo**
5. US4 → 导出清单管控 → Demo
6. US5 → 导入导出 → Demo
7. US6 → 预置 Bundle → Demo
8. Phase 9 → 校验 + MCP 工具 → Final

---

## Notes

- All FK columns use `VARCHAR(512)` FQN references, not BIGINT IDs
- All FQN generation/parsing goes through `FqnGenerator`, no manual string concatenation
- All error codes reference `ErrorCodes` constants, no hardcoded numbers
- api module contains only DTOs, interfaces, enums, constants — no business logic
- FQN segments (`:`, `.`) are reserved delimiters — write-stage validation intercepts illegal chars
- MVP scale: ≤5 Bundle, ≤10 Package/Bundle, ≤20 elements/Package
