# Feature Specification: 认知算子实现层

**Feature Branch**: `001-cognition-dimensions`

**Created**: 2026-08-11

**Status**: Draft

**Input**: User description: "根据 @docs/speckit/cognition-dimensions.md 内容，生成需求问题空间"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 本体论认知算子链 (ONTOLOGICAL) (Priority: P1)

Agent 需要从零开始探索一个业务领域——通过本体论分类下 7 个算子，按 bundle-discovery → package-explorer → entity-schema-inventory → relation-schema-inventory → domain-drilldown → instance-catalog → entity-profile 渐进链路逐层揭示领域全貌。每一步返回惰性节点（含 `has_children` + `suggested_next_call`），Agent 按建议逐步深入。

**Why this priority**: 本体论是 Agent 进入未知领域的"第一眼"——无本体论算子，Agent 无法获知"领域有什么"，后续所有能力全部失效。

**Independent Test**: 发送 DISCOVER 请求仅加载 ontological.* 算子，返回 Bundle→Package→EntitySchema→RelationSchema 完整链路，scope 过滤与 lazy 模式均正常工作。

**Acceptance Scenarios**:

1. **Given** 平台已发布 order:1.0.0 与 payment:1.0.0 两个 Bundle，**When** 调用 `ontological.bundle-discovery` 不限定 scope，**Then** 返回全部 Bundle 列表，每节点标记 has_children=true 并携带 suggested_next_call。
2. **Given** scope.bundles=["order:1.0.0"]，**When** 调用 `ontological.package-explorer`，**Then** 仅返回 order Bundle 的子 Package，其他 Bundle 不出现。
3. **Given** order:1.0.0 下存在 5 个 EntitySchema，**When** 调用 `ontological.entity-schema-inventory`，**Then** 返回 EntitySchema 清单，每项附带 instance_count 与 key_attributes。
4. **Given** 实体 Step_CheckInventory 存在，**When** 调用 `ontological.entity-profile`，**Then** 返回全属性字段 + EntitySchema 结构说明 + domain_location L1-L5 完整路径。

---

### User Story 2 - 结构论认知算子 (STRUCTURAL) (Priority: P1)

Agent 需理解实体的内部构成与外部归属——decomposition（由什么组成）、belonging（属于哪个整体）、domain-locator（知识树中的位置）三个算子提供结构认知。

**Why this priority**: 结构认知是 Agent 理解业务架构的核心能力——无法拆解复杂实体或定位其整体位置，Agent 决策会失去结构上下文。P1 与本体论构成 Agent 的空间认知基础。

**Independent Test**: 以 Step_CheckInventory 为输入同时调用三算子，验证 decomposition 子树根节点与 belonging 父链落在 domain-locator 路径上。

**Acceptance Scenarios**:

1. **Given** Step_CheckInventory 包含子步骤，**When** 调用 `structural.decomposition` FORWARD 方向，**Then** 返回 COMPOSITION 子树含所有子部件及层级关系。
2. **Given** entity 属于某整体，**When** 调用 `structural.belonging` BACKWARD 方向，**Then** 返回完整父链追溯至根。
3. **Given** entity 属于某 L3 域下 L5 叶子域，**When** 调用 `structural.domain-locator`，**Then** 沿 COMPOSITION 入边递归回溯至 L1 根节点，返回 L1→L5 路径坐标。

---

### User Story 3 - 关系论认知算子 (RELATIONAL) (Priority: P2)

Agent 需理解实体间的直接关联、邻域网络及变更影响传播链——direct-link（1 度）、neighborhood（N 度）、impact-trace（影响扩散与溯源）三级递进。

**Why this priority**: 关系认知是 Agent 推理基础——无影响传播链，Agent 无法预判变更的蝴蝶效应。P2 排在本体论和结构论之后。

**Independent Test**: 以 Step_CheckInventory 为起点依次调用三算子，验证 neighborhood 结果完整包含 direct-link 所有实体，impact-trace 正向扩散与反向溯源路径一致。

**Acceptance Scenarios**:

1. **Given** 实体通过多种关系与 5 个实体关联，**When** 调用 `relational.direct-link`，**Then** 按 AssociationType 返回 1 度出边+入边列表。
2. **Given** max_depth=2，**When** 调用 `relational.neighborhood`，**Then** 返回 2 度邻域内所有关联实体含间接关联。
3. **Given** 规则变更，**When** 调用 `relational.impact-trace` 正向扩散，**Then** 返回波及链路径；反向溯源时返回依赖该规则的上游实体。

---

### User Story 4 - 流程论认知算子 (PROCEDURAL) (Priority: P2)

Agent 需理解业务流程的端到端序列、局部导航及决策分支——flow-blueprint（蓝图）、adjacent-step（前后导航）、decision-branch（分支识别）三算子。

**Why this priority**: 流程认知是 Agent 执行任务编排的蓝图。P2 排在本体论和结构论之后。

**Independent Test**: 以订单履约域调用 flow-blueprint 获得序列后，以 CheckInventory 调用 adjacent-step 验证 prev/next 与蓝图一致；以 CheckPayment 调用 decision-branch 验证分支识别。

**Acceptance Scenarios**:

1. **Given** 订单履约域定义完整 PROCESS_SEQUENCE，**When** 调用 `procedural.flow-blueprint`，**Then** 返回最长路径序列（ReceiveOrder→...→CompleteOrder），每步标注 ENTRY/DECISION/EXIT。
2. **Given** CheckInventory 前驱=ConfirmOrder、后继=CheckPayment，**When** 调用 `procedural.adjacent-step`，**Then** 返回 prev/next 局部导航。
3. **Given** CheckPayment 出边 >1，**When** 调用 `procedural.decision-branch`，**Then** 识别决策分支点，每分支含 条件/PRIMARY-ALTERNATIVE 倾向/下游影响。

---

### User Story 5 - 约束论认知算子 (DEONTIC) (Priority: P2)

Agent 需获知实体的约束规则清单、每条规则的强制级别及触发条件与动作——rule-listing、level-classifier、condition-action 三算子逐层揭示约束语义。

**Why this priority**: 约束认知是 Agent 边界管控上游基础。P2 排在本体论和关系论之后。

**Independent Test**: 以 Step_CheckInventory 调用 rule-listing 获知规则后，用规则 FQN 依次调用 level-classifier 和 condition-action，验证级别分类与 condition/action 字段正确析出。

**Acceptance Scenarios**:

1. **Given** 实体关联两条规则，**When** 调用 `deontic.rule-listing`，**Then** 返回规则清单含每条摘要。
2. **Given** 两条规则 constraint_level 分别为 MANDATORY/RECOMMENDED，**When** 调用 `deontic.level-classifier`，**Then** 返回 [MANDATORY, RECOMMENDED] 级别标注。
3. **Given** 规则含 condition 与 action 字段，**When** 调用 `deontic.condition-action`，**Then** 返回触发条件与执行动作完整文本。

---

### User Story 6 - 能力论认知算子 (CAPABILITY) (Priority: P3)

Agent 需识别实体关联的执行能力清单、调用方式（REST/MCP/CLI/LocalMethod）及协议细节——tool-discovery、call-method、protocol-detail 三算子从"知道是什么"过渡到"知道怎么做"。

**Why this priority**: 能力认知是认知到行动的桥梁，但触发依赖前序分类建立完整认知上下文。P3 合理。

**Independent Test**: 以 Step_CheckInventory 调用 tool-discovery 获能力清单后，以 Cap_InventoryAPI 依次调用 call-method 和 protocol-detail，验证 call_method=REST 且 protocol 含 Http endpoint/method 等细节。

**Acceptance Scenarios**:

1. **Given** 实体关联两个能力，**When** 调用 `capability.tool-discovery`，**Then** 返回能力清单含基本摘要。
2. **Given** 能力的 call_method=REST，**When** 调用 `capability.call-method`，**Then** 识别为 REST 调用方式。
3. **Given** 能力含 interface_spec 字段，**When** 调用 `capability.protocol-detail`，**Then** 返回展开后协议细节（type/endpoint/method/schemas）。

---

### User Story 7 - 认知论与治理算子 (EPISTEMIC + GOVERNANCE) (Priority: P3)

认知论算子提供各 Bundle 版本锚点（时效性检查）；治理算子基于入口实体自动收窄 scope（蓝图→关联实体→Schema 三层收窄）。

**Why this priority**: 两者不属于独立探索能力而是对已有输出的增强与约束。P3 合理。

**Independent Test**: 任意请求响应携带 version_anchors；以 Step_CheckInventory 调用 scope-narrowing 返回三层收窄子集。

**Acceptance Scenarios**:

1. **Given** 两个 Bundle 已发布，**When** 发起任意认知查询，**Then** context_meta.version_anchors 包含各自已发布版本号。
2. **Given** Step_CheckInventory 为 entry，**When** 调用 `governance.scope-narrowing`，**Then** 返回三层收窄：(1)子蓝图 [ConfirmOrder,CheckInventory,CheckPayment,ReserveStock]；(2)关联规则；(3)去重 Schema 集合。

---

### Edge Cases

- scope.bundles 指定不存在 Bundle FQN 时，算应返回空结果而非报错。
- domain-drilldown 以未知 level 值过滤时，返回空结果并标注 level 无效。
- entity-profile/any entity-lookup 传入不存在 entity_fqn 时，返回失败 CognitionResult 标注 ENTITY_NOT_FOUND。
- decomposition 目标实体无 COMPOSITION 子节点时，返回空子树标注 NO_CHILDREN。
- impact-trace 正向扩散遇 scope 边界外实体时，在边界截断标注 scope_boundary_reached。
- flow-blueprint 目标域不存在 M1 蓝图实例时，返回空序列标注 NO_BLUEPRINT。
- rule-listing 目标实体无关联约束时，返回空规则列表。
- protocol-detail 目标能力无 interface_spec 字段时，返回空协议细节标注原因。
- Port 调用超时/上游不可用时，算子必须返回 CognitionResult.failure 含 UPSTREAM_UNAVAILABLE，不得抛异常。
- scope 过滤后所有结果为空时，仍正常返回成功状态（空结果），非失败。
- graph 遍历深度超合理阈值（如 maxDepth>5）时，自动限深标注 depth_limited。

## Requirements *(mandatory)*

### Functional Requirements

**算子实现通用规范**:

- **FR-001**: 每个 CognitionOperator 实现类必须通过 Spring 注册，使 OperatorRegistry 可在启动时发现并注册。
- **FR-002**: 每个算子的 operatorId 全局唯一，命名格式 `{category}.{能力名}`（如 `ontological.bundle-discovery`）。
- **FR-003**: 每个算子的 category 必须返回所属 DimensionCategory 枚举值，与类上声明一致。
- **FR-004**: execute(CognitionQueryContext) 必须返回 CognitionResult（含 operatorId/category/data/success/error）。
- **FR-005**: 算子失败时必须返回 CognitionResult.failure，不得抛出未捕获异常。required=false 的算子失败不得终止整体调用。

**Scope 裁剪**:

- **FR-006**: 所有算子查询必须按 context.scope 进行数据裁剪，只返回 scope 范围内结果。
- **FR-007**: 超 scope 范围实体不得出现在输出中，须标注已裁剪供上层汇总到 skipped_entities。

**Port 接口**:

- **FR-008**: 对上游 BC 数据访问统一通过只读 Port 接口（MetamodelReadPort/MetadataReadPort/GraphReadPort/ComputeEngineReadPort），禁止直接注入上游 Service。
- **FR-009**: Port 调用失败/超时时返回 CognitionResult.failure 标注 UPSTREAM_UNAVAILABLE，不得使调用崩溃。

**超时**:

- **FR-010**: 每个算子必须遵守 timeoutMs 超时约束，超时返回失败标注 OPERATOR_TIMEOUT。

**查询模式**:

- **FR-011**: 图边过滤模式: GraphReadPort 边查询 → MetadataReadPort 按 FQN 批量获取详情。
- **FR-012**: 领域树遍历模式: GraphReadPort 出入边遍历 → MetadataReadPort 获取节点详情 → 构建树。
- **FR-013**: 影响扩散模式: ComputeEngineReadPort.diffuseForward/traceBackward → 合并结果。
- **FR-014**: 全量清单模式: MetamodelReadPort 列举 → MetadataReadPort 统计实例数。

**本体论 (ONTOLOGICAL) 算子**:

- **FR-O01**: bundle-discovery 列出全部已发布 Bundle，支持 scope.bundles 过滤，返回 lazy 节点（has_children+suggested_next_call），产出 updated_scope.bundles。
- **FR-O02**: package-explorer 通过 Bundle/Package FQN 列出子 Package，支持 scope.packages 过滤，返回 lazy 节点。
- **FR-O03**: entity-schema-inventory 列出 EntitySchema 清单，每 Schema 含 instance_count+key_attributes，产出 updated_scope.entity_schemas。
- **FR-O04**: relation-schema-inventory 列出 RelationSchema 类型清单。
- **FR-O05**: domain-drilldown 沿 L1-L5 逐层下钻，支持 null 自动发现和指定 level 精确过滤，支持跨 Bundle EntitySchema FQN 作为 level，产出 updated_scope.domains。
- **FR-O06**: instance-catalog 列出某 EntitySchema 下全部 M1 实例，支持分页。
- **FR-O07**: entity-profile 返回单实体完整画像（全属性+EntitySchema 结构+domain_location 路径）。

**结构论 (STRUCTURAL) 算子**:

- **FR-S01**: decomposition 沿 COMPOSITION FORWARD 展开子树。
- **FR-S02**: belonging 沿 COMPOSITION BACKWARD 追溯父链。
- **FR-S03**: domain-locator 沿 COMPOSITION 入边递归回溯至 L1 根，返回完整路径坐标。

**关系论 (RELATIONAL) 算子**:

- **FR-R01**: direct-link 查询 1 度全出边+入边，按 AssociationType 分组。
- **FR-R02**: neighborhood 查询 N 度邻域（1-3 度）关联实体。
- **FR-R03**: impact-trace 支持正向扩散（BFS）+反向溯源（逆BFS）+完整路径详情。

**流程论 (PROCEDURAL) 算子**:

- **FR-P01**: flow-blueprint 沿 PROCESS_SEQUENCE 构建最长路径序列，每步标注 ENTRY/DECISION/EXIT；M1 蓝图不存在返回空并标注。
- **FR-P02**: adjacent-step 返回前一步（入边）+后一步（出边）局部导航。
- **FR-P03**: decision-branch 识别出边>1 决策分支点，每选项含条件+PRIMARY/ALTERNATIVE+下游影响。

**约束论 (DEONTIC) 算子**:

- **FR-D01**: rule-listing 通过 DEPENDENCY_INFLUENCE 入边+ASSOCIATION_REFERENCE 边+实体详情获取约束列表。
- **FR-D02**: level-classifier 从 constraint_level/level 字段分类 MANDATORY/RECOMMENDED/REFERENCE。
- **FR-D03**: condition-action 从 condition/action 字段析出触发条件与执行动作。

**能力论 (CAPABILITY) 算子**:

- **FR-C01**: tool-discovery 通过 ASSOCIATION_REFERENCE 出入边查询能力清单。
- **FR-C02**: call-method 从 call_method 字段识别 REST/MCP/CLI/LocalMethod。
- **FR-C03**: protocol-detail 展开 interface_spec 字段，通过 COMPOSITION 边展开 protocol 子类型。

**认知论 (EPISTEMIC) 算子**:

- **FR-E01**: freshness-check 对比 version_anchors 与缓存版本号判定新鲜度，所有模板输出自动携带不独立暴露 API。

**治理 (GOVERNANCE) 算子**:

- **FR-G01**: scope-narrowing 以 entry_entity_fqn 为锚三层收窄：(1)PROCESS_SEQUENCE 前后 1-2 步子蓝图；(2)关联实体 FQN 收集；(3)反查 entity_schema_fqn 去重。

**通用约束**:

- **FR-030**: 所有算子为确定性计算，不依赖 LLM 或向量语义相似度，结果可复现可审计。
- **FR-031**: 算子只读操作，不持有业务数据存储主权，不保存任务上下文/快照/会话。
- **FR-032**: 面向不确定场景的算子返回惰性节点模式（每层含 has_children+suggested_next_call）。
- **FR-033**: 内置八维分类为封闭集合，新增算子须归入既有分类，禁止创建新分类。

### Key Entities *(include if feature involves data)*

- **CognitionOperator**: 认知算子接口——operatorId/category/execute 三个核心方法，所有算子实现类必须实现并通过 Spring 注册。
- **AbstractCognitionOperator**: 抽象基类——持有 4 个 Port 字段，提供 scope 裁剪、分页、异常模板化等公共能力。
- **CognitionQueryContext**: 查询上下文——含 templateId/operatorId/category/scope/entityFqn/timeoutMs 等字段，execute 唯一入参。
- **CognitionResult**: 执行结果——含 operatorId/category/data/success/error，算子构造返回。
- **DimensionCategory**: 8 分类枚举——ONTOLOGICAL/STRUCTURAL/RELATIONAL/PROCEDURAL/DEONTIC/CAPABILITY/EPISTEMIC/GOVERNANCE，封闭不可扩展。
- **Scope**: 认知边界——bundles/packages/domainGroups/domains/entitySchemas 五字段，数据裁剪依据。
- **Port 接口**: 上游只读通道——MetamodelReadPort/MetadataReadPort/GraphReadPort/ComputeEngineReadPort，由 core 层 infrastructure/adapter 提供运行时注入。

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 8 分类 25 个认知算子全部实现并通过启动注册校验，注册成功率 100%。
- **SC-002**: 本体论 7 算子串行执行完毕端到端耗时不超过 3 秒（不含底层 BC 网络延迟）。
- **SC-003**: scope 过滤准确率 100%——scope 外实体不出现，scope 内实体不误裁。
- **SC-004**: 算子失败隔离率 100%——任一算子 failure 不得中断其他算子或导致调用崩溃。
- **SC-005**: Port 可用时所有算子执行成功率 100%；Port 不可用时 100% 返回 failure（UPSTREAM_UNAVAILABLE），不抛异常。
- **SC-006**: 同一输入同一数据状态下执行 10 次，返回数据完全一致，可复现率 100%。
- **SC-007**: 惰性模式 has_children 与实际子节点存在性一致（无假阳性/假阴性），suggested_next_call 指向算子均存在且可执行。

## Assumptions

- 底层 BC（metamodel/metadata/graph/compute-engine）Port 调用由 `metaforge-agent-cognition-core` adapter 提供，运行时注入到 AbstractCognitionOperator。
- DimensionCategory 8 分类枚举已在 `metaforge-agent-cognition-api` 契约层定义且稳定。
- CognitionQueryContext/CognitionResult 结构已在 api 层定义，本模块仅消费使用。
- AbstractCognitionOperator 提供 scope 裁剪/分页/异常模板化公共能力，算子通过继承复用。
- 算子不独立暴露 API 端点，所有消费通过引擎核心统一入口经模板层组合调用。
- 算子不关心被哪个模板消费，仅根据 CognitionQueryContext 执行查询返回结果。
- MVP 阶段算子启动时一次性注册，新增算子需重启（热加载留待后续迭代）。
