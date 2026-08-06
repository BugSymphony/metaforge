# Tasks: 语义关系实例全生命周期管理

**Input**: Design documents from `/specs/001-relation-instance-lifecycle/`

**Prerequisites**: plan.md (required), spec.md (required), data-model.md, contracts/, research.md, foundation-adaptation.md

**Tests**: 本特性规格包含性能基线与验收场景，集成测试任务已纳入各用户故事阶段。

**Organization**: 任务按用户故事分组，支持独立实现与验证。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 所属用户故事（US1/US2/US3/US4/US5/US6）
- 描述中包含确切文件路径

## Path Conventions

- **$BC_PATH**: `/data/ext/source-8/metaforge/metaforge-parent/metaforge-graph`
- **api 模块源码路径**: `$BC_PATH/metaforge-graph-api/src/main/java/com/metaforge/graph/api/`
- **core 模块源码路径**: `$BC_PATH/metaforge-graph-core/src/main/java/com/metaforge/graph/`
- **测试路径**: `$BC_PATH/metaforge-graph-core/src/test/java/com/metaforge/graph/`
- **迁移脚本路径**: `$BC_PATH/../metaforge-boot/src/main/resources/db/migration/`
- **i18n 资源**: `$BC_PATH/../metaforge-boot/src/main/resources/i18n/`

---

## Phase 1: Setup（项目初始化与基础设施接入）

**Purpose**: 创建 BC Maven 三级模块结构，完成 foundation-core 平台接入，配置通用能力

### Foundation 模块注册与构建集成

- [x] T001 创建 metaforge-graph 聚合父 POM 在 `$BC_PATH/pom.xml`，继承 metaforge-parent，声明 `<modules>` 包含 metaforge-graph-api 和 metaforge-graph-core，packaging 为 pom
- [x] T002 创建 metaforge-graph-api 子模块 POM 在 `$BC_PATH/metaforge-graph-api/pom.xml`，继承 metaforge-graph 聚合父 POM，无额外业务依赖
- [x] T003 创建 metaforge-graph-core 子模块 POM 在 `$BC_PATH/metaforge-graph-core/pom.xml`，依赖 metaforge-graph-api、metaforge-metamodel-api、metaforge-metadata-api、metaforge-framework（含 MapStruct、json-schema-validator）
- [x] T004 在 `$REPO_ROOT/metaforge-boot/pom.xml` 中添加 metaforge-graph-core 作为 `<dependency>` 完成模块注册
- [x] T005 在 `$REPO_ROOT/metaforge-parent/pom.xml` 的 `<modules>` 中添加 `<module>metaforge-graph</module>` 注册

### 数据库迁移脚本

- [x] T006 创建 Flyway DDL 迁移脚本 `V<n>__metaforge-graph_ddl.sql` 在 `$BC_PATH/../metaforge-boot/src/main/resources/db/migration/`，包含 semantic_relation_network Schema、relation_instance 主表、relation_instance_draft 草稿表、relation_version 历史表、entity_relation_index 索引表的完整 DDL（含索引与唯一约束），并启用 pg_trgm 扩展

### 配置文件

- [x] T007 创建 `application-metaforge-graph.yml` 在 `$BC_PATH/../metaforge-boot/src/main/resources/`，配置 metaforge.graph.* 命名空间下的 schema-cache（ttl-seconds、max-size）、validation.max-content-size、import.batch-size

---

## Phase 2: Foundational（领域层核心基础）

**Purpose**: 所有用户故事共同依赖的领域模型、值对象、枚举、常量、SPI 注册，**必须完成后方可开始用户故事实现**

**🔒 CRITICAL**: 本阶段阻塞全部用户故事。

### API 模块常量与枚举（SSOT）

- [x] T008 [P] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/enums/RelationType.java` 定义关系类型枚举（COMPOSITION / ASSOCIATION_REFERENCE / MAPPING_CORRESPONDENCE / DEPENDENCY_INFLUENCE / PROCESS_SEQUENCE）
- [x] T009 [P] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/enums/RelationStatus.java` 定义关系状态枚举（DRAFT / ACTIVE / DEPRECATED）
- [x] T010 [P] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/enums/ChangeType.java` 定义变更类型枚举（ACTIVATED / DEPRECATED）
- [x] T011 [P] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/constant/GraphErrorCode.java` 定义错误码常量类（32001-32015，含常量名与中文描述，覆盖所有错误码）
- [x] T012 [P] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/constant/GraphConstants.java` 定义业务常量类（最大 content 大小、FQN 分隔符、默认分页参数等）

### 领域层值对象

- [x] T013 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/model/valueobject/FQN.java` 创建 FQN 值对象（不可变，`value: String`，含格式校验与解析方法）
- [x] T014 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/model/valueobject/EntityFQN.java` 创建 EntityFQN 值对象
- [x] T015 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/model/valueobject/RelationSchemaFQN.java` 创建 RelationSchemaFQN 值对象（含版本号）
- [x] T016 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/model/valueobject/VersionNumber.java` 创建 VersionNumber 值对象（≥1，单调递增）
- [x] T017 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/model/valueobject/RelationName.java` 创建 RelationName 值对象（最大 512 字符）
- [x] T018 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/model/valueobject/RelationDescription.java` 创建 RelationDescription 值对象
- [x] T019 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/model/valueobject/CardinalityRule.java` 创建 CardinalityRule 值对象（sourceCardinality + targetCardinality 字符串）

### 领域层 FQN 生成器

- [x] T020 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/service/FqnGenerator.java` 创建 FQN 生成器工具类，提供 `generate(EntityFQN source, RelationTypeFQN type, EntityFQN target): FQN` 和 `parse(FQN): FqnComponents` 方法，格式 `{源实体FQN}#{关系类型FQN}#{目标实体FQN}`

### 基础设施层 JPA 持久化实体 (JPO)

- [x] T021 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/jpa/RelationInstanceJpo.java` 创建主表 JPA 实体（@Entity, @Table schema = "semantic_relation_network", content 字段用 @Convert + JsonbConverter）
- [x] T022 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/jpa/RelationInstanceDraftJpo.java` 创建草稿表 JPA 实体
- [x] T023 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/jpa/RelationVersionJpo.java` 创建历史表 JPA 实体
- [x] T024 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/jpa/EntityRelationIndexJpo.java` 创建索引表 JPA 实体

### 基础设施层 JPA DAO 和 JSONB 转换器

- [x] T025 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/jpa/RelationInstanceJpaRepository.java` 创建 Spring Data JPA DAO 接口（含 findByFqn、findBySourceEntityFqn、findByTargetEntityFqn 等方法）
- [x] T026 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/jpa/RelationInstanceDraftJpaRepository.java` 创建草稿表 DAO 接口
- [x] T027 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/jpa/RelationVersionJpaRepository.java` 创建历史表 DAO 接口
- [x] T028 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/jpa/EntityRelationIndexJpaRepository.java` 创建索引表 DAO 接口
- [x] T029 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/jpa/ContentJsonbConverter.java` 创建 content 字段 JSONB 转换器（调用 JsonbUtils.toJsonb/fromJsonb）
- [x] T030 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/jpa/EmbeddingJsonbConverter.java` 创建 embedding 字段 JSONB 转换器

### Foundation SPI 扩展注册

- [x] T031 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/config/GraphExceptionHandlerSpi.java` 实现 ExceptionHandlerSpi，注册为 @Component @Order(100)，映射 GraphBizException 错误码到 ApiResponse
- [x] T032 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/config/GraphHealthCheckSpi.java` 实现 HealthCheckSpi，检查数据库连通性
- [x] T033 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/config/GraphBizException.java` 创建 BC 统一业务异常基类（继承 BizException，含 code + message）

### i18n 资源文件

- [x] T034 在 `metaforge-boot/src/main/resources/i18n/messages_metaforge-graph_zh_CN.properties` 创建中文国际化资源（所有错误码对应的中文消息）
- [x] T035 在 `metaforge-boot/src/main/resources/i18n/messages_metaforge-graph_en_US.properties` 创建英文国际化资源

**Checkpoint**: 领域层基础就绪——值对象、枚举、常量、FQN 生成器、JPA 基础设施、SPI 扩展全部完成，可开始用户故事实现

---

## Phase 3: User Story 1 - 关系实例草稿编辑与版本生效 (Priority: P1) 🎯 MVP

**Goal**: 实现基于 RelationSchema 的关系草稿手动创建、编辑、删除，以及草稿生效的原子四步事务。这是 BC 的核心写入链路。

**Independent Test**: 创建一条草稿、编辑 content、执行生效，验证主表出现记录、草稿表清空、历史表新增 v1、索引表更新，全链路 ≤100ms。

### API 模块 DTO 与接口定义

- [x] T036 [P] [US1] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/CreateDraftRequest.java` 创建草稿创建请求 DTO（含 @NotBlank 校验注解）
- [x] T037 [P] [US1] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/UpdateDraftContentRequest.java` 创建草稿内容更新请求 DTO
- [x] T038 [P] [US1] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/RelationInstanceDraftDto.java` 创建草稿响应 DTO
- [x] T039 [P] [US1] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/RelationInstanceDto.java` 创建生效关系响应 DTO（含 currentVersion、审计字段）
- [x] T040 [P] [US1] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/DeactivationCheckResult.java` 创建下线前置校验结果 DTO
- [x] T041 [US1] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/service/RelationDraftService.java` 定义 RelationDraftService 接口（createDraft / createDraftFromActive / updateDraftContent / getDraft / deleteDraft），标注 @OpenHostService 语义注释
- [x] T042 [US1] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/service/RelationActivationService.java` 定义 RelationActivationService 接口（activate / deprecate / reactivate / checkDeactivationPreconditions），标注 @OpenHostService 语义注释

### 领域层聚合根

- [x] T043 [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/model/aggregate/RelationInstance.java` 创建生效态关系聚合根（含 FQN、端点 FQN、relationType、content、currentVersion 等属性和 deprecate 业务方法）
- [x] T044 [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/model/aggregate/RelationInstanceDraft.java` 创建草稿态关系聚合根（含 updateContent、activate 业务方法，创建后 fqn/relationSchemaFqn/端点不可变更校验）

### 领域层服务

- [x] T045 [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/service/RelationSchemaValidationService.java` 创建 JSON Schema 结构校验领域服务，基于 RelationSchemaFQN 获取对应 JSON Schema 并校验 content，返回结构化错误信息
- [x] T046 [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/service/CardinalityValidationService.java` 创建基数约束校验领域服务，查询主表已存在的同类关系数量并与 CardinalityRule 比对

### 领域层仓储端口

- [x] T047 [P] [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/repository/RelationInstanceRepository.java` 定义主表仓储端口接口（findByFqn / save / deleteByFqn / countBy... 等方法，入参返回值均为领域对象）
- [x] T048 [P] [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/repository/RelationInstanceDraftRepository.java` 定义草稿表仓储端口接口
- [x] T049 [P] [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/repository/RelationVersionRepository.java` 定义历史表仓储端口接口（仅 INSERT 操作）

### 基础设施层仓储适配器

- [x] T050 [P] [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/adapter/RelationInstanceRepositoryAdapter.java` 实现主表仓储端口，内部注入 JpaRepository，使用 MapStruct 转换 JPO ↔ 领域对象
- [x] T051 [P] [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/adapter/RelationInstanceDraftRepositoryAdapter.java` 实现草稿表仓储端口适配器
- [x] T052 [P] [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/adapter/RelationVersionRepositoryAdapter.java` 实现历史表仓储端口适配器

### 基础设施层 MapStruct 转换器

- [x] T053 [P] [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/converter/RelationInstanceConverter.java` 创建主表领域对象 ↔ JPO ↔ DTO 的 MapStruct 转换器接口
- [x] T054 [P] [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/converter/RelationDraftConverter.java` 创建草稿 MapStruct 转换器接口
- [x] T055 [P] [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/converter/RelationVersionConverter.java` 创建历史版本 MapStruct 转换器接口

### 上游 BC 对接领域端口（仅接口定义）

- [x] T056 [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/repository/RelationSchemaRepository.java` 定义上游元模型访问领域端口接口（getRelationSchemaSchema / isSchemaPublished 等方法）
- [x] T057 [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/repository/MetadataEntityGateway.java` 定义上游元数据访问领域端口接口（isEntityActive / getEntityInfo 等方法）

### 基础设施层上游 BC 适配器实现

- [x] T058 [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/adapter/MetamodelGatewayAdapter.java` 实现 RelationSchemaRepository 端口，内部注入 metaforge-metamodel-api 的 ElementDefinitionService
- [x] T059 [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/adapter/MetadataGatewayAdapter.java` 实现 MetadataEntityGateway 端口，内部注入 metaforge-metadata-api 的 MetadataQueryService

### 应用层服务实现

- [x] T060 [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/application/service/RelationDraftServiceImpl.java` 实现 RelationDraftService，调用 FqnGenerator 生成 FQN、Validator 校验 JSON Schema、MetadataEntityGateway 校验端点有效性、DraftRepository 持久化
- [x] T061 [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/application/service/RelationActivationServiceImpl.java` 实现 RelationActivationService，执行四步原子事务（主表写入/覆盖 → 历史表归档 → 草稿表删除 → 索引更新），使用 @Transactional

### 接口适配层 REST Controller

- [x] T062 [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/interfaces/rest/RelationDraftController.java` 创建草稿管理 REST Controller（@Tag name = "语义关系管理"），映射 POST/PUT/GET/DELETE 端点
- [x] T063 [US1] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/interfaces/rest/RelationActivationController.java` 创建版本生效 REST Controller，映射 activate/deprecate/reactivate/check-deprecation 端点

### 集成测试

- [x] T064 [US1] 在 `metaforge-graph-core/src/test/java/com/metaforge/graph/integration/persistence/RelationDraftPersistenceTest.java` 编写草稿表 CRUD 集成测试（继承 BaseIntegrationTest）
- [x] T065 [US1] 在 `metaforge-graph-core/src/test/java/com/metaforge/graph/integration/rest/RelationDraftApiTest.java` 编写草稿管理 REST API 集成测试（验证 HTTP 200/409/422 状态码与错误码）
- [x] T066 [US1] 在 `metaforge-graph-core/src/test/java/com/metaforge/graph/integration/rest/RelationActivationApiTest.java` 编写生效 REST API 集成测试（验证四步事务原子性、版本号递增、索引更新）

**Checkpoint**: US1 完成——草稿创建/编辑/删除 + 原子生效/下线/重新生效 + 上游元模型/元数据对接全部可独立验证

---

## Phase 4: User Story 2 - 实体生命周期驱动的自动关系构建 (Priority: P1)

**Goal**: 监听 metadata-management BC 的元数据变更事件，自动解析实体关联引用字段并生成/更新/销毁关系实例。

**Independent Test**: 触发一次上游 MetadataChangeEvent（ACTIVATE），验证本 BC 自动创建对应关系并生效，关系 FQN 遵循统一规则。

### API 模块事件消费契约

- [x] T067 [US2] 确认 metaforge-metadata-api 依赖已引入，MetadataChangeListener 接口（`com.metaforge.metadata.api.event.MetadataChangeListener`）已由上游发布，本 BC 无需重复定义，直接在 `metaforge-graph-core` 中实现该接口即可

### 接口适配层事件监听器实现

- [x] T068 [US2] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/interfaces/event/MetadataChangeEventListener.java` 实现 MetadataChangeListener（注册为 @Component），按变更类型分发处理：
  - ACTIVATE：解析实体 content 中符合 RelationSchema 的关联引用字段，调用 FqnGenerator 生成关系 FQN，创建草稿并执行自动生效
  - DEPRECATE：查询该实体作为源端/目标端的所有生效关系，执行下线校验流程
  - 以实体 FQN + 版本号作为幂等键去重

### 应用层自动构建扩展

- [x] T069 [US2] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/application/service/RelationAutoBuildService.java` 创建自动构建服务，封装关联引用解析、增量对比（变更前后关联引用 diff）、批量关系生成/更新/销毁逻辑，遵循最终一致性语义（独立事务）

### 幂等处理基础设施

- [x] T070 [US2] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/event/IdempotencyStore.java` 创建事件幂等存储（基于 ConcurrentHashMap），以实体 FQN + 版本号为幂等键

### 集成测试

- [x] T071 [US2] 在 `metaforge-graph-core/src/test/java/com/metaforge/graph/integration/event/MetadataChangeEventListenerTest.java` 编写事件监听集成测试——发布模拟 MetadataChangeEvent 验证自动构建、幂等去重、空关联场景

**Checkpoint**: US2 完成——实体变更事件自动驱动关系构建、更新、销毁，幂等处理就绪

---

## Phase 5: User Story 3 - 多维关系查询与拓扑浏览 (Priority: P1)

**Goal**: 实现 FQN 精准查询、出入边查询（含过滤）、FQN 前缀范围查询、统一多维过滤查询（6 维度，维度间 AND 维度内 OR）、管理员全状态聚合查询、拓扑完整性校验。

**Independent Test**: 主表中存在多类生效关系，分别执行各查询模式并验证结果正确性与性能基线。

### API 模块查询 DTO 与接口定义

- [x] T072 [P] [US3] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/RelationQueryRequest.java` 创建多维过滤查询请求 DTO（含 relationTypes / sourceEntityTypes / targetEntityTypes / sourceEntityFqns / targetEntityFqns / relationSchemaFqns / nameKeyword / descriptionKeyword / 时间范围 / PageRequest 共 10 个维度字段）
- [x] T073 [P] [US3] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/AdminQueryRequest.java` 创建管理端查询请求 DTO
- [x] T074 [US3] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/service/RelationQueryService.java` 定义 RelationQueryService 接口（getByFqn / getOutboundRelations / getInboundRelations / listByConditions / multiFilter / adminQuery）
- [x] T075 [US3] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/service/RelationTopologyService.java` 定义 RelationTopologyService 接口（getDependentRelations / validateTopology / getRelationCount）

### API 模块拓扑 DTO

- [x] T076 [P] [US3] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/TopologyValidationRequest.java` 创建拓扑校验请求 DTO
- [x] T077 [P] [US3] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/TopologyValidationReport.java` 创建拓扑校验报告 DTO（含 totalChecked / issuesFound / List<TopologyIssue>）
- [x] T078 [P] [US3] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/RelationCount.java` 创建关系计数 DTO

### 领域层查询扩展

- [x] T079 [US3] 扩展 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/repository/RelationInstanceRepository.java` 增加多维过滤查询方法（multiFilter 返回 PageResult<RelationInstance>）、出入边查询方法、前缀查询方法
- [x] T080 [US3] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/service/DependencyCheckService.java` 创建依赖关系校验领域服务，判断目标关系是否被其他 DEPENDENCY_INFLUENCE 类型的生效关系引用

### 基础设施层动态查询实现

- [x] T081 [US3] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/adapter/RelationQuerySpecification.java` 创建 JPA Specification 动态查询构建器，支持维度间 AND 维度内 OR 的复合查询条件组装，含 ILIKE 模糊匹配、pg_trgm GIN 索引、参数化查询防注入

### 应用层服务实现

- [x] T082 [US3] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/application/service/RelationQueryServiceImpl.java` 实现 RelationQueryService，注入 JpaRepository 执行查询，使用 PageHelper.toSpringPageRequest / fromSpringPage 转换分页
- [x] T083 [US3] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/application/service/RelationTopologyServiceImpl.java` 实现 RelationTopologyService

### 接口适配层 REST Controller

- [x] T084 [US3] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/interfaces/rest/RelationQueryController.java` 创建查询 REST Controller，映射 GET /relations/{fqn}、GET /relations/outbound、GET /relations/inbound、GET /relations、POST /relations/filter、GET /admin/relations 共 6 个端点
- [x] T085 [US3] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/interfaces/rest/RelationTopologyController.java` 创建拓扑查询 REST Controller

### 集成测试

- [x] T086 [US3] 在 `metaforge-graph-core/src/test/java/com/metaforge/graph/integration/rest/RelationQueryApiTest.java` 编写查询 API 集成测试（覆盖 FQN 精准查询、出入边查询、多维过滤、分页、空结果、SQL 特殊字符防注入）
- [x] T087 [US3] 在 `metaforge-graph-core/src/test/java/com/metaforge/graph/integration/rest/RelationTopologyApiTest.java` 编写拓扑校验 API 集成测试

**Checkpoint**: US3 完成——多维查询、拓扑浏览、管理端全状态查询全部可独立验证

---

## Phase 6: User Story 6 - 关系实例变更事件通知 (Priority: P1)

**Goal**: 关系实例正式态变更（生效/下线）时发布 RelationChangeEvent，下游 BC 通过 RelationChangeListener 监听消费。

**Independent Test**: 执行一次生效操作，验证事务提交后事件成功发布，下游测试监听器收到含正确 FQN、changeType 和 version 的 RelationChangeEvent。

### API 模块事件定义

- [x] T088 [P] [US6] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/event/RelationChangeEvent.java` 定义 RelationChangeEvent 类（继承 ApplicationEvent），含 fqn / changeType / version / relationSchemaFqn / sourceEntityFqn / targetEntityFqn / eventTime 字段
- [x] T089 [P] [US6] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/event/RelationChangeListener.java` 定义 RelationChangeListener 接口（@FunctionalInterface），下游 BC 实现此接口消费事件

### 领域层事件发布端口

- [x] T090 [US6] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/domain/event/RelationEventPublisher.java` 定义领域事件发布器端口接口（publishActivated / publishDeprecated 方法）

### 基础设施层事件发布实现

- [x] T091 [US6] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/event/SpringRelationEventPublisher.java` 实现 RelationEventPublisher，使用 ApplicationEventPublisher + @TransactionalEventListener(phase = AFTER_COMMIT) 确保事务成功后发布

### 应用层事件集成

- [x] T092 [US6] 在 RelationActivationServiceImpl 的 activate/deprecate 方法中注入 RelationEventPublisher，事务成功后调用 publishActivated / publishDeprecated 方法

### 集成测试

- [x] T093 [US6] 在 `metaforge-graph-core/src/test/java/com/metaforge/graph/integration/event/RelationChangeEventTest.java` 编写事件发布集成测试——验证生效/下线/重新生效场景正确发布事件，事务回滚不发布事件

**Checkpoint**: US6 完成——关系变更事件从发布到消费全链路可独立验证

---

## Phase 7: User Story 4 - 关系下线与历史版本追溯 (Priority: P2)

**Goal**: 实现关系下线（含依赖校验）、历史版本列表查询、单版本详情、两版本差异对比、基于历史版本重新生效。

**Independent Test**: 对已生效关系执行下线并验证主表记录移除但历史表保留，历史表数据不可 UPDATE/DELETE。

### API 模块历史 DTO 与接口定义

- [x] T094 [P] [US4] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/RelationVersionDto.java` 创建历史版本响应 DTO
- [x] T095 [P] [US4] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/DiffRequest.java` 创建版本差异对比请求 DTO
- [x] T096 [P] [US4] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/VersionDiffDto.java` 创建版本差异结果 DTO（含 addedFields / modifiedFields / deletedFields）
- [x] T097 [P] [US4] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/FieldDiff.java` 创建字段差异 DTO
- [x] T098 [US4] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/service/RelationHistoryService.java` 定义 RelationHistoryService 接口（listVersions / getVersionDetail / compareVersions）

### 基础设施层历史查询适配器

- [x] T099 [US4] 扩展 `metaforge-graph-core/src/main/java/com/metaforge/graph/infrastructure/persistence/adapter/RelationVersionRepositoryAdapter.java` 增加按 FQN+版本号查询、按 FQN 列表查询的方法，数据库层限制为仅 SELECT/INSERT

### 应用层服务实现

- [x] T100 [US4] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/application/service/RelationHistoryServiceImpl.java` 实现 RelationHistoryService，内部执行递归 JSON 对比算法生成字段级差异（新增/修改/删除分类）

### 接口适配层 REST Controller

- [x] T101 [US4] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/interfaces/rest/RelationHistoryController.java` 创建历史追溯 REST Controller，映射 GET /versions/{fqn}、GET /versions/{fqn}/{version}、POST /versions/diff 三个端点

### 集成测试

- [x] T102 [US4] 在 `metaforge-graph-core/src/test/java/com/metaforge/graph/integration/rest/RelationHistoryApiTest.java` 编写历史追溯 API 集成测试（验证历史表只读、版本列表倒序、差异对比正确性）

**Checkpoint**: US4 完成——下线依赖校验 + 历史追溯 + 版本对比全部可独立验证

---

## Phase 8: User Story 5 - 批量导入导出 (Priority: P2)

**Goal**: 实现 YAML/JSON 格式批量导入（进入草稿态）和多粒度导出（FQN 前缀/关系类型/指定 FQN 列表）。导入支持"跳过/报错"幂等策略。

**Independent Test**: 导出一组生效关系为 JSON 文件，重新导入验证所有关系进入草稿态（非主表），主表无变更。

### API 模块导入导出 DTO 与接口

- [x] T103 [P] [US5] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/ImportRequest.java` 创建导入请求 DTO（含 content / format / strategy）
- [x] T104 [P] [US5] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/ImportResultDto.java` 创建导入结果 DTO（含 totalCount / successCount / skipCount / failureCount / items）
- [x] T105 [P] [US5] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/ImportItemResult.java` 创建单条导入结果 DTO
- [x] T106 [P] [US5] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/dto/ExportResultDto.java` 创建导出结果 DTO
- [x] T107 [P] [US5] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/enums/ImportFormat.java` 定义导入格式枚举（JSON / YAML）
- [x] T108 [P] [US5] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/enums/ExportFormat.java` 定义导出格式枚举
- [x] T109 [P] [US5] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/enums/ImportStrategy.java` 定义导入策略枚举（SKIP / ERROR）
- [x] T110 [US5] 在 `metaforge-graph-api/src/main/java/com/metaforge/graph/api/service/RelationImportExportService.java` 定义 RelationImportExportService 接口（importRelations / exportByFqnPrefixes / exportByRelationTypes / exportByFqns）

### 应用层服务实现

- [x] T111 [US5] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/application/service/RelationImportExportServiceImpl.java` 实现 RelationImportExportService，导入侧逐条解析 → FQN 生成 → 结构校验 → 写入草稿表，content >10MB 拒绝，导出侧 JSON/YAML 序列化

### 接口适配层 REST Controller

- [x] T112 [US5] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/interfaces/rest/RelationImportExportController.java` 创建导入导出 REST Controller，映射 POST /import 和 POST /export 端点

### 集成测试

- [x] T113 [US5] 在 `metaforge-graph-core/src/test/java/com/metaforge/graph/integration/rest/RelationImportExportApiTest.java` 编写导入导出 API 集成测试（验证导入→草稿表、导出→重新导入闭环、超 10MB 拒绝、SKIP vs ERROR 策略）

**Checkpoint**: US5 完成——批量导入导出、幂等策略、格式闭环可独立验证

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: 跨用户故事的完善工作——性能优化、MCP 工具提供者、契约测试、快速验证、文档

### MCP 工具提供者

- [x] T114 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/interfaces/mcp/RelationQueryMcpTools.java` 创建关系查询 MCP 工具提供者（封装 get_relation_by_fqn / list_outbound_relations / list_inbound_relations / multi_filter_relations 四个工具）
- [x] T115 [P] 在 `metaforge-graph-core/src/main/java/com/metaforge/graph/interfaces/mcp/RelationTopologyMcpTools.java` 创建拓扑查询 MCP 工具提供者（封装 get_relation_topology / list_relation_versions / compare_relation_versions 三个工具）

### 契约测试

- [x] T116 [P] 在 `metaforge-graph-core/src/test/java/com/metaforge/graph/contract-export/RelationServiceContractTest.java` 编写对外契约测试——验证 Application Service 接口方法签名与 contracts/application-service.md 一致
- [x] T117 [P] 在 `metaforge-graph-core/src/test/java/com/metaforge/graph/contract-export/RestApiContractTest.java` 编写 REST API 契约测试——验证所有端点的 URL、HTTP 方法、请求/响应格式与 contracts/rest-api.md 一致
- [x] T118 [P] 在 `metaforge-graph-core/src/test/java/com/metaforge/graph/contract-adapt/MetamodelGatewayContractTest.java` 编写上游对接测试——验证 MetamodelGatewayAdapter 调用 metaforge-metamodel-api 的 ElementDefinitionService 正确
- [x] T119 [P] 在 `metaforge-graph-core/src/test/java/com/metaforge/graph/contract-adapt/MetadataGatewayContractTest.java` 编写上游对接测试——验证 MetadataGatewayAdapter 调用 metaforge-metadata-api 的 MetadataQueryService 正确

### 快速验证（quickstart.md 场景）

- [x] T120 按 `quickstart.md` 执行全部 9 个验证场景，确认端到端链路通过

### 性能与文档

- [x] T121 在 `$BC_PATH/../metaforge-boot/src/main/resources/application-metaforge-graph.yml` 中配置 Caffeine 缓存参数（RelationSchema JSON Schema 缓存 TTL 30 分钟、最大 200 条）
- [x] T122 [P] 生成 OpenAPI 文档（SpringDoc 自动生成，验证 Swagger UI 中 "语义关系管理" 分组显示所有端点）

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 无依赖，可立即开始
- **Phase 2 (Foundational)**: 依赖 Phase 1 完成 —— 阻塞所有用户故事
- **Phase 3 (US1, P1)**: 依赖 Phase 2 完成 —— 可独立开始
- **Phase 4 (US2, P1)**: 依赖 Phase 2 完成 + US1 中的 T058/T059（上游适配器）和 T060/T061（应用服务）
- **Phase 5 (US3, P1)**: 依赖 Phase 2 完成 + US1 中的 T043/T050（聚合根与主表仓储适配器）
- **Phase 6 (US6, P1)**: 依赖 Phase 2 完成 + US1 中的 T061（生效服务）
- **Phase 7 (US4, P2)**: 依赖 Phase 2 完成 + US1 的 T043/T050/T052 + US3 的 T080（依赖校验服务）
- **Phase 8 (US5, P2)**: 依赖 Phase 2 完成 + US1 的 T044/T051（草稿聚合根与仓储适配器）
- **Phase 9 (Polish)**: 依赖所有用户故事完成

### User Story Inter-Dependencies

```
Phase 1 → Phase 2 ──┬──→ Phase 3 (US1) ──┬──→ Phase 6 (US6)
                    │                      │
                    ├──→ Phase 5 (US3) ────┤
                    │                      │
                    ├──→ Phase 4 (US2)     │
                    │                      │
                    ├──→ Phase 7 (US4) ────┘
                    │
                    └──→ Phase 8 (US5)

                                          └──→ Phase 9 (Polish)
```

- US3、US4、US5 均可独立启动于 Foundational 完成后，但部分任务依赖 US1 中创建的聚合根/仓储适配器
- US1 完成后 US6 方可集成事件发布
- US4 需要 US1 中的主表/历史表适配器 + US3 中的依赖校验服务
- US5 需要 US1 中的草稿聚合根/仓储适配器

### Within Each User Story

- DTO/枚举定义 → 接口定义 → 领域模型 → 领域服务 → 仓储适配器 → 应用服务 → REST Controller → 集成测试

### Parallel Opportunities

- Phase 1: T001-T005（Maven 结构）与 T006（Flyway）、T007（配置文件）可并行
- Phase 2: T008-T012（api 常量枚举）、T013-T019（值对象）、T021-T030（JPA 基础设施）、T031-T033（SPI）、T034-T035（i18n）全部可并行
- US1: T036-T039（DTO）、T047-T049（仓储端口）、T050-T052（适配器）、T053-T055（MapStruct）、T064-T066（测试）可组内并行
- US3: T072-T078（DTO）、T079-T081（领域扩展）可组内并行
- US6: T088-T089（事件定义）可并行
- US4: T094-T097（DTO）可组内并行
- US5: T103-T109（DTO 枚举）可组内并行
- Phase 9: T114-T115（MCP）、T116-T119（契约测试）、T122（文档）全部可并行

### Within User Story 1

```bash
# 并行：API DTO 定义
Task: "T036 CreateDraftRequest.java"
Task: "T037 UpdateDraftContentRequest.java"
Task: "T038 RelationInstanceDraftDto.java"
Task: "T039 RelationInstanceDto.java"
Task: "T040 DeactivationCheckResult.java"

# 并行：领域仓储端口
Task: "T047 RelationInstanceRepository.java"
Task: "T048 RelationInstanceDraftRepository.java"
Task: "T049 RelationVersionRepository.java"

# 并行：MapStruct 转换器
Task: "T053 RelationInstanceConverter.java"
Task: "T054 RelationDraftConverter.java"
Task: "T055 RelationVersionConverter.java"

# 并行：集成测试
Task: "T064 RelationDraftPersistenceTest.java"
Task: "T065 RelationDraftApiTest.java"
Task: "T066 RelationActivationApiTest.java"
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. 完成 Phase 1: Setup
2. 完成 Phase 2: Foundational（关键——阻塞所有故事）
3. 完成 Phase 3: User Story 1（关系草稿编辑与版本生效）
4. **停止并验证**: 独立测试 US1 全部验收场景
5. 可交付/演示 MVP

### Incremental Delivery

1. Setup + Foundational → 基础设施就绪
2. + US1 → 核心写入链路完成 → 交付/演示（MVP！）
3. + US2 → 自动构建就绪 → 交付/演示
4. + US3 → 查询与拓扑浏览就绪 → 交付/演示
5. + US6 → 事件通知就绪 → 交付/演示
6. + US4 → 下线与历史追溯就绪 → 交付/演示
7. + US5 → 批量导入导出就绪 → 交付/演示
8. + Polish → 全功能完善

### Parallel Team Strategy

多人协作场景：

1. 团队共同完成 Phase 1 + Phase 2
2. Phase 2 完成后：
   - Developer A: User Story 1（核心写入）
   - Developer B: User Story 3（查询——可独立于 US1 的接口定义与领域查询层）
   - Developer C: User Story 5 的 DTO/接口定义（前置工作）
3. US1 完成后：
   - Developer A: User Story 6（事件发布）
   - Developer B: User Story 4（下线与历史追溯）
   - Developer C: User Story 5（导入导出实现）+ User Story 2（自动构建监听）
4. 全部故事完成后：Developer A/B/C 共同完成 Polish

---

## Notes

- 所有 Java 代码注释使用简体中文（全局宪法 XI），变量名/方法名使用英文
- 所有错误码从 `GraphErrorCode` 常量类获取，禁止硬编码
- 所有 FQN 生成/解析通过 `FqnGenerator`，禁止手动字符串拼接
- 所有 JSONB 序列化通过 `JsonbUtils`，禁止自定义 Jackson 配置
- 不定义独立的 DataSource、CacheManager、MessageSource、SpringDoc 配置 bean
- REST 响应统一由 foundation-core 全局切面包装为 `ApiResponse<T>`，Controller 直接返回业务 DTO
- 分页统一使用 `PageRequest` / `PageResult<T>` + `PageHelper`
- OpenAPI 文档仅通过 `@Tag` 注解分组，不自定义 SpringDoc 配置
- 禁止依赖上游 BC 的 `core` 模块，仅通过 `api` 模块消费
