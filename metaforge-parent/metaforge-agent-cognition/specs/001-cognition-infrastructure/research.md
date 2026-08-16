# Research: 认知基础架构层技术决策

**Feature**: `001-cognition-infrastructure` | **Date**: 2026-08-11

## 1. 多模块拆分策略

### Decision: 四级 Maven 聚合（根聚合 + api/core/starter 三子模块）

**Rationale**:
- **契约与实现强制分离**：`-api` 模块仅含接口与数据结构，无任何实现依赖，下游 BC 仅需依赖 `-api` 即可安全调用认知能力
- **装配入口解耦**：`-starter` 模块作为聚合层，使 `metaforge-boot` 仅需依赖 `-starter` 即可接入整个 cognition 体系，未来扩展 `-dimensions` 与 `-templates` 模块时 zero-impact
- **运行时发现**：`-core` 不编译依赖 `-dimensions`，通过 Spring `@Autowired List<CognitionOperator>` 在运行时发现算子 Bean
- **符合 DDD 菱形架构**：api（契约层）↔ core（实现层）↔ starter（装配层），边界清晰

**Alternatives considered**:
- 单模块结构：契约与实现混放，下游 BC 会引入不必要的运行时依赖（如上游 api 传递依赖），违反宪法 III"契约与实现分层"
- api + core 双模块（无 starter）：`metaforge-boot` 需直接依赖 `-core`，未来扩展需修改 boot POM，违反"零影响 boot"装配目标

## 2. 上游依赖边界与 Port 模式

### Decision: Port 接口定义在 `-api` 模块，由 `-core` 基础设施层适配器实现

**Rationale**:
- Port 接口是纯粹的上游读取契约，非领域逻辑，放入 `-api` 可被 `-core` 和未来 `-dimensions` 模块共享
- 避免 domain 层受上游依赖污染（Port 方法签名可能引用上游 DTO，如 `EntitySchemaDto`）
- 读取操作不涉及领域逻辑，属基础设施适配范畴，Port + Adapter 模式天然匹配
- `-core` 的 `infrastructure/adapter/` 层通过注入上游 `api` 模块的 Application Service Bean 实现 Port 接口
- Port 仅定义生效态数据查询方法，不涉及草稿态、历史版本

**Alternatives considered**:
- Port 定义在 domain 层：domain 会依赖上游 DTO 类型，违反"领域层纯净"原则
- 直接在 core 中注入上游 Service：绕过 Port 契约，`-dimensions` 无法复用相同上游读取逻辑，且不利于单元测试 mock

## 3. 语义查询引擎能力全量 Port 契约化

### Decision: `ComputeEngineReadPort` 收敛 compute-engine 全部对外能力

**Rationale**:
- compute-engine 是无状态计算层，其全部能力（GraphQueryService, PathReasoningService, ImpactTracingService）都是本 BC 的依赖项
- 统一收敛到一个 Port 接口，确保本 BC 对 compute-engine 的访问只有唯一入口
- 禁止在 core 层直接注入 compute-engine api Service，必须通过 `ComputeEngineReadPortAdapter` 间接调用
- 便于未来 `-dimensions` 复用同一套计算能力契约，也便于单元测试时 mock

**Alternatives considered**:
- 分散使用上游 Service：违反"统一访问入口"原则，导致依赖关系不可追踪

## 4. 三重开放主机服务（OHS）定义

### Decision: REST + MCP + Application Service 三层同语义交付

**Rationale**:
- **REST**：通用系统集成与管理，通过 `POST /api/v1/cognition/{templateId}` 暴露
- **MCP**：Agent 生态原生接入，通过 Spring AI 发布 `cognition_execute` Tool
- **Application Service**：进程内调用，供同 JVM 下的其他 BC 或模块直接注入 `CognitionQueryService` 调用
- 三种服务共享同一模板路由语义（`templateId` 驱动），接口签名等价，仅传输协议不同
- BC 宪法 Override 1 将双协议提升为 MUST，REST + MCP 双通道为强制交付要求

**Alternatives considered**:
- 仅 REST 单通道：违反 BC 宪法 MUST 原则，Agent 生态无法原生接入
- 三套独立实现：增加维护成本，语义一致性风险高

## 5. 认知算子 SPI 设计

### Decision: SPI 接口定义在 `-api` 模块，实现类通过 Spring Bean 自动发现

**Rationale**:
- `CognitionOperator` 接口（含 `operatorId()`, `category()`, `execute()` 方法）定义在 `-api`，`-dimensions` 模块实现
- 算子通过类字段/注解声明 `category`（所属 8 分类之一），无独立元文件
- `-core` 启动时通过 `@Autowired List<CognitionOperator>` 发现所有 Bean，经 `OperatorRegistry` 校验注册
- 模板通过 `operatorId` 引用算子，跨分类混排，运行时按 category 分组执行与输出

**Alternatives considered**:
- 维度元文件 + levels 嵌套模型：层级复杂，新增分类需修改引擎核心，违反"声明式扩展铁律"
- 算子注册在 `-dimensions` 模块：违背"契约与实现分层"原则

## 6. 模板配置化与热加载

### Decision: YAML 模板文件由 `metaforge-boot` 统一托管，启动时 `TemplateScanner` 扫描注册

**Rationale**:
- 内置 6 个模板（DISCOVER/ORIENT/BRIEF/GUIDE/FORECAST/DELEGATE）存放于 boot 的 `classpath:cognition/templates/` 目录
- `TemplateScanner` 启动时扫描 classpath + 外部目录 `file:${META_FORGE_CONFIG}/cognition/templates/`，校验通过后注册到 `TemplateRegistry`（ConcurrentHashMap 缓存）
- 内置 classpath 模板不热加载；外部目录模板支持文件监听热加载（CREATE/MODIFY/DELETE 事件）
- 新增业务场景 = 新增 YAML 文件，零代码变更

**Alternatives considered**:
- 数据库存储模板：需要自建数据表，违反"无自有数据表"原则
- 全部模板热加载：classpath 模板为 jar 内资源，文件监听不可用且无业务必要性

## 7. 对象转换规范

### Decision: MapStruct 标准化转换，存放于 `-core` 的 `infrastructure/mapper/` 包

**Rationale**:
- MapStruct 是编译期代码生成工具，零运行时开销
- 覆盖 DTO ↔ 领域对象、上游 DTO ↔ 领域对象的双向转换
- 统一存放于 `infrastructure/mapper/`，禁止在 domain 层或 api 模块引入 MapStruct 依赖
- 领域层内部禁止使用对象转换工具，保持领域模型纯净

**Alternatives considered**:
- 手动编码转换：代码冗长、易出错、维护成本高
- ModelMapper：运行时反射，性能开销大，类型不安全

## 8. 配置规范化

### Decision: 所有配置属性前缀统一为 `metaforge.agent-cognition`

**Rationale**:
- 使用 `application-agent-cognition.yml` 独立配置文件，环境特定覆盖（`application-cognition-dev.yml`）
- 配置项包含：模板扫描路径、算子扫描路径、热加载开关与间隔、默认值（depth/archetype/format/max_tokens/page_size）、算子执行超时、Token 估算策略、版本锚策略、深度裁剪比例与最小保留数
- 所有配置项提供合理默认值，支持零配置启动

## 9. 平台能力复用决策

### Decision: 全面复用 foundation-core 平台能力，禁止重复实现

**Specific decisions**:
| Platform Capability | Reuse Strategy |
|---------------------|---------------|
| 统一响应格式 | 复用 `ApiResponse<T>`，禁止自定义响应包装类 |
| 异常处理 | 通过 `ExceptionHandlerSpi` 注册 34000-34099 段错误码 |
| 分页 | 复用 `PageRequest`/`PageResult<T>`，禁止自定义分页 DTO |
| JSONB 序列化 | 使用 `JsonbUtils`，禁止自定义序列化实现 |
| API 文档 | 仅使用 `@Tag` 注解分组，禁止自定义 SpringDoc 配置 |
| 国际化 | 注入 `MessageSource` 使用，禁止自定义 MessageSource bean |
| 缓存 | 模板注册表内部使用 Caffeine（key: `agent-cognition:template:<id>`） |

## 10. 运行时架构决策

| Decision | Rationale |
|----------|-----------|
| 虚拟线程 | Spring Boot 3 默认启用，虚拟线程减少算子并行调度时的线程切换开销 |
| 无数据库连接 | 本 BC 无自有数据表，不配置数据源、JPA、jOOQ、Flyway |
| 无消息队列 | MVP 阶段无异步解耦需求，算子调度在请求线程内完成 |
| 同步算子执行 | MVP 阶段算子顺序执行（按 priority 排序），并行执行留待 P1 迭代 |
| 结构化日志 | 每次请求记录：入口参数、路由 templateId、各算子耗时、最终错误码 |

## 10a. Step 1 地基收拢决策（核心引擎聚焦）

**Date**: 2026-08-13

### Decision: `-core` 收拢为纯引擎层，`-dimensions`/`-templates` 暂从构建移除

**Rationale**:
- **聚焦地基**：认知接口（SPI）、模板引擎、算子编排、输出组装是认知引擎的根基，先打牢再承载具体算子与模板
- **内置 agent 库需重构**：现 `-dimensions` 算子硬编码了内置库不存在的 `PROCESS_SEQUENCE` 关系、且能力算子不返回实体语义（对齐 opencode Tool 分层后需重构），`-templates` 模板基于旧元模型——先重构元模型，再重建算子与模板
- **空注册容错**：`OperatorRegistryInitializer`（`@Autowired(required=false) List<CognitionOperator>`）与 `TemplateScanner`（classpath 扫描）在无算子/模板时均不崩溃，`-core` 可独立编译运行

### Decision: 模板 config 支持双层结构（全局 + 算子级）

**Rationale**:
- `config.global` → `CognitionQueryContext.templateConfig`（全模板共享，兼容单层如 ORIENT `levelAliases`）
- `config.operators.{operatorId}` → `CognitionQueryContext.operatorConfig`（算子级精确配置，对齐 opencode 每个 Tool 的独立 parameters）
- `TemplateDefinition` 提供 `getGlobalConfig()`/`getOperatorConfig(operatorId)` 辅助，`OperatorOrchestrationService.buildContext` 透传

### Decision: `OperatorId` 正则放宽允许连字符

**Rationale**: 真实算子 ID（如 `ontological.bundle-discovery`）均含连字符，原正则 `[A-Za-z][A-Za-z0-9._]+` 拒绝导致编排期必崩；修正为 `[A-Za-z][A-Za-z0-9._-]+`。

---

## 10b. Step 2 内置库 V4 元模型重构决策

**Date**: 2026-08-13

### Decision: 全量重写 `V4__metamodel_governance_init.sql`（不破不立），移除 `V007`

**Rationale**:
- 旧 agent 库建模与 opencode Agent 架构错位：无 mode（primary/subagent/all）、无 Skill 分层、权限粒度粗、Agent 与主题域无直接关系
- 关系建模按场景重设计，遵循"**能力被多方使用、由使用方维护**"

### Decision: 能力分配关系（使用方 → Capability）

| 关系 | 方向 | 语义 |
|------|------|------|
| `AgentHasCapability` | Agent → Capability | Agent 拥有能力 |
| `TaskRequiresCapability` | Task → Capability | 任务需要能力 |
| `StepUsesCapability` | ExecutionStep → Capability | 步骤使用能力（替代 `CapabilityAssignedTo`） |

**Rationale**: Capability 是被动资源，Agent/Task/Step 均可引用，分配关系由使用方声明（而非 Capability 主动指向）。

### Decision: 域归属（主题域组成 Agent/Task）

| 关系 | 方向 | 语义 |
|------|------|------|
| `SubjectDomainComposesAgent` | SubjectDomain → Agent | 主题域组成 Agent |
| `SubjectDomainComposesTask` | SubjectDomain → Task | 主题域组成任务（替代 `TaskBelongsToSubjectDomain` 逆向） |

### Decision: Agent 完整组成与委派

| 关系 | 方向 | 语义 |
|------|------|------|
| `AgentUsesProfile` | Agent → AgentProfile | 认知原型 |
| `AgentHasPermission` | Agent → AgentPermission | 权限（含 authority_level） |
| `AgentExecutesTask` | Agent → Task | Agent 执行任务（委派目标，替代 AgentRole 间接链路） |
| `AgentDelegatesTo` | Agent → Agent | 子 Agent 委派 |

### Decision: 移除 AgentRole

**Rationale**: AgentRole 的 `authority_level`/`required_capabilities`/`bound_archetype` 三块职责分别被 `AgentPermission.authority_level`、`AgentHasCapability` 关系、`AgentProfile.archetype` 承接，冗余移除（含 `AgentHasRole`/`RoleAssignedToTask`）。

### Decision: 协议包引用关系（Capability → protocol.X）

| 关系 | 方向 | 语义 |
|------|------|------|
| `CapabilityImplementsHttp` / `McpTool` / `Cli` / `LocalMethod` | Capability → protocol.X | 能力引用具体协议实例（ASSOCIATION_REFERENCE） |

**Rationale**: relation_schema 是"类型对类型"无法多态，故每协议一条关系、协议包维护；`protocol-detail` 算子按 `relationSchemaFqnPrefix="metaforge:1.0.0.protocol."` 前缀查询（graph `RelationQueryRequest.relationSchemaFqnPrefix`），新增协议零改动。

### 测试数据

`metaforge-agent-cognition/seed/agent-library-seed.sql`（32 实体 + 41 关系实例），覆盖订单履约域 + 支付域闭环，手动/脚本初始化。

---

## 11. 未解决问题（NEEDS CLARIFICATION）

| # | Question | Decision Required | Status |
|---|----------|-------------------|--------|
| 1 | 模板 YAML 文件的具体存放位置是否与 `metaforge-boot` 协商？ | 确定 boot 模块中 `classpath:cognition/templates/` 目录由谁创建与维护 | RESOLVED: 由 boot 模块统一托管，本 BC 的 TemplateScanner 仅扫描该路径 |
| 2 | `metaforge-agent-cognition-dimensions` 模块何时启动开发？ | 确定 P1 迭代时间节点 | RESOLVED: P1 迭代实施，MVP 阶段仅 SP I接口先行 |
| 3 | MCP Server 的 Spring AI 版本与配置方式？ | 确定 Spring AI MCP Server 的具体启动类与配置 | RESOLVED: 使用 Spring AI 1.0.x MCP Server Boot Starter，配置由 foundation-core 统一管理 |
