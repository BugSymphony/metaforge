# Feature Specification: 元认知指导层

**Feature Branch**: `001-agent-cognition-guidance`

**Created**: 2026-08-01

**Status**: Draft

**Input**: 基于 MetaForge 产品说明报告、四个底层 BC（metamodel/metadata/graph/compute-engine）的全面分析，构建面向 Agent 的元认知中间层（CognitionGuidance BC）产品需求问题空间。

**架构定位**: Server 侧 Java BC——纯结构化查询引擎，Java 引擎 + YAML 模板配置混合架构。消费端通过统一模板驱动入口 `/{templateId}`（内置 bundle-catalog / cognition-guidance / task-brief / step-guide / navigate / sub-task-brief 模板）获取自包含的元认知简报。不接受自然语言，不持有 LLM，无状态 FQN 寻址。

---

## Clarifications

### Session 2026-08-01

- Q: 单个视角执行应在多长时间后判定为超时？ → A: 每个视角 200ms 超时（与 500ms 总目标保持合理比例）
- Q: changeWatch 对底层事件的消费应采用什么可靠性保证？ → A: best-effort（尽力而为），错过的事件不重放，不引入持久化事件日志
- Q: 未指定 max_tokens 时，系统默认 Token 上限是多少？ → A: 默认 8000 Token（适配主流 LLM 上下文窗口，L2/L3 均可能触发裁剪）
- Q: YAML 配置文件（templates/perspectives）变更后是否需要重启服务方能生效？ → A: 需要重启服务，配置在启动时一次性加载至内存

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — 跨 Bundle 通用认知查询 (Priority: P1)

消费端接入任意 Bundle（不限于预定义领域），通过模板配置获取该 Bundle 知识的结构化认知视图。引擎通过图拓扑和字段名启发式自动识别适用实体类型，动态组合视角执行多 BC 查询，按深度裁剪和代理原型规则组装输出。

**Why this priority**: 多 Bundle 通用查询是本 BC 的核心架构能力——决定了引擎的扩展性和跨领域适用性。没有此能力，每次新增 Bundle 类型或调整视角组合都需要修改核心代码。

**Independent Test**: 对代码库 Bundle 调用认知查询，指定 composition_tree 和 relationship_graph 视角，验证返回正确的组成树和关系图谱，不包含与代码库无关的实体。

**Acceptance Scenarios**:

1. **给定** 一个代码库 Bundle 已有 Module/Package/Class/Method 四类 Schema 的 M1 实例和 COMPOSITION/DEPENDENCY_INFLUENCE 两类关系实例，**当**调用 cognitionGuidance 指定 perspectives 为 ["composition_tree", "constraint_set"]，**则**引擎自动发现——Class 和 Module 有 COMPOSITION 边（适用 composition_tree），Method 有 DEPENDENCY_INFLUENCE 边（适用 constraint_set）——仅激活对应视角，返回正确结果。

2. **给定** 全新接入的 Bundle 只有最基础的 M1 实例但尚未建立关系边，**当**调用 cognitionGuidance 指定 perspectives 为 ["relationship_graph", "constraint_set"]，**则**关系图谱因无关系边返回空标注"当前 Bundle 暂无关系实例"，constraint_set 因无 DEPENDENCY_INFLUENCE 边也返回空标注，entity_profile 正常返回每个实体的完整内容。

3. **给定** 多个 Bundle 同时传入 bundle_fqns，**当**调用 cognitionGuidance 指定 perspectives 为 ["constraint_set"]，**则**引擎对每个 Bundle 分别按图拓扑发现约束，合并为跨 Bundle 统一约束视图，标注来源 Bundle。

---

### User Story 2 — 实体上下文感知的过滤 (Priority: P1)

消费端需要了解与某个特定实体直接相关的认知——不需要全 Bundle 视图。传入 entity_fqn 后，引擎自动转为实体级上下文模式：Bundle 级视角跳过，实体级/双向视角通过图查询以 entity_fqn 为端点做图边过滤，仅返回关联到该实体的约束、能力、决策、关联实体。Bundle 范围由 entity_fqn 的 FQN 前缀即席恢复，不依赖任何已存上下文。

**Why this priority**: 实体级过滤是本 BC 在"执行时刻"的核心价值——消费端不应在全量简报中翻阅提取信息。图边过滤能力将上下文精准收敛到当前实体，大幅减少无关信息噪音。

**Independent Test**: 对指定实体调用 stepGuide，验证仅返回与该实体直接相关的内容，不包含全 Bundle 其他实体的信息。

**Acceptance Scenarios**:

1. **给定** 某 Bundle 蓝图有 8 个 ExecutionStep 实例，**当**调用 stepGuide 传入其中某一 Step 的 entity_fqn，**则**引擎从 entity_fqn 前缀恢复 bundle_fqns，按实体级上下文遍历所有实体级/双向视角——constraint_set 通过图查询过滤出相关约束；capability_catalog 按 entity_fqn 过滤出相关查询 API；decision_matrix 检测决策点。所有 Bundle 级视角跳过，通过 adjacent_context 提供局部导航。

2. **给定** entity_fqn 前缀不属于任何已发布 Bundle 或实体 FQN 不存在，**当**调用 stepGuide，**则**引擎校验 FQN 归属失败，返回 INVALID_ENTITY_FQN 错误码和候选列表。

---

### User Story 3 — 无状态 FQN 寻址与版本锚一致感知 (Priority: P1)

引擎不保存任何任务上下文——所有查询无状态、幂等、可重复。每次输出附带 data_version_anchors（各 Bundle 当前已发布版本号 + 查询时间戳），供消费端追溯数据版本。底层元数据发生变更时，changeWatch 事件驱动机制自动评估变更影响并生成影响报告；消费端对比前后两次查询的版本锚即可判定认知是否过期。

**Why this priority**: 无状态设计保证水平扩展、幂等与缓存友好——杜绝"过期快照"这一类状态一致性问题。

**Independent Test**: 相同的两次查询返回一致内容（除时间戳外）；version_anchors 与各 Bundle 实际已发布版本完全一致。

**Acceptance Scenarios**:

1. **给定** 引擎收到两次完全相同的 taskBrief 请求，**当**依次执行，**则**两次返回的视角章节内容一致（仅 context_meta 的查询时间戳不同）。

2. **给定** 调用 stepGuide 传入 entity_fqn，**当**引擎执行实体级查询，**则**Bundle 范围由 FQN 前缀自动限定，无需单独传入 bundle_fqns。

3. **给定** 底层一条规则实体版本从 v1 生效为 v2，**当** changeWatch 监听到元数据变更事件，**则**变更影响服务以变更实体 FQN 为起点正向扩散影响并生成报告。

---

### User Story 4 — 多级认知深度与代理原型适配 (Priority: P2)

不同代理场景对认知输出的详细程度和优先级侧重有不同要求。通过 cognition_depth（L1/L2/L3）控制输出粒度：L1 导航级最多 3 个视角；L2 执行级（默认）最多 7 个视角；L3 深化级全部 14 个视角全量展开。通过 agent_archetype 调整视角输出优先级——执行型代理约束和蓝图前置，探索型代理组成结构和关系图谱前置，审计型代理约束和影响追溯前置，编排型代理流程蓝图和决策图谱前置。同时支持 max_tokens 参数自动裁剪。

**Why this priority**: 深度和原型适配是"一刀切"到"按需服务"的分水岭。P2 在 P1 核心查询和实体过滤能力稳定后交付——通过配置化规则适配不同代理的消费需求。

**Independent Test**: 以 cognition_depth L1 调用 taskBrief，验证仅返回最多 3 个视角。以 agent_archetype exploration 调用，验证 composition_tree 排在输出第一位。以 max_tokens 极低值调用，验证自动降为 L1 模式。

---

### User Story 5 — 自包含标准化多格式输出 (Priority: P2)

所有认知查询结果遵循统一根 JSON 结构（context_meta + 各认知视角章节），以 FQN 为核心标识内联完整语义。消费端拿到输出后不需对任何底层 BC 做二次查询即可完整理解全部认知内容。支持 json（结构化 JSON）和 prompt（Markdown 格式语义说明，可直接注入大模型上下文）两种输出格式，两者语义内容完全一致。

**Why this priority**: 自包含标准化输出是"引擎→消费端"的接口契约，决定了消费端集成 MetaForge 的成本。

**Independent Test**: 调用 taskBrief 获取 JSON 格式输出，验证每个实体附带名称和语义描述；获取 Prompt 格式输出，验证语义内容与 JSON 完全一致且可直接注入 LLM 上下文；验证空视角保留章节标题标注。

---

### User Story 6 — 层级化子任务元认知指导 (Priority: P2)

父代理将任务中的特定实体委派给子代理执行。每个子代理通过 subTaskBrief（scope_mode=INHERITED）获得无状态收窄后的元认知简报——以 entry_entity_fqn 为锚执行三层收窄（蓝图收窄→实体收集→Schema 收窄）。同级子代理的简报由无状态构造天然隔离。scope_mode=PURE 仅返回该实体的 entity_profile。

**Why this priority**: 层级化子任务是真实代理编排中的核心场景——代理不应将所有子任务信息扁平推送给 LLM。

**Independent Test**: 对同一 Bundle 的不同步骤分别调用 INHERITED 收窄简报，验证各自简报内容相互隔离，不包含对方上下文。

**Acceptance Scenarios**:

1. **给定** 某 Bundle 蓝图第 2 步为库存校验，**当**子代理调用 subTaskBrief 指定该步骤为入口实体、scope_mode=INHERITED，**则**引擎执行三层收窄返回收窄版简报——规则仅与库存相关、Schema 仅 3 个、蓝图仅 2 步，不包含其他步骤的相关规则和 API。

2. **给定** 子代理 A 和子代理 B 分别以不同步骤调用 INHERITED 收窄，**当**调用各自简报，**则**子代理 A 看不到子代理 B 的支付网关和审批规则，子代理 B 看不到子代理 A 的库存 API。

---

### Edge Cases

- cognitionGuidance 中 perspectives[] 包含 Bundle 级视角但传入了 entity_fqn 时，Bundle 级视角自动跳过并在 context_meta 中标注。
- bundle_fqns 格式非法时返回 INVALID_BUNDLE_FQN 错误码。列表为空时返回 EMPTY_BUNDLE_FQNS 错误。
- 该 Bundle 没有任何 DEPENDENCY_INFLUENCE 边时，constraint_set 返回空并标注。
- 流程探索中遇到循环引用（PROCESS_SEQUENCE 形成闭环）时，自动截断，标注循环路径。
- 部分视角查询超时时（单视角超时阈值为 200ms），已完成视角正常返回，超时视角标注 truncated=true 和 TIMEOUT 原因。
- entity_fqn 前缀不属于任何已发布 Bundle 时，返回 INVALID_ENTITY_FQN 错误码和候选列表。
- Token 预算极低（max_tokens < 500）时，自动降为 L1 模式。
- cognitation_depth 超出有效范围时回退默认 L2。未知 agent_archetype 回退默认 execution。

---

## Requirements *(mandatory)*

### Functional Requirements

#### 认知视角

- **FR-PSP-001**: 系统必须提供 entity_profile 视角（scope=BOTH）——通过 FQN 精准查询获取实体的完整 M1 实例内容，通过元模型获取所属 EntitySchema 的结构定义用于字段语义解释。

- **FR-PSP-002**: 系统必须提供 domain_location 视角（scope=ENTITY）——从 entity_fqn 出发沿 COMPOSITION 入边反向追溯该实体在 L1-L5 业务知识树中的完整归属路径。未接入 L1-L5 分类树时返回空路径标注。

- **FR-PSP-003**: 系统必须提供 composition_tree 视角（scope=ENTITY）——递归展开指定实体的 COMPOSITION 树（FORWARD 子树 / BACKWARD 父链 / BOTH 双向），适用于任意 Bundle 的 COMPOSITION 关系拓扑。

- **FR-PSP-004**: 系统必须提供 relationship_graph 视角（scope=ENTITY）——以 entity_fqn 为中心展开关系邻域（默认 1-3 度），结果按 AssociationType 分组，每组列出关联远端实体及关系语义说明。

- **FR-PSP-005**: 系统必须提供 constraint_set 视角（scope=BOTH）——约束来源包括：(a) 以 entity_fqn 为端点沿 DEPENDENCY_INFLUENCE 入边和 ASSOCIATION_REFERENCE 边查询约束实体；(b) 获取每条约束实体的完整内容；(c) 从 EntitySchema 的 JSON Schema 定义中提取必填字段、枚举值、取值范围等硬边界。约束级别通过 constraint_level 字段自动识别（MANDATORY/RECOMMENDED/REFERENCE），无此字段默认 REFERENCE。

- **FR-PSP-006**: 系统必须提供 capability_catalog 视角（scope=BOTH）——以 entity_fqn 为端点获取能力实体 FQN 列表、能力实体详情，并自动展开 protocol 子类型详情。

- **FR-PSP-007**: 系统必须提供 flow_blueprint 视角（scope=BUNDLE）——将 PROCESS_SEQUENCE 关系拓扑构建为有序步骤序列。M1 蓝图实例不存在时返回空蓝图并标注。

- **FR-PSP-008**: 系统必须提供 decision_matrix 视角（scope=BOTH）——查询 entity_fqn 的 PROCESS_SEQUENCE 出边（>1 为决策点），评估每选项下游影响。

- **FR-PSP-009**: 系统必须提供 impact_trace 视角（scope=ENTITY）——执行正向影响扩散、反向依赖溯源、影响路径详情计算影响范围和依赖关系。changeWatch 内部委托此视角做变更影响范围计算。

- **FR-PSP-010**: 系统必须提供 prerequisite_chain 视角（scope=BOTH）——从 entity_fqn 出发沿 DEPENDENCY_INFLUENCE 入边反向追溯前置依赖链。

- **FR-PSP-011**: 系统必须提供 domain_navigation 视角（scope=BUNDLE）——沿 L1 SubjectDomainGroup → L2 SubjectDomain → Task 逐层下钻。默认懒加载——仅返回当前层子节点概要 + has_more；expand=all 时全量展开。

- **FR-PSP-012**: 系统必须提供 instance_catalog 视角（scope=BOTH）——按 entityTypes 过滤交付指定 Bundle 的 M1 实例及其关系清单。ENTITY 模式按 entity_fqn 做关系边过滤。

- **FR-PSP-013**: 系统必须提供 bundle_directory 视角（scope=BUNDLE）——交付平台已发布 Bundle 实例列表及其已填充的主题域树（L1→L2→Task）。

- **FR-PSP-014**: 系统必须提供 schema_inventory 视角（scope=BUNDLE）——枚举指定 Bundle 下已发布的所有 EntitySchema FQN 和名称，以及每类 Schema 的 M1 实例数量统计。某类型无实例时保留条目并标注 count=0。

#### 查询入口与内置模板

> 架构说明：本 BC 采用统一模板驱动架构——所有查询收敛为单一入口 `POST /api/v1/cognition/{templateId}`，通过 `cognition-templates.yml` 中预置的模板（bundle-catalog / cognition-guidance / task-brief / step-guide / navigate / sub-task-brief）路由到对应的视角组合与上下文模式。早期命名为 bundleCatalog / cognitionGuidance / taskBrief / stepGuide / navigate 的查询端点均映射为上述模板 ID。

- **FR-EP-001**: 系统必须提供统一查询入口 `POST /api/v1/cognition/{templateId}`——通过 templateId 路由到 cognition-templates.yml 中注册的模板配置（视角组合、深度裁剪、原型适配、上下文模式、输出格式），委托统一认知执行引擎处理后返回。模板未注册时返回 TEMPLATE_NOT_FOUND 错误码。

- **FR-EP-002**: 统一查询入口必须接受结构化请求体（CognitionRequest）——bundle_fqns（必填）、perspectives（可选）、entity_fqn（可选，传入则实体级上下文）、entityTypes（可选）、subject_domain_fqn（可选）、scope_mode（可选，INHERITED|PURE）、cognition_depth（可选，默认 L2）、agent_archetype（可选，默认 execution）、max_tokens（可选，默认 8000）、context_parameters（可选）、cursor/page_size（可选）、expand（可选）、format（可选，默认 json）。根据 entity_fqn 是否传入自动推导上下文模式。无任何任务 ID 参数。

- **FR-EP-003**: 系统必须预置 bundle-catalog 模板——组合式模板，返回已发布 Bundle 实例列表及其 L1 主题域分组→L2 主题域→Task 实例的主题域树。

- **FR-EP-004**: 系统必须预置 cognition-guidance 模板——统一元认知查询引擎，支持全部 14 个认知视角的动态组合执行，接受 perspectives 参数按需裁剪视角集合。

- **FR-EP-005**: 系统必须预置 task-brief 模板——一站式简报便利封装，内部按执行型代理语义组装 10 视角章节。接受 bundle_fqns（必填）、cognition_depth、agent_archetype、max_tokens、context_parameters。多次调用幂等。

- **FR-EP-006**: 系统必须预置 step-guide 模板——组合式模板，内部以实体级上下文执行 entity_profile、constraint_set、capability_catalog、decision_matrix、impact_trace、relationship_graph 六个视角的查询。附加 FQN 归属校验和 adjacent_context 组装。

- **FR-EP-007**: 系统必须预置 navigate 模板和 sub-task-brief 模板——navigate 为渐进懒加载导航入口，接受 anchor_fqn、level、cursor、page_size、expand 参数；sub-task-brief 支持 entry_entity_fqn 与 scope_mode（INHERITED|PURE），实现层级化子任务的三层范围收窄（见 FR-SCP-002）。

#### 层级化作用域与收窄

- **FR-SCP-001**: 系统必须通过 scope_mode 参数（INHERITED|PURE）区分层级化子任务与无状态工具调用。

- **FR-SCP-002**: INHERITED 模式下，引擎必须执行三层范围收窄：(a) 蓝图收窄——定向邻接遍历（relationTypes=[PROCESS_SEQUENCE]，从 entry_entity_fqn 出发）；(b) 实体收集——通过图查询收集关联实体 FQN；(c) Schema 收窄——从实体反查 entity_schema_fqn 去重。

- **FR-SCP-003**: 收窄后的认知输出必须同时包含父上下文中收窄到仅与子相关的部分和子自有部分。

- **FR-SCP-004**: 收窄结果必须相互独立隔离——同级子任务互不可见对方的上下文。

#### 变更影响感知

- **FR-CHG-001**: changeWatch 事件驱动机制必须以 best-effort 语义监听元数据变更事件和关系变更事件——仅处理当前存活实例收到的事件，错过的事件不重放也不引入持久化事件日志。以变更实体 FQN 为起点委托影响追溯视角计算受影响实体范围，生成影响报告。

#### 标准化输出格式

- **FR-OUT-001**: 所有认知指导输出必须以 FQN 为核心标识，内联实体语义摘要和关系语义说明。确保消费端无需二次查询底层 BC。

- **FR-OUT-002**: 所有查询输出必须遵循统一根 JSON 结构——context_meta（含 data_version_anchors）+ 各认知视角章节。

- **FR-OUT-003**: 系统必须透传底层计算引擎的截断标记（truncated/truncatedReason）并在 context_meta 中汇总标注。

- **FR-OUT-004**: 系统必须支持 json 和 prompt 两种输出格式，两者语义内容完全一致。prompt 格式为 Markdown 可直接注入大模型上下文。

#### 通用能力

- **FR-GEN-001**: 系统必须支持三级认知深度（L1 导航级 / L2 执行级 / L3 深化级）。未知值回退默认 L2。

- **FR-GEN-002**: 系统必须支持代理原型差异化（execution / exploration / audit / orchestration）。未知原型回退默认 execution。

- **FR-GEN-003**: 系统必须支持 max_tokens 参数（可选，默认 8000 Token）——超限时自动启动裁剪，裁剪保留全视角覆盖。max_tokens < 500 时自动降为 L1 模式。

- **FR-GEN-004**: 所有底层 BC 查询必须仅基于生效态正式版本数据执行。

- **FR-GEN-005**: 系统必须不持有底层 BC 数据存储主权、不保存任何任务上下文——不存储元数据/关系/元模型的副本，不存储快照/会话/继承链。仅存储自身 YAML 配置。

- **FR-GEN-006**: 所有查询端点必须不接受自然语言参数。自然语言到结构化的转换由消费层完成。

- **FR-GEN-007**: 所有元认知能力必须不依赖 LLM 或向量语义相似度。语义搜索通过 FQN 前缀查询和文本匹配实现。

- **FR-GEN-008**: YAML 配置文件（cognition-templates.yml / cognition-perspectives.yml）在服务启动时一次性加载至内存为 POJO 并缓存。运行时配置变更需重启服务方能生效，不实现热加载。

#### 实体级即时指导

- **FR-ENT-001**: 实体级约束过滤必须通过图查询以 entity_fqn 为端点查询 IN 方向（DEPENDENCY_INFLUENCE）和 OUT 方向（ASSOCIATION_REFERENCE）的边，合并去重获取约束实体 FQN 列表。遵从 FR-PSP-005 的约束查询机制，增加 entity_fqn 图边过滤限定。

- **FR-ENT-002**: 实体级能力过滤必须通过图查询以 entity_fqn 为端点查询 ALL 方向，结合 entityTypes 参数筛选对端实体绑定到能力类 Schema 的关系实例。

- **FR-ENT-003**: 实体级决策分析必须查询 OUT 方向（PROCESS_SEQUENCE）获取 entity_fqn 的所有出边——出边数量>1 为决策点，评估每选项下游影响。

- **FR-ENT-004**: 实体级影响追溯必须以 entity_fqn 为起点执行正向影响扩散（深度 3），通过反向追溯获取回滚路径。

- **FR-ENT-005**: 实体级输出必须包含 adjacent_context——查询 PROCESS_SEQUENCE 出边获取后几步 FQN 列表，入边获取前一步 FQN。

### Key Entities

> 所有视角输出实体的完整字段级定义以 `data-model.md` 为权威来源（含 14 个视角输出实体、配置模型、领域服务接口和上游客户端端口）。以下为概要描述。

- **QueryParameters（查询参数）**: 认知查询入参实体。包含 bundle_fqns（必填）、perspectives（可选）、entity_fqn（可选）、entityTypes（可选）、subject_domain_fqn（可选）、scope_mode（可选）、cognition_depth（可选，默认 L2）、agent_archetype（可选，默认 execution）、max_tokens（可选，默认 8000）、context_parameters（可选）、cursor/page_size（可选）、expand（可选）、format（可选）。无任何任务 ID 字段。

- **EntityProfile（实体画像）**: entity_profile 视角输出。包含实体的完整 M1 实例内容（全部属性字段）、所属 EntitySchema 的结构说明、历史版本信息。

- **DomainLocation（领域定位）**: domain_location 视角输出。包含从实体 FQN 沿 COMPOSITION 入边反向追溯的完整路径（每层含 fqn/name/description/entitySchemaFqn）。

- **CompositionTree（组成结构）**: composition_tree 视角输出。包含 COMPOSITION 关系展开的树形结构（nodes + children + depth），支持 FORWARD/BACKWARD/BOTH 方向。

- **RelationshipGraph（关系图谱）**: relationship_graph 视角输出。包含按 AssociationType 分组的邻域实体列表和关系列表。

- **ConstraintSet（约束规则）**: constraint_set 视角输出。包含 constraints（约束条目列表，含约束级别）、hard_boundaries（JSON Schema 硬边界）、soft_boundaries（ASSOCIATION_REFERENCE 软边界）。

- **CapabilityCatalog（能力目录）**: capability_catalog 视角输出。包含 capabilities（每个含 name、description、interface_spec 摘要、call_method、protocol 子类型详情）。

- **FlowBlueprint（流程蓝图）**: flow_blueprint 视角输出。包含 steps（步骤序列，每步含 FQN、name、preconditions、outputs）、branch_points、entry_step、exit_steps。

- **DecisionMatrix（决策图谱）**: decision_matrix 视角输出。包含 decision_points（决策点列表，每个含可选路径、触发条件、下游影响、推荐倾向）。

- **ImpactTrace（影响追溯）**: impact_trace 视角输出。包含 forward_impact（正向分层影响实体）、backward_dependency（反向分层依赖实体）、impact_paths（详细传导路径）。

- **PrerequisiteChain（前置依赖）**: prerequisite_chain 视角输出。包含按层级展开的依赖树（每层含实体 FQN、依赖类型、是否阻塞、实体当前状态）。

- **GuidanceResult（统一查询输出）**: 统一查询入口（任意 templateId）输出载体。根据模板配置的 perspectives[] 动态包含对应的认知视角章节和 context_meta。

- **TaskMetacognitionBrief（任务元认知简报）**: task-brief 模板输出。包含全部 10 个认知视角章节和 context_meta。纯 FQN 寻址、无任务 ID、幂等。详细结构见 data-model.md §2.3 TaskBriefResponse。

- **StepGuidance（实体即时指导）**: step-guide 模板输出。包含实体级过滤后的约束/能力/决策/关系和 adjacent_context。详细结构见 data-model.md §2.3 StepGuideResponse。

- **DataVersionAnchor（数据版本锚）**: 每次认知指导生成时记录的数据版本参照——各 Bundle 已发布版本号 + 查询时间戳。详细结构见 data-model.md §1.3 DataVersionAnchor。

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 消费端从"接入"到"获得可执行认知"遵循渐进式 FQN 导航——首交互即可获得领域树/任务列表，单次导航数据量 O(page_size)，可按需逐层下钻。

- **SC-002**: 单次一站式简报生成（10 视角全覆盖、L2 级深度、百级规则/步骤规模）响应时间不超过 500ms。

- **SC-003**: 领域定位的 L1-L5 路径追溯与 graph BC COMPOSITION 拓扑完全一致（100% 准确率）。

- **SC-004**: 约束清单按实体属性中 constraint_level 字段分类，准确率 100%。

- **SC-005**: 流程蓝图中的步骤序列与底层 PROCESS_SEQUENCE 关系拓扑完全一致。

- **SC-006**: L1 模式下简报 Token 量不超过 2000 Token。

- **SC-007**: 变更影响感知在底层事件发布后 200ms 内完成报告生成。

- **SC-008**: 版本锚与查询时各 Bundle 实际已发布版本一致率 100%。

- **SC-009**: 输出自包含——消费端无需二次查询底层 BC。

- **SC-010**: stepGuide（实体级上下文）响应时间不超过 150ms。

- **SC-011**: 同一视角在 metaforge Bundle 和用户自定义 Bundle 上的结构输出模式一致——仅实体类型的名称和来源 Schema 不同，组装逻辑相同。

- **SC-012**: 所有 14 个视角在任意 Bundle 上均正常工作，无额外配置需求。

---

## Assumptions

- 四个底层 BC（metamodel / metadata / graph / compute-engine）已提供生效态数据的查询能力——本 BC 不负责底层数据的查询实现。
- graph BC 的 5 种 AssociationType 关系类型已广泛用于标注各种 Bundle 的关系实例——这是视角的图查询基础。
- metaforge-consumer 负责将 Agent 自然语言转化为本 BC 所需的结构化参数。本 BC 不接受自然语言输入。
- MVP 阶段不实现：跨 Bundle 可见性过滤的完整授权体系、向量语义相似度驱动的 Bundle 匹配、LLM 辅助推理。
- 所有查询端点的幂等性由无状态设计保证，服务端不存储任何任务上下文或会话状态。
