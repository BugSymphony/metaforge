# Implementation Plan: 语义查询与推理引擎

**Branch**: `001-semantic-query-reasoning` | **Date**: 2026-08-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-semantic-query-reasoning/spec.md`

## Summary

构建 `metaforge-compute-engine` BC（语义查询引擎），作为语义体系的**纯无状态计算内核层**。基于上游 BC（metamodel-governance、metadata-management、semantic-relation-network）的生效态数据，通过 jOOQ 直接操作 PostgreSQL，提供多维图查询（邻接查询、组合层级树、子图提取、图模式匹配、多条件复合检索、批量语义查询）、路径推理（两点间路径、传递闭包、多跳语义推理、路径可达性判定）、影响溯源（正向影响扩散、反向依赖溯源、影响路径详情）三类核心能力。

技术方案采用 DDD 菱形端口-适配器架构，多模块 Maven 拆分（api + core），三种开放主机服务（REST / MCP / Application Service），配置化 AssociationType 传导规则管理。

## Technical Context

**Language/Version**: Java 21 + Spring Boot 3

**Primary Dependencies**:
- `metaforge-framework`（foundation-core 平台能力层）
- `metaforge-metamodel-api`（上游元模型语义契约，获取 EntitySchema/RelationSchema 定义与 AssociationType 枚举）
- `metaforge-metadata-api`（上游元数据查询契约，获取生效态实体数据）
- `metaforge-graph-api`（上游关系查询契约，获取关系实例与拓扑数据）
- jOOQ（类型安全 SQL DSL，替代 JPA/JDBC Template 进行跨 Schema 只读查询）
- MapStruct（对象转换工具，DTO ↔ 领域对象 / Record ↔ 领域对象）
- SpringDoc OpenAPI（REST API 文档，复用 foundation-core `@Tag` 机制）
- Spring AI MCP（MCP 协议工具集发布）
- PostgreSQL 16（递归 CTE 图遍历，WITH RECURSIVE）

**Storage**: 无自有数据表。所有查询通过 jOOQ 跨 Schema 只读访问上游 BC 的表：
- `metadata_management.metadata_entity`（实体主表）
- `metadata_management.metadata_entity_version`（实体历史表）
- `semantic_relation_network.relation_instance`（关系主表）
- `semantic_relation_network.entity_relation_index`（双向索引）
- `metamodel_governance.element_definitions`（元模型元素定义）

**Testing**: JUnit 5 + Spring Boot Test + TestContainers（PostgreSQL 容器化集成测试）

**Target Platform**: Linux 服务器，单 JVM 进程（Monorepo Maven 多模块单体应用）

**Project Type**: Monorepo 多模块子 BC（api + core 三级 Maven 结构）

**Performance Goals**: 
- 3 度邻接查询 < 200ms
- 组合层级树查询 < 150ms
- 路径推理查询 < 300ms
- 批量 200 FQN 查询 < 200ms
- 超时熔断 2000ms

**Constraints**:
- 禁止自有数据表与持久化写入
- 禁止依赖上游 `core` 模块（仅通过 `api` 模块 + jOOQ 跨 Schema 查询访问）
- 禁止引入外部图库（Neo4j）或内存图引擎（JGraphT）
- 仅支持生效态数据查询（历史版本/草稿版本不参与默认计算）
- 所有配置属性前缀 `metaforge.compute-engine.*`
- 错误码范围 33000-33999
- 禁止重复实现 foundation-core 已提供的平台能力

**Scale/Scope**:
- 关系数量 < 10000 条
- 实体总量 ≤ 1000 条
- 图遍历深度 ≤ 10（默认 5）
- 批量查询上限 200 FQN
- 路径模式长度 ≤ 4 段（3 条关系边）
- 多跳推理 ≤ 3 步
- 单实例百级 QPS

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Global Constitution Compliance

| # | Principle | Level | Compliance Strategy | Status |
|---|-----------|-------|---------------------|--------|
| I | 元模型唯一权威性 | MUST | 实体/关系结构定义唯一来源于已发布元模型（EntitySchema/RelationSchema）；AssociationType 传导规则通过本 BC 配置文件独立补充（不修改元模型） | PASS |
| II | 显式导入边界管控 | MUST | 本 BC 不参与导入授权，由 `agent-consumption` BC 统一执行。本 BC 作为中间计算层，执行下游 BC 的查询请求 | PASS |
| III | 全链路权限过滤 | MUST | 本 BC 不执行权限过滤，由 `agent-consumption` BC 在上游完成过滤后调用本 BC | PASS |
| IV | 版本统一收敛 | MUST | 通过 `metaforge-metamodel-api` 查询已发布版本元模型，不接触草稿态或未发布版本 | PASS |
| V | 纯组合无继承设计 | SHOULD | 本 BC 不定义元模型；领域模型按聚合根/实体/值对象建模，遵循纯组合范式 | PASS |
| VI | 合约化双协议标准接口 | SHOULD | REST（管理操作 + 系统集成）+ MCP（Agent 语义查询工具集）+ Application Service（进程内调用）三通道发布。BC 宪法 Override 已声明：MCP 由 agent-consumption 统一发布，本 BC 仅暴露 Application Service 供其消费 | PASS |
| VII | Bundle 模块化治理 | SHOULD | 本 BC 不参与导出清单管控；MVP 阶段不处理跨 Bundle 不可见元素 | PASS |
| VIII | Agent 友好型输出 | SHOULD | 本 BC 返回标准 JSON 结构化结果；Agent 友好型格式化由 agent-consumption 统一完成。BC 宪法 Override 已声明 | PASS |
| IX | 纯元数据边界坚守 | MUST | 所有操作仅涉及元数据查询与推理，不存储、不触碰具体业务交易数据 | PASS |
| X | 文档中文规范 | MUST | 所有规划文档、JavaDoc 注释使用简体中文；代码标识符、变量名使用英文 | PASS |
| XI | 代码注释中文规范 | SHOULD | 关键业务逻辑、图遍历算法、推理规则处使用中文注释 | PASS |

### BC Constitution Compliance

| # | Principle | Level | Compliance Strategy | Status |
|---|-----------|-------|---------------------|--------|
| I | 元模型唯一权威原则 | MUST | 所有推理查询语义规则来源于已发布元模型定义 + `metaforge.compute-engine.transitivity-rules` 配置；不引入外部知识或 LLM 推理 | PASS |
| II | 生效数据基准原则 | MUST | 所有查询默认仅基于生效态数据执行（主表 metadata_entity + relation_instance） | PASS |
| III | 计算存储分离原则 | MUST | 纯无状态计算层，不创建自有 Schema，不持有存储主权，不缓存数据跨请求 | PASS |
| IV | 结果结构化原则 | MUST | 所有输出以 FQN 为核心标识，内联 EntitySummary 与 RelationSummary | PASS |
| V | 过滤前置原则 | MUST | 7 维过滤参数在 jOOQ CTE 遍历过程中实时生效（WHERE 子句前置），不参与遍历且不计入深度 | PASS |
| VI | 深度上限与安全熔断原则 | MUST | 两层深度约束：(1) 全局 `max-depth` 默认 5 硬上限 10；(2) per-AssociationType `maxDepth`。超时默认 2000ms | PASS |
| VII | 下游透明原则 | SHOULD | 仅提供标准化结构化计算结果，不感知下游消费形态 | PASS |

**Gate Verdict**: ALL principles PASS. No violations.

## Foundation Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Foundation Capability | Contract Source | Compliance Action | Status |
|---|----------------------|-----------------|-------------------|--------|
| 1 | 统一响应格式 | `rest-api-contract.md` | REST API 使用 `ApiResponse<T>` 标准格式，禁止自定义包装类 | PASS |
| 2 | SPI 异常处理扩展 | `api-contracts.md` §1 | 实现 `ExceptionHandlerSpi` 注册 BC 自定义异常（错误码 33000-33999） | PASS |
| 3 | JSONB 序列化 | `api-contracts.md` §3 | 使用 `JsonbUtils.toJsonb()` / `fromJsonb()` 统一序列化，禁止自定义 | PASS |
| 4 | 分页组件 | `api-contracts.md` §2 | 检索型查询复用 `PageRequest` / `PageResult<T>`，禁止自定义分页 DTO | PASS |
| 5 | 虚拟线程 | `platform-capabilities.md` §1 | 不配置线程池，继承 foundation-core 全局虚拟线程配置 | PASS |
| 6 | 日志脱敏 | `platform-capabilities.md` §2 | 不配置日志脱敏规则（敏感字段已内置）；无额外敏感字段需求 | PASS |
| 7 | OpenAPI 文档 | `platform-capabilities.md` §3 | 仅使用 `@Tag` 标注 Controller 分组，禁止自定义 SpringDoc 配置 | PASS |
| 8 | 国际化 | `platform-capabilities.md` §4 | 注入 `MessageSource` 使用，禁止自定义 MessageSource bean | PASS |
| 9 | 健康检查 | `platform-capabilities.md` §5 | 实现 `HealthCheckSpi` 注册 BC 自定义健康检查项（上游 BC 连通性） | PASS |
| 10 | 安全基线 | `platform-capabilities.md` §6 | 不配置安全过滤器，继承 foundation-core 全局安全配置 | PASS |
| 11 | 数据源 | `platform-capabilities.md` §7 | 不配置数据源或事务管理器，使用统一数据源 + jOOQ DSLContext | PASS |
| 12 | Flyway 统一管理 | `platform-capabilities.md` §9 | 若有数据库视图/函数，Flyway 脚本放入 `metaforge-boot/src/main/resources/db/migration/` 统一维护 | PASS |
| 13 | 测试基类 | `platform-capabilities.md` §10 | 继承 `BaseUnitTest` / `BaseIntegrationTest`，不引入 TestContainers 依赖 | PASS |
| 14 | Maven 模块注册 | `build-system-integration.md` §2 | 在 `metaforge-parent/pom.xml` `<modules>` 中添加 `metaforge-compute-engine`；在 `metaforge-boot/pom.xml` 添加 `metaforge-compute-engine-core` 依赖 | PASS |
| 15 | POM 标准模板 | `build-system-integration.md` §1 | 继承 `metaforge-parent`，依赖 `metaforge-framework`，禁止 `<dependencyManagement>`，不覆盖版本属性 | PASS |
| 16 | Schema 写校验 | `platform-capabilities.md` §8 | 本 BC 无自有 Schema，仅跨 Schema SELECT 查询（属于合约允许范围） | PASS |

**Foundation Gate Verdict**: ALL items PASS.

## Project Structure

### Documentation (this feature)

```text
specs/001-semantic-query-reasoning/
├── plan.md                   # This file (/speckit.plan command output)
├── research.md               # Phase 0 output
├── foundation-adaptation.md  # Phase 1 output (foundation contracts are imported)
├── data-model.md             # Phase 1 output
├── quickstart.md             # Phase 1 output
├── contracts/                # Phase 1 output
│   ├── application-service.md      # Application Service 契约
│   ├── rest-api.md                 # REST API 契约
│   └── mcp-tools.md                # MCP 工具集契约
└── tasks.md                  # Phase 2 output (/speckit.tasks command)
```

### Source Code (BC root)

```text
metaforge-parent/metaforge-compute-engine/
├── pom.xml                                 # BC 聚合父 POM (packaging=pom)
├── metaforge-compute-engine-api/
│   ├── pom.xml                             # api 子模块 POM
│   └── src/main/java/com/metaforge/computeengine/api/
│       ├── constant/
│       │   └── ComputeEngineErrorCodes.java   # 错误码常量类（33000-33999）
│       ├── dto/
│       │   ├── request/                       # 请求 DTO
│       │   │   ├── AdjacencyQueryRequest.java
│       │   │   ├── CompositionTreeQueryRequest.java
│       │   │   ├── SubgraphQueryRequest.java
│       │   │   ├── PatternMatchRequest.java
│       │   │   ├── CompoundSearchRequest.java
│       │   │   ├── BatchQueryRequest.java
│       │   │   ├── PathQueryRequest.java
│       │   │   ├── ClosureQueryRequest.java
│       │   │   ├── MultiHopQueryRequest.java
│       │   │   ├── ReachabilityCheckRequest.java
│       │   │   ├── ImpactDiffusionRequest.java
│       │   │   └── ImpactPathRequest.java
│       │   ├── response/                      # 响应 DTO
│       │   │   ├── GraphQueryResult.java
│       │   │   ├── PathResult.java
│       │   │   ├── ClosureResult.java
│       │   │   └── ImpactTraceResult.java
│       │   └── common/
│       │       ├── FilterCriteria.java        # 7 维过滤参数
│       │       ├── EntitySummary.java         # 实体摘要
│       │       ├── RelationSummary.java       # 关系摘要
│       │       ├── TraversalDirection.java    # 遍历方向枚举
│       │       ├── MatchMode.java             # 匹配模式枚举
│       │       └── TruncatedReason.java       # 截断原因枚举
│       ├── enums/
│       │   ├── AssociationType.java           # 关联类型枚举
│       │   ├── WeightStrategy.java            # 权重策略枚举
│       │   └── PatternWildcard.java           # 模式匹配通配符常量
│       ├── service/                           # Application Service 接口
│       │   ├── GraphQueryService.java         # 多维图查询
│       │   ├── PathReasoningService.java      # 路径推理
│       │   └── ImpactTracingService.java      # 影响溯源
│       └── event/                             # 域事件定义
│           └── QueryCompletedEvent.java       # 查询完成事件（可选，不实现）
├── metaforge-compute-engine-core/
│   ├── pom.xml                             # core 子模块 POM
│   └── src/main/java/com/metaforge/computeengine/
│       ├── application/
│       │   └── service/                       # Application Service 实现
│       │       ├── GraphQueryServiceImpl.java
│       │       ├── PathReasoningServiceImpl.java
│       │       └── ImpactTracingServiceImpl.java
│       ├── domain/
│       │   ├── model/
│       │   │   ├── aggregate/                 # 聚合根
│       │   │   │   ├── GraphQuery.java        # 图查询聚合根
│       │   │   │   ├── PathQuery.java         # 路径推理聚合根
│       │   │   │   └── ImpactQuery.java       # 影响溯源聚合根
│       │   │   ├── entity/                    # 领域实体
│       │   │   │   ├── TraversalPath.java
│       │   │   │   ├── ClosuredEntity.java
│       │   │   │   └── ImpactEntity.java
│       │   │   └── valueobject/              # 值对象
│       │   │       ├── FQN.java
│       │   │       ├── GraphPattern.java
│       │   │       ├── PathSegment.java
│       │   │       ├── TraversalDepth.java
│       │   │       ├── InfluenceScope.java
│       │   │       ├── EntitySnapshot.java
│       │   │       ├── RelationSnapshot.java
│       │   │       ├── FilterCriteriaVO.java
│       │   │       └── TransitivityRule.java
│       │   ├── port/                          # 领域查询端口
│       │   │   ├── EntityDataPort.java        # 实体数据查询端口
│       │   │   ├── RelationDataPort.java      # 关系数据查询端口
│       │   │   └── MetamodelSemanticPort.java # 元模型语义查询端口
│       │   ├── service/                       # 领域服务
│       │   │   ├── GraphTraversalService.java # 图遍历算法
│       │   │   ├── PathInferenceService.java  # 路径推理引擎
│       │   │   ├── ImpactAnalysisService.java # 影响分析器
│       │   │   ├── TransitivityRuleService.java # 传导规则加载与查询
│       │   │   └── FilterPredicateService.java  # 过滤谓词构建
│       │   └── exception/                     # 领域异常
│       │       ├── ComputeEngineException.java
│       │       ├── EntityNotFoundException.java
│       │       ├── TraversalDepthExceededException.java
│       │       ├── QueryTimeoutException.java
│       │       └── InvalidFilterException.java
│       ├── infrastructure/
│       │   ├── config/
│       │   │   └── ComputeEngineProperties.java  # 配置属性绑定
│       │   ├── persistence/
│       │   │   └── jooq/                         # jOOQ 适配器
│       │   │       ├── EntityDataPortImpl.java
│       │   │       ├── RelationDataPortImpl.java
│       │   │       ├── jooq/GeneratedTables.java # jOOQ 代码生成表引用
│       │   │       └── converter/                # Record ↔ Domain 转换器
│       │   │           ├── EntityConverter.java
│       │   │           └── RelationConverter.java
│       │   ├── gateway/
│       │   │   └── MetamodelGatewayAdapter.java  # 元模型 API 适配器
│       │   ├── mapper/                           # MapStruct 转换器
│       │   │   ├── GraphQueryMapper.java
│       │   │   ├── PathResultMapper.java
│       │   │   └── ImpactTraceMapper.java
│       │   └── spi/                              # SPI 扩展
│       │       ├── ComputeEngineExceptionHandler.java
│       │       └── ComputeEngineHealthCheck.java
│       ├── interfaces/
│       │   ├── rest/                             # REST Controller
│       │   │   ├── GraphQueryController.java
│       │   │   ├── PathReasoningController.java
│       │   │   └── ImpactTracingController.java
│       │   └── mcp/                              # MCP 工具集
│       │       └── ComputeEngineMcpTools.java
│       └── resources/
│           └── application-metaforge-compute-engine.yml  # BC 配置
├── contracts/                             # OHS 契约文档（独立于代码库）
│   ├── application-service.md            # Application Service 契约
│   ├── rest-api.md                        # REST API 契约
│   └── mcp-tools.md                       # MCP 工具集契约
└── context/
    ├── constitution.md                    # BC 宪法（已存在）
    ├── upstream-contracts/                # 上游契约（已存在）
    └── foundation-contracts/              # foundation 契约（已存在）
```

**Structure Decision**:
- Selected structure type: Monorepo 多模块子 BC（Option 4 变体，三级 Maven 结构）
- BC relative path to REPO_ROOT: `metaforge-parent/metaforge-compute-engine/`
- Internal architecture: DDD 菱形端口-适配器架构，core 模块内严格遵循 `interfaces → application → domain ← infrastructure` 单向向内依赖规则。
- Cross-BC dependency status: 依赖 3 个上游 BC（metamodel-governance, metadata-management, semantic-relation-network）的 api 模块 + foundation-core 平台层；下游由 agent-consumption 通过 api 模块消费

**BC Boundary Confirmation**:
- 所有核心业务逻辑封装在 `metaforge-compute-engine/` 范围内，不引用其他 BC 内部实现
- Export contracts: Application Service 接口定义在 `api` 模块，契约文档在 `contracts/` 目录
- Import contracts: 上游依赖仅通过 `context/upstream-contracts/` 契约文件 + 上游 `api` 模块 Maven 依赖
- Foundation compliance: 所有 foundation 接入遵循 `foundation-adaptation.md` 设计

## Complexity Tracking

> No violations requiring justification. All constitution and foundation checks pass.

