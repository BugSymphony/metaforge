# Implementation Plan: 元数据全生命周期管理

**Branch**: `001-metadata-full-lifecycle` | **Date**: 2026-08-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-metadata-full-lifecycle/spec.md`

## Summary

实现 metadata-management BC 的元数据实体全生命周期管理能力。采用 **「主表（唯一生效版）+ 草稿表（编辑隔离）+ 历史表（只读归档）」三表存储架构**，遵循 DDD 菱形架构与 foundation-core 平台规范，通过多模块拆分（Root/api/core）构建高内聚低耦合的代码结构。核心职责：草稿管理（P1）、版本生效与生命周期管控（P1）、多维度查询检索（P2）、历史版本追溯与差异对比（P2）、变更事件通知（P2）、批量导入导出（P3）。

技术上以 Java 21 + Spring Boot 3 为基座，复用 foundation-core 提供的统一响应切面（`ApiResponse<T>`）、分页组件（`PageRequest`/`PageResult<T>`）、JSONB 序列化工具（`JsonbUtils`）、国际化消息体系、`ExceptionHandlerSpi` 扩展点等平台预置能力。通过 `metaforge-metamodel-api` 强依赖上游 metamodel-governance BC 获取已发布的 EntitySchema JSON Schema 执行实时结构校验。

## Technical Context

**Language/Version**: Java 21 + Spring Boot 3（全 BC 启用虚拟线程 Virtual Threads）

**Primary Dependencies**:
- `metaforge-framework`（foundation-core 框架层，包含 JSONB 序列化、递归 CTE、缓存、测试基类）
- `metaforge-metamodel-api`（上游 metamodel-governance BC 契约层，消费 EntitySchema JSON Schema）
- Spring Boot Starter Data JPA + PostgreSQL（持久化）
- Spring Boot Starter Validation（参数校验）
- Spring Boot Starter Web（REST API）
- Spring AI（MCP Server 发布）
- MapStruct（对象转换，仅 core 模块 infrastructure 层）
- Lombok / Jackson

**Storage**: PostgreSQL 16，单实例部署，`metadata_management` Schema 独立隔离。核心三表：
- `metadata_entity`（主表）：唯一生效版本，对外服务唯一数据源
- `metadata_entity_draft`（草稿表）：编辑态数据物理隔离，对外不可见
- `entity_version`（历史表）：只读归档，仅支持 INSERT，禁止 UPDATE/DELETE

**Testing**: JUnit 5 + Spring Boot Test + TestContainers（继承 foundation 提供的 `BaseUnitTest` / `BaseIntegrationTest`）

**Target Platform**: Linux 服务器，单 JVM 进程运行（Monorepo 多模块单体架构）

**Project Type**: Monorepo 多模块子 BC（Maven 聚合父模块 + api/core 双子模块）

**Performance Goals**:
- 单条草稿创建（含校验）≤ 50ms
- 主表 FQN 精准查询 ≤ 20ms
- FQN 前缀范围查询（百级结果）≤ 100ms
- 单批次 500 条批量导入 ≤ 5s
- 草稿生效原子操作 ≤ 100ms
- 全历史版本列表查询 ≤ 100ms

**Constraints**:
- 严禁绕过 foundation-core 平台预置能力重复实现（全局异常切面、ApiResponse、MessageSource、PageRequest/PageResult、JsonbUtils 等）
- 仅通过 `metaforge-metamodel-api` 消费上游 EntitySchema JSON Schema，禁止反向依赖或直接访问上游基础设施层
- FQN 生成必须使用统一 `FQN Generator` 工具类，严禁 `String.join` 或 `+` 拼接
- 所有常量、异常码、错误码必须集中管理于 `metaforge-metadata-api` 模块，严禁硬编码
- MapStruct 仅限 `core` 模块 `infrastructure` 层使用，API 模块及 Domain 层严禁引入
- REST API 响应统一复用 `ApiResponse<T>`，禁止自定义包装类

**Scale/Scope**: MVP 阶段 1-2 个垂直业务领域试点，元数据实体总量 ≤ 1000 条，并发消费 Agent ≤ 5 个

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### 全局宪法 MUST 级原则检查

| # | 原则 | 级别 | 合规策略 | 状态 |
|---|------|------|---------|------|
| I | 元模型唯一权威性 | MUST | metadata-management 通过 `metaforge-metamodel-api` 消费已发布的 EntitySchema JSON Schema 执行结构校验，自身不定义、不修改、不缓存元模型结构。 | PASS |
| II | 显式导入边界管控 | MUST | metadata-management 自身不涉及 Agent 导入授权逻辑——该职责归属 `agent-consumption` BC。本 BC 仅负责元数据的生命周期管理。 | PASS |
| III | 全链路权限过滤 | MUST | 本 BC 的查询接口不自行实现权限过滤，由上游 `agent-consumption` BC 在调用前完成白名单过滤。本 BC 默认仅查询主表生效版本。 | PASS |
| IV | 版本统一收敛 | MUST | 元数据实例的 entity_schema_fqn 字段绑定完整版本号的元模型（如 `order:1.0.0.pkg_order.Order`），禁止绑定草稿态元模型。 | PASS |
| V | 纯组合无继承设计 | SHOULD | 本 BC 管理 M1 层元数据实例，其组合层级通过 FQN 前缀与 parent_fqn 实现。元模型层面的组合/继承决策由 metamodel-governance 控制。 | PASS |
| IX | 纯元数据边界坚守 | MUST | 本 BC 仅存储 EntitySchema 对应的实体元数据实例（M1 层），不涉及 RelationSchema 关系实例、不存储具体业务交易数据、不引入业务域/分类/标签等治理概念。 | PASS |
| X | 文档中文规范 | MUST | 所有治理文档（plan、spec、constitution）正文使用简体中文；术语（Bundle、MCP、BC、FQN、JSON Schema）保留英文。 | PASS |
| XI | 代码注释中文规范 | SHOULD | 关键业务逻辑、复杂算法、接口说明处使用简体中文注释；代码标识符、变量名、方法名使用英文。 | PASS |

### BC 宪法原则检查

| # | 原则 | 级别 | 合规策略 | 状态 |
|---|------|------|---------|------|
| I | 三表正交存储架构 | MUST | 严格按 metadata_entity（主表）+ metadata_entity_draft（草稿表）+ entity_version（历史表）三层物理隔离存储；三表职责正交，不混合存储、不交叉状态。 | PASS |
| II | FQN 全局唯一标识 | MUST | FQN 文法与元模型对齐 `<segment> ::= [A-Za-z][A-Za-z0-9_-]*`，segment 禁止 `.`；子实体 FQN = parent_fqn + "." + segment；使用统一 FQN Generator 工具类。 | PASS |
| III | 版本全生命周期 | MUST | 草稿→生效→下线三态流转；生效为原子事务（主表写入 + 历史表归档 + 草稿表删除）；下线前校验外部引用与子实体状态。 | PASS |
| IV | 强结构校验前置 | MUST | 所有写入操作实时调用对应版本 EntitySchema 的 JSON Schema 执行全字段结构校验。校验失败返回结构化错误（字段路径 + 违规类型 + 规则引用）。 | PASS |
| V | 单正式版本原则 | MUST | 任意 FQN 同一时刻最多一条生效记录；同一 FQN 最多一条草稿；不支持多版本并行发布。 | PASS |
| VI | 纯元数据边界 | MUST | 仅管理 EntitySchema 对应的实体元数据实例；不涉及 RelationSchema 关系实例；不引入业务域/分类/标签等治理概念。 | PASS |
| VII | 历史版本追溯与差异对比 | SHOULD | 历史表归档所有正式发布版本；支持按 FQN 查全版本列表（倒序）、按 FQN+版本号查详情、任意两版本字段级差异对比。 | PASS |
| VIII | 多维度查询检索 | SHOULD | 支持 FQN 精准查询、FQN 前缀范围查询（OR 并集逻辑）、元模型类型查询、属性条件组合查询；默认仅返回生效版本，支持分页排序；管理端全状态聚合查询。 | PASS |
| IX | 批量导入导出 | SHOULD | YAML/JSON 格式批量导入导出；FQN 为唯一标识；幂等支持"跳过/报错"策略；导入成功后仅写入草稿表；导出格式与导入完全兼容。 | PASS |

**Gate Verdict**: 全局宪法 MUST 级原则 ALL PASS，BC 宪法 MUST 级原则 ALL PASS。无违规项，无需记录 Complexity Tracking。

## Foundation Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### 平台预置能力合规检查

| 检查项 | 平台预置能力 | 本 BC 合规策略 | 状态 |
|--------|-------------|---------------|------|
| 全局异常处理 | `ExceptionHandlerSpi` 扩展点注册 | 自定义业务异常通过 `ExceptionHandlerSpi` 注册映射，禁止自定义 `@RestControllerAdvice` | PASS |
| 统一响应格式 | `ApiResponse<T>` | REST API 返回值统一使用 `ApiResponse<T>` 包装，禁止自定义响应包装类 | PASS |
| API 文档 | SpringDoc OpenAPI 自动生成 | 仅通过 `@Tag(name = "metadata-management")` 标注 Controller 分组，禁止自定义 SpringDoc 配置 | PASS |
| 国际化 | 平台预配置 `MessageSource` | 注入 `MessageSource` 使用，禁止自定义 MessageSource Bean；扩展消息文件放置于 `i18n/` 目录 | PASS |
| 分页组件 | `PageRequest` / `PageResult<T>` | 统一复用平台分页组件，禁止自定义分页 DTO | PASS |
| JSONB 处理 | `JsonbUtils` | 统一使用 `JsonbUtils.toJsonb()` / `fromJsonb()`，禁止自定义序列化实现 | PASS |
| 虚拟线程 | 平台自动启用 | 禁止配置线程池、`TaskExecutor` Bean | PASS |
| 缓存 | Caffeine `CacheManager` | 按需注入 `CacheManager`，key 命名遵循 `<bc-name>:<entity>:<id>` 规范 | PASS |
| 日志脱敏 | 平台内置脱敏规则 | 禁止自定义日志脱敏；如需扩展则实现 `LogMaskSpi` | PASS |
| 健康检查 | Actuator 自动暴露 | 自定义健康检查项实现 `HealthCheckSpi`，禁止自定义 Actuator 端点 | PASS |
| 数据源管理 | HikariCP + 单数据源 | 禁止配置数据源或事务管理器；所有数据库操作使用平台统一数据源 | PASS |
| Flyway 迁移 | 平台统一管理 | 禁止配置 Flyway 或引入 flyway-core 依赖；迁移脚本命名遵循 `V<n>__metadata_<purpose>.sql` | PASS |
| 测试基类 | `BaseUnitTest` / `BaseIntegrationTest` | 测试类继承 foundation 提供的基类，禁止引入 TestContainers 或自定义测试数据源 | PASS |
| 跨 Schema 写约束 | 单数据所有权原则 | 仅对 `metadata_management` Schema 执行 INSERT/UPDATE/DELETE，跨 BC Schema 仅限 SELECT（在合约字段范围内） | PASS |
| Servlet 安全基线 | XSS 过滤、CORS | 禁止自定义 Security Filter、`CorsFilter`、`WebMvcConfigurer.addCorsMappings()` | PASS |

### 构建系统集成合规检查

| 检查项 | 要求 | 本 BC 合规策略 | 状态 |
|--------|------|---------------|------|
| POM 继承 | 继承 `metaforge-parent` | api/core 子模块的 POM 继承 `metaforge-metadata`（Root），Root 的 POM 继承 `metaforge-parent` | PASS |
| 依赖声明 | 仅白名单依赖 + 禁止 `<version>` 标签 | 仅依赖 `metaforge-framework` + `metaforge-metamodel-api` + 白名单内 Spring Boot Starter；版本由 BOM 统一管理 | PASS |
| 禁止 `<dependencyManagement>` | BC 级 POM 不得声明 | 仅 Root POM 使用 `<dependencyManagement>` 管理子模块版本 | PASS |
| 模块注册 | `metaforge-boot/pom.xml` 注册 + 根 `pom.xml` `<modules>` | 在 `metaforge-parent/pom.xml` 的 `<modules>` 中新增 `<module>metaforge-metadata</module>` | PASS |

**Foundation Gate Verdict**: ALL checks PASS。本 BC 严格遵循 foundation-core 所有强制约束，不重复造轮子、不修改平台核心代码。

### Phase 1 设计后重检

*以下为 Phase 1 设计完成后（data-model、contracts、foundation-adaptation 产出后）的二次复核。*

| 检查项 | 设计决策 | 合规结论 |
|--------|---------|---------|
| 三表正交存储 | data-model.md 明确定义 metadata_entity / metadata_entity_draft / entity_version 三种实体，物理隔离、职责正交、无交叉状态 | PASS |
| FQN Generator | FqnGenerator 作为领域服务放置于 `core/domain/service/`，提供 `generateChildFqn` / `extractParentFqn` / `splitSegments` / `extractRootFqn` 四项标准 API；禁止 `String.join` 拼接；变更事件 `MetadataChangeEvent` 与监听器接口定义于 `api/event/` 供跨 BC 协作 | PASS |
| 常量集中管理 | `MetadataErrorCodes` 常量类放置于 `metaforge-metadata-api/constants/`，31000-31099 范围分配；所有业务异常引用常量 | PASS |
| 异常注册 | `MetadataExceptionHandler` 实现 `ExceptionHandlerSpi`，注册于 `infrastructure/spi/`；自定义异常继承 `BizException` | PASS |
| MapStruct 限制 | 仅在 `core/infrastructure/mapper/` 使用 MapStruct；`api` 模块、`domain` 层无 MapStruct 依赖 | PASS |
| 上游访问 | `MetamodelGatewayAdapter` 实现 `domain/repository/EntitySchemaRepository` 端口，通过 `metaforge-metamodel-api` 调用上游；不直接访问上游数据库 | PASS |
| 跨 BC 依赖 | `metaforge-metadata-api` 作为 SDK 发布；下游 BC 仅可依赖 `-api` 模块，禁止依赖 `-core` | PASS |
| REST 响应格式 | 所有 Controller 返回 `ApiResponse<T>`，使用 `@Tag(name = "metadata-management")` 分组 | PASS |
| 配置命名空间 | `metaforge.metadata.*` 前缀，通过 `@ConfigurationProperties(prefix = "metaforge.metadata")` 绑定 | PASS |
| 变更事件 | 使用 Spring `ApplicationEvent` + `@TransactionalEventListener(AFTER_COMMIT)`，不引入 MQ | PASS |
| 契约文档化 | contracts/ 下完成 Application Service (Java)、REST API、MCP 三种 OHS 契约的 Markdown 文档定义 | PASS |

**重检结论**: 所有设计决策与全局宪法 + BC 宪法 + foundation-core 合约完全一致，无新增违规项。Phase 1 设计通过。

## Project Structure

### Documentation (this feature)

```text
specs/001-metadata-full-lifecycle/
├── plan.md                   # 本文档（/speckit.plan 命令输出）
├── research.md               # Phase 0 输出（/speckit.plan 命令）
├── foundation-adaptation.md  # Phase 1 输出（foundation 合约导入后生成）
├── data-model.md             # Phase 1 输出（/speckit.plan 命令）
├── quickstart.md             # Phase 1 输出（/speckit.plan 命令）
├── contracts/                # Phase 1 输出（/speckit.plan 命令）
│   ├── application-service.md    # 进程内 Application Service 契约
│   ├── rest-api.md               # REST API 契约
│   └── mcp.md                    # MCP 契约
└── tasks.md                  # Phase 2 输出（/speckit.tasks 命令 - 非 /speckit.plan 创建）
```

### Source Code (BC root)

```text
metaforge-parent/metaforge-metadata/         # $BC_PATH — Root 聚合父模块
├── pom.xml                                  # 聚合父 POM（packaging: pom）
│
├── metaforge-metadata-api/                  # 子模块：契约层（SDK 发布）
│   ├── pom.xml
│   └── src/main/java/com/metaforge/metadata/api/
│       ├── constants/                       # 常量集中管理
│       │   ├── MetadataErrorCodes.java      #   错误码常量（31000-31099）
│       │   ├── MetadataStatusConstants.java #   状态常量（DRAFT/ACTIVE/DEPRECATED）
│       │   └── MetadataValidationConstants.java # 校验常量
│       ├── enums/                           # 枚举
│       │   ├── MetadataStatus.java          #   生命周期状态（DRAFT/ACTIVE/DEPRECATED）
│       │   ├── ImportStrategy.java          #   导入策略（SKIP/ERROR）
│       │   ├── ChangeType.java              #   变更事件类型（ACTIVATE/DEPRECATE）
│       │   └── DiffType.java                #   差异类型（ADDED/MODIFIED/DELETED）
│       ├── dto/                             # DTO
│       │   ├── request/                     #   请求 DTO
│       │   │   ├── CreateDraftRequest.java
│       │   │   ├── UpdateDraftContentRequest.java
│       │   │   ├── ActivateDraftRequest.java
│       │   │   ├── DeactivateEntityRequest.java
│       │   │   ├── MetadataQueryRequest.java
│       │   │   ├── AdminQueryRequest.java
│       │   │   ├── DiffRequest.java
│       │   │   ├── ImportRequest.java
│       │   │   └── ExportRequest.java
│       │   └── response/                    #   响应 DTO
│       │       ├── MetadataEntityDto.java
│       │       ├── MetadataEntityDraftDto.java
│       │       ├── EntityVersionDto.java
│       │       ├── VersionDiffDto.java
│       │       ├── ImportResultDto.java
│       │       ├── ExportResultDto.java
│       │       └── ValidationErrorDetailDto.java
│       ├── service/                         # Application Service 接口（@OpenHostService）
│       │   ├── MetadataDraftService.java     #   草稿管理服务
│       │   ├── MetadataActivationService.java # 版本生效/下线服务
│       │   ├── MetadataQueryService.java     #   查询检索服务
│       │   ├── MetadataHistoryService.java   #   历史版本追溯服务
│       │   ├── MetadataImportExportService.java # 批量导入导出服务
│       └── event/                           # 变更事件（跨 BC 协作使用）
│           ├── MetadataChangeEvent.java      #   元数据变更事件（Spring ApplicationEvent）
│           └── MetadataChangeListener.java   #   变更事件监听器接口（下游 BC 实现）
│
└── metaforge-metadata-core/                 # 子模块：实现层
    ├── pom.xml
    └── src/main/java/com/metaforge/metadata/
        ├── domain/                           # 领域层
        │   ├── model/
        │   │   ├── aggregate/               #   聚合根
        │   │   │   ├── MetadataEntity.java
        │   │   │   └── MetadataEntityDraft.java
        │   │   ├── entity/                  #   领域实体
        │   │   │   └── EntityVersion.java
        │   │   └── valueobject/             #   值对象
        │   │       ├── FQN.java
        │   │       ├── EntitySchemaFQN.java
        │   │       ├── JsonSchemaSnapshot.java
        │   │       └── VersionNumber.java
        │   ├── repository/                  #   Repository 接口（返回领域对象）
        │   │   ├── MetadataEntityRepository.java
        │   │   ├── MetadataEntityDraftRepository.java
        │   │   ├── EntityVersionRepository.java
        │   │   └── EntitySchemaRepository.java  # 上游访问端口
│       ├── service/                     #   领域服务
│       │   ├── FqnGenerator.java             #   统一 FQN 生成/解析工具类
│       │   ├── SchemaValidationService.java
│       │   ├── FqnUniquenessService.java
│       │   ├── DraftActivationService.java
│       │   ├── EntityDeactivationService.java
│       │   ├── VersionDiffService.java
│       │   ├── event/                          #   领域事件（变更通知）
│       │   │   └── MetadataEventPublisher.java     #   领域事件发布端口（接口）
        │   └── exception/                   #   领域异常
        │       ├── MetadataValidationException.java
        │       ├── FqnConflictException.java
        │       ├── EntityNotFoundException.java
        │       ├── DraftNotFoundException.java
        │       ├── ActivationFailedException.java
        │       └── DeactivationBlockedException.java
        │
        ├── application/                      # 应用层
        │   └── service/                     #   Application Service 实现
        │       ├── MetadataDraftServiceImpl.java
        │       ├── MetadataActivationServiceImpl.java
        │       ├── MetadataQueryServiceImpl.java
        │       ├── MetadataHistoryServiceImpl.java
        │       ├── MetadataImportExportServiceImpl.java
        │       └── MetadataEventServiceImpl.java
        │
        ├── infrastructure/                   # 基础设施层
        │   ├── config/                      #   配置类
        │   │   └── MetadataAutoConfiguration.java
        │   ├── persistence/
        │   │   ├── jpa/                     #   JPO（@Entity 注解的持久化对象）
        │   │   │   ├── MetadataEntityJpo.java
        │   │   │   ├── MetadataEntityDraftJpo.java
        │   │   │   └── EntityVersionJpo.java
        │   │   └── adapter/                #   Repository 实现（对象转换）
        │   │       ├── MetadataEntityRepositoryImpl.java
        │   │       ├── MetadataEntityDraftRepositoryImpl.java
        │   │       ├── EntityVersionRepositoryImpl.java
        │   │       └── MetamodelGatewayAdapter.java  # 上游 EntitySchema 访问适配器
        │   ├── mapper/                      #   MapStruct 转换器
        │   │   ├── MetadataEntityMapper.java
        │   │   ├── MetadataDraftMapper.java
        │   │   └── EntityVersionMapper.java
│       ├── spi/                         #   SPI 扩展点实现
│       │   ├── MetadataExceptionHandler.java  # ExceptionHandlerSpi 实现
│       │   └── MetadataHealthCheck.java       # HealthCheckSpi 实现
│       └── event/                        #   事件发布实现（Spring）
│           └── SpringMetadataEventPublisher.java  # 实现 MetadataEventPublisher 端口；通过 Spring ApplicationEventPublisher + MetadataChangeListener 双向分发
        │
        └── interfaces/                       # 接口层
            ├── rest/                        #   REST 控制器
            │   ├── MetadataDraftController.java
            │   ├── MetadataActivationController.java
            │   ├── MetadataQueryController.java
            │   ├── MetadataHistoryController.java
            │   └── MetadataImportExportController.java
            └── mcp/                         #   MCP 工具集
                └── MetadataMcpTools.java
```

**Structure Decision**:
- 选择结构类型：Monorepo 多模块子 BC（Option 4），与 metamodel BC 保持一致的三级 Maven 结构
- BC 相对路径（相对于 REPO_ROOT）：`metaforge-parent/metaforge-metadata/`
- 目录布局：Root 聚合 POM → `metaforge-metadata-api`（契约层/SDK）→ `metaforge-metadata-core`（实现层）
- 选择理由：与 metamodel BC 的模块拆分模式对齐，api 模块作为纯契约层对外发布 SD K，core 模块强依赖 api 并实现所有契约。符合 DDD 菱形架构规范——领域层纯净不感知 ORM/上游技术细节，基础设施层负责持久化适配与上游访问。
- 内部架构说明：`domain/repository/` 定义接口返回领域对象；`infrastructure/persistence/adapter/` 实现接口并负责 JPO ↔ 领域对象转换；JPO 存放于 `infrastructure/persistence/jpa/`，严禁泄露至 Domain 层。
- 跨 BC 依赖状况：上游依赖 `metaforge-metamodel-api`（通过 `MetamodelGatewayAdapter` 调用）；对外通过 `metaforge-metadata-api` 发布 SDK（供 `semantic-relation-network`、`semantic-query-engine`、`agent-consumption` 消费）。

**BC Boundary Confirmation**:
- 所有核心业务逻辑封装于 `$BC_PATH` 范围内，不直接引用其他 BC 的内部实现代码
- 导出合约：所有对外暴露的公共接口统一定义于 `$BC_PATH/context/contracts/`，由本 BC 维护，内部实现不暴露
- 导入合约：上游 BC 依赖仅使用 `$BC_PATH/context/upstream-contracts/metamodel-governance/` 中的合约文件，不跨 BC 直接代码调用
- 所有跨 BC 交互严格遵循合约规范
- Foundation 合规：所有 foundation 访问严格遵循 `foundation-adaptation.md` 设计，不修改 foundation-core 源码

## Complexity Tracking

> 无违规项需记录 —— Constitution Check 与 Foundation Check 全部 PASS。

| 违规项 | 为何必需 | 为何更简单的替代方案不够 |
|--------|---------|------------------------|
| — | — | — |
