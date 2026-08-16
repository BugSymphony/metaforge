# Implementation Plan: 认知基础架构层 (cognition-infrastructure)

**Branch**: `001-cognition-infrastructure` | **Date**: 2026-08-11 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-cognition-infrastructure/spec.md`

## Summary

构建 `metaforge-agent-cognition` BC，作为纯无状态计算与编排层，实现**模板驱动的认知算子编排引擎**。核心能力：通过 YAML 模板配置声明算子清单（operators），以统一路由入口按模板 ID 调度 8 个内置认知分类下的算子执行，产出可直接注入 LLM 上下文的结构化认知结果。本 BC 不持有数据存储主权，所有数据与计算能力通过 `api` 模块定义的 Port 接口契约（`MetamodelReadPort`/`MetadataReadPort`/`GraphReadPort`/`ComputeEngineReadPort`）从上游四个 BC 获取。对外通过 REST + MCP + Application Service 三重开放主机服务交付认知能力。

## Technical Context

**Language/Version**: Java 21 + Spring Boot 3（虚拟线程启用）

**Primary Dependencies**: Spring Boot Web, Spring AI (MCP Server), MapStruct, foundation-core (`metaforge-framework`), 上游 api 模块（`metaforge-metamodel-api`, `metaforge-metadata-api`, `metaforge-graph-api`, `metaforge-compute-engine-api`）

**Storage**: N/A（纯无状态，无自有数据表，无 JPA/jOOQ/Flyway）

**Testing**: JUnit 5 + Spring Boot Test + TestContainers（上游适配器集成测试）

**Target Platform**: Linux server，单 JVM 单体部署（Monorepo Maven 多模块架构）

**Project Type**: Monorepo 多模块子 BC（四级 Maven 结构：聚合父 → api/core/starter 三子模块）

**Performance Goals**: 认知查询端到端响应 ≤ 3 秒（不含上游 BC 网络延迟）；模板热加载 ≤ 5 秒；scope 过滤准确率 100%

**Constraints**: 纯无状态运行，不持有任何持久化状态；不依赖 LLM/向量相似度；强制双协议（REST+MCP）交付；零配置可用

**Scale/Scope**: MVP 阶段 6 个内置模板，8 种认知分类（封闭枚举），单用户单 Agent 调度

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Global Constitution (MUST 级 — 不可覆盖)

| # | Principle | Level | Compliance Strategy | Status |
|---|-----------|-------|---------------------|--------|
| I | 元模型唯一权威性 | MUST | 本 BC 通过 `MetamodelReadPort` 从 `metamodel-governance` BC 只读消费元模型定义，不创建、不修改、不缓存元模型数据。所有 Schema 查询走上游 api 契约。 | PASS |
| II | 显式导入边界管控 | MUST | scope 五字段中的 `bundles` 白名单即为 Agent 授权边界，经 `ScopeResolver` 校验，越界内容不输出并在 `context_meta.skipped_entities` 标注。 | PASS |
| III | 全链路权限过滤 | MUST | scope 贯穿 API 入参 → 校验 → 算子上下文 → 输出裁剪全管线，算子内部按 scope 裁剪查询范围，顶层 `context_meta.scope_applied` 记录实际应用 scope。 | PASS |
| IV | 版本统一收敛 | MUST | 通过 `DataVersionAnchor` 值对象记录各 Bundle 已发布版本号，`context_meta.version_anchors` 标注数据版本锚。不自行管理元模型版本。 | PASS |
| V | 纯组合无继承设计 | SHOULD | N/A — 本 BC 不定义元模型元素，不涉及组合/继承设计决策。 | PASS |
| VI | 合约化双协议标准接口 | SHOULD | **BC 宪法 Override 1 将其强化为 MUST**：REST 端点 + MCP Tool 双通道交付，任一缺失视为未完成。 | PASS |
| VII | Bundle 模块化治理 | SHOULD | 通过 scope.bundles 字段声明参与治理的 Bundle FQN 列表，不自行维护导出清单。 | PASS |
| VIII | Agent 友好型输出 | SHOULD | **BC 宪法 Override 2 细化增强**：输出支持 json（结构化 JSON）与 prompt（Markdown）双格式，语义完全等价、自包含（含完整 context_meta）、超限自动裁剪并显式标记。 | PASS |
| IX | 纯元数据边界坚守 | MUST | 本 BC 不存储任何业务数据，不持有数据主权。所有数据由调用方传入，输出结果自包含。自身配置以 YAML 文件形式持久化。 | PASS |
| X | 文档中文规范 | MUST | 本 plan 文档及所有 SDD 文档正文使用简体中文，术语（Bundle、MCP、REST、SPI、Port）保留英文。 | PASS |
| XI | 代码注释中文规范 | SHOULD | 生成代码时关键业务逻辑、复杂算法、接口说明处使用简体中文注释。 | PASS |

### BC-Specific Constitution

| # | Principle | Level | Compliance Strategy | Status |
|---|-----------|-------|---------------------|--------|
| I | 纯机制层定位 | MUST | 本 BC 仅承载模板解析、算子编排、scope 过滤、archetype 过滤、Token 裁剪、结果聚合等编排逻辑。所有具体认知算子实现（CognitionOperator）由 `metaforge-agent-cognition-dimensions` 模块（Step 1 暂移除、Step 3 重建）通过 SPI 挂载。 | PASS |
| II | 声明式扩展铁律 | MUST | 新增认知场景 = YAML 模板配置声明 + CognitionOperator SPI 实现 + 模板引用 operatorId。8 分类为封闭集合，不可通过配置扩展。新增能力只能在既有分类下新增算子。 | PASS |
| III | 契约与实现分层 | MUST | `-api` 模块仅含接口与数据结构（Port、SPI、枚举、DTO、常量），不依赖上游 core 模块。`-core` 依赖 `-api` 和上游 api 模块，不编译依赖 `-dimensions`。运行时通过 `@Autowired List<CognitionOperator>` 发现算子 Bean。 | PASS |
| IV | 统一认知入口 | MUST | 单一入口 `POST /api/v1/cognition/{templateId}`，REST/MCP/Application Service 三层共享同一模板路由语义。入参为确定性结构化参数，不接受自然语言。 | PASS |
| V | 无 LLM 依赖 | MUST | 所有认知分类、匹配、推理基于规则引擎与确定性算法，不调用 LLM API，不使用向量语义相似度。 | PASS |
| VI | 注册与校验治理 | SHOULD | `TemplateRegistry` 启动时扫描校验模板 YAML，失败跳过并记录 WARN，不影响已注册模板。`OperatorRegistry` 启动时校验算子类 category 声明的合法性。MVP 阶段算子仅启动时加载。 | PASS |
| VII | Scope 边界强制 | SHOULD | scope 五字段贯穿全管线：入参校验 → `ScopeResolver` 校验 → `CognitionQueryContext` 注入 → 算子内部裁剪 → `context_meta.skipped_entities` 标注越界。DELEGATE 模板产出 `delegated_scope` 子 Agent 入参。 | PASS |
| VIII | 输出自包含与等价双格式 | SHOULD | `OutputFormatter` SPI 支持 json/prompt 两种格式，`JsonOutputFormatter` 与 `PromptOutputFormatter` 语义完全等价。输出携带完整 `context_meta`（版本锚、scope、Token 估算、生成时间、跳过列表、裁剪标记）。 | PASS |

### Gate Verdict

ALL MUST & SHOULD principles **PASS**. 无违规项。BC 宪法 Override 1 (REST+MCP 双通道) 已纳入合规策略；Override 2 (Agent 友好型输出) 已通过 OutputFormatter SPI 与 context_meta 完整输出满足。

## Foundation Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Foundation contracts imported from: `$BC_PATH/context/foundation-contracts/foundation-core/`

| Capability | Contract | Compliance Strategy | Status |
|------------|----------|---------------------|--------|
| 虚拟线程 | platform-capabilities.md §1 | 不配置任何线程池，复用 foundation 提供的虚拟线程环境。 | PASS |
| 日志脱敏 | platform-capabilities.md §2 | 不自行配置日志脱敏规则。若有自定义敏感字段，通过 `LogMaskSpi` 扩展。 | PASS |
| API 文档 (SpringDoc) | platform-capabilities.md §3 | Controller 使用 `@Tag(name = "agent-cognition")` 声明 Swagger 分组，不自定义 SpringDoc 配置。 | PASS |
| 国际化 (i18n) | platform-capabilities.md §4 | 注入 `MessageSource` 使用，异常消息走国际化体系。不自定义 MessageSource bean。 | PASS |
| 可观测性 (Actuator) | platform-capabilities.md §5 | 不自定义 Actuator 配置。自定义健康检查通过 `HealthCheckSpi` 扩展。 | PASS |
| 安全基线 | platform-capabilities.md §6 | 不配置安全过滤器。本 BC API 无独立认证层，通过 scope 自我声明身份。 | PASS |
| 数据源 | platform-capabilities.md §7 | 不配置数据源/事务管理器。本 BC 无自有数据库，不执行任何数据库操作。 | PASS |
| 跨 Schema 写校验 | platform-capabilities.md §8 | N/A — 本 BC 无自有 Schema，不执行任何写操作。 | PASS |
| Flyway 迁移 | platform-capabilities.md §9 | 不配置 Flyway，不提供迁移脚本。本 BC 无数据表。 | PASS |
| 测试基类 | platform-capabilities.md §10 | 继承 `BaseUnitTest`/`BaseIntegrationTest` 编写测试。 | PASS |
| 统一响应格式 | api-contracts.md §2 | REST API 复用 `ApiResponse<T>` 标准格式，不自定义响应包装类。 | PASS |
| 分页组件 | api-contracts.md §2 | 复用 `PageRequest`/`PageResult<T>` 组件，不自定义分页 DTO。 | PASS |
| JSONB 序列化 | api-contracts.md §3 | 使用 `JsonbUtils` 工具类进行 JSONB 序列化，不自行实现。 | PASS |
| 缓存管理 | api-contracts.md §3 | 按需使用 foundation Caffeine 缓存（TemplateRegistry 内部缓存），key 命名遵循 `<bc-name>:<entity>:<id>` 约定。 | PASS |
| 异常处理 SPI | api-contracts.md §1/§4 | 自定义业务异常通过 `ExceptionHandlerSpi` 注册至全局异常处理器，错误码范围 34000-34099。不自定义全局异常切面。 | PASS |
| 构建系统集成 | build-system-integration.md | `-api`/`-core`/`-starter` 子模块继承 `metaforge-parent` POM，通过 `metaforge-framework` 依赖引入 foundation。`-starter` 作为 `metaforge-boot` 聚合入口。 | PASS |
| 配置 Schema | configuration-schema.md | BC 专用配置使用 `metaforge.agent-cognition.*` 前缀，写入 `application-agent-cognition.yml`。不覆盖 foundation 提供的全局配置项。 | PASS |

**Foundation Gate Verdict**: ALL PASS. 本 BC 严格遵循 foundation-core 合约约束，不重复实现任何平台级通用能力，不修改 foundation 核心代码。

## Project Structure

### Documentation (this feature)

```text
specs/001-cognition-infrastructure/
├── plan.md                   # 本文件
├── research.md               # Phase 0 输出
├── foundation-adaptation.md  # Phase 1 输出
├── data-model.md             # Phase 1 输出
├── quickstart.md             # Phase 1 输出
├── contracts/                # Phase 1 输出
│   ├── application-service.md
│   ├── rest-api.md
│   └── mcp-tools.md
└── tasks.md                  # Phase 2 输出（/speckit.tasks 命令生成）
```

### Source Code (BC root)

```text
metaforge-parent/metaforge-agent-cognition/
├── pom.xml                                    # BC 根聚合父 POM
├── metaforge-agent-cognition-api/             # 子模块: 契约层
│   ├── pom.xml
│   └── src/main/java/com/metaforge/agent/cognition/api/
│       ├── service/                           # Application Service 接口
│       │   └── CognitionQueryService.java
│       ├── dto/
│       │   ├── request/                       # 入参 DTO (CognitionRequest, Scope 等)
│       │   │   ├── CognitionRequest.java
│       │   │   └── Scope.java
│       │   └── response/                      # 出参 DTO (CognitionResponse, ContextMeta 等)
│       │       ├── CognitionResponse.java
│       │       └── ContextMeta.java
│       ├── enums/                             # 枚举
│       │   ├── DimensionCategory.java         # 8 分类枚举
│       │   ├── AgentArchetype.java            # 4 原型枚举
│       │   ├── OutputFormat.java              # 输出格式枚举
│       │   └── CognitionDepth.java            # 认知深度枚举
│       ├── constants/                         # 常量
│       │   └── AgentCognitionErrorCodes.java  # 异常码常量
│       ├── port/                              # 上游 BC 读取端口 Port 接口
│       │   ├── MetamodelReadPort.java
│       │   ├── MetadataReadPort.java
│       │   ├── GraphReadPort.java
│       │   └── ComputeEngineReadPort.java
│       └── spi/                               # 认知算子 SPI 接口
│           ├── CognitionOperator.java         # 算子 SPI
│           ├── CognitionQueryContext.java     # 算子查询上下文
│           ├── CognitionResult.java           # 算子执行结果
│           └── OutputFormatter.java           # 输出格式化 SPI
├── metaforge-agent-cognition-core/            # 子模块: 实现层
│   ├── pom.xml
│   └── src/main/java/com/metaforge/agent/cognition/core/
│       ├── interfaces/                        # 接口适配层 (REST/MCP)
│       │   ├── rest/
│       │   │   └── CognitionController.java
│       │   └── mcp/
│       │       └── CognitionMcpTools.java
│       ├── application/                       # 应用层
│       │   └── service/
│       │       └── CognitionQueryServiceImpl.java
│       ├── domain/                            # 领域层
│       │   ├── model/
│       │   │   ├── aggregate/
│       │   │   │   └── CognitionQuery.java
│       │   │   ├── entity/
│       │   │   │   ├── TemplateDefinition.java
│       │   │   │   └── OperatorDefinition.java
│       │   │   └── valueobject/
│       │   │       ├── TemplateId.java
│       │   │       ├── OperatorId.java
│       │   │       ├── Scope.java
│       │   │       ├── DataVersionAnchor.java
│       │   │       ├── TokenBudget.java
│       │   │       └── Priority.java
│       │   └── service/                       # 领域服务
│       │       ├── ScopeResolutionService.java
│       │       ├── TemplateResolutionService.java
│       │       ├── OperatorOrchestrationService.java
│       │       ├── OutputAssemblyService.java
│       │       ├── ContextMetaService.java
│       │       ├── DelegatedScopeService.java
│       │       ├── DepthTrimmingService.java
│       │       └── ArchetypeFilterService.java
│       └── infrastructure/                    # 基础设施层
│           ├── registry/
│           │   ├── TemplateRegistry.java
│           │   ├── TemplateScanner.java
│           │   ├── OperatorRegistry.java
│           │   └── FormatterRegistry.java
│           ├── adapter/                       # Port 接口适配器
│           │   ├── MetamodelReadPortAdapter.java
│           │   ├── MetadataReadPortAdapter.java
│           │   ├── GraphReadPortAdapter.java
│           │   └── ComputeEngineReadPortAdapter.java
│           ├── formatter/
│           │   ├── JsonOutputFormatter.java
│           │   └── PromptOutputFormatter.java
│           ├── exception/
│           │   └── AgentCognitionExceptionHandler.java
│           └── mapper/                        # MapStruct 转换器
│               └── CognitionDtoMapper.java
├── metaforge-agent-cognition-starter/         # 子模块: 聚合装配层
│   └── pom.xml                                # 聚合依赖 -api, -core（-dimensions/-templates 重构后加回）
└── context/
    ├── constitution.md                        # BC 宪法
    ├── upstream-contracts/                    # 上游契约导入
    │   ├── metamodel-governance/
    │   ├── metadata-management/
    │   ├── semantic-relation-network/
    │   └── semantic-query-engine/
    └── foundation-contracts/                  # 基础层契约导入
        └── foundation-core/
```

**Structure Decision**:
- **Selected structure type**: Monorepo 多模块子 BC (Option 4 + 六级结构)
- **BC relative path to REPO_ROOT**: `metaforge-parent/metaforge-agent-cognition/`
- **Real directory layout**: 四级 Maven 聚合（根聚合 → api/core/starter），`-core` 内部遵循 DDD 菱形端口-适配器架构（interfaces → application → domain ← infrastructure）
- **Selection rationale**: 契约与实现强制分离（api 可被下游 BC 安全依赖），starter 为 boot 提供单一依赖入口。Step 1 收拢地基：`-dimensions`/`-templates` 暂从构建移除，`-core` 独立承载认知接口、模板引擎、算子编排、输出组装；两者依据内置 agent 库元模型重构后加回。
- **Internal architecture note**: `-core` 严格遵循 DDD 分层：REST Controller/MCP Tool 置于 `interfaces`，Port 适配器置于 `infrastructure/adapter`，领域服务置于 `domain/service`，MapStruct 转换器置于 `infrastructure/mapper`。Port 接口定义于 `api` 模块（非 domain），供 `-core` 与未来 `-dimensions` 共享
- **Cross-BC dependency status**: 依赖 4 个上游 BC（metamodel-governance, metadata-management, semantic-relation-network, semantic-query-engine）；向下游 BC 导出 `metaforge-agent-cognition-api` 模块契约

**BC Boundary Confirmation**:
- 所有 BC 核心业务逻辑封装于 `$BC_PATH` 范围内，不直接引用 REPO_ROOT 下其他 BC 的内部实现代码
- 导出契约：所有对外公共接口统一定义在 `$BC_PATH/context/contracts/`，由本 BC 维护
- 导入契约：所有上游 BC 依赖仅使用 `$BC_PATH/context/upstream-contracts/` 下契约文件，不做跨 BC 直接代码调用
- 所有跨 BC 交互严格遵循契约规范，导入来源同时支持标准命令导入与手动添加
- Foundation 合规：所有 foundation 接入严格遵循 `foundation-adaptation.md` 设计，不修改 foundation 核心源码

## Complexity Tracking

无 Constitution Check 或 Foundation Check 违规项需要 justify。

