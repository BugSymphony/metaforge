# Implementation Plan: 语义关系实例全生命周期管理

**Branch**: `001-relation-instance-lifecycle` | **Date**: 2026-08-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-relation-instance-lifecycle/spec.md`

## Summary

实现 semantic-relation-network BC（模块代号 `metaforge-graph`）的关系实例全生命周期管理能力。核心架构采用 DDD 菱形端口-适配器模式，以「主表 + 草稿表 + 历史表」三表正交存储架构支撑 M1 层关系元数据的草稿编辑、版本生效、双向索引维护、拓扑生命周期管控与历史版本追溯。通过 Application Service / REST API / MCP 三种开放主机服务对外暴露能力，严格遵循 foundation-core 平台能力复用规范与跨 BC 依赖边界约束。

## Technical Context

**Language/Version**: Java 21 + Spring Boot 3（全 BC 启用虚拟线程）

**Primary Dependencies**: Spring Boot 3 (Web, JPA, Validation, Cache), Spring AI, PostgreSQL 16, Flyway, Caffeine, MapStruct 1.5+, Jackson (JSONB), JUnit 5 + TestContainers

**Storage**: PostgreSQL 16（`semantic_relation_network` Schema，三表架构：`relation_instance` 主表 + `relation_instance_draft` 草稿表 + `relation_version` 历史表）

**Testing**: JUnit 5 + Spring Boot Test + TestContainers（继承 `BaseIntegrationTest` / `BaseUnitTest`）

**Target Platform**: Linux 服务器，单 JVM 进程运行（Monorepo Maven 多模块，最终打包为单体应用）

**Project Type**: Monorepo 多模块子 BC（Maven 聚合父 POM + api + core 三级结构）

**Performance Goals**: 草稿创建 ≤50ms（含 JSON Schema 校验）、FQN 精准查询 ≤20ms、出入边查询 ≤50ms（百级结果集）、草稿生效四步事务 ≤100ms、批量导入 500 条 ≤6s、多维过滤查询 ≤100ms

**Constraints**: MVP 阶段单租户部署，不引入 Redis/MQ/Neo4j，不实现多版本并行发布，不实现审批工作流

**Scale/Scope**: MVP 1-2 个垂直业务领域试点，≤5 个 Bundle，元数据实体总量 ≤1000 条，并发消费 Agent ≤5 个

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### 全局宪法合规检查

| # | 原则 | 级别 | 合规策略 | 状态 |
|---|------|------|----------|------|
| I | 元模型唯一权威性 | MUST | 关系实例的创建与校验严格以 metamodel-governance BC 发布的 RelationSchema 为唯一权威约束来源，禁止创建脱离 Schema 约束的关系。通过 `metaforge-metamodel-api` 模块订阅已发布 RelationSchema。 | PASS |
| II | 显式导入边界管控 | MUST | 不负责 Agent 导入授权（由 agent-consumption BC 统一执行），仅提供标准化的拓扑查询结果。 | PASS |
| III | 全链路权限过滤 | MUST | 查询结果的最终权限过滤由 agent-consumption BC 执行，本 BC 提供原始拓扑数据。 | PASS |
| IV | 版本统一收敛 | MUST | 关系实例通过 `relation_schema_fqn`（含版本号）绑定元模型版本，遵循整体版本管理体系。 | PASS |
| V | 纯组合无继承设计 | SHOULD | 不涉及元模型结构设计，消费端直接使用上游发布的纯组合式 RelationSchema。 | PASS |
| VI | 合约化双协议标准接口 | SHOULD | BC 宪法已覆盖为 REST + Application Service + ApplicationEvent，MCP 由 agent-consumption 统一发布。详见 BC Override 1。 | PASS |
| VII | Bundle 模块化治理 | SHOULD | 不负责导出清单管理，仅消费上游发布的导出清单进行跨域关系校验。 | PASS |
| VIII | Agent 友好型输出 | SHOULD | BC 宪法已覆盖，查询输出为下游内部消费，非直接面向 Agent。详见 BC Override 2。 | PASS |
| IX | 纯元数据边界坚守 | MUST | 仅存储关系元数据（概念、关系类型、属性），不触碰具体业务交易数据。 | PASS |
| X | 文档中文规范 | MUST | 所有治理文档正文使用简体中文撰写，术语（Bundle、FQN、REST、MCP）、代码标识符保留英文。 | PASS |
| XI | 代码注释中文规范 | SHOULD | 关键业务逻辑、复杂算法、接口说明处使用简体中文注释；代码变量名、方法名使用英文。 | PASS |

### BC 级宪法合规检查

| # | 原则 | 级别 | 合规策略 | 状态 |
|---|------|------|----------|------|
| I | 关系实例一等公民原则 | MUST | 遵循统一 FQN 体系、三表存储架构、生命周期流程与治理规则。通过 `FqnGenerator` 统一生成关系 FQN。 | PASS |
| II | 三表正交存储原则 | MUST | 严格采用 `relation_instance`（主表）+ `relation_instance_draft`（草稿表）+ `relation_version`（历史表）三表架构，单表仅承载单一状态。 | PASS |
| III | 结构合规强制校验原则 | MUST | 所有写入操作均通过绑定的 RelationSchema JSON Schema 全字段校验，校验失败直接拦截。 | PASS |
| IV | 双向索引事务一致性原则 | MUST | 关系生效/下线时在同一事务内自动更新源实体出边索引与目标实体入边索引。 | PASS |
| V | 原子生效与事务回滚原则 | MUST | 草稿生效为原子事务：主表写入 → 历史表归档 → 草稿表删除 → 双向索引更新，四步一体。 | PASS |
| VI | 实体-关系拓扑同步原则 | MUST | 监听 metadata-management BC 的元数据变更事件，自动解析关联引用并生成对应关系实例。 | PASS |
| VII | 历史版本不可篡改原则 | MUST | 历史表仅允许 INSERT，通过数据库权限禁止 UPDATE/DELETE，全量归档内容包含完整快照。 | PASS |
| VIII | 草稿与生产物理隔离原则 | MUST | 编辑操作仅作用于草稿表，对外查询默认仅返回主表生效版本，草稿对外完全不可见。 | PASS |

### BC 覆盖项合规

| 覆盖项 | 父原则 | 覆盖内容 | 合规状态 |
|--------|--------|----------|----------|
| Override 1 | VI. 合约化双协议标准接口 (SHOULD) | 本 BC 通过 REST + Application Service + ApplicationEvent 三种接口发布能力，不独立暴露 MCP。 | PASS |
| Override 2 | VIII. Agent友好型输出 (SHOULD) | 查询输出目标为下游内部 BC，Agent 友好型格式化由 agent-consumption 统一完成。 | PASS |

**Gate Verdict**: 全部 MUST 与 SHOULD 原则 **PASS**。无违规项，无需记录复杂度追踪。

## Foundation Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | 能力域 | 合同要求 | 合规实施 | 状态 |
|---|--------|----------|----------|------|
| 1 | 构建系统集成 | BC POM 继承 metaforge-parent，仅依赖 `metaforge-framework`，禁止声明 `<dependencyManagement>` | api/core 子模块 POM 按标准模板构建；根 POM 使用聚合 `<modules>` 注册 | PASS |
| 2 | 数据源与事务 | 统一数据源，BC 仅操作自有 Schema，禁止定义独立数据源 bean | 复用 foundation-core 预配置的 HikariCP 连接池与 `@Transactional` | PASS |
| 3 | Flyway 迁移 | 迁移脚本遵循 `V<n>__<bc-name>_ddl.sql` 命名，统一放在 metaforge-boot | 提供 `V<n>__metaforge-graph_ddl.sql` + `V<n+1>__metaforge-graph_init.sql` | PASS |
| 4 | 虚拟线程 | 禁止自定义线程池配置 | 不配置任何 `ThreadPoolTaskExecutor`，完全复用平台虚拟线程 | PASS |
| 5 | 全局异常处理 | 通过 `ExceptionHandlerSpi` 注册自定义异常，禁止自定义 `@RestControllerAdvice` | 实现 `GraphExceptionHandlerSpi` 注册业务异常码映射 | PASS |
| 6 | 统一响应格式 | REST API 响应复用 `ApiResponse<T>`，禁止自定义响应包装类 | 所有 Controller 返回 `ApiResponse<T>`（由全局切面自动包装） | PASS |
| 7 | 分页组件 | 复用 `PageRequest` / `PageResult<T>`，禁止自定义分页 DTO | 查询接口使用 `PageRequest` 入参、`PageResult<T>` 出参 | PASS |
| 8 | JSONB 序列化 | 使用 `JsonbUtils` 进行 JSONB 字段序列化，禁止自定义实现 | `content` 字段序列化/反序列化统一调用 `JsonbUtils.toJsonb()/fromJsonb()` | PASS |
| 9 | 缓存 | 使用 Caffeine 缓存管理器，key 命名遵循 `<bc-name>:<entity>:<id>` | 按需使用 `CacheManager`，key 格式 `metaforge-graph:<entity>:<id>` | PASS |
| 10 | OpenAPI 文档 | 仅通过 `@Tag` 注解标注 Controller 分组，禁止自定义 SpringDoc 配置 | Controller 使用 `@Tag(name = "metaforge-graph")` 分组 | PASS |
| 11 | i18n 国际化 | 通过 `MessageSource` 注入使用，禁止自定义 MessageSource bean | 扩展 `messages_metaforge-graph_*.properties`，注入使用 | PASS |
| 12 | 健康检查 | 通过 `HealthCheckSpi` 注册自定义健康检查项 | 实现数据库连接状态检查 SPI | PASS |
| 13 | 测试基础设施 | 继承 `BaseUnitTest` / `BaseIntegrationTest`，不引入独立 TestContainers | 测试类继承 foundation 提供的测试基类 | PASS |
| 14 | 日志脱敏 | 禁止自定义日志脱敏工具，通过 `LogMaskSpi` 扩展 | 如需扩展脱敏规则，实现 `LogMaskSpi` | PASS |
| 15 | 跨 Schema 写入校验 | BC 仅操作 `semantic_relation_network` Schema | 所有表均归属自有 Schema，不跨 Schema 写操作 | PASS |
| 16 | 属性前缀配置 | 所有配置属性统一前缀 `metaforge.graph` | `application.yml` 中 BC 专属配置使用 `metaforge.graph.*` 前缀 | PASS |

**Gate Verdict**: 全部 Foundation Check 项 **PASS**。BC 严格遵循 foundation-core 平台能力复用规范，不重复实现已有通用能力，通过 SPI 扩展点接入自定义逻辑。

## Project Structure

### Documentation (this feature)

```text
specs/001-relation-instance-lifecycle/
├── plan.md                   # 本文件（实施计划）
├── research.md               # Phase 0 输出（技术调研）
├── foundation-adaptation.md  # Phase 1 输出（foundation-core 适配方案）
├── data-model.md             # Phase 1 输出（数据模型设计）
├── quickstart.md             # Phase 1 输出（验证指南）
├── contracts/                # Phase 1 输出（接口契约文档）
│   ├── application-service.md
│   ├── rest-api.md
│   └── mcp-tools.md
└── tasks.md                  # Phase 2 输出（/speckit.tasks 命令 - 非本命令生成）
```

### Source Code (BC root)

```text
# BC 模块结构：Monorepo 多模块子 BC（三级 Maven 结构）
# $BC_PATH = /data/ext/source-8/metaforge/metaforge-parent/metaforge-graph

metaforge-graph/                              # BC 根聚合父模块
├── pom.xml                                    # 聚合父 POM：统一管理子模块依赖版本、公共插件配置
│
├── metaforge-graph-api/                       # 【契约层】对外暴露的 SDK
│   ├── pom.xml                                # 依赖：无（纯接口定义模块）
│   └── src/main/java/com/metaforge/graph/api/
│       ├── dto/                               # 对外暴露的 DTO
│       │   ├── RelationInstanceDto.java       # 生效态关系实例 DTO
│       │   ├── RelationInstanceDraftDto.java  # 草稿态关系 DTO
│       │   ├── RelationVersionDto.java        # 历史版本 DTO
│       │   ├── CreateDraftRequest.java        # 创建草稿请求
│       │   ├── UpdateDraftContentRequest.java # 更新草稿内容请求
│       │   ├── RelationQueryRequest.java      # 多维过滤查询请求
│       │   └── ...
│       ├── enums/                             # 枚举定义
│       │   ├── RelationType.java              # 关系类型枚举（COMPOSITION/ASSOCIATION_REFERENCE/...）
│       │   ├── RelationStatus.java            # 关系状态枚举（DRAFT/ACTIVE/DEPRECATED）
│       │   └── ChangeType.java                # 变更类型枚举（ACTIVATED/DEPRECATED）
│       ├── constant/                          # 常量定义（异常码、错误码、业务常量 SSOT）
│       │   ├── GraphErrorCode.java            # 错误码常量（32000-32099）
│       │   └── GraphConstants.java            # 业务常量
│       ├── event/                             # 事件定义（对外发布契约）
│       │   ├── RelationChangeEvent.java       # 关系变更事件
│       │   └── RelationChangeListener.java    # 关系变更事件监听器接口（下游 BC 实现）
│       └── service/                           # Application Service 接口定义
│           ├── RelationDraftService.java
│           ├── RelationActivationService.java
│           ├── RelationQueryService.java
│           ├── RelationHistoryService.java
│           ├── RelationImportExportService.java
│           └── RelationTopologyService.java
│
├── metaforge-graph-core/                      # 【实现层】黑盒实现
│   ├── pom.xml                                # 依赖：metaforge-graph-api, metaforge-metamodel-api, metaforge-metadata-api
│   └── src/main/java/com/metaforge/graph/
│       ├── application/                       # 应用层
│       │   └── service/                       # Application Service 实现
│       │       ├── RelationDraftServiceImpl.java
│       │       ├── RelationActivationServiceImpl.java
│       │       ├── RelationQueryServiceImpl.java
│       │       ├── RelationHistoryServiceImpl.java
│       │       ├── RelationImportExportServiceImpl.java
│       │       └── RelationTopologyServiceImpl.java
│       │
│       ├── domain/                            # 领域层
│       │   ├── model/                         # 领域模型（三级分包）
│       │   │   ├── aggregate/                 # 聚合根
│       │   │   │   ├── RelationInstance.java          # 生效态关系聚合根
│       │   │   │   └── RelationInstanceDraft.java     # 草稿态关系聚合根
│       │   │   ├── entity/                    # 领域实体
│       │   │   │   └── RelationVersion.java           # 历史版本实体（或值对象）
│       │   │   └── valueobject/               # 值对象
│       │   │       ├── FQN.java                       # 关系 FQN
│       │   │       ├── RelationSchemaFQN.java         # 元模型 FQN（含版本号）
│       │   │       ├── EntityFQN.java                 # 实体 FQN
│       │   │       ├── VersionNumber.java             # 版本号
│       │   │       ├── JsonSchemaSnapshot.java        # JSON Schema 快照
│       │   │       ├── CardinalityRule.java           # 基数约束
│       │   │       ├── RelationName.java              # 关系名称
│       │   │       ├── RelationDescription.java       # 关系描述
│       │   │       └── ContentSnapshot.java           # 属性内容快照
│       │   ├── service/                       # 领域服务
│       │   │   ├── RelationSchemaValidationService.java   # JSON Schema 结构校验
│       │   │   ├── CardinalityValidationService.java      # 基数约束校验
│       │   │   ├── DependencyCheckService.java             # 依赖关系校验
│       │   │   └── FqnGenerator.java                      # FQN 生成器
│       │   ├── repository/                    # 领域仓储端口（接口定义）
│       │   │   ├── RelationInstanceRepository.java        # 主表仓储端口
│       │   │   ├── RelationInstanceDraftRepository.java   # 草稿表仓储端口
│       │   │   ├── RelationVersionRepository.java         # 历史表仓储端口
│       │   │   ├── RelationSchemaRepository.java          # 上游元模型访问端口（领域层抽象）
│       │   │   └── MetadataEntityGateway.java             # 上游元数据访问端口（领域层抽象）
│       │   └── event/                         # 领域事件发布端口（接口定义）
│       │       └── RelationEventPublisher.java            # 领域事件发布器接口
│       │
│       ├── infrastructure/                    # 基础设施层
│       │   ├── persistence/                   # 持久化适配
│       │   │   ├── jpa/                       # JPA 持久化实体(JPO) + DAO
│       │   │   │   ├── RelationInstanceJpo.java
│       │   │   │   ├── RelationInstanceDraftJpo.java
│       │   │   │   ├── RelationVersionJpo.java
│       │   │   │   ├── RelationInstanceJpaRepository.java
│       │   │   │   ├── RelationInstanceDraftJpaRepository.java
│       │   │   │   └── RelationVersionJpaRepository.java
│       │   │   └── adapter/                   # 仓储适配器实现
│       │   │       ├── RelationInstanceRepositoryAdapter.java
│       │   │       ├── RelationInstanceDraftRepositoryAdapter.java
│       │   │       ├── RelationVersionRepositoryAdapter.java
│       │   │       ├── MetamodelGatewayAdapter.java       # 元模型访问适配器（调用 metamodel api）
│       │   │       └── MetadataGatewayAdapter.java        # 元数据访问适配器（调用 metadata api）
│       │   ├── event/                         # 事件基础设施
│       │   │   └── SpringRelationEventPublisher.java      # Spring 事件发布器实现
│       │   └── converter/                     # MapStruct 对象转换器
│       │       ├── RelationInstanceConverter.java         # DTO ↔ 领域对象 ↔ JPO
│       │       ├── RelationDraftConverter.java
│       │       └── RelationVersionConverter.java
│       │
│       ├── interfaces/                        # 接口适配层（入站适配器）
│       │   ├── rest/                          # REST Controller
│       │   │   ├── RelationDraftController.java
│       │   │   ├── RelationActivationController.java
│       │   │   ├── RelationQueryController.java
│       │   │   ├── RelationHistoryController.java
│       │   │   └── RelationImportExportController.java
│       │   ├── event/                         # 事件监听器实现（消费上游事件）
│       │   │   └── MetadataChangeEventListener.java      # 监听 metadata-management 实体变更
│       │   └── mcp/                           # MCP 工具提供者
│       │       ├── RelationQueryMcpTools.java             # 关系查询 MCP 工具
│       │       └── RelationTopologyMcpTools.java          # 拓扑查询 MCP 工具
│       │
│       └── config/                            # BC 配置（SPI 注册等）
│           ├── GraphExceptionHandlerSpi.java              # 自定义异常处理器 SPI
│           └── GraphHealthCheckSpi.java                   # 健康检查 SPI
│
├── src/test/java/com/metaforge/graph/          # 测试代码
│   ├── unit/                                   # 单元测试
│   │   ├── domain/                             # 领域逻辑测试
│   │   └── application/                        # 应用服务测试
│   ├── integration/                            # 集成测试
│   │   ├── persistence/                        # 持久化集成测试
│   │   └── rest/                               # REST API 集成测试
│   ├── contract-export/                        # 对外契约测试
│   └── contract-adapt/                         # 上游对接适配测试
│
└── context/                                    # BC 上下文资产（设计与治理文档）
    ├── constitution.md                         # BC 宪法
    ├── contracts/                              # 本 BC 对外发布的公共接口契约
    ├── upstream-contracts/                     # 导入的上游 BC 契约（只读）
    │   ├── metamodel-governance/
    │   └── metadata-management/
    └── foundation-contracts/                   # 导入的基础设施契约（只读）
        └── foundation-core/
```

**Structure Decision**:
- **Selected structure type**: Monorepo 多模块子 BC（Option 4 变体——三级 Maven 聚合结构）
- **BC relative path to REPO_ROOT**: `metaforge-parent/metaforge-graph`
- **Selection rationale**: 遵循 DDD 菱形架构，api（契约层）与 core（实现层）物理隔离，确保下游 BC 仅依赖 api 模块，core 模块作为黑盒实现不对外暴露。Maven 三级聚合结构（根聚合 → api/core 子模块）统一依赖版本治理与插件配置。
- **Internal architecture note**: core 模块内严格遵循 `interfaces → application → domain ← infrastructure` 单向向内依赖规则。领域层按 aggregate/entity/valueobject 三级分包。持久化采用 domain repository（端口）→ infrastructure persistence adapter（适配器）→ jpa（JPO）三层范式。
- **Cross-BC dependency status**: 
  - 强依赖 2 个上游 BC：`metamodel-governance`（通过 `metaforge-metamodel-api`）与 `metadata-management`（通过 `metaforge-metadata-api`）
  - 对外暴露 3 种开放主机服务：Application Service（6 个接口）、REST API（5 个 Controller）、MCP Tools（2 组工具）
  - 对外发布事件：RelationChangeEvent（ACTIVATED/DEPRECATED）
  - 消费上游事件：MetadataChangeEvent（ACTIVATE/DEPRECATE）

**BC Boundary Confirmation**:
- 本 BC 所有核心业务逻辑封装在 `metaforge-graph/` 范围内，不直接引用其他 BC 的内部实现
- 对外契约：所有公共接口定义在 `metaforge-graph-api/` 模块及 `context/contracts/` 文档中
- 上游依赖：仅通过 `metaforge-metamodel-api` 与 `metaforge-metadata-api` 模块消费上游契约，禁止依赖上游 core 模块
- Foundation 合规：所有 foundation 接入严格遵循 `foundation-adaptation.md` 设计，不修改 foundation-core 源码

## Complexity Tracking

> 无 Constitution Check / Foundation Check 违规项，无需填写。
