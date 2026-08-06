# Tasks: 元数据全生命周期管理

**Input**: Design documents from `/specs/001-metadata-full-lifecycle/`

**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: 本功能规格要求中未显式要求 TDD，测试任务按 foundation 基础设施标准纳入。

**Organization**: 按用户故事（User Story）分组，支持独立实现和独立测试。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 所属用户故事（US1, US2, US3, US4, US5, US6）

## Path Conventions

- BC 根路径: `metaforge-parent/metaforge-metadata/`
- API 模块: `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/`
- Core 模块: `metaforge-metadata-core/src/main/java/com/metaforge/metadata/`
- 测试: `metaforge-metadata-core/src/test/java/com/metaforge/metadata/`

---

## Phase 1: Setup（项目初始化与模块注册）

**Purpose**: 创建 Maven 模块结构，注册到父工程，建立基础骨架

- [X] T001 创建 `metaforge-metadata` 聚合父 POM（packaging=pom），继承 `metaforge-parent`，声明子模块 `metaforge-metadata-api` / `metaforge-metadata-core` 及 `dependencyManagement`
- [X] T002 [P] 创建 `metaforge-metadata-api` 子模块 POM，依赖 `metaforge-framework`，创建基础包路径 `com.metaforge.metadata.api`
- [X] T003 [P] 创建 `metaforge-metadata-core` 子模块 POM，依赖 `metaforge-metadata-api` + `metaforge-framework` + `metaforge-metamodel-api`，引入 `jackson-dataformat-yaml`、`com.networknt:json-schema-validator`、MapStruct + Processor，测试依赖 `metaforge-framework` (test-jar) + `spring-boot-starter-test`
- [X] T004 在 `metaforge-parent/pom.xml` 的 `<modules>` 中注册 `<module>metaforge-metadata</module>`
- [X] T005 在 `metaforge-boot/pom.xml` 中添加 `metaforge-metadata-core` 依赖（runtime scope）

---

## Phase 2: Foundational（阻塞性前置——所有用户故事的前置依赖）

**Purpose**: API 模块契约层 + Core 模块领域骨架 + 数据库 + 基础设施适配，所有用户故事依赖此阶段完成

**⚠️ CRITICAL**: 用户故事实现不可在 Phase 2 完成前开始

### API 模块：常量、枚举、DTO、服务接口

- [X] T006 [P] 创建错误码常量类 `MetadataErrorCodes` 在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/constants/MetadataErrorCodes.java`（31000-31099 范围，12 个错误码）
- [X] T007 [P] 创建状态常量类 `MetadataStatusConstants` 在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/constants/MetadataStatusConstants.java`
- [X] T008 [P] 创建枚举 `MetadataStatus`（DRAFT/ACTIVE/DEPRECATED）在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/enums/MetadataStatus.java`
- [X] T009 [P] 创建枚举 `ImportStrategy`（SKIP/ERROR）在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/enums/ImportStrategy.java`
- [X] T010 [P] 创建枚举 `ChangeType`（ACTIVATE/DEPRECATE）在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/enums/ChangeType.java`
- [X] T011 [P] 创建枚举 `DiffType`（ADDED/MODIFIED/DELETED）、`MatchMode`（EXACT/PREFIX）、`ImportFormat`、`ExportFormat` 在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/enums/`
- [X] T012 [P] 创建请求 DTO 包：`CreateDraftRequest`、`UpdateDraftContentRequest`、`MetadataQueryRequest`、`AttributeCondition`、`AdminQueryRequest`、`DiffRequest`、`ImportRequest`、`ExportRequest` 在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/dto/request/`
- [X] T013 [P] 创建响应 DTO 包：`MetadataEntityDto`、`MetadataEntityDraftDto`、`EntityVersionDto`、`VersionDiffDto`、`FieldDiff`、`ImportResultDto`、`ImportItemResult`、`ExportResultDto`、`ValidationErrorDetailDto`、`DeactivationCheckResult` 在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/dto/response/`
- [X] T014 [P] 创建变更事件类 `MetadataChangeEvent`（继承 `ApplicationEvent`）在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/event/MetadataChangeEvent.java`
- [X] T015 [P] 创建事件监听器接口 `MetadataChangeListener` 在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/event/MetadataChangeListener.java`

### API 模块：Application Service 接口（@OpenHostService）

- [X] T016 [P] 创建 `MetadataDraftService` 接口（含完整 Javadoc）在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/service/MetadataDraftService.java`
- [X] T017 [P] 创建 `MetadataActivationService` 接口（含完整 Javadoc）在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/service/MetadataActivationService.java`
- [X] T018 [P] 创建 `MetadataQueryService` 接口（含完整 Javadoc）在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/service/MetadataQueryService.java`
- [X] T019 [P] 创建 `MetadataHistoryService` 接口（含完整 Javadoc）在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/service/MetadataHistoryService.java`
- [X] T020 [P] 创建 `MetadataImportExportService` 接口（含完整 Javadoc）在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/service/MetadataImportExportService.java`
- [X] T021 [P] 创建 `MetadataEventService` 接口（含完整 Javadoc）在 `metaforge-metadata-api/src/main/java/com/metaforge/metadata/api/service/MetadataEventService.java`

### Core 模块：领域层骨架

- [X] T022 [P] 创建值对象 `FQN` 在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/model/valueobject/FQN.java`
- [X] T023 [P] 创建值对象 `EntitySchemaFQN` 在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/model/valueobject/EntitySchemaFQN.java`
- [X] T024 [P] 创建值对象 `VersionNumber` 在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/model/valueobject/VersionNumber.java`
- [X] T025 [P] 创建值对象 `JsonSchemaSnapshot` 在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/model/valueobject/JsonSchemaSnapshot.java`
- [X] T026 [P] 创建聚合根 `MetadataEntity`（含 `updateContent()` / `incrementVersion()` 领域行为）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/model/aggregate/MetadataEntity.java`
- [X] T027 [P] 创建聚合根 `MetadataEntityDraft`（含 `updateContent()` 领域行为）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/model/aggregate/MetadataEntityDraft.java`
- [X] T028 [P] 创建领域实体 `EntityVersion` 在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/model/entity/EntityVersion.java`
- [X] T029 [P] 创建领域异常类：`MetadataValidationException`、`FqnConflictException`、`EntityNotFoundException`、`DraftNotFoundException`、`ActivationFailedException`、`DeactivationBlockedException`（均继承 `BizException`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/exception/`

### Core 模块：领域服务与 Repository 端口

- [X] T030 创建 `FqnGenerator` 领域服务（`@Component`，提供 `generateChildFqn` / `extractParentFqn` / `splitSegments` / `extractRootFqn` / `isValidSegment` / `isReservedCharInSegment`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/service/FqnGenerator.java`
- [X] T031 创建 `FqnUniquenessService` 领域服务（联合主表+草稿表查重）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/service/FqnUniquenessService.java`
- [X] T032 创建 Repository 端口接口：`MetadataEntityRepository`、`MetadataEntityDraftRepository`、`EntityVersionRepository`、`EntitySchemaRepository`（上游访问端口）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/repository/`
- [X] T033 创建 `MetadataEventPublisher` 领域事件发布端口（接口 `publish(MetadataChangeEvent)`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/service/event/MetadataEventPublisher.java`

### Core 模块：基础设施层——JPA 持久化

- [X] T034 [P] 创建 JPO `MetadataEntityJpo`（`@Entity`，`@Table(schema = "metadata_management")`，content 字段 `@JdbcTypeCode(SqlTypes.JSON)`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/infrastructure/persistence/jpa/MetadataEntityJpo.java`
- [X] T035 [P] 创建 JPO `MetadataEntityDraftJpo`（同上 pattern）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/infrastructure/persistence/jpa/MetadataEntityDraftJpo.java`
- [X] T036 [P] 创建 JPO `EntityVersionJpo`（同上 pattern，`(fqn, version)` 联合唯一索引）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/infrastructure/persistence/jpa/EntityVersionJpo.java`
- [X] T037 [P] 创建 Spring Data JPA 接口：`MetadataEntityJpaRepository`、`MetadataEntityDraftJpaRepository`、`EntityVersionJpaRepository` 在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/infrastructure/persistence/jpa/`
- [X] T038 [P] 创建 MapStruct Mapper：`MetadataEntityMapper`（JPO ↔ Domain）、`MetadataDraftMapper`（JPO ↔ Domain）、`EntityVersionMapper`（JPO ↔ Domain）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/infrastructure/mapper/`
- [X] T039 创建 Repository Adapter 实现（依赖 JpaRepository + Mapper）：`MetadataEntityRepositoryImpl`、`MetadataEntityDraftRepositoryImpl`、`EntityVersionRepositoryImpl` 在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/infrastructure/persistence/adapter/`

### Core 模块：基础设施层——配置与 SPI

- [X] T040 [P] 创建 `MetamodelGatewayAdapter`（实现 `EntitySchemaRepository` 端口，通过 `ElementDefinitionService.getEntitySchema()` 调用上游）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/infrastructure/persistence/adapter/MetamodelGatewayAdapter.java`
- [X] T041 [P] 创建 SPI 扩展 `MetadataExceptionHandler`（实现 `ExceptionHandlerSpi`，匹配本 BC 异常类型 → `ApiResponse.error()`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/infrastructure/spi/MetadataExceptionHandler.java`
- [X] T042 [P] 创建 SPI 扩展 `MetadataHealthCheck`（实现 `HealthCheckSpi`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/infrastructure/spi/MetadataHealthCheck.java`
- [X] T043 [P] 创建 `SpringMetadataEventPublisher`（实现 `MetadataEventPublisher`，注入 `ApplicationEventPublisher`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/infrastructure/event/SpringMetadataEventPublisher.java`
- [X] T044 创建配置类 `MetadataAutoConfiguration`（`@Configuration`，`@EnableJpaRepositories`，`@ComponentScan`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/infrastructure/config/MetadataAutoConfiguration.java`
- [X] T045 创建 `MetadataProperties` 配置属性类（`@ConfigurationProperties(prefix = "metaforge.metadata")`，含 `schema.validation.cache-ttl`、`import.max-batch-size`、`export.default-format`、`history.readonly`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/infrastructure/config/MetadataProperties.java`

### 数据库迁移

- [X] T046 创建 Flyway 迁移脚本 `V005__metadata_ddl.sql`（metadata_management schema + 三表 DDL + 索引 + 约束）放置于 `metaforge-boot/src/main/resources/db/migration/`

**Checkpoint**: Foundation 完成——用户故事实现现在可以并行开始

---

## Phase 3: User Story 1 - 草稿管理与实时校验 (Priority: P1) 🎯 MVP

**Goal**: 领域知识工程师基于已发布 EntitySchema 创建/编辑/删除草稿，每次保存时实时执行 JSON Schema 结构校验

**Independent Test**: 创建草稿 → 保存成功写草稿表 → 修改为违规值 → 被拦截返回结构化错误 → 删除草稿 → 确认草稿表无残留，全程主表无变更

### Implementation for User Story 1

- [X] T047 [P] [US1] 创建 `SchemaValidationService` 领域服务（注入 `ObjectMapper` + `JsonSchemaFactory`，调用上游 EntitySchema JSON Schema 执行全字段校验，失败返回 `List<ValidationErrorDetailDto>`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/service/SchemaValidationService.java`
- [X] T048 [US1] 创建 `MetadataDraftServiceImpl` Application Service（实现 `MetadataDraftService` 接口，注入 `MetadataEntityDraftRepository` + `MetadataEntityRepository` + `FqnGenerator` + `FqnUniquenessService` + `SchemaValidationService` + `MetamodelGatewayAdapter`，实现 `createDraft` / `createDraftFromActive` / `updateDraftContent` / `getDraft` / `deleteDraft` 业务逻辑）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/application/service/MetadataDraftServiceImpl.java`
- [X] T049 [US1] 创建 `MetadataDraftController` REST 接口（`@Tag(name = "metadata-management")`，端点：`POST /drafts`、`GET /drafts/{fqn}`、`PUT /drafts/{fqn}/content`、`DELETE /drafts/{fqn}`、`POST /drafts/from-active/{fqn}`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/interfaces/rest/MetadataDraftController.java`

**Checkpoint**: 草稿管理功能独立可用——草稿创建、编辑、删除、校验均可验证，主表无变更

---

## Phase 4: User Story 2 - 版本生效与生命周期管控 (Priority: P1)

**Goal**: 草稿生效为原子事务（主表写入+历史归档+草稿删除），生效版本可下线（引用校验+子实体校验），支持从历史版本重新生效

**Independent Test**: 创建草稿→生效→验证主表版本号=1、历史表归档=1条、草稿已删除→下线→验证主表无记录→重新生效→主表恢复

### Implementation for User Story 2

- [X] T050 [US2] 创建 `DraftActivationService` 领域服务（`@Transactional`，原子操作：主表写入/覆盖 + 历史表归档 `VersionNumber.increment()` + 草稿删除；任意一步失败全量回滚不产生脏数据；生效前预校验包括 JSON Schema 校验 + 父实体状态 + 元模型版本有效性）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/service/DraftActivationService.java`
- [X] T051 [US2] 创建 `EntityDeactivationService` 领域服务（下线前校验：① 外部活跃引用检查：通过 `EntitySchemaRepository` 端口注入的 adapter 调用上游 `semantic-relation-network` BC 的查询接口获取引用清单；② FQN 前缀匹配本地主表查询生效子实体列表。存在任一拦截条件则抛 `DeactivationBlockedException` 返回详细清单。原子操作：主表删除 + 发布变更事件）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/service/EntityDeactivationService.java`
- [X] T052 [US2] 创建 `MetadataActivationServiceImpl` Application Service（实现 `MetadataActivationService` 接口，注入 `DraftActivationService` + `EntityDeactivationService` + `MetadataEventPublisher`，实现 `activate` / `deactivate` / `reactivate` / `checkDeactivationPreconditions`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/application/service/MetadataActivationServiceImpl.java`
- [X] T053 [US2] 创建 `MetadataActivationController` REST 接口（端点：`POST /entities/{fqn}/activate`、`POST /entities/{fqn}/deactivate`、`POST /entities/{fqn}/reactivate`、`GET /entities/{fqn}/deactivation-check`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/interfaces/rest/MetadataActivationController.java`

**Checkpoint**: 草稿→生效→下线三态流转闭环可用，主表+历史表+草稿表三表协同验证通过

---

## Phase 5: User Story 3 - 多维度查询检索 (Priority: P2)

**Goal**: FQN 精准查询 / FQN 前缀 OR 并集查询 / 元模型类型查询 / 属性条件组合查询 / 管理员全状态聚合查询，默认仅返回主表生效版本

**Independent Test**: 准备 3 条前缀为 "SalesOrder_" 的生效元数据→FQN 前缀查询返回 3 条→精准查询返回 1 条→管理员全状态查询返回草稿+生效+下线聚合结果

### Implementation for User Story 3

- [X] T054 [US3] 在 `MetadataEntityJpaRepository` 中扩展查询方法：`findByFqn()`、`findByFqnPrefixIn()`（OR 并集）、`findByEntitySchemaFqn()`、`findByContentFieldValue()`（JSONB `@>` 操作符），`MetadataEntityDraftJpaRepository.findByFqn()`，`EntityVersionJpaRepository.findByFqn()` 在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/infrastructure/persistence/jpa/`
- [X] T055 [US3] 在 `MetadataEntityRepositoryImpl` 中实现分页查询逻辑（使用 `PageHelper.toSpringPageRequest()` / `PageHelper.fromSpringPage()`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/infrastructure/persistence/adapter/MetadataEntityRepositoryImpl.java`
- [X] T056 [US3] 创建 `MetadataQueryServiceImpl` Application Service（实现 `MetadataQueryService` 接口，注入 `MetadataEntityRepository` + `MetadataEntityDraftRepository` + `EntityVersionRepository` + `MetadataEntityMapper`，实现 `getByFqn` / `listByFqnPrefixes` / `listByEntitySchema` / `queryByAttributes` / `adminQuery`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/application/service/MetadataQueryServiceImpl.java`
- [X] T057 [US3] 创建 `MetadataQueryController` REST 接口（端点：`GET /entities/{fqn}`、`GET /entities`、`GET /entities/query/fqn-prefix`、`GET /entities/query/entity-schema`、`GET /admin/metadata`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/interfaces/rest/MetadataQueryController.java`

**Checkpoint**: 四种查询模式可用，默认返回生效版本，管理员全状态查询可用

---

## Phase 6: User Story 4 - 历史版本追溯与差异对比 (Priority: P2)

**Goal**: 按 FQN 查全版本列表（倒序）、按版本号查详情、任意两版本字段级差异对比

**Independent Test**: 同一 FQN 执行 3 次草稿→生效 → 查全版本列表返回 3 条（倒序）→ 查 version=2 详情 → 对比 version=1 与 version=3 返回差异结果

### Implementation for User Story 4

- [X] T058 [US4] 在 `EntityVersionJpaRepository` 中扩展查询方法：`findByFqnOrderByVersionDesc()`、`findByFqnAndVersion()` 在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/infrastructure/persistence/jpa/EntityVersionJpaRepository.java`
- [X] T059 [US4] 创建 `VersionDiffService` 领域服务（递归 JSON 扁平化为 `Map<String, Object>`，对比生成 `List<FieldDiff>` 按 ADDED/MODIFIED/DELETED 分类）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/domain/service/VersionDiffService.java`
- [X] T060 [US4] 创建 `MetadataHistoryServiceImpl` Application Service（实现 `MetadataHistoryService` 接口，注入 `EntityVersionRepository` + `EntityVersionMapper` + `VersionDiffService`，实现 `listVersions` / `getVersionDetail` / `compareVersions`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/application/service/MetadataHistoryServiceImpl.java`
- [X] T061 [US4] 创建 `MetadataHistoryController` REST 接口（端点：`GET /history/{fqn}/versions`、`GET /history/{fqn}/versions/{version}`、`POST /history/diff`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/interfaces/rest/MetadataHistoryController.java`

**Checkpoint**: 历史版本全量追溯能力可用，差异对比分类展示

---

## Phase 7: User Story 5 - 变更事件通知 (Priority: P2)

**Goal**: 元数据生效/下线时自动发布变更事件，通知下游 BC 同步数据

**Independent Test**: 生效操作 → 验证 `MetadataChangeEvent` 已发布（含 FQN、ChangeType=ACTIVATE、版本号、时间戳）→ 下线操作 → 验证事件已发布（ChangeType=DEPRECATE）

### Implementation for User Story 5

- [X] T062 [US5] 创建 `MetadataEventServiceImpl` Application Service（实现 `MetadataEventService` 接口，注入 `MetadataEventPublisher`，实现 `publishChangeEvent` 构建 `MetadataChangeEvent` 并发布）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/application/service/MetadataEventServiceImpl.java`
- [X] T063 [US5] 在 `MetadataActivationServiceImpl` 中集成事件发布：生效/下线事务提交后调用 `MetadataEventService.publishChangeEvent()`（使用 `@TransactionalEventListener(phase = AFTER_COMMIT)` 确保事务提交后发布）

**Checkpoint**: 变更事件正确发布，生效/下线操作后下游可监听到事件

---

## Phase 8: User Story 6 - 批量导入导出 (Priority: P3)

**Goal**: YAML/JSON 批量导入（逐条校验、幂等策略、仅写草稿表）、按 FQN 前缀/元模型类型/FQN 列表导出生效元数据

**Independent Test**: 准备 10 条 JSON 导入文件→导入成功 8 条写草稿表（2 条 FQN 重复跳过）→生效 5 条→按 FQN 前缀导出→导出 5 条 JSON 与导入格式完全兼容

### Implementation for User Story 6

- [X] T064 [US6] 创建 `MetadataImportExportServiceImpl` Application Service（实现 `MetadataImportExportService` 接口，注入 `SchemaValidationService` + `FqnGenerator` + `MetadataEntityDraftRepository` + `MetadataEntityRepository` + `MetadataEntityMapper` + `ObjectMapper`，实现 `importMetadata`（逐条解析→校验→草稿写入→返回结果）、`exportByFqnPrefixes` / `exportByEntitySchema` / `exportByFqns`）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/application/service/MetadataImportExportServiceImpl.java`
- [X] T065 [P] [US6] 在 `MetadataImportExportServiceImpl` 中实现 `validateBatch` 方法（支持按元模型类型或 FQN 前缀范围发起批量合规校验，遍历主表生效元数据逐条执行 JSON Schema 校验，输出包含通过率、违规 FQN 清单、违规详情的校验报告，对应 FR-043）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/application/service/MetadataImportExportServiceImpl.java`
- [X] T066 [US6] 创建 `MetadataImportExportController` REST 接口（端点：`POST /import`、`POST /export`、`POST /validate-batch`，支持 YAML/JSON 格式检测）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/interfaces/rest/MetadataImportExportController.java`

**Checkpoint**: 批量导入导出功能可用，格式兼容可复现

---

## Phase 9: MCP 工具集与集成测试

**Purpose**: MCP 协议工具暴露 + 端到端集成测试 + 文档验证

- [X] T067 创建 `MetadataMcpTools` MCP 工具 Bean（`@Component`，注入 `MetadataQueryService` + `MetadataHistoryService`，暴露 `getMetadataEntity` / `queryMetadataByPrefix` / `queryMetadataBySchema` / `queryMetadataByAttribute` / `getEntityVersionHistory` / `getEntityVersionDetail` / `compareVersions` 七个 `@Tool` 方法）在 `metaforge-metadata-core/src/main/java/com/metaforge/metadata/interfaces/mcp/MetadataMcpTools.java`
- [X] T068 [P] 编写集成测试 `MetadataDraftServiceIntegrationTest`（继承 `BaseIntegrationTest`，覆盖 US1 场景：创建/编辑/删除/校验）在 `metaforge-metadata-core/src/test/java/com/metaforge/metadata/application/service/MetadataDraftServiceIntegrationTest.java`
- [X] T069 [P] 编写集成测试 `MetadataActivationServiceIntegrationTest`（覆盖 US2 场景：生效/下线/原子事务回滚/重新生效）在 `metaforge-metadata-core/src/test/java/com/metaforge/metadata/application/service/MetadataActivationServiceIntegrationTest.java`
- [X] T070 [P] 编写集成测试 `MetadataQueryServiceIntegrationTest`（覆盖 US3 场景：精准查询/前缀OR并集/元模型类型/属性条件/管理员全状态）在 `metaforge-metadata-core/src/test/java/com/metaforge/metadata/application/service/MetadataQueryServiceIntegrationTest.java`
- [X] T071 [P] 编写集成测试 `MetadataHistoryServiceIntegrationTest`（覆盖 US4 场景：版本列表/详情/差异对比）在 `metaforge-metadata-core/src/test/java/com/metaforge/metadata/application/service/MetadataHistoryServiceIntegrationTest.java`
- [X] T072 [P] 编写集成测试 `MetadataImportExportServiceIntegrationTest`（覆盖 US6 场景：导入/导出/批量校验/格式兼容/幂等策略）在 `metaforge-metadata-core/src/test/java/com/metaforge/metadata/application/service/MetadataImportExportServiceIntegrationTest.java`
- [X] T073 [P] 编写性能基准测试 `MetadataPerformanceTest`（继承 `BaseIntegrationTest`，使用 `@RepeatedTest` 或 JMH 验证 SC-001~SC-006：草稿创建≤50ms、精准查询≤20ms、前缀查询≤100ms、批量导入 500 条≤5s、生效≤100ms、历史列表≤100ms）在 `metaforge-metadata-core/src/test/java/com/metaforge/metadata/performance/MetadataPerformanceTest.java`
- [ ] T074 运行 quickstart.md 全量验证场景，确认 5 个端到端场景通过

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无前置依赖——可立即开始
- **Foundational (Phase 2)**: 依赖 Setup 完成——**阻塞所有用户故事**
- **User Story 1 (Phase 3)**: 依赖 Foundational 完成
- **User Story 2 (Phase 4)**: 依赖 Foundational 完成 + US1（生效依赖草稿存在）
- **User Story 3 (Phase 5)**: 依赖 Foundational 完成——可与 US2 并行
- **User Story 4 (Phase 6)**: 依赖 Foundational 完成——可与 US3/US5 并行
- **User Story 5 (Phase 7)**: 依赖 US2（事件在生效/下线时触发）
- **User Story 6 (Phase 8)**: 依赖 Foundational 完成——可与 US3/US4 并行
- **Phase 9 (MCP + Test)**: 依赖 US1-US6 全部完成

### User Story Dependencies

```
Phase 1 (Setup)
    ↓
Phase 2 (Foundational)
    ↓
    ├── Phase 3: US1 (Draft Management) ← P1 MVP 必须
    │       ↓
    ├── Phase 4: US2 (Activation) ← 依赖 US1
    │       ↓
    │       └── Phase 7: US5 (Events) ← 依赖 US2
    │
    ├── Phase 5: US3 (Query) ← 可并行，独立
    ├── Phase 6: US4 (History) ← 可并行，独立
    └── Phase 8: US6 (Import/Export) ← 可并行，独立
                ↓
        Phase 9 (MCP + Tests)
```

### Within Each User Story

- 领域服务 → Application Service → REST Controller
- 同一 Story 内无依赖的模型/接口可并行（标记 [P]）
- Story 完成后方可进入下一优先级

### Parallel Opportunities

- Phase 2 中 T006-T021（API 模块所有常量/枚举/DTO/接口）可完全并行
- Phase 2 中 T022-T029（领域模型）可完全并行
- Phase 2 中 T034-T039（JPA + Mapper + Adapter）可完全并行
- US3 / US4 / US6 三个 P2/P3 Story 可在 US2 之后完全并行实施
- Phase 9 中 T068-T073 六个集成+性能测试可完全并行

---

## Parallel Example: Foundational Phase (Phase 2)

```bash
# 并行批次 1: API 模块契约层（16 个文件，无依赖）
Task T006-T021: 常量、枚举、DTO、事件类、服务接口

# 并行批次 2: Core 模块领域层（8 个文件，无依赖）
Task T022-T029: 值对象、聚合根、实体、异常

# 并行批次 3: 基础设施层（10 个文件，依赖批次 1+2）
Task T034-T039: JPO、JpaRepository、Mapper、Adapter

# 并行批次 4: 配置与 SPI（6 个文件）
Task T040-T045: Gateway、ExceptionHandler、HealthCheck、EventPublisher、Config
```

---

## Implementation Strategy

### MVP First (US1 + US2)

1. 完成 Phase 1: Setup — 模块注册完成
2. 完成 Phase 2: Foundational — 基础设施就绪
3. 完成 Phase 3: US1 — 草稿管理功能可用
4. 完成 Phase 4: US2 — 生效/下线闭环
5. **STOP and VALIDATE**: 独立验证草稿→生效→下线完整链路
6. 可部署/演示（MVP 核心闭环已达成）

### Incremental Delivery

1. Setup + Foundational → 基础就绪
2. US1 + US2 → 核心闭环 → 部署 (MVP!)
3. US3 + US5 → 查询与事件 → 部署
4. US4 → 历史追溯 → 部署
5. US6 → 批量操作 → 部署
6. 每个 Story 独立增值，不破坏已有功能

### Parallel Team Strategy

多开发者协作：

1. 团队一起完成 Setup + Foundational
2. Foundational 完成后：
   - 开发者 A: US1 (草稿) → US2 (生效)
   - 开发者 B: US3 (查询)
   - 开发者 C: US4 (历史) + US6 (导入导出)
3. 各 Story 独立完成并集成

---

## Notes

- [P] 任务 = 不同文件、无依赖，可并行
- [Story] 标签映射任务到具体用户故事，便于追溯
- 每个用户故事应可独立完成和测试
- 每完成一个逻辑组建议提交
- 严禁重复实现 foundation-core 预置能力（全局异常切面、ApiResponse、MessageSource、PageRequest/PageResult、JsonbUtils 等）
- 所有 FQN 操作统一使用 `FqnGenerator`，严禁 `String.join` 或 `+` 拼接
