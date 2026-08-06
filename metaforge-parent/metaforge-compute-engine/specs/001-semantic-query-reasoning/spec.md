# Feature Specification: 语义查询与推理引擎问题空间

**Feature Branch**: `001-semantic-query-reasoning`

**Created**: 2026-08-01

**Status**: Draft

**Input**: 根据 docs/Metaforge 需求-查询推理引擎.md 文档内容，生成需求问题空间

## Clarifications

### Session 2026-08-01

- Q: 分页策略应如何分配？→ A: 检索型查询（FR-005）分页；结构型查询（邻接、子图、模式匹配、路径、溯源）返回完整结果，依赖深度/数量上限控制规模
- Q: 超时熔断阈值的默认值应设为多少？→ A: 2000ms（统一默认值，可通过配置覆盖）
- Q: FR-015 统一四维过滤中属性字段的匹配模式？→ A: 仅精准匹配（FQN 前缀过滤 + 实体/关系类型精确匹配 + 属性字段精确等值匹配），与 FR-005 的复杂匹配模式分离
- Q: FR-015 的四维过滤参数每维是必填还是可选？→ A: 全部可选，未传递的维度视为不限，调用方按需组合
- Q: 结果截断时是否需要统一截断标记？→ A: 所有结果载体统一增加 truncated（boolean）+ truncatedReason（枚举：DEPTH_EXCEEDED / COUNT_EXCEEDED / TIMEOUT）字段

### Session 2026-08-01

- Q: 遍历最大深度硬上限 5 是否太小，能否支撑下游上下文构建 BC？→ A: 默认 5 不变，硬上限提升至 10（可配置范围 1-10），为下游上下文构建 BC 预留扩展空间。
- Q: fqnPrefix 过滤的是元模型类型 FQN 还是数据实例 FQN？→ A: M1 层数据实例 FQN（metadata_entity.fqn / relation_instance.fqn 命名空间前缀），支持多前缀列表（OR 取并集）；M2 元模型类型通过 entityTypes/relationTypes 精确匹配。

### Session 2026-08-01

- Q: 传递关系与传导矩阵的配置方式？→ A: 开放配置文件（`application.yml`），在 `metaforge.compute-engine.transitivity-rules` 下定义 AssociationType 的传递属性映射表，脱离元模型独立维护。
- Q: FR-004 图模式匹配通配符 `*`/`?` 的匹配粒度——匹配整个 FQN 还是仅名称段？→ A: 匹配整个 EntitySchema/RelationSchema FQN（不拆分名称段），`*` = 任意完整 FQN，`?` = 任意关系类型完整 FQN。
- Q: 关系类型差异化深度的配置整合方式？→ A: per-type `maxDepth` 合并到同一传导规则配置表中，与 `transitive`/`direction`/`weightStrategy` 一起定义，单配置源管理。
- Q: FR-002 组合祖先链上溯的实现方式？→ A: 扩展 FR-002 增加 `direction` 参数（FORWARD=向下子树 / BACKWARD=向上父链 / BOTH=双向），复用 FR-001 的方向参数语义。
- Q: 增强过滤各维度的匹配模式指定方式？→ A: 每个 FQN 维度增加独立 `matchMode` 字段（PREFIX / EXACT），调用方显式指定；关系实例 FQN 额外支持 PATTERN 模式。

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 多维图遍历与检索 (Priority: P1)

作为调用方（语义上下文构建模块），需要获取指定实体在语义关系网络中的邻接关系、组合结构、子图范围或模式匹配结果，以便组装 Agent 所需的完整语义上下文。

**Why this priority**: 图查询是计算推理层和场景化分析层的能力基础，所有上层推理（路径推理、影响溯源）均依赖图查询层的遍历与匹配能力。P1 完成后方可进入 P2 推理域。

**Independent Test**: 对包含 100+ 实体、50+ 关系边的语义图谱执行 3 度邻接查询、组合层级树查询、子图提取查询、图模式匹配查询、多条件复合检索、批量语义查询六类查询，全部返回正确的结构化结果且附带实体与关系内联摘要。

**Acceptance Scenarios**:

1. **Given** 语义关系网络中存在实体 A 及其 3 度范围内的邻居实体与关系边，**When** 调用方以实体 A 为起点发起 3 度全方向邻接查询，**Then** 系统按层级返回每度发现的实体清单、关联关系 FQN、关系类型，同一实体在多路径出现时仅返回一次并标注最短深度。
2. **Given** 实体 B 的 FQN 前缀下存在组合层级子树（根+3层子节点），**When** 调用方发起组合层级树查询，**Then** 系统返回保留树形嵌套结构的结果，每个节点包含 FQN、实体类型、子节点列表、节点深度。
3. **Given** 语义关系网络中存在实体 C 且周边有多个关联实体，**When** 调用方以实体 C 为中心发起 2 度子图提取查询，**Then** 系统返回子图内的全部实体集合、全部关系集合及实体-关系邻接映射，实体与关系均附带内联摘要。
4. **Given** 语义关系网络中存在符合模式 `EntityA -[RelationX]-> EntityB -[RelationY]-> EntityC` 的路径实例，**When** 调用方发起图模式匹配查询，**Then** 系统返回所有匹配路径实例，每条标注实体 FQN、关系 FQN、实体类型、关系类型。
5. **Given** 语义关系网络中存在多种类型、多种属性值的实体，**When** 调用方以实体类型+属性条件组合发起多条件复合检索，**Then** 系统返回匹配的实体列表，支持与/或逻辑组合与分页排序。
6. **Given** 调用方持有 200 个实体 FQN 列表，**When** 发起批量语义查询，**Then** 系统一次性返回每个 FQN 对应的实体摘要及关联关系摘要，不存在的 FQN 单独标记状态不影响其他返回。
7. **Given** 调用方指定 FQN 前缀列表过滤参数（如 ["com.example.product.", "com.example.order."]），**When** 执行任意图查询，**Then** 仅返回 FQN 匹配任一前缀的实体和关系，超范围内容不参与遍历且不计入深度。

---

### User Story 2 - 路径推理与语义关联分析 (Priority: P1)

作为调用方（语义上下文构建模块、业务分析人员），需要查询两点间的可达路径、自动展开传递闭包、跨语义类型推理间接关联、快速判定路径可达性，以便理解实体间的间接关联关系和语义传导链路。

**Why this priority**: 路径推理是语义分析的核心能力，支撑依赖链分析、溯源分析等高级场景。与 P1 图查询域同为 P0 必保能力。

**Independent Test**: 对包含多种 AssociationType 关系类型的语义图谱，执行两点间路径查询（含最短路径）、传递闭包推理、多跳语义推理、路径可达性判定四种操作，全部返回正确的推理路径与可达实体集合。

**Acceptance Scenarios**:

1. **Given** 实体 D 与实体 E 之间存在多条可达路径（含最短路径），**When** 调用方发起两点间路径查询并指定仅查询最短路径，**Then** 系统返回最少边数的路径，按顺序列出途经实体 FQN 与关系 FQN，标注路径长度。
2. **Given** 传导规则配置中 COMPOSITION 被标记为传递关系，实体 F 通过多层 COMPOSITION 关联到多个子实体，**When** 调用方对实体 F 发起传递闭包推理，**Then** 系统返回所有通过 COMPOSITION 传递可达的实体 FQN 列表，按传递层级分层分组。
3. **Given** 传导规则配置中 COMPOSITION→ASSOCIATION_REFERENCE 为合法语义传导路径，实体 G 组成实体 H，实体 H 引用实体 I，**When** 调用方发起多跳语义推理（COMPOSITION→ASSOCIATION_REFERENCE），**Then** 系统返回从 G 到 I 的推理路径，标注每步的实体、关系、传导语义说明。
4. **Given** 实体 J 与实体 K 之间实际存在可达路径，**When** 调用方发起路径可达性快速判定，**Then** 系统在找到首条路径后立即返回可达结果及最短深度，响应时间显著低于全路径查询。

---

### User Story 3 - 影响溯源与变更评估 (Priority: P2)

作为业务分析人员，当实体发生变更时，需要快速了解该变更正向影响哪些下游实体、反向被哪些上游实体依赖，并能查看具体影响路径详情，以支撑变更评估、故障排查等决策场景。

**Why this priority**: 影响溯源是面向场景化分析的高级能力，依赖图查询与路径推理层能力。P2 优先级低于核心查询与推理能力。

**Independent Test**: 对包含多度关联关系的语义图谱，对指定起点执行正向影响扩散、反向依赖溯源、影响路径详情查询三种操作，全部返回正确的影响范围统计与路径明细。

**Acceptance Scenarios**:

1. **Given** 实体 M 通过多种关系类型关联到 3 度范围内的下游实体，**When** 业务分析人员对实体 M 发起正向影响扩散查询（指定关系类型、最大深度 3），**Then** 系统返回影响实体总数、按实体类型分层统计、影响实体明细（含 FQN、实体类型、所在层级、到达路径深度），同一实体被多路径影响时仅统计一次。
2. **Given** 实体 N 被多个上游实体依赖，**When** 业务分析人员对实体 N 发起反向依赖溯源，**Then** 系统返回所有依赖 N 的上游实体列表，按层级分组展示，附带实体与关系内联摘要。
3. **Given** 正向影响扩散查询已识别出实体 P 是实体 Q 的影响实体，**When** 业务分析人员查看 P 到 Q 的具体影响路径详情，**Then** 系统返回所有影响传导路径，每条标注途经实体 FQN、关系 FQN、关系类型、传导方向，路径自包含无需额外查询。

---

### Edge Cases

- 查询起点实体 FQN 不存在时，系统返回明确错误状态与提示，不返回空结果集。
- 遍历深度超过配置上限（默认 5 度，可配置至 10 度）时，系统返回已有结果并附带「深度超限、结果已截断」的提示信息。
- 过滤参数组合导致无任何结果时，系统返回空结果集并标注过滤条件摘要。
- 图模式匹配返回结果数量超过上限（500）时，系统截断返回并提示超限。
- 单次查询执行时间超过超时熔断阈值时，系统自动中断并返回已获取的部分结果及超时提示。
- 批量查询传入 FQN 列表为空时，系统返回空结果并标注原因。
- 批量查询传入的 FQN 列表超限（>200）时，系统拒绝请求并提示上限。
- 传递闭包推理中遇到循环引用时，系统自动去重并停止在该循环分支的进一步扩展。
- 多跳语义推理中指定的关系类型序列在传导规则配置中无合法传导定义时，系统返回空结果并说明无可用传导路径。
- 推理过程中依赖的下游模块（实体元数据管理、语义关系网络）不可用时，系统返回明确的服务不可用错误。
- 遍历过程中某 AssociationType 的 per-type `maxDepth` 超限时，该类型边停止进一步扩展，但不影响其他类型边在深度范围内的继续遍历。
- 组合层级树查询指定 BACKWARD 方向且当前实体无父节点时，返回空列表并标注起点的根节点标识。

## Requirements *(mandatory)*

### Functional Requirements

#### 多维图查询域

- **FR-001**: 系统必须支持指定起点实体 FQN 的多度邻接查询，支持正向（出边）、反向（入边）、双向遍历方向。遍历深度上限受两阶段约束：(1) 全局默认深度 `metaforge.compute-engine.traversal.max-depth`（默认 5，硬上限 10）；(2) 传导规则配置表中各 AssociationType 的 per-type `maxDepth` 限制（如 COMPOSITION 深 5、DEPENDENCY_INFLUENCE 深 2、ASSOCIATION_REFERENCE 深 1 等），遍历时取全局深度与类型深度中的较小值。不同关系类型在同一遍历中可达到不同的深度，且被类型深度超限截断的路径不再计入该类型边的进一步扩展。
- **FR-002**: 系统必须支持组合层级树查询，基于 COMPOSITION 关系递归展开指定节点的组合结构。支持 `direction` 参数指定遍历方向：FORWARD（从当前节点向下展开子树，保留树形嵌套结构）、BACKWARD（从当前节点向上追溯完整父链，返回扁平层级列表）、BOTH（从当前节点双向展开，向上父链 + 向下子树合并输出）。
- **FR-003**: 系统必须支持子图提取查询，给定一个或多个中心实体 FQN 及扩展深度（1~3 度），返回子图内全部实体集合、关系集合及邻接映射。
- **FR-004**: 系统必须支持线性路径图模式匹配查询，模式格式为 EntityTypeA -[RelationType]-> EntityTypeB ...，支持实体类型通配符 `*` 和关系类型通配符 `?`。`*` 匹配任意完整的 EntitySchema FQN（如 `order:1.0.0.pkg_order.Order`），`?` 匹配任意完整的 RelationSchema FQN，均不拆分名称段。模式长度上限 4 段（3 条关系边）。
- **FR-005**: 系统必须支持多条件复合检索，按实体类型、属性条件（精准/模糊/范围匹配）、关系条件组合过滤，支持与/或逻辑与分页排序。
- **FR-006**: 系统必须支持批量语义查询，单次最多传入 200 个 FQN，返回每个 FQN 对应的实体摘要与关联关系摘要，不存在的 FQN 单独标记。
- **FR-007**: 邻接查询与子图提取查询结果必须自动去重，同一实体在多路径中出现时仅返回一次并标注最短到达深度。

#### 路径推理域

- **FR-008**: 系统必须支持两点间路径查询，可指定遍历方向、关系类型、最大深度约束，支持查询全部路径或仅最短路径。
- **FR-009**: 系统必须支持传递闭包推理，基于 `metaforge.compute-engine.transitivity-rules` 配置文件中定义的 AssociationType 传递性（`transitive`）与方向（`direction`）自动识别可传递关系类型，计算指定起点的传递闭包并按层级分层输出。传递路径的遍历深度受各 AssociationType 的 per-type `maxDepth` 约束，遇到不可传递关系类型或深度超限时该分支截断。
- **FR-010**: 系统必须支持多跳语义推理，基于 `metaforge.compute-engine.transitivity-rules` 配置文件中定义的 AssociationType 传导矩阵（`direction`、`weightStrategy`），组合多种关系类型进行跨语义跳跃推理，最大跳跃步数 3 步。推理路径中相邻跳的关系类型须满足配置中定义的传导兼容性，权重策略（multiply/add/max）用于路径置信度或成本计算。
- **FR-011**: 系统必须支持路径可达性快速判定，找到任意一条可达路径即返回结果，性能优先于完整性。

#### 影响溯源域

- **FR-012**: 系统必须支持正向影响扩散查询，沿指定关系类型正向 BFS 扩展，返回分层统计的影响实体明细。
- **FR-013**: 系统必须支持反向依赖溯源查询，沿指定关系类型反向 BFS 追溯，返回依赖实体的分层统计明细。
- **FR-014**: 系统必须支持影响路径详情查询，返回指定两点间的所有影响传导路径，按长度排序，路径内联实体与关系摘要。

#### 通用能力

- **FR-015**: 所有查询接口必须统一支持以下过滤参数，各维度均为可选（未传递视为不限，调用方按需组合），过滤在遍历过程中实时生效，被过滤内容不参与遍历且不计入深度，各维度取交集（外层 AND）。每维内为集合（内层 OR），FQN 类维度均可通过 `matchMode` 字段显式指定匹配策略：
  - **AssociationType 类型**：枚举值列表（COMPOSITION / DEPENDENCY_INFLUENCE / PROCESS_SEQUENCE / ASSOCIATION_REFERENCE / MAPPING_CORRESPONDENCE），精确匹配；
  - **sourceFqns**：源实体 FQN 列表，`matchMode` 支持 PREFIX / EXACT；
  - **targetFqns**：目标实体 FQN 列表，`matchMode` 支持 PREFIX / EXACT；
  - **relationInstanceFqns**：关系实例 FQN 列表，`matchMode` 支持 PREFIX / EXACT / PATTERN（根据 FQN 格式做模式匹配）；
  - **entityTypes**：M2 EntitySchema FQN 列表，`matchMode` 支持 PREFIX / EXACT；
  - **relationTypes**：M2 RelationSchema FQN 列表，`matchMode` 支持 PREFIX / EXACT；
  - **propertyFilters**：属性字段 JSONB 精确等值匹配列表（仅精确等值匹配，不支持 matchMode）。
- **FR-016**: 所有查询与推理结果必须以 FQN 为核心标识，内联实体摘要（FQN、展示名、元模型类型 FQN）与关系摘要（FQN、关系类型、端点 FQN），确保下游无需额外补查询。
- **FR-017**: 所有查询默认仅基于生效态数据执行，历史版本、草稿版本、已下线版本不参与默认计算。
- **FR-018**: 所有推理规则必须唯一来源于已发布的元模型定义（EntitySchema/RelationSchema 定义、AssociationType 语义枚举）及 `metaforge.compute-engine.transitivity-rules` 配置，不引入外部知识、自定义规则或 LLM 辅助推理。其中实体/关系结构来源于元模型，AssociationType 传递性与传导矩阵来源于配置。
- **FR-019**: 单次查询必须设置超时熔断机制，默认超时阈值 2000ms（可通过配置覆盖），超时后自动中断并返回已获取的部分结果及超时提示。
- **FR-020**: 多度查询、路径推理、传递推理、影响溯源等图遍历操作统一受深度约束，层次为：(1) 全局默认深度 `metaforge.compute-engine.traversal.max-depth`（默认 5，硬上限 10，范围 1-10）；(2) 传导规则配置中各 AssociationType 的 per-type `maxDepth`（如 COMPOSITION=5、DEPENDENCY_INFLUENCE=2、ASSOCIATION_REFERENCE=1），遍历时取全局深度与类型深度中的较小值。超出配置深度返回截断提示及 `DEPTH_EXCEEDED` 标记。
- **FR-021**: 所有查询输入必须校验合法性（非空 FQN、有效枚举值、数值范围），非法参数返回明确错误码与原因。
- **FR-022**: 分页仅适用于检索型查询（FR-005 多条件复合检索）；结构型查询（FR-001 邻接查询、FR-002 组合层级树、FR-003 子图提取、FR-004 图模式匹配、FR-008~FR-014 路径推理与影响溯源类）返回完整结果，以深度上限和数量上限控制输出规模，不分页截断。
- **FR-023**: 所有查询结果载体必须包含统一截断标记字段 `truncated: boolean` 和 `truncatedReason` 枚举（DEPTH_EXCEEDED / COUNT_EXCEEDED / TIMEOUT），确保下游可区分完整结果与截断结果。
- **FR-024**: 系统必须通过 `metaforge.compute-engine.transitivity-rules` 配置统一管理 AssociationType 传导规则，每个类型包含 `transitive`（是否可传递）、`direction`（forward/backward/directed/bidirectional）、`weightStrategy`（multiply/add/max）、`maxDepth`（该类型遍历深度上限）、`description`（传导语义说明）属性。配置启动时加载并可用于运行时热更新。

### Key Entities

所有结果载体均包含统一截断标记 `truncated: boolean` 与 `truncatedReason` 枚举（DEPTH_EXCEEDED / COUNT_EXCEEDED / TIMEOUT）。

- **GraphQueryResult**: 图查询结果载体，包含实体集合、关系集合、邻接映射，所有实体与关系均附带内联摘要（FQN、展示名、类型 FQN、端点 FQN）。
- **PathResult**: 路径推理结果载体，包含路径列表、每步途经的实体与关系排序及各跳语义说明。
- **ClosureResult**: 传递闭包结果载体，包含按层级分组的可达实体列表、到达最短距离、途经关系类型统计。
- **ImpactTraceResult**: 影响溯源结果载体，包含影响实体总数、按类型分层统计、影响实体明细及关联关系明细。
- **FilterCriteria**: 过滤参数集合，各维度取交集生效（外层 AND），每维内集合取并集（内层 OR）：
  - `associationTypes`：AssociationType 枚举值列表，精确匹配；
  - `sourceFqns`：源实体 FQN 列表 + `matchMode`（PREFIX / EXACT）；
  - `targetFqns`：目标实体 FQN 列表 + `matchMode`（PREFIX / EXACT）；
  - `relationInstanceFqns`：关系实例 FQN 列表 + `matchMode`（PREFIX / EXACT / PATTERN）；
  - `entityTypes`：M2 EntitySchema FQN 列表 + `matchMode`（PREFIX / EXACT）；
  - `relationTypes`：M2 RelationSchema FQN 列表 + `matchMode`（PREFIX / EXACT）；
  - `propertyFilters`：属性字段 JSONB 精确等值匹配列表。
- **TransitivityRule**: AssociationType 传导规则实体，每个 AssociationType 对应一条规则，包含 `type`（AssociationType 枚举）、`transitive`（是否可传递）、`direction`（forward/backward/directed/bidirectional）、`weightStrategy`（multiply/add/max）、`maxDepth`（该类型遍历深度上限，默认值按类型差异化配置）、`description`（传导语义说明）。配置来源于 `metaforge.compute-engine.transitivity-rules`。
- **EntitySummary**: 实体摘要，包含 FQN、展示名、元模型类型 FQN。
- **RelationSummary**: 关系摘要，包含 FQN、关系类型 FQN、源实体 FQN、目标实体 FQN。

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 单实体 3 度邻接查询（百级结果集）响应时间不超过 200ms。
- **SC-002**: 组合层级树查询（3 层百级节点）响应时间不超过 150ms。
- **SC-003**: 子图提取查询（3 度百级节点百级边）响应时间不超过 300ms。
- **SC-004**: 图模式匹配查询（3 跳、千级实体网）响应时间不超过 500ms。
- **SC-005**: 两点间最短路径查询（默认 5 度以内）响应时间不超过 300ms。
- **SC-006**: 传递闭包推理（3 度百级结果）响应时间不超过 300ms。
- **SC-007**: 单实体影响溯源（3 度百级节点）响应时间不超过 200ms。
- **SC-008**: 批量查询 200 个 FQN 响应时间不超过 200ms。
- **SC-009**: 图查询结果准确率 100%，无遗漏、无多余数据。
- **SC-010**: 图模式匹配结果准确率 100%，无错配、无漏配。
- **SC-011**: 路径推理与传递闭包推理准确率 100%，与实际拓扑完全一致。
- **SC-012**: 过滤参数收敛准确率 100%，过滤结果与各维度交集一致。
- **SC-013**: 查询服务在底层模块正常时可用性不低于 99.9%。
- **SC-014**: 单次查询超时自动中断，不级联影响其他并发查询。

## Assumptions

- 底层模块（实体元数据管理、语义关系网络）已提供生效态数据的查询能力，包括 FQN 精准查询、前缀范围查询、批量查询、双向索引（出入边）等基础接口。
- 元模型治理模块已提供 EntitySchema/RelationSchema 定义及 AssociationType 语义枚举。AssociationType 的传递性、传导矩阵、类型差异化深度等传导规则由本模块通过 `metaforge.compute-engine.transitivity-rules` 配置文件独立维护，不依赖元模型扩展字段。
- 调用方具备基本的 HTTP 客户端能力，能通过结构化参数调用 REST API。
- MVP 阶段采用单实例部署模式，不涉及分布式集群下的数据一致性问题。
- 语义关系网络中关系数量在万级以内，图遍历可在百毫秒级完成。
- 查询请求的并发量处于中等水平（单实例百级 QPS），不涉及高并发下的资源竞争调度。
- 结果内联摘要所需的实体展示名、元模型类型 FQN 等信息在底层模块已有存储，可通过查询接口获取。
- 本模块不持有数据存储主权、不写入数据、不对数据做版本管理——仅作为无状态计算层。
- 所有深度上限默认可配置，默认值为 5，配置硬上限 10（1-10 范围）。传导规则配置中可对每个 AssociationType 设置差异化 `maxDepth`，遍历时取全局深度与类型深度中的较小值。提升深度会增加 CTE 遍历开销，10 度以内 1000 实体规模仍可控制在 800ms 内。
- MVP 阶段不实现：(1) 跨 Bundle 不可见元素的处理策略；(2) 事件驱动的缓存一致性（元数据/关系变更事件通知缓存）；(3) 向量语义搜索。
