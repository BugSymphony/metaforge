# Implementation Plan: 元认知指导层

**Branch**: `001-agent-cognition-guidance` | **Date**: 2026-08-01 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-agent-cognition-guidance/spec.md`

## Summary
元认知指导层（Metaforge Agent Cognition）是一个纯无状态计算与编排层，为下游 Agent 提供结构化的元认知上下文。系统采用统一模板驱动架构，将 5 个查询端点收敛为单一 `/{templateId}` 入口，支持 14 种认知视角的编排执行。技术实现遵循 DDD 菱形架构（interfaces → application → domain ← infrastructure），通过端口-适配器模式隔离 4 个上游 BC 依赖，使用 MapStruct 统一对象转换，Caffeine 实现内存缓存，Spring AI MCP 暴露工具接口。整个 BC 不持有任何数据存储主权，所有数据通过上游 BC 的 api 模块按需获取。

## Technical Context
**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3, Spring AI (MCP), MapStruct, metaforge-framework (foundation-core)
**Storage**: N/A — 纯无状态计算与编排层，不持有任何数据存储主权，无自有数据表。Caffeine 内存缓存（仅缓存 GuidanceResult，TTL 30min，重启即清空）属瞬态运行时优化，不构成持久化数据存储。
**Testing**: JUnit 5 + Spring Boot Test + TestContainers
**Target Platform**: Linux server, 单 JVM 进程部署
**Project Type**: Monorepo multi-module sub-BC
**Performance Goals**: 一站式简报生成(L2/10视角) ≤500ms; stepGuide(实体级) ≤150ms; 变更影响感知 ≤200ms
**Constraints**: 无 LLM、无向量、不接受自然语言、无状态幂等、单视角超时 200ms、max_tokens 默认 8000、配置需重启生效
**Scale/Scope**: MVP: concurrent agents ≤5, metadata entities ≤1000, relation edges ≤500

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| 无状态纯计算 | PASS | BC 不持有数据存储，不引入 JPA/MyBatis，所有数据从上游 BC 的 api 模块获取 |
| 模板驱动 | PASS | 所有认知场景通过 `cognition-templates.yml` 定义，`TemplateRegistryService` 集中管理 |
| 原子视角组合 | PASS | 14 个视角独立实现，每视角 <300 行，通过 `PerspectiveOrchestrationService` 编排 |
| 确定性输出 | PASS | 无 LLM 调用，无随机采样，相同输入总是产生相同输出 |
| Token 预算控制 | PASS | `TokenBudgetService` 按配置截断输出，max_tokens 默认 8000 |
| 配置驱动 | PASS | 模板、视角权重、超时、缓存策略全部外部化到 YAML/properties |
| 编译期依赖隔离 | PASS | domain 层不依赖具体框架（Spring MVC/WebFlux/JPA），通过端口接口隔离上游 |
| 版本锚定 | PASS | 支持 `DataVersionAnchor` 作为缓存键和响应版本标记 |

## Foundation Check

| Check | Status | Notes |
|-------|--------|-------|
| 配置前缀 `metaforge.agent-cognition` | PASS | `AgentCognitionProperties` 使用 `@ConfigurationProperties(prefix = "metaforge.agent-cognition")` |
| SPI 自动注册 | PASS | `AgentCognitionExceptionHandler` 实现 foundation-core 的 `ExceptionHandlerSpi` 接口 |
| 响应模板统一 `ApiResponse<T>` | PASS | Controller 返回类型统一为 `ApiResponse<CognitionResult>` |
| 分页支持 `PageRequest` | PASS | `instanceCatalog` 视角内支持 `PageRequest` 分页参数 |
| JSONB 序列化 `JsonbUtils` | PASS | 缓存序列化、模板序列化使用 foundation-core 的 `JsonbUtils` |
| i18n 消息 | PASS | 错误消息使用 `MessageSource` + `LocaleContextHolder` |
| 虚拟线程 | PASS | Spring Boot 3 默认启用虚拟线程，视角并行执行受益 |
| SpringDoc API 文档 | PASS | Controller 方法添加 `@Operation` 注解 |
| 依赖方向合规 | PASS | core → api (允许)，core → foundation-core (允许)，core → 上游 BC api (允许，通过domain/port隔离) |

## Project Structure

### Documentation
```text
specs/001-agent-cognition-guidance/
├── spec.md                       # 功能规格（6 US / 40 FR / 12 SC）
├── plan.md                       # 本实现计划
├── research.md                   # 技术调研与技术选型依据
├── foundation-adaptation.md      # foundation-core 能力适配分析
├── data-model.md                 # 领域模型与数据字典（实体定义/TemplateId/DataVersionAnchor 等）
├── quickstart.md                 # 消费端接入与快速验证指南
├── contracts/                    # 挂载契约（OHS）
│   ├── application-service.md    # 统一执行入口 / 模板注册表
│   ├── rest-api.md               # POST /api/v1/cognition/{templateId}
│   └── mcp-tools.md              # cognition_execute 工具定义
└── checklists/
    └── requirements.md           # 需求质量核对清单
```
> 领域模型、实体字段定义与数据字典以 `data-model.md` 为权威来源；模板 ID 与统一入口契约以 `contracts/rest-api.md` 和 `contracts/application-service.md` 为准。

### Source Code (BC root)
```text
metaforge-agent-cognition/                    # BC根聚合父POM
├── pom.xml                                   # 聚合父POM（<modules>声明子模块，继承metaforge-parent）
├── metaforge-agent-cognition-api/            # 契约层（api子模块）
│   ├── pom.xml
│   └── src/main/java/com/metaforge/agent/cognition/api/
│       ├── service/                          # Application Service接口
│       │   ├── CognitionQueryService.java    # 统一认知执行入口
│       │   ├── TemplateRegistryService.java  # 模板注册表接口
│       │   └── CognitionOutputService.java   # 输出格式转换接口
│       ├── dto/
│       │   ├── request/                      # 请求DTO
│       │   │   ├── CognitionRequest.java     # 统一认知查询请求
│       │   │   ├── TaskBriefRequest.java     # 一站式简报请求
│       │   │   ├── StepGuideRequest.java     # 实体即时指导请求
│       │   │   ├── NavigateRequest.java      # 渐进式导航请求
│       │   │   └── SubTaskBriefRequest.java  # 子任务简报请求
│       │   └── response/                     # 响应DTO
│       │       ├── GuidanceResult.java       # 统一查询输出聚合根
│       │       ├── ContextMeta.java          # 上下文元信息
│       │       ├── DataVersionAnchor.java    # 数据版本锚
│       │       ├── AdjacentContext.java      # 局部导航上下文
│       │       ├── StepGuidanceResult.java   # 步骤指导结果
│       │       ├── EntityProfile.java        # entity_profile 视角
│       │       ├── DomainLocation.java       # domain_location 视角
│       │       ├── CompositionTree.java      # composition_tree 视角
│       │       ├── RelationshipGraph.java    # relationship_graph 视角
│       │       ├── ConstraintSet.java        # constraint_set 视角
│       │       ├── CapabilityCatalog.java    # capability_catalog 视角
│       │       ├── FlowBlueprint.java        # flow_blueprint 视角
│       │       ├── DecisionMatrix.java       # decision_matrix 视角
│       │       ├── ImpactTrace.java          # impact_trace 视角
│       │       ├── PrerequisiteChain.java    # prerequisite_chain 视角
│       │       ├── BundleDirectory.java      # bundle_directory 视角
│       │       ├── DomainNavigation.java     # domain_navigation 视角
│       │       ├── InstanceCatalog.java      # instance_catalog 视角
│       │       ├── SchemaInventory.java      # schema_inventory 视角
│       │       ├── TaskBriefResponse.java    # taskBrief 端点响应
│       │       ├── StepGuideResponse.java    # stepGuide 端点响应
│       │       ├── NavigateResponse.java     # navigate 端点响应
│       │       └── BundleCatalogResponse.java # bundleCatalog 端点响应
│       ├── perspective/                      # 视角执行器SPI（供第三方插件开发）
│       │   ├── PerspectiveExecutor.java      # 视角执行器接口（插件扩展点）
│       │   └── PerspectiveExecutionContext.java # 视角执行上下文
│       ├── enums/                            # 枚举
│       │   ├── CognitionDepth.java           # L1/L2/L3 三级深度
│       │   ├── AgentArchetype.java           # execution/exploration/audit/orchestration
│       │   ├── PerspectiveCode.java          # 14 个视角编码
│       │   ├── ScopeMode.java                # INHERITED/PURE
│       │   ├── OutputFormat.java             # JSON/PROMPT
│       │   ├── ContextMode.java              # BUNDLE_LEVEL/ENTITY_LEVEL
│       │   ├── PerspectiveScope.java         # ENTITY/BUNDLE/BOTH
│       │   └── ConstraintLevel.java          # MANDATORY/RECOMMENDED/REFERENCE
│       └── constant/
│           └── AgentCognitionErrorCodes.java  # 错误码常量（34001-34006）
├── metaforge-agent-cognition-core/           # 实现层（core子模块）
│   ├── pom.xml
│   └── src/main/java/com/metaforge/agent/cognition/core/
│       ├── interfaces/                       # 接口适配层
│       │   ├── rest/
│       │   │   └── CognitionController.java  # REST端点 POST /api/v1/cognition/{templateId}
│       │   └── mcp/
│       │       └── CognitionMcpTools.java    # MCP工具集 cognition_execute
│       ├── application/                      # 应用层
│       │   ├── service/
│       │   │   ├── CognitionQueryServiceImpl.java   # 核心编排实现
│       │   │   ├── TemplateRegistryServiceImpl.java # 模板注册表实现
│       │   │   ├── CognitionOutputServiceImpl.java  # 输出格式转换实现
│       │   │   └── ChangeWatchServiceImpl.java      # 变更影响感知实现
│       │   ├── executor/                     # 视角执行器注册与实现
│       │   │   ├── PerspectiveRegistry.java  # 执行器注册表（注入 api 模块的 PerspectiveExecutor 插件）
│       │   │   └── impl/                     # 14 个内置视角执行器实现
│       │   │       ├── EntityProfileExecutor.java
│       │   │       ├── DomainLocationExecutor.java
│       │   │       ├── CompositionTreeExecutor.java
│       │   │       ├── RelationshipGraphExecutor.java
│       │   │       ├── ConstraintSetExecutor.java
│       │   │       ├── CapabilityCatalogExecutor.java
│       │   │       ├── FlowBlueprintExecutor.java
│       │   │       ├── DecisionMatrixExecutor.java
│       │   │       ├── ImpactTraceExecutor.java
│       │   │       ├── PrerequisiteChainExecutor.java
│       │   │       ├── DomainNavigationExecutor.java
│       │   │       ├── InstanceCatalogExecutor.java
│       │   │       ├── BundleDirectoryExecutor.java
│       │   │       └── SchemaInventoryExecutor.java
│       │   └── assembler/
│       │       └── OutputAssembler.java      # 输出组装器（裁剪、排序、格式转换）
│       ├── domain/                           # 领域层（框架无关）
│       │   ├── model/
│       │   │   ├── aggregate/                # 聚合根
│       │   │   │   ├── GuidanceResult.java   # 统一查询输出聚合根
│       │   │   │   └── CognitionQuery.java   # 认知查询聚合根
│       │   │   ├── entity/                   # 领域实体（14 个视角输出载体）
│       │   │   │   ├── EntityProfile.java
│       │   │   │   ├── DomainLocation.java
│       │   │   │   ├── CompositionTree.java
│       │   │   │   ├── RelationshipGraph.java
│       │   │   │   ├── ConstraintSet.java
│       │   │   │   ├── CapabilityCatalog.java
│       │   │   │   ├── FlowBlueprint.java
│       │   │   │   ├── DecisionMatrix.java
│       │   │   │   ├── ImpactTrace.java
│       │   │   │   ├── PrerequisiteChain.java
│       │   │   │   ├── BundleDirectory.java
│       │   │   │   ├── DomainNavigation.java
│       │   │   │   ├── InstanceCatalog.java
│       │   │   │   ├── SchemaInventory.java
│       │   │   │   └── PerspectiveResult.java
│       │   │   └── valueobject/              # 值对象
│       │   │       ├── TemplateId.java       # 模板标识符
│       │   │       ├── PerspectiveCode.java  # 视角编码
│       │   │       ├── PerspectiveScope.java # 视角作用域
│       │   │       ├── CognitionDepth.java   # 认知深度
│       │   │       ├── AgentArchetype.java   # 代理原型
│       │   │       ├── ContextMode.java      # 上下文模式
│       │   │       ├── ScopeMode.java        # 层级化作用域模式
│       │   │       ├── OutputFormat.java     # 输出格式
│       │   │       ├── ConstraintLevel.java  # 约束级别
│       │   │       ├── DataVersionAnchor.java # 数据版本锚
│       │   │       ├── ContextMeta.java      # 上下文元信息
│       │   │       ├── AdjacentContext.java  # 局部导航上下文
│       │   │       ├── QueryParameters.java  # 查询参数
│       │   │       └── CognitionOutput.java  # 输出载体
│       │   ├── port/                         # 上游客户端端口
│       │   │   ├── MetamodelClientPort.java  # 元模型治理 BC 端口
│       │   │   ├── MetadataClientPort.java   # 元数据管理 BC 端口
│       │   │   ├── GraphClientPort.java      # 语义关系网络 BC 端口
│       │   │   └── ComputeEngineClientPort.java # 语义查询引擎 BC 端口
│       │   └── service/                      # 领域服务接口
│       │       ├── TemplateResolutionService.java    # 模板解析
│       │       ├── PerspectiveOrchestrationService.java # 视角编排
│       │       ├── TokenBudgetService.java           # Token 预算控制
│       │       ├── VersionAnchorService.java         # 版本锚定
│       │       ├── FqnValidationService.java         # FQN 校验
│       │       ├── ScopeNarrowingService.java        # 作用域收窄
│       │       ├── ChangeWatchService.java           # 变更影响感知
│       │       └── OutputFormattingService.java      # 输出格式化
│       ├── infrastructure/                   # 基础设施层
│       │   ├── adapter/                      # 上游 BC 适配器实现
│       │   │   ├── MetamodelClientAdapter.java
│       │   │   ├── MetadataClientAdapter.java
│       │   │   ├── GraphClientAdapter.java
│       │   │   └── ComputeEngineClientAdapter.java
│       │   ├── config/                       # 配置类
│       │   │   ├── TemplateConfig.java       # 模板 YAML 加载
│       │   │   ├── PerspectiveConfig.java    # 视角 YAML 加载
│       │   │   ├── AgentCognitionProperties.java  # @ConfigurationProperties
│       │   │   └── CognitionCacheConfig.java # Caffeine 缓存配置
│       │   ├── mapper/                       # MapStruct 转换器
│       │   │   ├── CognitionMapper.java      # CognitionRequest ↔ QueryParameters
│       │   │   └── UpstreamDtoMapper.java    # 上游 DTO ↔ 领域对象
│       │   ├── spi/                          # SPI 扩展点
│       │   │   ├── AgentCognitionExceptionHandler.java # 全局异常处理
│       │   │   └── CognitionHealthCheck.java           # 健康检查
│       │   └── event/                        # 事件监听
│       │       └── ChangeEventListener.java  # 变更事件监听器
└── src/test/java/com/metaforge/agent/cognition/
    ├── unit/
    │   ├── domain/                           # 领域层单元测试
    │   │   ├── model/                        # 值对象 & 聚合根测试
    │   │   └── service/                      # 领域服务测试
    │   └── application/                      # 应用层单元测试
    │       └── executor/                     # 视角执行器测试
    ├── integration/                          # 集成测试
    │   └── perspective/                      # 视角编排集成测试
    └── contract-export/                      # 合约导出测试
```
> YAML 配置文件（cognition-templates.yml、cognition-perspectives.yml）部署于 `metaforge-boot/src/main/resources/cognition/`，由 TemplateConfig / PerspectiveConfig 通过 classpath 加载。

**Structure Decision**: 
- Selected structure type: Monorepo multi-module sub-BC (Option 4)
- BC relative path: metaforge-parent/metaforge-agent-cognition/
- Three level structure: parent POM + api + core
- Internal architecture: DDD diamond layers. 注意：服务接口分布在两层——`api/service/` 为外部契约层服务接口（CognitionQueryService / TemplateRegistryService / CognitionOutputService，面向消费者），`domain/service/` 为领域层服务接口（OutputFormattingService / TokenBudgetService 等，面向内部编排）。两者职责不同，不可混淆。
- Cross-BC dependency: depends on 4 upstream BCs' api modules

## Complexity Tracking
None — all constitution checks pass.
