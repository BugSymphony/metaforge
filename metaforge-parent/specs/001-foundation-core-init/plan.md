# Implementation Plan: foundation-core 基座初始化

**Branch**: `001-foundation-core-init` | **Date**: 2026-08-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-foundation-core-init/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

构建 MetaForge 全平台统一开发脚手架与运行时基座 `foundation-core`。基于 Java 21 + Spring Boot 3 + Maven 多模块分层架构（metaforge-parent → metaforge-common → metaforge-framework → metaforge-server → metaforge-boot → 业务 BC），提供依赖版本治理、运行时环境标准化（虚拟线程、TraceId、统一响应体、全局异常处理）、横切技术能力（Caffeine 缓存、Jackson 序列化、参数校验、OpenAPI 文档、i18n、可观测性、安全基线）、SPI 六大扩展点机制、业务 BC 极简接入规范（4 步接入）及测试基座能力。所有能力默认生效，业务 BC 零配置接入。

## Technical Context

**Language/Version**: Java 21 (启用虚拟线程 Virtual Threads)。推荐使用 Java `record` 类型定义不可变数据载体（如 DTO、值对象、SPI 返回值），减少样板代码，提升可读性。例如 `HealthCheckSpi.HealthCheckResult`、`ApiResponse<T>` 的构造辅助、配置属性绑定类等场景均可使用 record 替代传统 POJO。

**Primary Dependencies**: Spring Boot 3.4.x (spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-validation, spring-boot-starter-actuator), Spring AI 1.0.0-M6 (MCP Server 发布)，Hibernate 6.x, Flyway 10.x, Jackson 2.18.x, Caffeine 3.x, PostgreSQL JDBC Driver 42.7.x, SpringDoc OpenAPI 2.7.x, TestContainers 1.20.x, JUnit 5

**Storage**: PostgreSQL 16（开发环境通过 docker-compose 提供），HikariCP 连接池，各 BC 共享单实例但通过独立 Schema 逻辑隔离。不引入 Redis 或其他外部缓存中间件。

**Testing**: JUnit 5 + Spring Boot Test + TestContainers（PostgreSQL 容器化集成测试）。metaforge-framework 提供 BaseUnitTest（纯单元测试基类，无 Spring 上下文）和 BaseIntegrationTest（集成测试基类，内置 TestContainers PostgreSQL 自动启停）。

**Target Platform**: Linux 服务器，单 JVM 进程运行。开发环境需 JDK 21 + Maven 3.9+ + Docker/Docker Compose。

**Project Type**: Maven 多模块单体仓库（Monorepo），基础设施/库类型 BC。最终以 metaforge-boot 为唯一启动入口打包为单一可执行 JAR。

**Performance Goals**: 应用冷启动 < 10s；REST API 单请求响应 < 200ms（不含 DB 查询）；BaseUnitTest 单测试 < 100ms；BaseIntegrationTest 含 TestContainers 首次启动 < 30s。

**Constraints**: metaforge-common 层 compile scope 依赖仅限 JDK 标准库 + Jackson（core + databind + annotations）+ SLF4J API，严禁引入 Spring Framework/Servlet API/JDBC/JPA/Caffeine/Flyway。业务 BC 不依赖 metaforge-server，通过 metaforge-framework 获得所需工具层。所有横切能力默认生效，不提供启用/禁用开关。

**Scale/Scope**: MVP 阶段 3-5 个业务 BC，元数据实体总量 ≤ 1000 条，并发 Agent ≤ 5 个。基础模块 5 个（common / framework / server / boot / bc-sample）。

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Global Constitution Compliance (MUST-level principles from `.specify/memory/global-constitution.md`)

| # | Principle | Level | Compliance Strategy | Status |
|---|-----------|-------|---------------------|--------|
| I | 元模型唯一权威性 | MUST | foundation-core 为纯技术基础设施 BC，不涉及元模型定义与管理。元模型治理能力归属下游 `metamodel-governance` BC。不冲突。 | PASS |
| II | 显式导入边界管控 | MUST | 不适用——foundation-core 不管理 Agent 导入授权。不冲突。 | PASS |
| III | 全链路权限过滤 | MUST | 不适用——foundation-core 不执行元数据查询/推理的权限过滤。不冲突。 | PASS |
| IV | 版本统一收敛 | MUST | 通过 metaforge-parent BOM 实现全平台依赖版本唯一仲裁（FR-001），符合语义化版本规范。 | PASS |
| IX | 纯元数据边界坚守 | MUST | foundation-core 仅提供通用技术能力，不存储任何业务元数据。FR-002 明确 common 层不引入 JDBC/JPA，证明无数据持有意图。 | PASS |
| X | 文档中文规范 | MUST | 所有 spec/plan/contract 文档正文使用简体中文。 | PASS |

### BC Constitution Compliance (`context/constitution.md`)

| # | Principle | Level | Compliance Strategy | Status |
|---|-----------|-------|---------------------|--------|
| I | 纯技术属性铁则 | MUST | foundation-core 仅承载通用技术能力（缓存、序列化、校验、文档等），不包含任何业务领域逻辑。FR-002/FR-003 明确禁止业务语义。 | PASS |
| II | 唯一基座原则 | MUST | foundation-core 为全局唯一技术基座，FR-025 禁止业务 BC 重复实现已提供的通用能力。 | PASS |
| III | 单向依赖铁则 | MUST | FR-005 定义严格单向分层依赖，metaforge-boot 通过 Maven Enforcer 强制禁止反向依赖。 | PASS |
| IV | 零侵入接入原则 | MUST | FR-023 定义 4 步接入，无需继承基类/实现接口。**与 BC 宪法的轻微差异**：BC 宪法提及 `metaforge.foundation.<capability>.enabled` 开关，但 spec FR-025 明确所有横切能力默认生效、不提供开关配置。spec 的澄清决定（Session 2026-08-01）已明确此策略变更。 | PASS (spec overrides) |
| V | 契约与兼容准则 | MUST | FR-030~FR-037 定义了 6 份契约文档，存放于 `specs/<feature_dir>/contracts/`。**差异说明**：BC 宪法要求契约存放于 `context/contracts/`，但 spec 澄清决定（Session 2026-08-01）明确 `context/contracts/` 仅供 `speckit.contract.export` 命令导出，plan/generate 阶段不得直接写入。 | PASS (spec overrides) |
| VI | SPI 扩展治理 | MUST | FR-021/FR-022 定义六大 SPI 扩展点，FR-022 确保 BC 级隔离。SPI 接口定义于 metaforge-common 的 contracts 包中。 | PASS |
| VII | 强制接入原则 | MUST | FR-023 要求业务 BC 依赖 metaforge-framework 并在 metaforge-boot 中注册 GAV。**差异说明**：BC 宪法原文使用 `foundation-core` 为依赖目标，spec 已将依赖目标细化为 `metaforge-framework`（传递提供 common + Spring/JPA/Web/Cache工具层），版本由 metaforge-parent BOM 统一管控。 | PASS (spec overrides) |
| VIII | 能力解耦 | SHOULD | FR-011~FR-017 各横切能力通过独立 AutoConfiguration 类实现，分别对应缓存/序列化/校验/文档/i18n/可观测性/安全。 | PASS |
| IX | 分层依赖约束 | SHOULD | FR-002/FR-002a 明确 common/framework 两层依赖边界。**差异说明**：BC 宪法要求 common 层严禁引入 Jackson（`import com.fasterxml.jackson`），spec FR-002 允许 Jackson + SLF4J API 作为基础序列化/日志门面。spec 澄清决定（Session 2026-08-01）明确此为有意识的设计决策：Jackson 为纯 API 级依赖，无容器耦合，确保 common 可在非 Spring 环境复用。BC 宪法中此原则为 SHOULD 级，spec 可覆盖。 | PASS (spec overrides) |
| X | 数据边界 | MUST | foundation-core 不持有业务数据。FR-020/FR-020a 明确数据访问仅提供通用技术封装，不定义业务 Schema。 | PASS |

### Gate Verdict

全局宪法 11 条 MUST/SHOULD 原则中 6 条适用，**全部 PASS**。BC 宪法 10 条原则中 3 条存在 spec 与 BC 宪法的设计演进差异（Principle IV 开关策略、Principle V 契约路径、Principle VII/IX 依赖目标与 common 依赖边界），均为 spec 澄清阶段有意识的设计决策修正，不构成违规。

## Foundation Check

*无导入的基础设施基础合约。foundation-core 自身即为基础设施基础 BC，不依赖其他基础合约。本 section 移除。*

## Constitution Re-check (Post Phase 1 Design)

*Phase 1 设计完成后重新评估：所有设计产物（data-model.md, contracts/*, quickstart.md）均符合 spec 要求与宪法约束。无新增违规。PASS。*

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md                   # This file (/speckit.plan command output)
├── research.md               # Phase 0 output (/speckit.plan command)
├── foundation-adaptation.md  # Phase 1 output, generated only if foundation contracts are imported
├── data-model.md             # Phase 1 output (/speckit.plan command)
├── quickstart.md             # Phase 1 output (/speckit.plan command)
├── contracts/                # Phase 1 output (/speckit.plan command)
└── tasks.md                  # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (BC root)

```text
# Monorepo Multi-module Sub-BC (Option 4 applied)
# Path relation: $BC_PATH = REPO_ROOT/metaforge-parent
# Root POM: metaforge-parent/pom.xml (Maven reactor root)
metaforge-parent/                          # $BC_PATH — BC root
├── pom.xml                                # 父 POM，BOM 依赖治理 + reactor 聚合 + flatten-maven-plugin
├── docker-compose.yml                     # PostgreSQL 16 开发环境 (FR-026)
│
├── metaforge-common/                      # 纯 Java 通用基础层 (FR-002)
│   ├── pom.xml
│   └── src/main/java/com/metaforge/common/
│       ├── exception/                     # 异常基类体系 (BaseException, SystemException, ValidationException, BizException)
│       ├── constant/                      # 全局常量池 (Constants, ErrorCodes)
│       ├── spi/                           # SPI 扩展点接口定义 (FR-021)
│       │   ├── ExceptionHandlerSpi.java
│       │   ├── RequestInterceptorSpi.java
│       │   ├── LogMaskSpi.java
│       │   ├── HealthCheckSpi.java
│       │   ├── SerializationSpi.java
│       │   └── ValidationSpi.java
│       ├── dto/                           # 通用 DTO (FR-030)
│       │   ├── ApiResponse.java           # 统一响应体
│       │   ├── PageRequest.java           # 分页请求 DTO
│       │   ├── PageResult.java            # 分页结果 DTO
│       │   ├── BaseEntity.java            # 基础实体 DTO（含审计字段）
│       │   └── TraceConstants.java        # TraceId 常量（Header名等）
│       └── util/                          # 通用工具类
│           ├── PageUtils.java             # 内存分页工具 (FR-019)
│           └── JsonbUtils.java            # Jackson JSONB 序列化工具 (FR-012)
│
├── metaforge-framework/                   # 框架工具层 (FR-002a)
│   ├── pom.xml
│   └── src/main/java/com/metaforge/framework/
│       ├── spring/                        # Spring 应用上下文工具
│       │   └── SpringContextHolder.java
│       ├── jpa/                           # JPA/Hibernate 查询辅助
│       │   └── JpaQueryHelper.java
│       ├── web/                           # Web 请求/响应工具
│       │   ├── RequestUtils.java
│       │   └── PageHelper.java            # common DTO ↔ Spring Pageable/Page 转换 (FR-019)
│       ├── cache/                         # 缓存抽象模板
│       │   └── CacheTemplate.java
│       └── test/                          # 测试基类 (FR-028/FR-029)
│           ├── BaseUnitTest.java          # 单元测试基类（无 Spring 上下文）
│           └── BaseIntegrationTest.java   # 集成测试基类（含 TestContainers PostgreSQL）
│
├── metaforge-server/                      # Spring Boot 自动装配层 (FR-003)
│   ├── pom.xml
│   └── src/main/java/com/metaforge/server/
│       ├── config/                        # AutoConfiguration 类（每项能力独立装配类）
│       │   ├── VirtualThreadConfig.java   # 虚拟线程配置 (FR-006)
│       │   ├── WebMvcConfig.java          # Web MVC 配置（含统一响应包装）
│       │   ├── JacksonConfig.java         # Jackson 全局序列化配置 (FR-012)
│       │   ├── CacheConfig.java           # Caffeine 缓存配置 (FR-011)
│       │   ├── DataSourceConfig.java      # 数据源 + HikariCP 连接池 (FR-018)
│       │   ├── FlywayConfig.java          # Flyway 迁移管理 (FR-020a)
│       │   ├── OpenApiConfig.java         # SpringDoc OpenAPI 3.0 配置 (FR-014)
│       │   ├── I18nConfig.java            # 国际化配置 (FR-015)
│       │   ├── ActuatorConfig.java        # Actuator 可观测性配置 (FR-016)
│       │   └── SecurityConfig.java        # 安全基线配置 (FR-017)
│       ├── web/                           # Web 层通用组件
│       │   ├── GlobalResponseBodyAdvice.java  # 统一响应体自动包装 (FR-007)
│       │   ├── GlobalExceptionHandler.java    # 全局异常统一拦截 (FR-009)
│       │   └── TraceIdFilter.java            # TraceId 生成与透传 (FR-008)
│       ├── spi/                           # SPI 注册与生命周期管理 (FR-021)
│       │   └── SpiRegistry.java
│       └── resources/
│           └── META-INF/spring/
│               └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│
├── metaforge-boot/                        # 启动模块（唯一运行入口）(FR-004)
│   ├── pom.xml
│   ├── src/main/java/com/metaforge/
│   │   └── MetaforgeApplication.java     # @SpringBootApplication + @ComponentScan("com.metaforge")
│   └── src/main/resources/
│       ├── application.yml               # 单文件配置 或 统一引入各 BC 配置
│       ├── application-bc-sample.yml     # bc-sample 专属配置示例 (FR-005c)
│       └── db/migration/                 # Flyway 迁移脚本扁平目录 (FR-020a)
│           ├── V1__bc_sample_ddl.sql     # 示例 BC 建表
│           └── V2__bc_sample_init.sql    # 示例 BC 初始化数据
│
├── bc-sample/                             # 示例业务 BC (FR-005d)
│   ├── pom.xml
│   └── src/main/java/com/metaforge/sample/
│       ├── controller/
│       │   └── SampleController.java     # 示例 REST Controller（带 @Tag）
│       ├── service/
│       │   └── SampleService.java        # 示例 Service（注入 CacheManager 等）
│       ├── repository/
│       │   └── SampleRepository.java     # 示例 JPA Repository
│       └── model/
│           └── SampleEntity.java         # 示例 JPA Entity
│
├── specs/001-foundation-core-init/        # FEATURE_DIR — 本文档目录
│   ├── spec.md                            # 特性规格
│   ├── plan.md                            # 本文件
│   ├── research.md                        # Phase 0 产物
│   ├── data-model.md                      # Phase 1 产物
│   ├── quickstart.md                      # Phase 1 产物
│   └── contracts/                         # Phase 1 产物（6 份契约）
│       ├── api-contracts.md
│       ├── rest-api-contract.md
│       ├── rest-api-contract.yaml
│       ├── platform-capabilities.md
│       ├── configuration-schema.md
│       └── build-system-integration.md
│
└── context/                               # BC 上下文（治理文档）
    ├── constitution.md                    # BC 宪法
    └── feature.json
```

**Structure Decision**:
- Selected structure type: Monorepo multi-module sub-BC (Option 4)
- BC relative path to REPO_ROOT: `metaforge-parent/`
- Real directory layout: 5 个平台模块（common / framework / server / boot / bc-sample）+ 业务 BC 模块平铺在 metaforge-parent 根目录下
- Selection rationale: foundation-core 为基础设施基础 BC，需要提供多模块分层架构供业务 BC 依赖。Maven reactor 聚合模式是标准实践，模块间通过父子 POM 管理依赖版本与构建顺序
- Internal architecture note: 严格单向分层依赖——boot → server → framework → common，boot → 全部 BC。业务 BC 依赖 framework（传递获得 common + Spring/JPA/Web/Cache 工具），不直接依赖 server
- Cross-BC dependency status: foundation-core 为最底层基础设施 BC，无上游业务 BC 依赖。向下游所有业务 BC 导出公共契约（6 份契约文档）

**BC Boundary Confirmation**:
- All core business logic of the current BC is encapsulated within the `$BC_PATH` scope, no direct reference to internal implementation code of other BCs under REPO_ROOT
- Export contracts: All public interfaces provided externally are uniformly defined under `$BC_PATH/context/contracts/` (post-export), maintained by this BC, and internal implementation is not exposed
- Import contracts: No upstream business BC dependencies (foundation-core is the infrastructure base layer)
- All cross-BC interactions strictly follow the contract specifications, and the import source supports both standard command import and manual addition

## Complexity Tracking
> **Fill ONLY if Constitution Check / Foundation Check has violations that must be justified**
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |