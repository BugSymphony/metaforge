# Global Architecture Plan: MetaForge

**Version**: `v0.1` | **Date**: 2026-08-01 | **Upstream Spec**: `.specify/memory/global-specify.md`

**Input**: Candidate subdomains & business services from `.specify/memory/global-specify.md`

**Governing Constitution**: `.specify/memory/global-constitution.md`

**Note**: This template is filled in by the `/speckit.global.plan` command. See `.specify/templates/global-plan-template.md` for the execution workflow.

---

## System Overview *(mandatory)*

MetaForge 是 AI Agent 时代的语义基础设施——为 AI Agent 及其开发者提供声明式元模型定义、结构化元数据治理、语义关系图谱与推理三大底层核心能力。系统采用领域驱动设计（DDD）方法论进行架构划分，以 5 个限界上下文（Bounded Context）承载从元模型定义、元数据管理、语义网络构建、查询推理到 Agent 消费接入的全链路闭环。MVP 阶段采用 Java 21 + Spring Boot 3 统一技术基座，PostgreSQL 作为单一关系型数据库，通过递归 CTE 替代图数据库实现图查询能力，不引入 Redis、MQ 等中间件，以最小技术栈验证核心语义闭环。

---

## Architecture Context *(mandatory)*

### Unified Tech Stack Baseline

- **Core Language/Framework**: Java 21 + Spring Boot 3，全 BC 启用虚拟线程（Virtual Threads）以简化异步编程模型
- **MCP 发布框架**: Spring AI，用于 `agent-consumption` BC 将语义查询与推理能力发布为 MCP Server 工具集
- **Build Tool**: Maven（多模块聚合工程，父子 POM 统一依赖治理）

### Global Storage Specification

- **Relational Database**: PostgreSQL 16，单实例部署，所有 BC 共享同一数据库实例
  - 各 BC 通过独立 Schema 实现逻辑隔离，禁止跨 BC 直接写表
  - 图遍历查询通过 PostgreSQL 递归 CTE（WITH RECURSIVE）实现，不引入图数据库
- **Cache**: 无外部缓存中间件（不含 Redis）；各 BC 按需使用 Caffeine 内存缓存，缓存数据不跨 BC 共享
- **Message Queue**: MVP 阶段不引入消息队列，BC 间通信采用同步 REST 调用

### Global Testing Specification

- **Testing System**: JUnit 5 + Spring Boot Test + TestContainers（PostgreSQL 容器化集成测试）
- **Contract Testing**: BC 间接口以 OpenAPI 3.0 规范文档化，人工审核合约变更

### Deployment Architecture

- **Target Environment**: 单台 Linux 服务器（或本地开发机），单 JVM 进程运行
- **Deployment Mode**: Monorepo 单仓库多模块，Gradle/Maven 统一构建，单 jar 部署（各 BC 作为独立 Maven 模块编译，最终打包为单体应用）

### Global Performance Targets

- **MVP 性能基线**: 单用户操作场景下，元数据 CRUD 操作响应时间 < 200ms，含递归 CTE 的全链路遍历查询 < 2s（500 实体规模），全量一致性校验 < 10s（500 实体规模）
- **并发能力**: MVP 阶段不设硬性并发指标，以单用户流畅体验为默认基准 [NEEDS CLARIFICATION: 产品文档未定义 MVP 并发性能基线，当前暂定"单用户流畅体验"，建议在首个迭代验收后根据实际表现校准]

### Global Mandatory Constraints

- 所有对外接口（REST + MCP）遵循宪法约定的双协议标准，接口变更必须保持向后兼容
- 元数据实体使用 JSONB 存储以适应动态结构，但 Schema 校验逻辑必须严格以元模型定义为基准
- 禁止任何 BC 绕过 `metamodel-governance` 的版本管控直接操作元数据
- 禁止任何 BC 存储具体业务交易数据，违反纯元数据边界假定（宪法 IX）

### Business Scale Estimation

- MVP 阶段目标业务规模：1–2 个垂直业务领域试点，单领域 ≤ 5 个 Bundle，每 Bundle ≤ 10 个 Package，每 Package ≤ 20 个元模型元素，元数据实体总量 ≤ 1000 条，并发消费 Agent ≤ 5 个

---

## Global Constitution Check *(mandatory, gate)*

*GATE: Must pass before formal BC partition. Re-check after BC collaboration design.*

| # | Principle | Level | Compliance Strategy | Status |
|---|-----------|-------|---------------------|--------|
| I | 元模型唯一权威性 | MUST | `metamodel-governance` BC 为全平台元模型定义的唯一写入入口，所有下游 BC 通过其发布的只读合约消费元模型结构。禁止任何 BC 绕过该 BC 直接创建或修改元模型定义。 | PASS |
| II | 显式导入边界管控 | MUST | `agent-consumption` BC 作为导入授权的唯一执行点，Agent 必须通过该 BC 声明导入并获取白名单后方可消费元数据。 | PASS |
| III | 全链路权限过滤 | MUST | `agent-consumption` BC 在每次查询/推理请求中基于 Agent 白名单执行过滤。所有查询 BC 在返回结果前必须经过该过滤层。 | PASS |
| IV | 版本统一收敛 | MUST | `metamodel-governance` BC 以 Bundle 为单元进行整体式版本固化，禁止局部模块独立版本号。所有 BC 依赖的元模型版本契约必须在导入时显式声明。 | PASS |
| V | 纯组合无继承设计 | SHOULD | `metamodel-governance` BC 在元模型结构建模中强制纯组合关系，不引入继承语义。JSONB 存储天然支持扁平化组合。 | PASS |
| VI | 合约化双协议标准接口 | SHOULD | REST 接口覆盖元模型管理、元数据 CRUD 等管理操作；MCP 接口（Spring AI）覆盖 Agent 消费场景。`agent-consumption` BC 为 MCP Server 的唯一发布方。 | PASS |
| VII | Bundle 模块化治理 | SHOULD | `metamodel-governance` BC 通过导出清单（Export Manifest）管控命名空间边界，`agent-consumption` BC 基于导出清单执行导入范围校验。 | PASS |
| VIII | Agent 友好型输出 | SHOULD | `agent-consumption` BC 负责将所有查询/推理结果格式化为 JSON Schema 兼容的结构化输出，确保零解析开销注入 Agent 上下文。 | PASS |
| IX | 纯元数据边界坚守 | MUST | 所有 BC 数据存储均限于元数据范畴（概念、关系、规则定义），不触碰具体业务交易数据。数据库 Schema 设计必须明确区分元数据表与可能的配置表。 | PASS |
| X | 文档中文规范 | MUST | 所有治理文档（constitution、specify、plan）正文使用简体中文；术语（Bundle、MCP、BC、REST）、代码标识符保留英文。 | PASS |
| XI | 代码注释中文规范 | SHOULD | BC 实现阶段关键业务逻辑、复杂算法、接口说明处使用简体中文注释；代码变量名、方法名使用英文。 | PASS |

**Gate Verdict**: ALL MUST & SHOULD principles **PASS**. No violations detected for Phase 0. Phase 2 re-check will validate collaboration patterns against principle III (全链路过滤) and IX (纯元数据边界).

---

## Architecture Structure *(mandatory)*

### Global Documentation Structure

```text
.specify/memory/
├── global-constitution.md    # 全局治理基线
├── global-specify.md         # 问题空间规格（本计划的输入）
└── global-plan.md            # 本文件，/speckit.global.plan 命令输出
```

### BC Logical Topology Structure

```text
# 基础设施基础层（Generic Domain，零业务逻辑）
└── foundation-core              # 核心技术基座 BC：统一依赖治理、虚拟线程配置、JSONB 序列化工具、递归 CTE 查询工具、Caffeine 缓存配置、全局异常处理

# 业务 BC 层
Core Domain
├── metamodel-governance         # 元模型治理 BC：元模型全生命周期——定义、校验、版本固化、导出清单管理
├── metadata-management          # 元数据管理 BC：元数据实体的创建、更新、版本追溯、结构合规校验、生命周期管理
├── semantic-relation-network    # 语义关系网络 BC：关系边自动构建、双向引用维护、关系拓扑管理
└── semantic-query-engine        # 语义查询引擎 BC：多维图查询、路径推理、一致性校验、影响溯源、升级预校验
Supporting Domain
└── agent-consumption            # Agent 消费接入 BC：导入授权、白名单过滤、上下文生成、MCP Server 发布、Skill 封装
```

### Foundation Access Core Principles

- **Core Uniqueness**: 全局仅一个 `foundation-core` BC，不设置可选扩展基础 BC。MVP 阶段所有横切关注点集中在核心基础层内管理。
- **Unidirectional Dependency**: 所有业务 BC（含 Core Supporting）仅单向依赖 `foundation-core`；`foundation-core` 不反向依赖任何业务 BC；业务 BC 间通过接口合约协作，无运行时反向依赖。
- **Non-intrusive Access**: 业务 BC 通过 Maven 依赖引入 `foundation-core` 的公共包（common-utils, common-config），不允许修改 foundation-core 源码。
- **Contract-first**: `foundation-core` 提供的 JSONB 序列化工具、递归 CTE 查询模板、缓存配置均通过公共 Java 接口发布，业务 BC 按合约调用。

**Structure Decision**: 从 5 个候选子域中映射出 5 个业务 BC + 1 个基础 BC，共计 6 个 BC。Core 域包含 4 个 BC（元模型治理、元数据管理、语义关系网络、语义查询引擎），Supporting 域包含 1 个 BC（Agent 消费接入）。逻辑拓扑为两层架构，上层业务 BC 层，下层 `foundation-core` 基础层。逻辑拓扑不绑定物理目录结构，实际工程结构通过 Maven 父子 POM 管理，物理路径由 `context.json` 独立注册。

### Engineering Structure & Module Registration Specifications

#### Core Principles

所有业务 BC 通过 Maven 父子 POM 聚合声明式注册到基础工程，禁止硬编码引用与修改 foundation-core 源码。Maven 自动识别 `<modules>` 注册条目完成模块加载。

#### Unified Registration Across Build Ecosystems

| Build Tool | Registration Implementation | Configuration Constraints |
| :--- | :--- | :--- |
| Maven | 父子 POM 聚合 `<modules>` 声明 | 根 `pom.xml` 的 `<modules>` 下新增条目；不得修改已有 BC 的 POM 配置 |

#### Minimum Modification Constraints for New BCs

新增业务 BC 仅允许修改两处：
1. 创建新 BC 的完整模块目录（含 `pom.xml`、源码、测试、资源文件）
2. 在根 `pom.xml` 的 `<modules>` 中添加一条新 `<module>` 条目

**严格禁止:** 修改 foundation-core 或任何已有 BC 的源代码、公共组件、全局配置。

---

## Bounded Contexts *(mandatory)*

### BC Partition Overview

共 6 个 BC：1 个基础设施基础 BC（`foundation-core`）+ 4 个 Core 域业务 BC + 1 个 Supporting 域业务 BC。分区逻辑沿用 global-specify 中定义的候选子域划分，保持候选子域与 BC 的一一映射关系。物理工程采用 Maven 多模块单体架构，各 BC 共享同一 PostgreSQL 数据库实例但通过独立 Schema 实现逻辑隔离。

### BC Formal Definitions

#### metamodel-governance (Type: Core)

- **BC Description**: 全平台语义规则的唯一源头，负责元模型（M2 层）的声明式定义、版本固化、导出清单管理与结构完整性校验。
- **Owns (Exclusive Responsibilities)**:
  - 元模型 Bundle/Package/元素三层架构的定义与结构校验
  - Bundle 版本固化（语义化版本快照，不可变）
  - 导出清单（Export Manifest）的命名空间边界管控
  - 业务规则在元模型层的声明与版本绑定
  - 跨版本差异分析
- **Provides (External Capabilities)**:
  - 元模型结构查询（供 `metadata-management` 消费进行结构合规校验）
  - 导出清单查询（供 `agent-consumption` 消费进行导入授权校验）
  - 业务规则定义查询（供 `semantic-query-engine` 消费进行推理与一致性校验）
  - 版本差异比对（供 `semantic-query-engine` 消费进行升级预校验）
  - 接口类型: REST API（管理操作）、Domain Object（BC 间内部调用）
- **Depends On (Upstream Dependencies)**:
  - `foundation-core`: JSONB 序列化工具、全局异常处理、缓存配置
  - 无业务 BC 上游依赖（元模型治理是系统语义源头）
- **Ubiquitous Language Glossary**:
  - **Bundle（领域模块）**: 一个完整业务领域的元模型组织单元，包含若干 Package，以 Bundle 为单元进行版本固化
  - **Package（分类包）**: Bundle 内的命名空间分组单元，承载一组语义相关的元模型元素
  - **元模型元素（Metamodel Element）**: 元模型层的最小定义单元，描述一种领域概念的类型结构、关系约束或业务规则
  - **导出清单（Export Manifest）**: 声明 Bundle 中哪些 Package 对外可见的白名单，未列入的 Package 仅模块内部可见
  - **版本快照（Version Snapshot）**: Bundle 在某一时间点的不可变状态记录，一旦固化不可修改
- **Core Domain Aggregates**:
  - **Bundle**: 领域模块的聚合根，包含 Bundle 元信息、Package 列表、版本历史，管理整个模块的完整生命周期
  - **Package**: 命名空间分类单元，隶属于一个 Bundle，包含一组元模型元素定义
  - **ElementDefinition**: 元模型元素定义，描述一个领域概念类型的字段结构、关系约束与规则模板
  - **ExportManifest**: 导出清单，绑定到特定版本快照，声明对外可见的 Package 命名空间集合
- **Mapped Business Services**: SVC-001, SVC-002, SVC-003, SVC-005, SVC-006, SVC-015

#### metadata-management (Type: Core)

- **BC Description**: 承载领域语义数据的全生命周期管理，基于上游元模型结构模板创建和维护具体的元数据实体，确保每条元数据严格符合元模型的结构约束。
- **Owns (Exclusive Responsibilities)**:
  - 元数据实体的创建、字段更新与历史版本追溯
  - 基于元模型的结构合规校验（字段类型、必填项、枚举值、关系引用）
  - 元数据实体的生命周期下线管理（校验下线影响面）
  - 元数据实体的 JSONB 存储与索引管理
- **Provides (External Capabilities)**:
  - 元数据 CRUD 接口（供 REST 客户端与 Agent 消费）
  - 元数据变更通知（供 `semantic-relation-network` 响应实体变更进行关系重建）
  - 元数据内容查询（供 `semantic-query-engine` 消费进行推理与查询）
  - 接口类型: REST API、Domain Event（内存事件，MVP 阶段不引入 MQ）
- **Depends On (Upstream Dependencies)**:
  - `foundation-core`: JSONB 序列化工具、递归 CTE 查询工具
  - `metamodel-governance`: 元模型结构模板（用于结构合规校验）
- **Ubiquitous Language Glossary**:
  - **元数据实体（Metadata Entity）**: 一条具体的领域概念描述记录，包含 JSONB 格式的结构化内容，严格遵循其所属元模型元素的结构定义
  - **结构合规校验（Schema Compliance Check）**: 将元数据实体的 JSONB 内容与其声明的元模型元素定义进行逐字段比对的过程
  - **实体版本（Entity Version）**: 元数据实体每次更新后产生的不可变历史记录
- **Core Domain Aggregates**:
  - **MetadataEntity**: 元数据实体的聚合根，包含实体内容（JSONB）、所属 Bundle/Package 引用、元模型元素类型引用、当前状态（活跃/下线）、版本历史
- **Mapped Business Services**: SVC-007, SVC-008, SVC-013, SVC-014

#### semantic-relation-network (Type: Core)

- **BC Description**: 基于元模型关系约束自动构建和管理元数据实体间的语义关系边，维护双向引用索引与关系拓扑，为下游查询与推理提供语义图谱骨架。
- **Owns (Exclusive Responsibilities)**:
  - 语义关系边的自动构建（基于元模型关系约束与元数据实体的引用字段）
  - 关系类型与基数的约束校验（关系边必须在元模型定义的关系类型范围内）
  - 双向引用索引维护（创建/删除实体时同步更新所有关联实体的引用列表）
  - 关系拓扑的增删管理（实体下线时级联标记受影响的关系边）
- **Provides (External Capabilities)**:
  - 关系拓扑查询（供 `semantic-query-engine` 消费进行图遍历与推理）
  - 受影响关系链查询（供 `metadata-management` 消费进行下线影响校验）
  - 接口类型: Domain Object（BC 间内部调用）
- **Depends On (Upstream Dependencies)**:
  - `foundation-core`: 递归 CTE 查询工具、缓存配置
  - `metamodel-governance`: 元模型定义的关系类型与基数约束
  - `metadata-management`: 元数据实体创建/变更事件（用于触发关系重建）
- **Ubiquitous Language Glossary**:
  - **关系边（Relation Edge）**: 两个元数据实体间的有向关联，包含关系类型（依赖/从属/映射/约束）、源实体 ID、目标实体 ID
  - **关系拓扑（Relation Topology）**: 以邻接表形式存储的完整语义图谱，所有关系边的集合
  - **双向引用（Bidirectional Reference）**: 每条关系边同时记录在源实体的"出边索引"和目标实体的"入边索引"中
- **Core Domain Aggregates**:
  - **RelationEdge**: 关系边的聚合根，包含源实体引用、目标实体引用、关系类型、基数、创建版本号
  - **EntityRelationIndex**: 实体关系索引，为每个元数据实体维护其所有出边和入边的汇总视图
- **Mapped Business Services**: SVC-009

#### semantic-query-engine (Type: Core)

- **BC Description**: 承载多维度图查询与规则推理两大核心技术能力，基于上游语义图谱关系拓扑与元数据实体数据，提供精准查询、关联查询、全链路遍历、路径推理、一致性校验、影响溯源与升级预校验等能力，是 Agent 消费语义数据的主要交互界面。
- **Owns (Exclusive Responsibilities)**:
  - 精准查询（按实体 ID、类型、属性检索）
  - 关联查询（以实体为中心的一度或多度关系扩展）
  - 全链路遍历查询（基于递归 CTE 的深度优先/宽度优先图遍历）
  - 路径推理（沿关系边验证传递性依赖与约束满足）
  - 一致性校验（逐实体验证其是否满足元模型中声明的全部业务规则）
  - 异常标记与报告生成（结构化违规清单）
  - 全链路影响溯源（基于关系拓扑逆向追溯受变更影响的实体范围）
  - 升级预校验（版本切换前的兼容性预评估）
- **Provides (External Capabilities)**:
  - 全量查询与推理结果（供 `agent-consumption` 消费生成结构化上下文）
  - 一致性校验报告（供 REST 客户端与 `agent-consumption` 消费）
  - 影响溯源报告（供 REST 客户端消费）
  - 接口类型: REST API（供管理端调用）、Domain Object（供 `agent-consumption` 内部调用）
- **Depends On (Upstream Dependencies)**:
  - `foundation-core`: 递归 CTE 查询模板工具、JSONB 解析工具
  - `metamodel-governance`: 业务规则定义（推理与一致性校验的约束来源）
  - `metadata-management`: 元数据实体内容（查询与推理的目标数据）
  - `semantic-relation-network`: 关系拓扑（图遍历与路径推理的骨架）
- **Ubiquitous Language Glossary**:
  - **递归 CTE 遍历（Recursive CTE Traversal）**: 使用 PostgreSQL WITH RECURSIVE 语句沿关系边表逐层展开的图遍历算法
  - **一致性校验报告（Consistency Report）**: 包含违规实体 ID、违规规则 ID、违规原因、违规路径上下文的格式化清单
  - **影响溯源（Impact Trace）**: 从变更点沿关系边正向或逆向传播计算受影响的实体集合
- **Core Domain Aggregates**:
  - **QueryResult**: 查询结果的聚合根，包含结果实体集合、关系边集合、遍历路径描述、查询元信息
  - **ConsistencyReport**: 一致性校验报告，包含违规项列表、校验范围、执行时间戳
  - **ImpactTraceReport**: 影响溯源报告，包含直接影响实体列表、间接传播影响实体列表、风险等级评估
- **Mapped Business Services**: SVC-010, SVC-011, SVC-012, SVC-016, SVC-017, SVC-018, SVC-019, SVC-020

#### agent-consumption (Type: Supporting)

- **BC Description**: 为 Agent 生态提供标准化接入通道，负责导入授权管理、全链路白名单权限过滤、Agent 友好型结构化上下文动态生成，并通过 Spring AI 将语义查询与推理能力发布为 MCP Server 工具集，同时封装为 opencode 兼容的消费 Skill。
- **Owns (Exclusive Responsibilities)**:
  - Agent 导入授权注册与管理（记录 Agent 身份及其导入的 Bundle 版本与 Package 范围）
  - 全链路白名单过滤（每次查询/推理请求基于 Agent 授权范围做过滤）
  - 结构化上下文生成（聚合查询结果与推理结论，组装为 Agent 友好型格式）
  - MCP Server 发布（通过 Spring AI 将语义能力暴露为 MCP 工具列表）
  - 消费 Skill 封装（提供 opencode CLI 兼容的 Skill 定义文件）
  - 版本废弃通知（Bundle 标记废弃时向关联 Agent 推送提醒）
- **Provides (External Capabilities)**:
  - MCP 协议工具集（供外部 Agent 平台通过 MCP 客户端调用）
  - opencode Skill 集（元数据查询、关系推理、上下文生成）
  - Agent 授权状态查询（供 `semantic-query-engine` 在执行查询前校验权限）
  - 接口类型: MCP（Agent 消费）、REST API（管理操作）、opencode Skill 文件（消费端）
- **Depends On (Upstream Dependencies)**:
  - `foundation-core`: JSON Schema 序列化工具、Spring AI 配置
  - `metamodel-governance`: 导出清单（用于校验导入范围合法性）
  - `semantic-query-engine`: 查询结果与推理结论（用于上下文生成与 Skill 响应）
- **Ubiquitous Language Glossary**:
  - **导入授权（Import Authorization）**: Agent 显式声明导入指定 Bundle 版本与 Package 范围后获取的数据访问白名单
  - **MCP Tool**: 通过 MCP 协议暴露的单个能力单元，对应一个具体的语义查询或推理操作
  - **消费 Skill**: 面向 opencode CLI 环境的预封装指令，Agent 通过 AI slash command 直接调用
  - **上下文块（Context Block）**: 聚合了实体详情、关系摘要、规则约束的结构化数据包，可直接注入 Agent 推理上下文
- **Core Domain Aggregates**:
  - **AgentRegistration**: Agent 注册记录，包含 Agent 唯一标识、已导入 Bundle 版本列表、授权 Package 范围、注册时间
  - **ImportAuthorization**: 导入授权记录，绑定 Agent 与特定 Bundle 版本的 Package 白名单，作为全链路过滤的数据依据
- **Mapped Business Services**: SVC-004, SVC-021, SVC-022, SVC-023, SVC-024, SVC-025

#### foundation-core (Type: Generic)

- **BC Description**: 核心技术基座，为所有业务 BC 提供统一依赖治理、运行时环境配置、JSONB 序列化工具、递归 CTE 查询模板、Caffeine 缓存配置及全局异常处理，不包含任何业务逻辑。
- **Owns (Exclusive Responsibilities)**:
  - Maven 父子 POM 统一依赖版本管理（Spring Boot 3、Spring AI、PostgreSQL Driver、TestContainers、Caffeine）
  - 全局 Spring Boot 自动配置（虚拟线程启用、Jackson JSONB 序列化、Caffeine 缓存管理器）
  - 递归 CTE 查询模板工具（通用图遍历查询封装）
  - 全局异常处理与统一错误码体系
  - 公共 DTO 基类与分页工具
- **Provides (External Capabilities)**:
  - 公共工具包 `common-utils`（JSONB 序列化、递归 CTE 模板、分页工具）
  - 公共配置包 `common-config`（虚拟线程配置、缓存配置、异常处理）
  - 接口类型: Maven 依赖引入（Java 公共库）
- **Depends On (Upstream Dependencies)**:
  - 无上游依赖（基础层为系统最底层）
- **Ubiquitous Language Glossary**:
  - **递归 CTE 模板（Recursive CTE Template）**: 封装了 WITH RECURSIVE 通用查询模式的工具类，业务 BC 传入表名、起始条件、递归条件、终止条件即可执行图遍历
- **Core Domain Aggregates**:
  - 无业务聚合根（纯技术基础设施）
- **Mapped Business Services**: N/A（基础设施 BC，不实现业务服务）

---

## BC Collaboration *(mandatory)*

### Foundation Contract Rules

#### Standard Access Rules

- **Contract release**: `foundation-core` 通过 Maven 坐标 `com.metaforge:foundation-core` 发布公共包（`common-utils`、`common-config`），所有业务 BC 通过 `pom.xml` 添加依赖引入。
- **Import guidance**: 业务 BC 在创建模块时，须在 `pom.xml` 中声明对 `foundation-core` 的依赖。`speckit.context.import` 命令可自动化此流程。
- **Adaptation principle**: 业务 BC 使用 foundation 提供的 JSONB 序列化器、递归 CTE 查询模板、缓存管理器时，仅需通过 Spring DI 注入合约接口。严禁修改 foundation-core 源码。
- **Module registration**: 新增 BC 时，在根 `pom.xml` 的 `<modules>` 中追加一条 `<module>` 声明即完成注册。

#### Foundation API Specification

| API Interface | Contract Class | Provided By | Consumed By |
|:---|:---|:---|:---|
| JSONB 序列化 | `JsonbSerializer` / `JsonbDeserializer` | `foundation-core` | 所有业务 BC |
| 递归 CTE 查询模板 | `RecursiveCteTemplate` | `foundation-core` | `semantic-query-engine`, `semantic-relation-network` |
| 缓存抽象 | `CacheManager` (Caffeine) | `foundation-core` | `metamodel-governance`, `metadata-management` |
| 全局异常处理 | `@RestControllerAdvice` | `foundation-core` | 所有 REST 暴露 BC |
| 虚拟线程配置 | `VirtualThreadConfig` | `foundation-core` | 所有业务 BC |

### Context Mapping Overview

| Upstream BC | Downstream BC | Collaboration Pattern | Rationale |
|-------------|---------------|-----------------------|-----------|
| `metamodel-governance` | `metadata-management` | Customer-Supplier (Conformist) | 元数据管理严格遵从元模型定义的结构模板，无定制化协商空间。下游完全接受上游发布的结构标准。 |
| `metamodel-governance` | `semantic-query-engine` | Published Language | 业务规则定义以结构化规则对象（Rule Definition）作为共享语言发布，查询引擎解析规则对象执行推理。 |
| `metamodel-governance` | `agent-consumption` | Customer-Supplier (Conformist) | Agent 消费接入严格遵从导出清单声明的命名空间边界，无协商空间。 |
| `metamodel-governance` | `semantic-relation-network` | Customer-Supplier (Conformist) | 关系网络严格遵从元模型定义的关系类型与基数约束。 |
| `metadata-management` | `semantic-relation-network` | Customer-Supplier + Event | 元数据实体变更后通过内存事件通知关系网络重建关系边。上游为数据源，下游响应变更。 |
| `metadata-management` | `semantic-query-engine` | Customer-Supplier | 查询引擎从元数据管理 BC 拉取实体内容用于查询和推理。MVP 阶段通过同步 REST 调用获取。 |
| `semantic-relation-network` | `semantic-query-engine` | Shared Kernel | 关系拓扑是查询引擎图遍历的骨架，两 BC 共享邻接表的关系数据模型。**MVP 特例**：关系数据物理上由 `semantic-relation-network` 写入，`semantic-query-engine` 仅具有只读查询权限。 |
| `semantic-query-engine` | `agent-consumption` | Customer-Supplier | 查询/推理结果是上下文生成和 Skill 响应的素材输入。消费 BC 组合上游查询结果进行格式化输出。 |

### Communication Standard

| Communication Type | Applicable Scenario | Standard Protocol | Mandatory Rule |
|--------------------|--------------------|-------------------|----------------|
| Synchronous REST Call | BC 间管理类查询（元模型结构查询、导出清单查询、元数据内容查询、关系拓扑查询） | RESTful HTTP + JSON | 连接超时 3s，读取超时 10s；仅允许读操作；禁止写穿透（跨 BC 写操作必须通过拥有方 BC） |
| In-Memory Domain Event | 元数据实体变更后通知关系网络 BC 重建关系边 | Spring ApplicationEvent | 同步事件，调用方线程内执行；事务内发布，失败回滚；不保证跨 BC 事务一致性 |
| MCP Protocol | Agent 消费方调用语义查询与推理能力 | MCP 标准协议 | 通过 Spring AI 发布工具；每个 MCP 请求携带 Agent 身份用于白名单过滤 |

### Core Cross-BC Events

| Event ID | Event Name | Producer BC | Consumer BC(s) | Core Payload |
|----------|------------|-------------|----------------|--------------|
| EVT-001 | MetadataEntityCreated | `metadata-management` | `semantic-relation-network` | entityId, elementTypeRef, bundleId, packageId, content(JSONB) |
| EVT-002 | MetadataEntityUpdated | `metadata-management` | `semantic-relation-network` | entityId, changedFields[], previousVersion |
| EVT-003 | MetadataEntityDeprecated | `metadata-management` | `semantic-relation-network` | entityId, deprecationTimestamp |
| EVT-004 | BundleVersionDeprecated | `metamodel-governance` | `agent-consumption` | bundleId, deprecatedVersion, successorVersion (nullable) |
| EVT-005 | BundleVersionPublished | `metamodel-governance` | `agent-consumption` | bundleId, newVersion, exportManifestSnapshot |

### Data Ownership Rules

- **单一权威源（Single Source of Truth）**: 每种核心业务数据仅有一个拥有者 BC——
  - 元模型定义 → `metamodel-governance`（独占写）
  - 元数据实体 → `metadata-management`（独占写）
  - 关系边 → `semantic-relation-network`（独占写）
  - Agent 授权 → `agent-consumption`（独占写）
- **跨 BC 数据库访问**: 所有 BC 共享同一 PostgreSQL 实例，通过独立 Schema 实现逻辑隔离。跨 BC 写操作严格禁止。跨 BC 读操作允许在以下条件同时满足时执行：
  1. 读取方 BC 仅执行 SELECT 查询，不执行 INSERT/UPDATE/DELETE
  2. 读取内容严格限定在已发布公共合约中声明的字段范围
  3. 读取方 BC 不得缓存或复制被读取数据到自有 Schema
- **MVP Schema 分离策略**: 
  - `metamodel_governance` Schema: bundles, packages, element_definitions, export_manifests, rule_definitions
  - `metadata_management` Schema: metadata_entities, entity_versions
  - `semantic_relation_network` Schema: relation_edges, entity_relation_index
  - `agent_consumption` Schema: agent_registrations, import_authorizations
  - `semantic_query_engine` 无自有 Schema，查询时跨 Schema 读取
- **数据同步规则**: 跨 BC 数据同步仅通过公共合约（REST 接口）或域事件实现，禁止直接跨 Schema INSERT/UPDATE

---

## Implementation Milestones *(mandatory)*

| Milestone | Deliverable BCs | Completion Criteria | Priority |
|-----------|-----------------|---------------------|----------|
| M1 基础设施阶段 | `foundation-core` | 1) Maven 父子 POM 工程搭建完成，所有 5 个业务 BC 模块注册就绪 2) Spring Boot 3 虚拟线程正常启用 3) JSONB 序列化器通过单元测试 4) 递归 CTE 查询模板通过集成测试 5) PostgreSQL 5 个 Schema 初始化脚本就绪 6) TestContainers 集成测试框架搭建完成 | P0 |
| M2 核心语义闭环阶段 | `metamodel-governance` → `metadata-management` → `semantic-relation-network` → `semantic-query-engine` | 1) 元模型定义→版本固化→发布→导入全链路可执行（Scenario 1） 2) 元数据创建→结构校验→关系自动构建→图查询链路可执行（Scenario 2） 3) 规则驱动一致性校验→异常标记报告链路可执行（Scenario 3） 4) 4 个 Core BC 的 REST 接口全部就绪并通过合约验证 | P1 |
| M3 Agent 消费接入阶段 | `agent-consumption` + M2 BC 集成 | 1) Agent 导入授权→白名单过滤全链路可执行（Scenario 1 的消费端） 2) MCP Server 发布并可用，Agent 可通过 MCP 连接执行精准查询、关联查询、全链路遍历（Scenario 4） 3) opencode Skill 文件封装就绪，可被 CLI 直接调用 4) 版本演进影响溯源链路可执行（Scenario 5） 5) 5 个 Business Scenario 全闭环验证通过 | P2 |

---

## Architecture Risk & Justification *(mandatory)*

| Architecture Decision | Necessity Rationale | Rejected Simpler Alternative & Reason |
|------------------------|---------------------|---------------------------------------|
| 使用 PostgreSQL 递归 CTE 替代图数据库实现图遍历查询 | MVP 阶段不引入图数据库以减少运维复杂度与技术栈膨胀。PostgreSQL 递归 CTE 在 1000 实体规模下的遍历性能完全满足 MVP 验证需求，且 SQL 标准语法团队熟悉度高。 | 嵌入内存图引擎（如 JGraphT）：数据持久化需额外同步逻辑，引入一致性风险；Neo4j 等专用图数据库：运维成本高，MVP 阶段过度工程化。 |
| 全业务 BC 共享单一 PostgreSQL 实例（Schema 隔离） | MVP 单实例部署，引入多数据库实例需额外容器编排与连接管理。Schema 级隔离在逻辑上满足 BC 边界独立要求，且递归 CTE 跨 Schema 查询性能优于跨库查询。 | 每 BC 独立数据库实例：MVP 运维复杂度显著增加，跨 BC 递归查询需要应用层数据聚合，增加网络开销。 |
| 5 个业务 BC 全部映射为独立 BC（而非合并为更少数） | 5 个候选子域在语义职责上差异显著——元模型定义（写规则）vs 元数据管理（写内容）vs 关系网络（写拓扑）vs 查询引擎（只读分析）vs Agent 消费（接入层）。合并会导致读写混合、职责重叠，违背单一职责原则。 | 合并为 3 个 BC（元模型+元数据、关系+查询、Agent 接入）：<br>元数据创建与结构校验强依赖元模型，合并后 AI 辅助的变更影响面过大；关系构建与查询推理的执行语义不同（写拓扑 vs 只读分析），合并不当。 |
| `semantic-query-engine` 无自有数据存储，通过跨 Schema 只读查询获取数据 | 查询引擎本质是"无状态计算层"，其输出均为对上游数据的聚合、遍历和推理结果。赋予其数据存储职责将导致与 `metadata-management` 和 `semantic-relation-network` 的数据重复，引入一致性风险。 | 为查询引擎建立物化视图缓存：MVP 500 实体规模下，实时 CTE 遍历性能足够，物化视图引入数据同步延迟与缓存失效逻辑，增加不必要的复杂度。 |
| BC 间使用同步 REST + 内存事件，不引入消息队列 | MVP 阶段无高并发、高可用诉求，同步调用链路清晰可调试。内存事件（Spring ApplicationEvent）足以覆盖元数据变更→关系重建的低频通知场景。 | 引入 RabbitMQ/Kafka：运维与开发学习成本显著增加，MVP 无异步解耦的强诉求，过早引入 MQ 违反最小化原则。 |
| MCP Server 由 `agent-consumption` BC 统一发布（非各 BC 分散发布） | 白名单过滤、上下文格式化、结构化输出均为消费接入层职责，统一放置确保权限过滤不可绕过。若各 BC 各自发布 MCP 工具，白名单过滤需在每个 BC 重复实现，增加不一致风险。 | 各 BC 独立发布 MCP 工具：渗透了全链路过滤原则（宪法 III），Agent 可能绕过 `agent-consumption` 直接访问底层 BC。 |

---

## Strategic Assumptions *(mandatory)*

- **ASM-001 纯元数据边界**: 系统仅存储元数据（概念、关系、规则的定义描述），不存储任何具体业务交易数据。所有下游设计以此边界为前提，一旦突破此边界，系统定位将从"基础设施"变为"业务系统"。
- **ASM-002 单进程单体部署**: MVP 阶段所有 BC 以 Maven 多模块方式编译，最终打包为单体 JAR 运行于单 JVM 进程。不涉及微服务拆分、服务发现、负载均衡。
- **ASM-003 PostgreSQL 为唯一持久化存储**: 不使用图数据库、文档数据库、搜索引擎或对象存储。JSONB 字段承载元数据的动态结构，邻接表承载关系拓扑，递归 CTE 承载图遍历。
- **ASM-004 无外部缓存中间件**: 不含 Redis，各 BC 独立使用 Caffeine 内存缓存，缓存数据不跨 BC 共享，服务重启后缓存自然清空，无持久化缓存一致性需求。
- **ASM-005 无消息队列**: BC 间异步通信场景（元数据变更→关系重建）使用 Spring 内存事件（ApplicationEvent），在调用方线程内同步执行。无消息持久化、重试、死信队列需求。
- **ASM-006 MCP 通过 Spring AI 发布**: `agent-consumption` BC 使用 Spring AI 框架将语义能力发布为 MCP Server 工具。消费端（opencode CLI）通过标准 MCP 客户端协议连接。不自行实现 MCP 协议栈。
- **ASM-007 消费端基于 opencode CLI**: 消费侧 Agent 环境为 opencode（AI-powered CLI），交互方式包括 AI slash commands（如 `/metaforge query`）和预封装 Skill 文件。不提供 Web UI 或其他前端界面。
- **ASM-008 单领域试点**: MVP 以 1-2 个明确边界的垂直业务领域作为元建模试点，单领域语义复杂度优先于跨领域泛化。后续扩展领域时，通过新增 Bundle 实现，不修改已有 BC 结构。
- **ASM-009 无增量元数据变更通知**: 关系网络 BC 通过 `metadata-management` 的内存事件感知实体变更。若未来引入异步批量元数据导入，当前同步事件机制需升级为消息队列。
- **ASM-010 [NEEDS CLARIFICATION: 元数据实体是否允许跨 Bundle 引用？当前架构假定实体引用关系限定在同一 Bundle 内，若需跨 Bundle 引用，关系边的所有权与校验规则需重新定义——涉及 `semantic-relation-network` 和 `metamodel-governance` 的协作边界扩展]**
