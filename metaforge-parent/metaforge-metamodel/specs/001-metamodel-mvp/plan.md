# Implementation Plan: 元模型治理核心能力 MVP

**Branch**: (none — 由 before_specify hook 创建) | **Date**: 2026-07-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-metamodel-mvp/spec.md`

## Summary

实现 metamodel-governance BC 的 M2 层元模型全生命周期管理能力：Bundle 版本治理、三层正交架构建模（EntitySchema / RelationSchema / AttributeTemplate）、Package 命名空间分类、FQN 全局标识体系、跨 Bundle 依赖管控、导出清单管理、声明式批量导入导出、FQN 前缀集合批量查询以及预置 `metaforge` 系统 Bundle。采用 Java 21 + Spring Boot 3 技术栈，严格遵循 DDD 菱形端口-适配器架构，通过 Maven 三级多模块结构（聚合根 → api / core 子模块）组织代码，三种开放主机服务（REST / MCP / Application Service）对外暴露能力，最大化复用 foundation-core 平台通用能力。

## Technical Context

| 维度 | 选择 | 说明 |
|------|------|------|
| **Language/Version** | Java 21 | 全局统一技术栈，启用虚拟线程 |
| **Framework** | Spring Boot 3 + Spring AI | REST API + MCP Server 发布 |
| **Build Tool** | Maven（多模块聚合） | 三级结构：BC 聚合根 → api + core 子模块 |
| **Storage** | PostgreSQL 16 | 单实例部署，Schema 级隔离 |
| **ORM** | Spring Data JPA + Hibernate | 持久化实体仅存放于 infrastructure 层 |
| **JSON Schema** | Jackson + foundation-core JsonbUtils | JSONB 序列化复用平台工具 |
| **Object Mapping** | MapStruct | DTO ↔ 领域对象 ↔ 持久化对象转换 |
| **Testing** | JUnit 5 + TestContainers | 继承 foundation-core BaseUnitTest / BaseIntegrationTest |
| **Target Platform** | Linux 服务器（单 JVM 进程） | Monorepo 单仓库多模块，最终打包单体 JAR |
| **Performance Goals** | CRUD < 200ms；Bundle 发布校验（100 元素）< 3s | 参考全局性能基线 |
| **Scale/Scope** | MVP: ≤ 5 Bundle，≤ 10 Package/Bundle，≤ 20 元素/Package | 单领域试点 |
| **API 模块契约约束** | `metaforge-metamodel-api` 仅含 DTO、接口、枚举、常量，禁止业务实现 | 对外 SDK 契约层，修改需向后兼容 |
| **常量管理** | 所有异常码、错误码、状态码统一定义在 api 模块 `constants/` 包 | 消除魔法值，单一数据源 |
| **FQN 生成约束** | 全 BC 通过 `FqnGenerator` 进行 FQN 生成/解析，禁止手动字符串拼接 | 确保全链路 FQN 语义一致 |
| **查询过滤** | 支持 EntitySchema / RelationSchema 按 FQN 前缀集合批量过滤（`List<String> fqnPrefixes`），分页 | 通过 Spring Data JPA Specification + B-tree 索引实现 |

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Principle | Level | Compliance Strategy | Status |
|---|-----------|-------|---------------------|--------|
| I | 元模型唯一权威性 | MUST (全局) | BC 作为元模型定义唯一写入入口，所有下游 BC 通过发布合约消费元模型结构。 | PASS |
| II | 显式导入边界管控 | MUST (全局) | 不直接实现——由 agent-consumption BC 执行。BC 通过导出清单提供授权依据。 | PASS |
| III | 全链路权限过滤 | MUST (全局) | 不直接实现——由 agent-consumption BC 执行。 | PASS |
| IV | 版本统一收敛 | MUST (全局) | Bundle 整体式版本固化，Package 与元素无独立版本号。草稿→已发布正向不可逆。 | PASS |
| V | 纯组合无继承设计 | SHOULD→MUST (BC Override) | EntitySchema 与 AttributeTemplate 仅支持组合挂载，不支持继承语义。 | PASS |
| VI | 合约化双协议标准接口 | SHOULD (全局) | REST API + MCP 双协议发布，内部 Application Service 进程内调用。 | PASS |
| VII | Bundle 模块化治理 | SHOULD→MUST (BC Override) | 导出清单白名单机制，未导出 Package 对外不可见。 | PASS |
| VIII | Agent 友好型输出 | SHOULD (全局) | 输出 JSON Schema Draft 2020-12 兼容格式。 | PASS |
| IX | 纯元数据边界坚守 | MUST (全局) | 仅存储元模型定义，不涉及具体业务数据。 | PASS |
| X | 文档中文规范 | MUST (全局) | 所有治理文档简体中文；术语保留英文。 | PASS |
| XI | 代码注释中文规范 | SHOULD (全局) | 关键业务逻辑、复杂算法、接口说明使用简体中文注释。 | PASS |
| I (BC) | 三层正交架构 | MUST (BC) | 治理管控层 / 核心语义层 / 属性定义层严格分离。 | PASS |
| II (BC) | 全局唯一标识 | MUST (BC) | FQN 为唯一标识，纯净 FQN 存储，派生字段不存储。 | PASS |
| III (BC) | 版本全生命周期 | MUST (BC) | 两态生命周期、enabled 推导、草稿创建限制、升级等级匹配校验。 | PASS |
| IV (BC) | 依赖治理 | MUST (BC) | 精确版本依赖、传递依赖约束、循环依赖零容忍。 | PASS |
| V (BC) | 导出即边界 | MUST (BC) | 导出清单唯一边界，RelationSchema 端点校验。 | PASS |
| VI (BC) | 纯组合复用 | MUST (BC) | 属性双态体系，发布时平铺合并生成 JSON Schema。 | PASS |
| VII (BC) | 属性标准对齐 | MUST (BC) | JSON Schema Draft 2020-12 规范子集（五类类型，不支持 object）。 | PASS |
| VIII (BC) | 校验前置 | MUST (BC) | 写入轻量校验 + 发布前全局校验两级体系，支持预览模式。 | PASS |
| IX (BC) | 预置不可侵犯 | MUST (BC) | metaforge Bundle 禁止删除与修改已发布核心结构。 | PASS |

**Gate Verdict**: ALL 20 principles (11 global + 9 BC-specific) **PASS**. No violations.

**Post-Design Re-check (2026-07-31)**: 全部原则仍为 PASS。新增的 name 字段（独立显示名）、embedding 字段（MVP 占位）、description 重命名、FqnGenerator 接口+实现分离模式、api 模块 `constants/` 常量集中管理、FQN 全链路强制使用生成器规则、FR-QRY-01~05 多维度过滤查询均未引入任何违规。

## Foundation Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Foundation Capability | BC Integration Strategy | Status |
|---|----------------------|------------------------|--------|
| F-1 | 虚拟线程 | 不配置任何线程池，直接使用 foundation-core 预配置的虚拟线程。 | PASS |
| F-2 | 日志脱敏 | 不配置自定义脱敏规则；如有特殊字段通过 LogMaskSpi 扩展。 | PASS |
| F-3 | OpenAPI 文档 | 仅使用 `@Tag(name = "metamodel")` 标注 Controller，不自定义 SpringDoc 配置。 | PASS |
| F-4 | 国际化 i18n | 注入 `MessageSource` 使用；BC 消息文件添加至 `i18n/` 目录。 | PASS |
| F-5 | 可观测性 | 不自定义 Actuator 配置；如需自定义健康检查，实现 HealthCheckSpi。 | PASS |
| F-6 | 安全基线 | 不配置安全过滤器，不使用自定义 CorsFilter。 | PASS |
| F-7 | 数据源 | 使用预配置 HikariCP + 统一事务管理器；BC 仅操作自身 Schema。 | PASS |
| F-8 | 跨 Schema 写校验 | 仅操作 `metamodel_governance` Schema，跨 Schema 读在合约范围内执行。 | PASS |
| F-9 | Flyway 迁移 | 迁移脚本按 `V<n>__metamodel_governance_ddl.sql` 和 `V<n+1>__metamodel_governance_init.sql` 命名提交。 | PASS |
| F-10 | 测试基类 | 继承 `BaseUnitTest` 和 `BaseIntegrationTest`，不引入 TestContainers 依赖。 | PASS |
| F-11 | 统一响应格式 | 使用 `ApiResponse<T>` 包装，不自定义响应包装类。 | PASS |
| F-12 | 分页组件 | 使用 `PageRequest` / `PageResult<T>` + `PageHelper`，不自定义分页 DTO。 | PASS |
| F-13 | JSONB 序列化 | 使用 `JsonbUtils.toJsonb()` / `fromJsonb()`，不自定义序列化实现。 | PASS |
| F-14 | 缓存 | 使用 Caffeine `CacheManager`，Key 命名: `metamodel:<entity>:<id>`。 | PASS |
| F-15 | 异常处理 | 自定义业务异常通过 `ExceptionHandlerSpi` 注册，错误码范围 30100-30199。 | PASS |
| F-16 | Maven 构建 | 继承 `metaforge-parent`，仅声明 `metaforge-framework` 依赖，不自定义版本。 | PASS |

**Gate Verdict**: ALL 16 foundation integration items **PASS**. See `foundation-adaptation.md` for detailed integration design.

## Project Structure

### Documentation (this feature)

```text
specs/001-metamodel-mvp/
├── spec.md                   # 功能规格说明
├── plan.md                   # 本文件
├── research.md               # Phase 0 技术调研
├── foundation-adaptation.md  # Phase 1 基础平台适配设计
├── data-model.md             # Phase 1 数据模型设计
├── quickstart.md             # Phase 1 快速验证指南
├── contracts/                # Phase 1 开放主机服务契约
│   ├── application-service.md
│   ├── rest-api.md
│   └── mcp-tools.md
└── tasks.md                  # Phase 2 任务拆解（/speckit.tasks 命令）
```

### Source Code (BC root)

```text
metaforge-parent/metaforge-metamodel/          # BC 根目录 = $BC_PATH
├── pom.xml                                    # 聚合父 POM
├── metaforge-metamodel-api/                   # api 子模块（契约层）
│   ├── pom.xml
│   └── src/main/java/com/metaforge/metamodel/api/
│       ├── dto/                                # 数据传输对象
│       │   ├── request/                        # 请求 DTO
│       │   └── response/                       # 响应 DTO
│       ├── enums/                              # 公共枚举
│       ├── constants/                          # 常量定义（错误码、状态码、业务常量 SSOT）
│       │   ├── ErrorCodes.java                 # 异常码常量
│       │   └── BusinessConstants.java          # 业务常量
│       └── service/                            # Application Service 接口
│           ├── BundleManagementService.java
│           ├── ElementDefinitionService.java
│           ├── PackageManagementService.java
│           ├── ExportManifestService.java
│           ├── ImportExportService.java
│           └── ValidationService.java
│
├── metaforge-metamodel-core/                  # core 子模块（实现层）
│   ├── pom.xml
│   └── src/main/java/com/metaforge/metamodel/
│       ├── interfaces/                        # 接口适配层（REST/MCP 入口）
│       │   ├── rest/                           # REST Controller
│       │   │   ├── BundleController.java
│       │   │   ├── EntitySchemaController.java
│       │   │   ├── RelationSchemaController.java
│       │   │   ├── AttributeTemplateController.java
│       │   │   ├── PackageController.java
│       │   │   ├── ExportManifestController.java
│       │   │   ├── ImportExportController.java
│       │   │   └── ValidationController.java
│       │   └── mcp/                            # MCP Tool Provider
│       │       └── MetamodelMcpTools.java
│       ├── application/                       # 应用层
│       │   └── service/                        # Application Service 实现
│       │       ├── BundleManagementServiceImpl.java
│       │       ├── BundleVersionManagementServiceImpl.java
│       │       ├── ElementDefinitionServiceImpl.java
│       │       ├── PackageManagementServiceImpl.java
│       │       ├── ExportManifestServiceImpl.java
│       │       ├── ImportExportServiceImpl.java
│       │       └── ValidationServiceImpl.java
│       ├── domain/                            # 领域层
│       │   ├── model/
│       │   │   ├── aggregate/                  # 聚合根
│       │   │   │   ├── Bundle.java
│       │   │   │   └── BundleVersion.java
│       │   │   ├── entity/                     # 领域实体
│       │   │   │   ├── Package.java
│       │   │   │   ├── EntitySchema.java
│       │   │   │   ├── RelationSchema.java
│       │   │   │   └── AttributeTemplate.java
│       │   │   └── valueobject/                # 值对象
│       │   │       ├── Fqn.java
│       │   │       ├── BundleCode.java
│       │   │       ├── SemanticVersion.java
│       │   │       ├── NativeAttribute.java
│       │   │       ├── AttributeDefinition.java
│       │   │       ├── AssociationType.java
│       │   │       ├── Cardinality.java
│       │   │       ├── UpgradeLevel.java
│       │   │       └── ValidationResult.java
│       │   ├── repository/                     # 仓储端口（纯接口）
│       │   │   ├── BundleRepository.java
│       │   │   ├── BundleVersionRepository.java
│       │   │   ├── PackageRepository.java
│       │   │   ├── EntitySchemaRepository.java
│       │   │   ├── RelationSchemaRepository.java
│       │   │   ├── AttributeTemplateRepository.java
│       │   │   ├── BundleDependencyRepository.java
│       │   │   └── ExportManifestRepository.java
│       │   └── service/                        # 领域服务
│       │       ├── FqnGenerator.java            # FQN 生成器接口
│       │       ├── FqnGeneratorImpl.java        # FQN 生成器实现（无状态 Bean）
│       │       ├── JsonSchemaCompiler.java
│       │       ├── AttributeMergeService.java
│       │       ├── CircularDependencyDetector.java
│       │       ├── BundleDependencyService.java
│       │       ├── ExportValidationService.java
│       │       ├── ExportService.java
│       │       ├── ImportService.java
│       │       ├── ValidationService.java
│       │       └── UpgradeLevelValidator.java
│       └── infrastructure/                    # 基础设施层
│           ├── persistence/
│           │   ├── adapter/                    # 仓储适配器（实现 domain/repository 接口）
│           │   │   ├── BundleRepositoryAdapter.java
│           │   │   ├── BundleVersionRepositoryAdapter.java
│           │   │   ├── PackageRepositoryAdapter.java
│           │   │   ├── EntitySchemaRepositoryAdapter.java
│           │   │   ├── RelationSchemaRepositoryAdapter.java
│           │   │   ├── AttributeTemplateRepositoryAdapter.java
│           │   │   ├── BundleDependencyRepositoryAdapter.java
│           │   │   └── ExportManifestRepositoryAdapter.java
│           │   └── jpa/                        # JPA 持久化实体（JPO）与 Spring Data DAO
│           │       ├── BundleJpo.java
│           │       ├── BundleVersionJpo.java
│           │       ├── PackageJpo.java
│           │       ├── EntitySchemaJpo.java
│           │       ├── RelationSchemaJpo.java
│           │       ├── AttributeTemplateJpo.java
│           │       ├── BundleDependencyJpo.java
│           │       ├── ExportManifestJpo.java
│           │       ├── BundleJpaRepository.java
│           │       ├── BundleVersionJpaRepository.java
│           │       ├── PackageJpaRepository.java
│           │       ├── EntitySchemaJpaRepository.java
│           │       ├── RelationSchemaJpaRepository.java
│           │       ├── AttributeTemplateJpaRepository.java
│           │       ├── BundleDependencyJpaRepository.java
│           │       └── ExportManifestJpaRepository.java
│           ├── mapper/                         # MapStruct 转换器
│           │   ├── BundleMapper.java
│           │   ├── BundleVersionMapper.java
│           │   ├── PackageMapper.java
│           │   ├── EntitySchemaMapper.java
│           │   ├── RelationSchemaMapper.java
│           │   └── AttributeTemplateMapper.java
│           ├── config/                         # BC 级配置
│           │   └── MetamodelProperties.java
│           └── spi/                            # Foundation SPI 扩展实现
│               ├── MetamodelExceptionHandler.java
│               └── MetamodelHealthCheck.java
│
│   ├── src/main/resources/                        # BC 级资源（如有）
│
├── src/test/java/com/metaforge/metamodel/
    ├── unit/                                   # 单元测试
    ├── integration/                            # 集成测试
    └── contract-export/                        # 导出合约测试
```

**Structure Decision**:
- **Structure type**: Monorepo multi-module sub-BC（Option 4 变体）
- **BC relative path**: `metaforge-parent/metaforge-metamodel/`
- **Module hierarchy**: BC 聚合根 POM → `api`（契约层）+ `core`（实现层）两子模块
- **Selection rationale**: BC 作为全平台语义源头，需对外暴露稳定契约（api 模块）供下游 BC 依赖，同时内部实现（core 模块）封装领域逻辑，禁止下游直接依赖。三级 Maven 结构满足 DDD 模块隔离约束与 foundation-core 构建规范。
- **Internal architecture**: DDD 菱形端口-适配器架构——`interfaces(rest/mcp) → application → domain ← infrastructure`
- **Cross-BC dependency status**: 被 `metadata-management`、`semantic-query-engine`、`semantic-relation-network`、`agent-consumption` 四个下游 BC 依赖。当前 BC 仅依赖 `foundation-core`（通过 `metaforge-framework`），无业务上游。

**BC Boundary Confirmation**:
- 当前 BC 所有核心业务逻辑封装在 `$BC_PATH` 范围内，不直接引用其他 BC 内部实现代码。
- 导出合约：对外公开接口统一定义在 `$BC_PATH/context/contracts/`，由本 BC 维护。
- Foundation 合规：所有 foundation 接入严格遵循 `foundation-adaptation.md` 设计，不修改 foundation-core 核心代码。

## Complexity Tracking

> 无 Constitution Check / Foundation Check 违规项，无需填写。

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (无违规项) | — | — |
