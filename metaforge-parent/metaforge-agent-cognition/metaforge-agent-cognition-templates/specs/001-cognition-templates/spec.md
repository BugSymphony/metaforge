# Feature Specification: 认知模板配置层

**Feature Branch**: `001-cognition-templates`

**Created**: 2026-08-11

**Status**: Draft

**Input**: 根据 docs/speckit/cognition-templates.md 文件内容，生成需求问题空间

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Agent通过模板消费结构化语义认知 (Priority: P1)

Agent 在执行任务时需要获取业务语境下的结构化元数据认知。Agent 通过指定模板标识（如 DISCOVER、ORIENT）和对应参数，系统按模板声明的算子组合、入参契约和 scope 边界，返回结构化语义上下文，Agent 可直接将结果注入推理上下文窗口而无需二次解析。

**Why this priority**: 模板的全部存在意义就是让 Agent 能消费到结构化的语义认知。这是模板配置层对外的唯一价值出口，没有消费端闭环，模板定义本身毫无意义。

**Independent Test**: 向已注册的认知引擎提交一个 DISCOVER 模板请求（入参 `{ parent_fqn: null }`），验证返回结果包含完整的 Bundle 列表且格式符合 `outputSchema` 声明，结果可直接被 Agent 注入上下文。

**Acceptance Scenarios**:

1. **Given** 认知引擎已启动且 6 个模板全部注册成功，**When** Agent 调用 DISCOVER 模板 `{ parent_fqn: null }`（无 scope 限制），**Then** 返回全平台 Bundle 列表，按模板声明的 outputSchema 格式（DISCOVER_RESULT）输出，含 `has_children` 和 `suggested_next_call` 字段。
2. **Given** 认知引擎已注册模板，**When** Agent 调用 BRIEF 模板 `{ entity_fqn: "Task_InventoryCheck" }`（实体在 scope 内），**Then** 返回 entity_profile + flow_blueprint + constraint_set + capability_catalog + relationship_graph 五项完整结果。
3. **Given** 认知引擎已注册模板，**When** Agent 调用 DELEGATE 模板时不传 scope 参数，**Then** 系统返回 34005 错误码，表明 scope 为必填项。
4. **Given** 认知引擎已注册模板，**When** Agent 调用 BRIEF 模板查询一个 scope 外关联实体，**Then** 返回结果中 context_meta.skipped_entities 明确标注被跳过的实体及跳过原因。

---

### User Story 2 - 模板文件被引擎自动发现注册 (Priority: P1)

模板配置文件部署到 classpath 后，认知引擎核心在启动时自动扫描所有 YAML 模板文件，校验其合法性（字段完整性、算子引用有效性、archetype 闭合性等），并将合法模板注册到 TemplateRegistry，使其可被路由消费。

**Why this priority**: 模板必须被引擎发现并注册才能被消费。扫描-校验-注册是模板从配置文件变为可消费资源的必经链路，与 User Story 1 构成"定义→消费"闭环。

**Independent Test**: 将 6 个模板 YAML 文件放入 classpath，启动引擎核心，通过注册表查询确认 6 个模板全部处于注册且启用状态，token 结构完整可路由。

**Acceptance Scenarios**:

1. **Given** classpath 包含 6 个符合规范的模板文件，**When** 引擎核心启动并执行 TemplateScanner，**Then** 所有 6 个模板被扫描发现、校验通过、成功注册，注册表中可查询到每个模板的 templateId。
2. **Given** 某个模板 YAML 文件的 `operators` 引用了未注册的算子 ID（如 `nonexistent.operator`），**When** 引擎核心扫描到该文件，**Then** 该模板校验失败、不被注册、记录告警日志，不影响其余 5 个合法模板的正常注册和消费。
3. **Given** 某个模板文件缺少必填字段（如 `inputSchema` 为空），**When** 引擎核心执行模板校验，**Then** 该模板被标记为无效，不进入注册表，其他模板不受影响。
4. **Given** 某个模板的 `enabled` 字段为 `false`，**When** 引擎核心扫描并注册该模板，**Then** 模板注册信息保留但不可被路由消费，Agent 调用该模板返回不可用状态。

---

### User Story 3 - 开发者新增自定义模板无需修改引擎代码 (Priority: P2)

当业务域出现新的 Agent 消费场景时，开发者只需在模板配置目录下新增一个符合规范的 YAML 文件，声明新的 templateId、算子组合、入参契约和输出结构。引擎核心在下次启动时自动发现并注册新模板，无需修改引擎核心或算子实现层代码。

**Why this priority**: 这是 BC 宪法"增量式模板演进"原则的直接体现。模板配置层作为纯数据层，其扩展不应触发其他模块变更。P2 因为在 MVP 阶段 6 个内置模板已覆盖核心场景，扩展需求在后续迭代中产生。

**Independent Test**: 在 classpath 模板目录新增一个 v2 模板 YAML 文件（如 "audit-template.yml"），重启引擎核心，确认新模板与原有 6 个模板均正常注册并可路由消费。

**Acceptance Scenarios**:

1. **Given** 6 个内置模板已正常运行，**When** 开发者在模板目录新增一个符合规范的 YAML 文件（声明唯一 templateId、合法算子引用、完整必填字段），**Then** 重启后新模板被扫描发现、校验通过、注册成功，其余 6 个模板不受任何影响。
2. **Given** 新增模板的 templateId 与已有模板重复，**When** 引擎核心校验该模板，**Then** 模板因标识重复被拒绝注册，记录告警，已注册的同名模板继续正常服务。

---

### User Story 4 - 模板校验失败不影响已注册模板 (Priority: P2)

当存在多个模板文件时，任一模板的校验失败（字段缺失、算子非法、archetype 不在枚举范围内等）不得污染或中断其他已通过校验的模板的正常注册和消费。校验失败的模板仅被跳过并告警，不对外暴露。

**Why this priority**: BC 宪法明确要求"校验失败不得影响其他已注册模板"。配置层作为声明式数据，每个模板必须自包含且失败隔离。P2 因为属于容错性需求，不影响核心正向流程。

**Independent Test**: 构造一个场景——6 个合法模板 + 1 个非法模板（如 operators 为空数组），启动引擎核心，验证 6 个合法模板全部正常工作，非法模板仅产生告警日志。

**Acceptance Scenarios**:

1. **Given** classpath 包含 3 个合法模板和 1 个 operators 为空的非法模板，**When** 引擎核心扫描所有文件，**Then** 3 个合法模板全部注册成功并可消费，非法模板仅记录告警且不污染注册表。
2. **Given** classpath 包含 1 个引用不存在算子的模板和 2 个引用合法算子的模板，**When** 引擎核心执行校验，**Then** 非法模板被跳过，2 个合法模板正常工作，非法模板的算子引用错误被明确记录在告警中。

---

### Edge Cases

- 模板 YAML 文件格式损坏（非合法 YAML）→ 该文件被跳过，不影响其他模板文件加载，记录明确的解析错误告警。
- 模板 `operators` 数组中某算子的 `archetypes` 为空数组 → 该算子对任何 Agent archetype 都不可执行，模板整体不应注册。
- 模板声明的算子集合大于引擎实际注册的算子集合 → 仅缺失的算子导致校验失败，注明缺失的 operatorId 列表。
- 模板 `inputSchema.required` 声明了 `properties` 中不存在的字段名 → 模板结构自相矛盾，应视为无效。
- 模板文件编码非 UTF-8 → 扫描时解析失败，记录编码错误告警并跳过该文件。
- 两个模板文件使用同一个 templateId → 第一个被扫描到的正常注册，第二个因重复被拒绝并告警。

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系统 MUST 支持通过 YAML 文件声明 Agent 认知模板，每个模板文件声明一个完整的消费场景定义。
- **FR-002**: 每个模板 MUST 包含以下必填字段：`templateId`（大写字母+数字+下划线）、`templateName`（中文显示名）、`description`（功能描述）、`operators`（跨分类扁平算子列表）、`inputSchema`（JSON Schema 风格入参定义）、`scopeBehavior`（scope 行为声明）、`outputSchema`（输出结构定义）、`contextMeta`（context_meta 生成规则）。
- **FR-003**: 每个模板的 `operators` 字段 MUST 为非空数组，其中的每个算子条目 MUST 包含 `operatorId`、`required`（布尔值）、`archetypes`（execution/exploration/audit/orchestration 的子集），可选包含 `priority`、`timeoutMs`、`description`。
- **FR-004**: 每个模板引用的所有 `operatorId` MUST 已被认知引擎的算子注册表注册，引用不存在的算子 ID 时该模板校验失败。
- **FR-005**: 每个算子的 `archetypes` 白名单 MUST 为封闭集合 `{execution, exploration, audit, orchestration}` 的子集，且不得为空数组。
- **FR-006**: 每个模板的 `scopeBehavior` MUST 声明 `acceptsScope`（布尔）、`scopeRequired`（布尔）、`producesUpdatedScope`（布尔）、`scopeFields`（`{bundles, packages, domain_groups, domains, entity_schemas}` 的子集）。
- **FR-007**: 当 `scopeRequired` 为 `true` 时，`acceptsScope` MUST 自动视为 `true`，无论模板文件声明何值。
- **FR-008**: 每个模板的 `outputSchema` MUST 声明 `type`（输出结构类型名）和 `formats`（至少包含 `["json", "prompt"]`）。
- **FR-009**: 每个模板的 `contextMeta` MUST 声明 `includeVersionAnchors`、`includeScopeApplied`、`includeTokenEstimate` 三个布尔字段，可选 `includeSkippedEntities`。
- **FR-010**: 每个模板的 `inputSchema` MUST 遵循 JSON Schema Draft 2020-12 结构，必填参数通过对象级 `required` 数组声明。
- **FR-011**: 系统 MUST 保证模板标识（`templateId`）全局唯一，同一标识不得重复注册。重复标识的模板被拒绝注册并告警。
- **FR-012**: 校验失败的模板 MUST NOT 被注册到 TemplateRegistry 且 MUST NOT 对外提供路由消费服务，仅记录告警信息。
- **FR-013**: 任一模板的校验失败 MUST NOT 影响其他已通过校验的模板的正常注册和消费。
- **FR-014**: 模板通过 `enabled` 字段支持启停控制，`enabled: false` 的模板保留注册信息但不被路由消费。
- **FR-015**: 模板文件 MUST 命名为 `{小写templateId}-template.yml`，文件编码 MUST 为 UTF-8，扩展名仅支持 `.yml`。
- **FR-016**: MVP 阶段 MUST 内置 6 个认知模板：DISCOVER（元模型发现）、ORIENT（业务域定位）、BRIEF（任务/实体全景）、GUIDE（单步执行指南）、FORECAST（变更影响链路）、DELEGATE（子任务上下文委派）。
- **FR-017**: 新增模板 MUST 仅通过新增声明文件方式完成，MUST NOT 要求修改引擎核心或算子实现层代码。
- **FR-018**: 系统 MUST 在模板校验失败时记录告警信息，告警内容 MUST 包含失败原因及受影响的模板标识。
- **FR-019**: 模板文件格式损坏（非合法 YAML）或编码异常时，系统 MUST NOT 加载该文件，记录明确错误并继续处理其余文件。

### Key Entities

- **认知模板 (Cognition Template)**: 一份完整的 Agent 消费场景声明。核心属性：模板标识（全局唯一）、模板名称、描述、算子组合列表、入参契约、scope 行为边界、输出结构规格、上下文元数据规则。模板是纯声明式配置，不含执行逻辑。
- **算子条目 (Operator Entry)**: 模板中引用的单个认知算子声明。核心属性：算子引用标识、优先级权重、是否必须成功、超时限制、可用的 Agent 原型白名单。算子的具体实现不在本 BC 范围。
- **Scope 行为声明 (Scope Behavior)**: 定义模板对 scope（认知范围边界）的处理规则。核心属性：是否接受 scope 入参、scope 是否必填、是否产出更新后的 scope、生效的 scope 字段列表。
- **输入契约 (Input Schema)**: JSON Schema 风格的入参定义，声明模板接受的参数名、类型、约束、默认值及必填项。
- **输出结构 (Output Schema)**: 声明模板产出的结果类型名称及支持的输出格式（JSON/提示文本等）。

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 6 个内置模板全部通过扫描校验并成功注册，注册成功率 100%。
- **SC-002**: Agent 可通过模板标识路由到对应模板并获取符合 outputSchema 定义的结构化结果，路由准确率 100%。
- **SC-003**: 任一模板校验失败时，其余已注册模板的消费不受任何影响，隔离有效性 100%。
- **SC-004**: 新增一个模板文件从创建到被引擎发现注册，无需修改任何引擎核心或算子层代码，零模块变更。
- **SC-005**: 模板声明的所有必填字段完整性在校验阶段被强制检查，缺少任一必填字段的模板 100% 被拒绝注册。
- **SC-006**: 模板引用的算子合法性在校验阶段被强制检查，引用不存在算子的模板 100% 被拒绝注册。
- **SC-007**: 开发者通过阅读模板 YAML 文件即可完整理解一个消费场景的能力组合、入参、边界和输出，无需查阅代码，可理解性达到 100%。

## Assumptions

- 认知引擎核心的 TemplateScanner 已实现 classpath 扫描能力，本 BC 只负责提供模板 YAML 文件。
- 算子注册表（Operator Registry）已由 `metaforge-agent-cognition-dimensions` 模块提供，模板引用的 17 个算子 ID 均已在该注册表中注册。
- 所有模板 YAML 文件存放于 `src/main/resources/cognition/templates/` 目录下，此目录为引擎核心 classpath 扫描的约定路径。
- MVP 阶段不涉及模板热加载能力，模板变更需重启引擎核心生效。
- 模板文件的版本号遵循语义化版本规范，模板版本号与模板内容的变更独立管理。
- Agent 通过 REST API (`POST /api/v1/cognition/{templateId}`) 消费模板，API 端点由引擎核心层提供。
- 模板配置层不含任何 Java 代码，packaging 为 `jar`（仅 resources），以运行时依赖方式被 `metaforge-boot` 装配层引入 classpath。
