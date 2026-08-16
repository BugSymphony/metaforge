# Feature Specification: 认知基础架构层 (cognition-infrastructure)

**Feature Branch**: `001-cognition-infrastructure`

**Created**: 2026-08-11

**Status**: Draft

**Input**: User description: "根据 @docs/speckit/cognition-infrastructure.md 内容生成需求问题空间"

## Clarifications

### Session 2026-08-11

- Q: 统一认知 API 的身份认证方式是？ → A: API 无独立认证层——scope 中的 bundles 白名单即为授权依据，调用方通过 MCP/REST 传输 scope 参数自我声明身份。
- Q: 认知算子是否支持运行时热加载？ → A: MVP 阶段算子仅启动时加载——算子在启动时一次性注册完成，后续新增算子需重启系统；热加载留待 P1 迭代。
- Q: MVP 阶段需要暴露哪些可观测性指标？ → A: 仅结构化日志——记录每次请求的入口参数、路由模板、各认知算子耗时、最终错误码（如有），不暴露实时指标端点。
- Q: 认知能力模型的落地形态是？（维度元文件 + levels 嵌套 vs 每个能力一个实现类） → A: 采用方案 A——彻底移除维度元文件与 levels 嵌套；认知算子的注册来源即实现类自身，分类通过类上 `category` 字段/注解声明（配置时同样以该字段标识所属分类，如 `RELATIONAL`）；模板配方与注册表引用算子标识。治理提示已记录：BC 宪法 II"声明式扩展铁律"要求声明式元文件 + SPI 实现 + 能力注册，方案 A 下"声明式元文件"由实现类的分类字段承担（仍为配置驱动、核心免改码）；FR-012 校验表述相应更新为"类声明分类与执行器实际归属一致性校验"；BC 宪法 I"纯机制层"定位不变——具体算子实现类仍由下游 BC 通过 SPI 挂载。
- Q: 认知能力行为单元采用什么规范术语？ → A: 采用"认知算子"（Cognition Operator）——每个认知算子 = 一个实现类（Spring Bean），类上 `category` 字段标识所属分类（8 分类枚举封闭集合之一，如 `RELATIONAL`），类名如 `RelationalDirectOperator`；取代原"子能力/能力级别"概念。原维度 `supportedLevels`/`levels` 语义由算子集合取代。
- Q: 模板算子配方（operatorRecipe）如何组织？ → A: 方案 A——一条配方 = 一个分类 + 该分类下算子清单；配方声明 `category`（如 RELATIONAL）与 `operators` 清单（如 [REL-1 直接关联, REL-7 子图模式]），required/timeoutMs 仍按配方维配置；分类即算子的分组键，运行时按分类+清单解析执行。
- Q: 认知深度裁剪（cognition_depth）的计算口径是什么？ → A: 方案 A——按分类（category）计数：L1 保留 ≤3 个分类、L2 ≤7 个分类、L3 全量；被保留的分类内算子全量保留；truncated_perspectives 记录被裁剪的分类名。原 FR-021"≤3 个维度视角"中的"视角"即分类视角，而非算子数。
- Q: 输出格式化（format）的扩展模型如何设计？ → A: 方案 A——定义 `OutputFormatter` SPI 接口（含 `supports(OutputFormat)` 与 `format(...)`），`json`/`prompt` 各为一个实现类（`JsonOutputFormatter`/`PromptOutputFormatter`），经 Spring 注入由格式化注册表按 format 分发；新增格式 = 实现接口 + 注册，引擎核心零改动。与算子 SPI 扩展方式一致，满足 BC 宪法 II"声明式扩展铁律"。
- Q: 引入算子模型后，Key Entities 与正文是否已全面同步？ → A: 已完成一致性审计与同步——Key Entities 新增 `OperatorRegistry`、`DimensionCategory`、`OutputFormat`、`TemplateRegistry` 实体，旧 `DimensionExecutor`/`DimensionQueryContext`/`DimensionResult` 更名后的规范实体直接以 `CognitionOperator`/`CognitionQueryContext`/`CognitionResult` 呈现；US-2 场景算子改用规范 operatorId（如 ontological.bundle-discovery），US-4 引用统一为 `relational.direct-link`；旧模型术语仅在 Clarifications 历史记录中保留（作为演进对照）。
- Q: required=true 的认知算子执行失败或超时时，模板整体响应语义？ → A: 模板整体失败，不产出部分结果——required=true 算子执行失败返回 `OPERATOR_EXECUTION_ERROR`(34009)、超时返回 `OPERATOR_TIMEOUT`(34008)；与既有 Edge Case（所有算子均 required=false 且全部失败 → 部分成功）互补，构成完整语义。
- Q: 算子配方（operatorRecipe）的组织形态？ → A: 采用方案 A（扁平清单）——每条配方直接列出任意分类的算子（`operators: [operatorId...]`，可跨分类混排），不再在配方中声明/按 category 分组；算子所属分类由实现类 `category` 字段决定，运行时按各算子分类分组执行与裁剪；`required`/`timeoutMs` 仍为配方级。取代此前 Q3-A"一条配方一个分类"的按分类分组组织（原"诀 CA"作演进对照）。
- Q: operatorRecipe 层级是否彻底移除？ → A: 是——模板定义不再有"配方列表"层级，`TemplateDefinition` 直接持有跨分类的 `operators` 扁平算子列表；每个算子条目 = `{ operatorId, priority, required, timeoutMs }`，required/timeoutMs 由配方级下放算子级，失败语义逐算子生效（required=true 失败→整体失败 34009/34008）。
- Q: 算子 priority 的语义与作用范围？ → A: 数值越大越优先（如 100 优先于 10），省略默认最低（0）；仅作用于算子执行顺序与输出内排列，与 agent_archetype 排序完全解耦，不参与裁剪。
- Q: cognition_depth 裁剪的粒度与依据？ → A: 改为算子级 priority 裁剪——depth（L1/L2/L3）决定保留上限，分类内算子按 priority 从高到低保留，分类仅作输出分组（dimensions.<category>），不再作为裁剪单位；truncated_perspectives 记录被裁剪的算子所属分类名。
- Q: agent_archetype 在算子约束中的角色？ → A: 方案 B（模板级配置）——archetype 是权限/能力维度（execution/exploration/audit/orchestration），非优先级：每个模板为各 archetype 单独配置可用算子集合（算子条目带 `archetypes` 白名单），引擎按请求的 archetype 过滤算子后执行；若请求的 agent_archetype 在该模板中不存在配置，不兜底、直接报错。
- Q: required 在 depth 裁剪中的作用？ → A: 方案 A——required=true 的算子豁免裁剪（强制保留、必须输出）；required=false 的算子按 priority 从高到低竞争剩余保留名额。取代前述"仅按 priority 裁剪"口径（priority 仍是 required=false 算子的唯一排序依据）。
- Q: depth（L1/L2/L3）保留上限的量化方式？ → A: 采用"比例制 + 最小保留数"——required=false 算子数 ≤ 最小保留数（默认 3）时全部保留、不做比例裁剪；超过最小保留数时按比例裁剪（L1 保留 1/3、L2 保留 2/3、L3 全量，比例可配置），且裁剪后保留数不低于最小保留数（取二者较大值）。required=true 算子不受此限制、恒强制保留。
- Q: 算子与模板两模块（-dimensions / -templates）为何暂从构建移除？ → A: 聚焦核心地基——`-core` 收拢为纯引擎层（认知接口 SPI、模板引擎、算子编排、输出组装），不再编译耦合具体算子实现与内置模板。算子与模板将依据内置 agent 库元模型重构后重建（-dimensions 与 -templates 目录保留源码待重建）。`-core` 通过 `@Autowired(required=false) List<CognitionOperator>` 与 `classpath:cognition/templates/` 扫描保持空注册容错，不崩溃。
- Q: 模板 config 是否支持算子级配置？ → A: 支持——config 采用双层结构 `{ global, operators }`：`global` 为全模板共享（CognitionQueryContext.templateConfig），`operators.{operatorId}` 为算子级精确配置（CognitionQueryContext.operatorConfig）。兼容单层结构（如 ORIENT `levelAliases`）自动视为 global。

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 统一认知 API 路由与消费 (Priority: P1)

Agent 或客户端通过统一的 REST 端点 `POST /api/v1/cognition/{templateId}` 发起认知查询请求，携带结构化参数（scope、params、format、cognition_depth 等），系统根据 templateId 路由到对应模板配置、编排认知算子执行、返回结构化认知结果。这是整个认知引擎的唯一对外入口，所有消费方通过该端点获取认知能力。

**Why this priority**: 统一 API 是整个 BC 的门面入口，没有 API 所有下游功能（模板注册、算子执行、输出组装）都无法被消费，是认知引擎的"喉舌"。

**Independent Test**: 发送 `POST /api/v1/cognition/DISCOVER` 请求（含合法 scope 与 params），系统返回 200 且响应包含 `template`、`context_meta`、`dimensions` 三个顶层字段。

**Acceptance Scenarios**:

1. **Given** 系统已启动且模板 DISCOVER 已注册，**When** Agent 发送 `POST /api/v1/cognition/DISCOVER` 携带 `{ params: { parent_fqn: null }, format: "json" }`，**Then** 返回 200，响应体包含 `template: "DISCOVER"`、`context_meta.version_anchors`、`dimensions.ontological.children` 全平台 Bundle 列表。
2. **Given** 系统已启动，**When** Agent 发送 `POST /api/v1/cognition/UNKNOWN` 携带合法请求体，**Then** 返回 404，错误码 `34001`（TEMPLATE_NOT_FOUND）。
3. **Given** 模板 DELEGATE 已注册且 scopeRequired=true，**When** Agent 发送 `POST /api/v1/cognition/DELEGATE` 不携带 `scope` 字段，**Then** 返回 400，错误码 `34005`（MISSING_SCOPE）。
4. **Given** 任意模板已注册，**When** Agent 发送 `POST /api/v1/cognition/{templateId}` 携带 `format: "prompt"`，**Then** 返回 200，响应体为 `{ format: "prompt", content: "Markdown 文本" }`，语义与 json 格式完全等价。
5. **Given** 任意模板已注册，**When** Agent 发送 `POST /api/v1/cognition/{templateId}` 携带 `format: "invalid"`，**Then** 返回 400，错误码 `34010`（INVALID_FORMAT）。

---

### User Story 2 - 模板注册表扫描与校验 (Priority: P1)

平台运维管理员无需手动注册模板——系统在启动时自动扫描 classpath 和外部配置目录下的模板 YAML 文件，对每个文件执行完整性校验（templateId 合法性、operators 非空、算子引用有效、算子所属分类合法、priority/archetypes 合法），校验通过的模板注册到缓存供运行时路由使用，校验失败的跳过并记录日志，不影响已注册模板的正常服务。

**Why this priority**: 模板注册表是 API 路由的核心基础设施——templateId 解析依赖注册表，没有注册成功的模板，API 无法路由到任何认知能力。与 User Story 1 共同构成最小可运行闭环。

**Independent Test**: 系统启动后，TemplateRegistry 缓存中包含所有 classpath 上校验通过的模板定义，调用 `resolve("DISCOVER")` 返回完整 TemplateDefinition，包含 operators、inputSchema、scopeBehavior、outputSchema。

**Acceptance Scenarios**:

1. **Given** classpath 上存在 `discover-template.yml` 文件且内容合法，**When** 系统启动完成，**Then** TemplateRegistry 缓存中包含 key="DISCOVER"，其 operators 列出 `ontological.bundle-discovery`/`ontological.package-explorer`/`ontological.entity-schema-inventory`/`ontological.relation-schema-inventory`。
2. **Given** 外部目录下存在一个 `custom-template.yml` 文件且内容合法，**When** 系统启动完成，**Then** 该模板被扫描并注册到缓存，可通过 `resolve("CUSTOM")` 获取。
3. **Given** 某个模板文件的 operators 引用了不存在的 operatorId，**When** 系统启动扫描该文件，**Then** 该模板被跳过不注册，WARN 日志包含文件名和不存在的 operatorId。
4. **Given** 某个模板文件的 templateId 字段为空或包含非法字符（如小写或特殊符号），**When** 系统启动扫描该文件，**Then** 该模板被跳过不注册，WARN 日志记录校验失败原因。
5. **Given** 外部模板目录下删除一个已注册的模板文件，**When** 热加载监听器检测到 DELETE 事件，**Then** 该模板从缓存中移除，后续请求返回 34001。

---

### User Story 3 - Scope 解析与过滤执行 (Priority: P1)

Agent 通过 scope 参数声明自己的认知边界（bundles、packages、domain_groups、domains、entity_schemas 五字段），系统在模板执行前验证 scope 合法性，在执行过程中将 scope 注入每个认知算子查询上下文，算子内部按 scope 裁剪查询范围，越界实体不输出并在 context_meta 中显式标注。DELEGATE 模板还产出收窄后的 delegated_scope 供子 Agent 使用。

**Why this priority**: Scope 是 BC 宪法 VII 的核心约束——所有认知查询必须受边界限制。没有 scope 解析，算子执行无法做白名单过滤，违反全链路权限过滤原则。与 API 入口和模板注册构成 P1 闭环的安全基座。

**Independent Test**: 发送 BRIEF 请求携带 `scope: { bundles: ["order:1.0.0"] }` 且 `entity_fqn` 属于其他 Bundle，系统返回 403 错误码 34004。

**Acceptance Scenarios**:

1. **Given** 模板 BRIEF 已注册且 acceptsScope=true，**When** Agent 发送 BRIEF 请求携带 `entity_fqn: "other:1.0.0.X"` 和 `scope: { bundles: ["order:1.0.0"] }`，**Then** 返回 403，错误码 `34004`（ENTITY_OUT_OF_SCOPE）。
2. **Given** 模板 DELEGATE 已注册且 scopeRequired=true，**When** Agent 发送 DELEGATE 请求携带 `scope: { bundles: ["order:1.0.0"], domains: [...] }`，**Then** 出参包含 `delegated_scope` 字段，其 bundles/domains/entity_schemas 均为父 scope 与子任务范围的交集。
3. **Given** 任意模板已注册且 scope 中 bundles 字段包含未发布的 Bundle FQN，**When** Agent 发送请求，**Then** 返回 400，错误码 `34003`（INVALID_SCOPE），提示无效的 Bundle FQN。
4. **Given** 模板 BRIEF 执行过程中发现关联实体超出 scope.domains 范围，**When** 系统组装输出，**Then** 关联实体不在输出中列出，context_meta.skipped_entities 包含被跳过的实体 FQN 列表。
5. **Given** 模板 FORECAST 执行影响扩散遍历，**When** 正向链路遇到 scope.bundles 外的实体，**Then** 在该边界截断遍历，并在输出中标注"scope boundary reached"。

---

### User Story 4 - 认知算子 SPI 加载与注册 (Priority: P2)

平台实现者可以通过实现 CognitionOperator 接口（类上 category 字段声明归属分类）来新增认知算子，无需修改引擎核心代码。系统在启动时通过 Spring 容器发现所有 CognitionOperator Bean，校验每个算子类声明分类的合法性与一致性，通过后注册到算子注册表供模板 operators 引用。

**Why this priority**: 认知算子 SPI 是扩展机制的基石（BC 宪法 II），但属于平台实现者的扩展能力而非消费端直接感知的能力。在 API、模板注册、Scope 三者就绪后，SPI 机制验证"SPI 扩展"可行性。P2 排在消费闭环之后。

**Independent Test**: 在现有 8 分类下新增一个测试认知算子——实现 CognitionOperator（类上声明 category）并在模板 operators 中引用，系统启动后 OperatorRegistry 可解析该算子且 execute 可正常调用。

**Acceptance Scenarios**:

1. **Given** 某个认知算子类声明 category=ONTOLOGICAL（category 字段/注解），**When** 系统启动，**Then** 该算子成功注册到 OperatorRegistry，可通过 operatorId() 返回值查找，且按 category 归入 ONTOLOGICAL 分类。
2. **Given** 某个认知算子类声明 category=STRUCTURAL，**When** 系统启动，**Then** 按 operatorId 无法通过 category=ONTOLOGICAL 的查询路径解析该算子。
3. **Given** 算子 "relational.direct-link"（关系论·直连）已注册且模板 operators 引用它，**When** 模板回调 `resolve("relational.direct-link")`，**Then** 返回已解析算子对象，execute 正常执行返回 CognitionResult。
4. **Given** 某个认知算子类存在但 category 字段缺失或非法，**When** 系统启动，**Then** WARN 日志记录该算子不可用，不注册到算子注册表。
5. **Given** DimensionCategory 枚举包含 8 个值，**When** 认知算子类声明 category 不属于枚举值，**Then** 该算子校验失败，不被注册。

---

### User Story 5 - 输出组装与格式化 (Priority: P2)

系统收集所有认知算子执行结果后，按 agent_archetype 过滤算子（模板级 archetypes 白名单）、按认知深度裁剪（算子级 priority，depth 决定保留上限）、按 Token 预算自动裁剪，生成 context_meta（包含版本锚、scope 应用范围、Token 估算、跳过实体列表），最终按 format 参数经 OutputFormatter SPI（json/prompt 各为实现类，注册表分发）组装为 json 结构或 prompt Markdown 文本。

**Why this priority**: 输出组装是认知管线最后一环，决定 Agent 消费体验。但依赖所有认知算子执行结果就绪后才生效——API 能路由、模板能加载、算子能执行后，输出组装才能完成全链路验证。P2 合理。

**Independent Test**: 发送 DISCOVER 请求携带 `format: "json"` 和 `max_tokens: 200`，系统返回的响应触发 L1 裁剪，context_meta.token_estimate ≤ 200，truncated_perspectives 标注被裁剪算子所属分类。

**Acceptance Scenarios**:

1. **Given** 模板 DISCOVER 执行完成且格式为 json，**When** 系统组装输出，**Then** 响应包含 `context_meta.version_anchors`（各 Bundle 已发布版本号）、`context_meta.scope_applied`（实际使用的 scope）、`context_meta.token_estimate`（Token 估算值）、`context_meta.generated_at`（生成时间戳）。
2. **Given** 请求包含 `cognition_depth: "L1"`，**When** 系统组装输出，**Then** required=true 的算子强制保留，required=false 的算子按 priority 从高到低保留（数量 ≤ 最小保留数时全部保留，超过时按 L1 比例 1/3 保留且不低于最小保留数），truncated_perspectives 标注被裁剪算子所属分类名称。
3. **Given** 请求包含 `agent_archetype: "audit"` 而模板 audit 配置仅含约束论（deontic）与影响类算子，**When** 系统执行，**Then** 仅执行并输出 audit 白名单内的算子，其他算子不执行、不输出。
4. **Given** 请求包含 `max_tokens: 500` 而完整输出 Token 估算为 4500，**When** 系统组装输出，**Then** 自动降为 L1 裁剪模式，context_meta.token_estimate ≤ 500。
5. **Given** 请求包含 `format: "prompt"`，**When** 系统组装输出，**Then** 响应格式为 `{ format: "prompt", content: "# DISCOVER 认知简报\n\n## 上下文元信息\n..." }`，内容与 json 格式语义完全等价。
6. **Given** 请求包含 `agent_archetype: "orchestration"` 而模板 DISCOVER 未为 orchestration 配置任何算子白名单，**When** 系统执行，**Then** 不兜底，返回错误码 `34012`（ARCHETYPE_NOT_SUPPORTED）。

---

### User Story 6 - 配置管理与行为参数治理 (Priority: P2)

平台运维管理员通过 `application-agent-cognition.yml` 配置文件统一管理引擎行为参数——模板扫描路径、算子扫描路径、热加载开关与间隔、默认值（cognition_depth、agent_archetype、format、max_tokens、page_size）、算子执行超时、Token 估算策略、版本锚策略。所有参数支持环境特定覆盖（dev/prod 配置文件），且零配置可用（所有参数具备合理默认值）。

**Why this priority**: 配置管理是运维面能力，在消费链路（API→模板→算子→输出）验证通过后才有运维治理的必要。P2 排在核心链路之后。

**Independent Test**: 修改 `application-agent-cognition.yml` 中 `metaforge.agent-cognition.defaults.max-tokens` 为 2000，重启后发送不带 max_tokens 的请求，系统使用 2000 作为默认 Token 预算。

**Acceptance Scenarios**:

1. **Given** `application-agent-cognition.yml` 中配置 `metaforge.agent-cognition.defaults.cognition-depth: "L2"`，**When** Agent 发送请求不携带 `cognition_depth` 字段，**Then** 系统使用默认值 L2 执行裁剪。
2. **Given** `application-cognition-dev.yml` 中配置 `metaforge.agent-cognition.templates.hot-reload.enabled: true` 且 `poll-interval-ms: 5000`，**When** 以 dev profile 启动系统并在外部模板目录新增模板文件，**Then** 5 秒内该模板被扫描并注册到缓存。
3. **Given** 未提供 `application-agent-cognition.yml` 文件（零配置场景），**When** 系统启动，**Then** 所有可配置项使用内置默认值，系统正常运行不报错。
4. **Given** `metaforge.agent-cognition.timeouts.operator-execute-default-ms: 5000`，**When** 某认知算子执行耗时 4500ms 后返回，**Then** 该算子结果正常纳入输出。
5. **Given** `metaforge.agent-cognition.version-anchor.bundle-resolve-strategy: "LATEST_PUBLISHED"`，**When** 系统生成 context_meta.version_anchors，**Then** 使用各 Bundle 最新已发布版本号。

---

### Edge Cases

- 当同一 templateId 在 classpath 和外部目录中同时存在时，外部目录的模板应覆盖 classpath 内置模板（外部优先）。
- 当模板热加载过程中外部模板文件被写入不完整内容（写入未完成）时，校验应失败并保留旧缓存，不因中间态文件污染注册表。
- 当请求携带 scope 为空（null 或缺省）且模板 scopeBehavior.scopeRequired=false 时，系统应正常执行，不报错；算子内部按 Scope.EMPTY 处理（全量无过滤）。
- 当所有算子都标记 required=false 且全部执行失败时，系统应返回部分成功状态，context_meta 中标注所有算子均失败且 token_estimate=0。
- 当某个 required=true 的认知算子执行失败或超时时，系统应返回模板整体失败——执行失败返回 `OPERATOR_EXECUTION_ERROR`(34009)、超时返回 `OPERATOR_TIMEOUT`(34008)，不产出部分结果。
- 当深度裁剪启动时，required=true 的算子不受裁剪影响（强制保留），仅 required=false 的算子按 priority 竞争保留名额；若 required=true 算子数量已超出 depth 保留上限，仍全部保留、输出相应扩张。
- 当请求 max_tokens 极低（< 500）时，系统自动降为 L1 深度，required=true 算子仍强制保留，required=false 算子仅保留最高优先级者。
- 当 required=false 算子数量较少（≤ 最小保留数）时，depth 裁剪不启动比例裁剪，全部保留；仅当算子数超过最小保留数时按比例裁剪。
- 当某个认知算子类 category 字段声明的分类非法时，该算子被跳过注册，不影响其他已注册算子。
- 当请求携带的 agent_archetype 在该模板的 operators 中不存在对应配置（无任何算子为其白名单声明）时，不兜底、直接报错返回 `ARCHETYPE_NOT_SUPPORTED`(34012)。

## Requirements *(mandatory)*

### Functional Requirements

**统一认知 API**:
- **FR-001**: 系统必须提供单一端点 `POST /api/v1/cognition/{templateId}`，接收 JSON 请求体，返回结构化认知结果。
- **FR-002**: 系统必须支持 `format` 参数输出 `json`（结构化 JSON）和 `prompt`（Markdown，可直接注入 LLM）两种格式，两种格式语义内容完全一致。
- **FR-003**: 系统必须对无效的 `templateId` 返回 `TEMPLATE_NOT_FOUND`(34001)，对无效的 `format` 参数返回 `INVALID_FORMAT`(34010)。
- **FR-004**: 系统必须同时通过 REST 端点与 MCP 协议双通道暴露认知能力，任一通道缺失视为未完成交付。

**模板注册表**:
- **FR-005**: 系统必须提供 `TemplateRegistry`，支持按 `templateId` 解析模板定义（operators、inputSchema、scopeBehavior、outputSchema）。
- **FR-006**: 系统必须在启动时自动扫描 `classpath:cognition/templates/*.yml` 和 `file:${META_FORGE_CONFIG}/cognition/templates/*.yml` 下的模板文件。
- **FR-007**: 系统必须校验每个模板文件的完整性——templateId 非空大写字母+数字+下划线；operators 至少声明一个算子条目；算子条目 = operatorId + priority + required + timeoutMs + archetypes 白名单，operatorId 可直接混排任意分类（不再声明配方级 category）；每个 operatorId 必须在算子注册表中存在，算子所属分类以其实实现类 `category` 字段为准，priority 为非负整数、archetypes 必须为 AgentArchetype 枚举子集。
- **FR-008**: 校验失败的模板不得注册到缓存，记录 WARN 日志。scopeRequired=true 时 acceptsScope 自动修正为 true。
- **FR-009**: 系统必须支持模板热加载——监听外部模板目录的文件变更事件（CREATE/MODIFY/DELETE），校验通过后原子替换缓存；内置 classpath 模板不支持热加载。

**认知算子 SPI 与注册**:
- **FR-010**: 系统必须定义 `CognitionOperator` 接口（operatorId、category、execute 等方法）及其配套的 `CognitionQueryContext` record 和 `CognitionResult` 结构。每个认知算子为独立实现类（Spring Bean），类上通过 category 字段/注解声明所属分类。
- **FR-011**: 系统内置 `DimensionCategory` 枚举（8 个分类：ONTOLOGICAL/STRUCTURAL/RELATIONAL/PROCEDURAL/DEONTIC/CAPABILITY/EPISTEMIC/GOVERNANCE），不可通过配置扩展；认知算子类上的 category 字段必须归入该枚举。
- **FR-012**: 系统必须通过 Spring 容器发现所有 CognitionOperator Bean，校验其类声明分类（category 字段/注解）的合法性与一致性后注册到算子注册表，供模板 operators 引用。MVP 阶段算子仅启动时加载，不支持运行时热加载。
- **FR-013**: 新增认知算子 = 实现 CognitionOperator 接口（类上 category 字段声明归属分类）+ 在模板文件中引用 operatorId，无需修改引擎核心代码。
- **FR-013a**: 算子条目 `priority` 决定算子的计算与输出优先级别——数值越大越优先（如 100 优先于 10），省略默认最低（0）；priority 作用于算子执行顺序、输出内排列，并作为 required=false 算子在 cognition_depth 裁剪时的竞争排序依据；与 agent_archetype 过滤相互独立。

**Scope 解析引擎**:
- **FR-014**: 系统必须在模板执行前校验 scope——若 scope 为空且 scopeBehavior.scopeRequired=true，返回 MISSING_SCOPE(34005)。
- **FR-015**: `bundles` 中每个 FQN 必须是已发布的 Bundle；`packages` 中每个 FQN 必须属于 bundles 中的某 Bundle；`entitySchemas` 中每个 FQN 对应的 EntitySchema 必须存在。
- **FR-016**: 认知算子执行时，引擎必须将 scope 注入 CognitionQueryContext，算子内部按 scope 裁剪查询范围。
- **FR-017**: 越界内容不得出现在输出中，须在 context_meta.skipped_entities 中显式标注被裁剪/跳过的范围。
- **FR-018**: 系统必须提供 DelegatedScopeGenerator，为子 Agent 生成收窄后的 delegated_scope，遵循交并规则（父 scope ∩ 子任务涉及范围）。

**输出组装器**:
- **FR-019**: 系统必须收集所有认知算子执行结果，按模板 outputSchema 组装为统一结构。
- **FR-019a**: 系统必须定义 `OutputFormatter` SPI 接口，包含 `supports(OutputFormat)` 与 `format(assembledOutput, contextMeta)` 方法；`json` 与 `prompt` 各为一个实现类（`JsonOutputFormatter`/`PromptOutputFormatter`），经 Spring 注入由格式化注册表按 format 参数分发。新增输出格式 = 实现 `OutputFormatter` 接口 + 注册，无需修改引擎核心代码。
- **FR-020**: 所有输出必须携带 context_meta，包含：template、version_anchors、scope_applied、token_estimate、generated_at、skipped_entities、truncated_perspectives。
- **FR-021**: 按 cognition_depth 裁剪输出——裁剪粒度为算子级：required=true 的算子豁免裁剪（强制保留、必须输出）；required=false 的算子按 priority 从高到低保留，采用"比例制 + 最小保留数"确定保留上限——required=false 算子数 ≤ 最小保留数（默认 3）时全部保留，超过时按比例保留（L1 保留 1/3、L2 保留 2/3、L3 全量），且保留数不低于最小保留数；分类仅作输出分组（dimensions.<category>），不再作为裁剪单位；truncated_perspectives 记录被裁剪算子所属的分类名。
- **FR-022**: 按 agent_archetype 过滤算子——archetype 为权限/能力维度（execution/exploration/audit/orchestration），模板中每个算子条目以 `archetypes` 白名单声明其可用 archetype；系统按请求的 agent_archetype 过滤算子后执行；若该 archetype 在模板中无任何算子配置，不兜底、直接报错返回 `ARCHETYPE_NOT_SUPPORTED`(34012)。
- **FR-023**: 当 max_tokens 超限时自动启动裁剪，token < 500 自动降为 L1。

**配置管理**:
- **FR-024**: 系统必须读取 `application-agent-cognition.yml`（或 `application.yml` 中 `metaforge.agent-cognition` 段落），支持环境特定覆盖。
- **FR-025**: 可配置项：模板扫描路径、算子扫描路径、热加载开关与间隔、默认值（cognition_depth/agent_archetype/format/max_tokens/page_size）、算子执行超时、Token 估算策略、版本锚策略、深度裁剪比例与最小保留数（depth.trim-ratio/min-keep）。
- **FR-026**: 所有可配置项必须提供合理默认值，支持零配置启动。

**API 安全约束**:
- **FR-027**: API 无独立认证层——调用方通过 scope 参数自我声明身份，scope 中的 bundles 白名单即为授权依据；系统依赖 Agent 导入声明中已授权的 Bundle 范围做边界约束。

**通用约束**:
- **FR-028**: 所有查询端点不接受自然语言参数，入参为确定性结构化参数。
- **FR-029**: 系统不持有底层 BC 数据存储主权，不保存任务上下文/快照/会话，仅存储自身 YAML 配置。
- **FR-030**: 所有认知能力不依赖 LLM 或向量语义相似度作为实现前提。
- **FR-031**: 输出自包含——消费端无需二次查询底层 BC 即可完整理解全部认知内容。
- **FR-032**: 系统必须在每次认知查询请求的关键路径上输出结构化日志，包含：请求入口参数、路由的 templateId、各认知算子执行耗时（毫秒）、最终响应状态与错误码（如有）。

### Key Entities *(include if feature involves data)*

- **TemplateDefinition**: 模板定义对象，包含 templateId（唯一标识）、templateName（中文名）、operators（跨分类扁平算子列表，每个条目 = operatorId + priority + required + timeoutMs + archetypes 白名单）、inputSchema（入参 JSON Schema）、scopeBehavior（acceptsScope/scopeRequired/producesUpdatedScope/scopeFields）、outputSchema（type/formats）、config（模板级配置数据 Map，支持双层结构 `{ global, operators }`——`global` 经 CognitionQueryContext.templateConfig 透传、`operators.{operatorId}` 经 operatorConfig 透传），是 API 路由与编排的配置依据。
- **TemplateRegistry**: 模板注册表——启动时扫描 classpath 与外部目录的模板 YAML，经完整性校验后将 TemplateDefinition 缓存，按 templateId 提供 resolve()，支持外部模板热加载与原子替换。
- **CognitionOperator**: 认知算子定义——认知能力的实现单元，每个算子为一个实现类（Spring Bean），通过类上 category 字段/注解声明所属分类（8 分类枚举之一），暴露 operatorId()、category()、execute(CognitionQueryContext) 方法，由下游 BC 经 SPI 挂载。
- **OperatorRegistry**: 算子注册表——通过 Spring 容器发现所有 CognitionOperator Bean，校验每个算子类声明分类（category 字段）的合法性与一致性后按 operatorId 注册，供模板 operators 引用，MVP 阶段仅启动时加载。
- **DimensionCategory**: 认知分类枚举——8 个封闭值（ONTOLOGICAL/STRUCTURAL/RELATIONAL/PROCEDURAL/DEONTIC/CAPABILITY/EPISTEMIC/GOVERNANCE），不可配置扩展；认知算子类上的 category 字段必须归入该枚举。
- **Scope**: 认知边界五字段 record——bundles、packages、domainGroups、domains、entitySchemas，Scope.EMPTY 表示无边界约束，贯穿 API 入参→校验→算子上下文→输出裁剪全管线。
- **CognitionQueryContext**: 认知算子查询上下文 record——包含 templateId、operatorId、category、scope、bundleFqns、entityFqn、templateParams、agentArchetype、cognitionDepth、cursor(Integer 页码)、pageSize、templateConfig（模板级全局配置）、operatorConfig（算子级配置，来自 config.operators.{operatorId}），是 CognitionOperator.execute() 的入参载体。
- **CognitionResult**: 认知算子执行结果对象——包含 operatorId（算子标识）、category（算子所属分类）、data（结果数据）、success（是否成功）、error（失败时的错误信息），由各认知算子返回。
- **ContextMeta**: 上下文元信息对象，包含 template（模板标识）、versionAnchors（各 Bundle 版本锚）、scopeApplied（实际使用的 scope）、tokenEstimate（Token 估算）、generatedAt（生成时间）、skippedEntities（被 scope 跳过的实体列表）、truncatedPerspectives（被认知深度裁剪的算子所属分类列表），内嵌在每个输出中。
- **OutputFormatter**: 输出格式化 SPI 接口——暴露 supports(OutputFormat) 与 format(assembledOutput, contextMeta)；json 与 prompt 各为一个实现类（JsonOutputFormatter/PromptOutputFormatter），经 Spring 注入由格式化注册表按 format 分发，新增格式通过实现接口扩展。
- **OutputFormat**: 输出格式枚举（json/prompt），作为 OutputFormatter.supports() 的分发键与 format 请求参数的取值来源，可由下游通过实现新 OutputFormatter 扩展。
- **AgentArchetype**: Agent 原型枚举——4 个封闭值（execution/exploration/audit/orchestration），是权限/能力维度而非优先级：模板中每个算子条目以 `archetypes` 白名单声明可用原型，系统按请求的 agent_archetype 过滤算子；请求原型在模板无配置时不兜底、直接报错。

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Agent 从发送认知查询请求到收到完整结构化响应的端到端耗时在认知算子全部返回正常结果时不超 3 秒（不含底层 BC 网络延迟）。
- **SC-002**: 系统启动时，classpath 上 6 个内置模板全部通过校验并注册到 TemplateRegistry 缓存，注册成功率为 100%（无合法模板被误判失败）。
- **SC-003**: 模板热加载从检测到文件变更到缓存原子替换完成的时间不超过 5 秒（受 poll-interval-ms 配置影响）。
- **SC-004**: 系统返回的所有认知查询响应中 context_meta 完整（含 version_anchors、scope_applied、token_estimate、generated_at），覆盖率为 100%。
- **SC-005**: scope 过滤功能对 entity_fqn 越界情况的阻断准确率为 100%（无误放），对 scope 内合法请求的通过率为 100%（无误拦）。
- **SC-006**: json 格式与 prompt 格式的输出语义内容完全等价——同一请求分别以两种格式发起，Agent 从中获取的认知信息量完全一致。
- **SC-007**: 所有标准化错误码（34001-34012）在对应异常场景下触发，错误响应包含错误码、错误级别、上下文消息，不得输出内部异常堆栈。

## Assumptions

- 底层 BC（metamodel BC、metadata BC、graph BC、compute-engine BC）的 API 接口已稳定可用，本 BC 通过客户端适配器调用其公开 API 获取元模型/元数据/关系图谱/计算结果。
- Spring Boot 框架作为运行时容器，依赖注入机制（`@Autowired List<CognitionOperator>`）用于认知算子 SPI 实现 Bean 的发现与关联。
- 模板文件（YAML）格式规范已由 `metaforge-agent-cognition-api` 模块的契约层定义，核心模块仅负责扫描、校验、加载；认知算子通过实现类声明（分类字段/注解）由用户模块提供，无独立 YAML 元文件。
- MVC + MCP 双通道交付依赖 `metaforge-framework` 提供的 REST 基础设施与 MCP Server 集成能力，本 BC 不自行实现协议层。
- MVP 阶段内置 6 个模板（DISCOVER/ORIENT/BRIEF/GUIDE/FORECAST/DELEGATE）覆盖完整的 Agent 消费管线，模板配置随 `metaforge-agent-cognition-templates` 模块以 jar 形式提供到 classpath。当前 `-templates` 模块暂从构建移除（Step 1 收拢地基），重建后恢复。
- 零配置可用意味着所有配置项具有生产级合理默认值，系统在缺少 `application-agent-cognition.yml` 时仍能正常启动并提供认知查询服务。
- 契约层模块（`-api`）仅包含接口与数据结构，不编译依赖底层 BC 或第三方实现模块；实现层模块（`-core`）编译依赖 `-api` 和 `metaforge-framework`，但不编译依赖 `-dimensions` 和 `-templates` 模块（Step 1 后两模块暂从构建移除，算子经 Spring SPI 空注册容错、模板经 classpath 扫描空注册容错）。
