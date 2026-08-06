# Feature Specification: 元模型治理核心能力 MVP

**Feature Branch**: (none — 由 before_specify hook 创建)

**Created**: 2026-08-01

**Status**: Draft

**Input**: User description: "根据 @docs/Metaforge 需求-元模型02.md 文档内容，生成需求问题空间"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 核心语义元素建模 (Priority: P1)

领域建模专家在 Bundle 草稿版本中定义实体Schema（Entity Schema）与关联Schema（Relation Schema），声明实体的属性定义（挂载属性模板组或添加原生属性），完成领域概念与实体间关系的结构化建模。发布时，系统自动完成属性平铺合并，生成标准化 JSON Schema 随版本固化。

**Why this priority**: 核心语义层是元模型系统的价值源头——所有下游元数据消费方（Agent、查询服务、推理引擎）都依赖已发布的 JSON Schema 进行校验与消费。没有语义元素建模能力，整个平台无法产生任何业务价值。

**Independent Test**: 创建包含 3 个 EntitySchema 和 2 个 RelationSchema 的草稿 Bundle，挂载属性模板组并添加原生属性，发布后通过 FQN 查询接口获取到每个元素的平铺合并后的 JSON Schema，且 Schema 符合 JSON Schema Draft 2020-12 规范子集。

**Acceptance Scenarios**:

1. **Given** 已存在一个处于草稿态的 Bundle 版本且其下已创建 Package，**When** 领域建模专家在 Package 下创建 EntitySchema 并填写名称（name）、描述（description）、挂载一个属性模板组 FQN、添加两个原生属性定义，**Then** 系统校验 EntitySchema 的 FQN 全局唯一，校验属性名无冲突（属性模板组展开后无同名、无与原生属性同名），校验通过后保存成功。

2. **Given** 已存在两个 EntitySchema（`Order` 和 `Item`），**When** 领域建模专家创建 RelationSchema 指定 source_fqn 为 `Order`、target_fqn 为 `Item`、关联类型为"组成"、源端基数为 1、目标端基数为 N，**Then** 系统校验两端 FQN 存在且可达（跨 Bundle 时校验可见性），校验通过后保存成功。

3. **Given** Bundle 草稿版本中包含多个 EntitySchema 和 RelationSchema 且各自挂载了属性模板组并定义了原生属性，**When** 领域建模专家执行发布操作，**Then** 系统自动完成每个 EntitySchema 和 RelationSchema 的属性平铺合并（原生属性 + 挂载属性模板组展开），生成扁平化 JSON Schema 写入 `json_schema` 字段并随版本固化，已发布版本的 json_schema 不可修改。

4. **Given** EntitySchema 已发布固化，**When** 领域建模专家尝试通过编辑修改已发布 EntitySchema 的属性定义，**Then** 系统拒绝修改并提示"已发布版本不可修改，请创建新草稿版本"。

---

### User Story 2 - Bundle 版本生命周期与依赖治理 (Priority: P1)

领域建模专家创建 Bundle 并管理其版本生命周期（草稿 → 已发布），在发布时声明升级等级（MAJOR/MINOR/PATCH），系统校验变更内容与声明的等级匹配。同时声明跨 Bundle 依赖，系统自动校验依赖链的完整性与无环性。

**Why this priority**: Bundle 版本治理是整个元模型治理底座的核心机制——没有版本收敛和依赖治理，元模型无法在多领域、多版本并存的真实环境中安全演进。依赖治理能力的缺失将导致消费方版本断裂与语义不一致。

**Independent Test**: 创建 Bundle，从 v1.0.0 发布后基于其创建新草稿声明 MINOR 升级并新增 EntitySchema，发布时系统校验通过。同时声明对另一个已发布 Bundle 的依赖，系统校验依赖目标存在且无循环依赖。

**Acceptance Scenarios**:

1. **Given** 系统中不存在任何 Bundle，**When** 领域建模专家创建名为 `order` 的 Bundle（bundle-code 符合 `[a-z][a-z0-9_-]{2,63}`）并填写描述字段（覆盖适用场景与能力边界），以默认 PATCH 升级等级创建首个草稿版本 v0.0.1，**Then** 系统校验 Bundle FQN 全局唯一，创建草稿版本成功，所有元素 enabled=false。

2. **Given** Bundle `order:v0.0.1` 已发布，**When** 领域建模专家从该最新已发布版本创建新草稿，声明升级等级为 MINOR，自动计算新版本号为 v0.1.0，**Then** 新草稿全量复制源版本内容（元模型、依赖、导出配置），且系统拒绝从历史非最新版本创建草稿。

3. **Given** Bundle 草稿版本 v0.1.0 中新增了 1 个 EntitySchema，声明的升级等级为 MINOR，**When** 领域建模专家执行发布，**Then** 系统校验变更内容（新增元素）与 MINOR 等级匹配，校验通过后发布成功，版本变为 v0.1.0 已发布态。

4. **Given** Bundle 草稿版本 v0.1.0 中删除了 1 个 EntitySchema，但声明的升级等级为 PATCH，**When** 领域建模专家执行发布，**Then** 系统校验发现变更内容（删除元素）与 PATCH 等级不匹配，阻止发布并提示升级等级不匹配。

5. **Given** Bundle `order` 草稿版本需引用 Bundle `common:v1.0.0` 中的元素，**When** 领域建模专家声明对 `common:v1.0.0` 的精确版本依赖，**Then** 系统校验目标 Bundle 版本存在且已发布，依赖声明保存成功。发布时自动校验全量依赖链无循环、无缺失。

---

### User Story 3 - Package 命名空间与分类管理 (Priority: P2)

领域建模专家在 Bundle 草稿版本中通过 Package 组织元模型元素的命名空间，支持多级嵌套构建分类树（上限 5 层）。同一 Bundle 版本内所有 Package 的元素默认互相可见。

**Why this priority**: Package 是元模型元素的组织容器，支撑大规模元模型的分类管理与命名空间隔离。虽然不直接承载语义，但缺少此能力将导致大型 Bundle 的元模型元素散乱无章、难以维护。

**Independent Test**: 在 Bundle 草稿版本中创建 3 层嵌套的 Package 树（`pkg_order.sub_pkg.inner_pkg`），在各层 Package 下分别创建 EntitySchema，验证 FQN 全局唯一且跨包引用正常解析。

**Acceptance Scenarios**:

1. **Given** Bundle 草稿版本已存在，**When** 领域建模专家创建 Package `pkg_order` 并填写描述（明确子领域范围），**Then** 系统校验 Package FQN 全局唯一，创建成功。

2. **Given** 已存在 Package `pkg_order`，**When** 领域建模专家在 `pkg_order` 下创建子 Package `sub_pkg`，**Then** 系统校验嵌套深度未超过 5 层上限，创建成功，父子关系建立。

3. **Given** Package 嵌套已达 5 层，**When** 领域建模专家尝试创建第 6 层子 Package，**Then** 系统校验嵌套深度超限并拦截写入。

4. **Given** Package `pkg_order` 下已有 5 个 EntitySchema，**When** 领域建模专家尝试删除该 Package，**Then** 系统前置校验发现 FQN 前缀下存在元素并拦截删除，提示需先清理下属元素。

---

### User Story 4 - 导出清单与跨 Bundle 可见性管控 (Priority: P2)

领域建模专家在 Bundle 发布前配置导出清单（Export Manifest），以 Package FQN 列表形式指定对外可见范围。导出清单随版本固化。导出包含 RelationSchema 的 Package 时，系统自动校验其关联两端 EntitySchema 所在包均已导出或可访问。

**Why this priority**: 导出清单是跨 Bundle 安全引用的边界保障——没有导出管控，元模型内部结构可能泄露或被下游耦合，破坏模块化治理闭环。此能力是实现 Bundle 模块化治理原则的执行层。

**Independent Test**: 发布 Bundle v1.0.0 时导出 `pkg_order` 和 `pkg_common` 两个包。下游 Bundle 声明依赖后仅能引用这两个包内的元素，尝试引用未导出的 `pkg_internal` 包内元素直接报错。

**Acceptance Scenarios**:

1. **Given** Bundle 草稿版本包含 3 个 Package（`pkg_order`、`pkg_common`、`pkg_internal`），**When** 领域建模专家配置导出清单仅包含 `pkg_order` 和 `pkg_common`，**Then** 导出清单保存成功。

2. **Given** 导出清单包含 `pkg_order`，其中存在 RelationSchema 的两个端点 EntitySchema 分别位于 `pkg_order` 和 `pkg_common` 中，**When** 领域建模专家执行发布，**Then** 系统自动校验两个端点所在包均已导出（pkg_order 和 pkg_common 均在导出清单中），校验通过。

3. **Given** 导出清单包含 `pkg_order`，其中存在 RelationSchema 的目标端 EntitySchema 位于未导出的 `pkg_internal` 中，**When** 领域建模专家执行发布，**Then** 系统校验发现目标端所在包未导出，拦截发布。

4. **Given** Bundle `order:v1.0.0` 已发布且导出清单包含 `pkg_order`，**When** 下游 Bundle 声明依赖后尝试引用 `order:v1.0.0.pkg_internal.SomeEntity`，**Then** 系统校验目标元素不在导出范围内，拦截引用并提示"目标元素不在导出范围内"。

---

### User Story 5 - 声明式批量导入导出 (Priority: P3)

领域建模专家通过 YAML/JSON 格式文件进行 Bundle 级或 Package 级元模型批量导入导出。导入严格按解析顺序执行（Bundle → Package → AttributeTemplate → EntitySchema → RelationSchema），导出文件格式与导入格式完全兼容。

**Why this priority**: 导入导出是多领域元模型标准化交付与复用的关键能力，支撑离线建模、跨环境迁移、版本归档等场景。但它依赖前述核心建模能力已经就绪，属于交付与集成层面的增强。

**Independent Test**: 导出一个已发布 Bundle 的完整元模型定义为 YAML 文件，将该文件导入到一个新 Bundle 中，导入后两个 Bundle 的草稿版本结构完全一致。

**Acceptance Scenarios**:

1. **Given** 系统中存在已发布 Bundle `order:v1.0.0` 的完整元模型定义，**When** 领域建模专家执行 Bundle 全量导出为 YAML 文件，**Then** 导出文件包含完整的依赖声明、导出清单、所有元素定义（EntitySchema、RelationSchema、AttributeTemplate），属性模板组随 Package 导出自动包含。

2. **Given** 一个合法的元模型导出 YAML 文件，**When** 领域建模专家执行 Bundle 全量导入到新 Bundle `order2`，**Then** 系统按 Bundle → Package → AttributeTemplate → EntitySchema → RelationSchema 顺序解析，各阶段依赖完整时导入成功，生成草稿版本（不自动发布）。

3. **Given** 导入文件引用的某个 BundleVersion FQN（作为依赖目标）在系统中不存在，**When** 领域建模专家执行导入，**Then** 系统在解析阶段检测到依赖缺失，终止导入并报错，不产生部分导入的脏数据。

4. **Given** 已导入的草稿版本 `order2` 与导出源 `order:v1.0.0` 的内容一致，**When** 领域建模专家再次导入同一 YAML 文件（选择"跳过"策略），**Then** 系统以 FQN 为基准检测到重复导入，跳过已有元素。

---

### User Story 6 - 预置系统 Bundle (Priority: P3)

系统初始化时自动预置 `metaforge` Bundle v1.0.0，包含 `agent` 和 `common` 两个包，提供 Agent 相关元模型和通用业务语义层级的基础建模能力。预置 Bundle 受特殊保护，禁止删除和修改已发布核心结构。

**Why this priority**: 预置 Bundle 为平台提供开箱即用的语义基座，降低领域建模专家的冷启动成本。但它属于基础设施初始化，而非持续交付价值的功能能力。

**Independent Test**: 系统首次启动后，通过 FQN 查询接口可访问 `metaforge:v1.0.0.agent.*` 和 `metaforge:v1.0.0.common.*` 下的预置元模型元素，尝试删除 `metaforge` Bundle 被拒绝。

**Acceptance Scenarios**:

1. **Given** 系统首次初始化完成，**When** 通过 FQN 查询接口检索 `metaforge:1.0.0.agent` 包下的 EntitySchema，**Then** 返回预置的 Agent 相关元模型元素列表，结构完整、无语法错误。

2. **Given** `metaforge` Bundle v1.0.0 已预置，**When** 管理员尝试删除 `metaforge` Bundle，**Then** 系统拒绝删除并提示"metaforge 为系统内置 Bundle，禁止删除"。

3. **Given** `metaforge` Bundle v1.0.0 已预置，**When** 领域建模专家在自定义 Bundle 中声明对 `metaforge:v1.0.0` 的依赖并引用 `common` 包中已导出的元素，**Then** 引用成功，可基于预置元模型进行领域扩展建模。

---

### Edge Cases

- FQN 全局唯一冲突：两个 Bundle 中创建同名元素时，系统在写入阶段校验 FQN 并拒绝重复创建。
- 并发编辑冲突：同一 Bundle 草稿版本被两个用户同时编辑时，采用最后写入覆盖策略，后写入者覆盖前写入者。
- 属性名冲突：多个属性模板组挂载同名属性，或模板组属性与原生属性同名时，写入阶段和发布阶段双环节拦截，不做优先级覆盖或自动重命名。
- 已发布元素 FQN 修改：已发布版本中的元素 FQN 不可修改，语义迁移必须通过新建版本完成。
- 非最新版本创建草稿：从历史非最新已发布版本创建新草稿时，系统直接拒绝，防止版本分叉。
- 发布为原子操作：校验不通过时全量回滚至草稿态，不产生中间态脏数据。
- 版本省略引用：元数据消费方代码中 FQN 可省略版本号按最新已发布版本解析，但元数据实例持久化时 FQN 必须为完整带版本格式。
- 不同 Bundle 独立传递依赖同一第三方 Bundle 的不同版本：彼此独立共存，互不影响。
- FQN 分隔符非法字符：segment（bundle-code、Package 段、元素短名）中出现 `:` 或 `.` 时，写入阶段直接拦截，提示"segment 不允许包含保留分隔符 : 和 ."。

## Requirements *(mandatory)*

### Functional Requirements

#### 核心语义元素建模（对应 User Story 1）

- **FR-001**: 系统必须支持在指定 Bundle 版本 + Package 下创建、修改、删除 EntitySchema，以 FQN 为唯一标识，FQN 全局唯一。

#### 向量描述字段

- **FR-EMB-01**: Bundle、Package、EntitySchema、RelationSchema 均提供 embedding 字段（类型 JSONB，存储浮点数组），MVP 阶段仅提供存储占位，不实现真正的向量生成与检索逻辑。

#### FQN 统一生成器

- **FR-FQN-01**: 系统必须提供独立的 FQN 统一生成器（`FqnGenerator` 接口 + `FqnGeneratorImpl` 实现），提供 FQN 的生成与解析能力。采用接口与实现分离模式，预留未来将公共接口提取至 `metaforge-common` 的扩展路径（不在当前 MVP 范围）。
- **FR-FQN-02**: FQN 生成器支持以下实体类型的 FQN 生成与解析：Bundle、BundleVersion、Package、EntitySchema、RelationSchema、AttributeTemplate。
- **FR-FQN-03**: FQN 解析方法支持从纯净 FQN 解析出各组成部分（bundle-code、version、路径 segment、短名、父 FQN），支持带类型前缀的 API 层 FQN 解析与剥离。
- **FR-FQN-04**: FQN 生成器的方法为纯函数（无副作用、无状态），不依赖数据库或外部服务。实现方式为无状态 Bean（可 Singleton 注入），方法本身纯函数式。
- **FR-FQN-05**: FQN 生成器不对外暴露为独立服务（REST/MCP）。FQN 生成能力作为 BC 内部领域服务使用；FQN 解析能力通过已有的 `resolveFqn` MCP 工具和 REST 查询接口向下游提供。
- **FR-FQN-06**: FQN 生成器仅做纯字符串变换，不承担业务格式校验职责。`parse()` 尽最大努力解析（格式完全不兼容时抛异常），`generate()` 不做输入校验。FQN 格式合规校验（bundle-code 正则、版本号格式、segment 命名规范）统一在上层写入校验和发布校验环节完成。

#### 元模型元素查询与过滤

- **FR-QRY-01**: 系统必须支持对 EntitySchema 和 RelationSchema 按 FQN 前缀集合进行批量过滤查询（`List<String> fqnPrefixes`），如 `["order:1.0.0.pkg_order.", "order:1.0.0.pkg_common."]` 匹配多个 Package 下的所有元素。单个前缀即为指定前缀下的所有元素。
- **FR-QRY-02**: FQN 前缀集合查询支持分页（PageRequest / PageResult）。元素类型（EntitySchema / RelationSchema）通过 `elementType` 参数筛选。
- **FR-002**: EntitySchema 的人类可读名称（name）字段必填，用于界面展示与用户友好标识，不与 FQN 推导的短名关联。
- **FR-002a**: EntitySchema 的描述（description）字段必填，需包含业务含义、适用场景两个核心要素，缺一不可保存。
- **FR-003**: 系统必须支持 EntitySchema 通过两种方式定义属性：挂载属性模板组（以 FQN 引用）和定义原生属性（Native Attribute），草稿态保留组合式结构。
- **FR-004**: 系统必须在写入阶段校验同一 EntitySchema 内属性名唯一性——挂载的属性模板组之间、属性模板组与原生属性之间均不得出现同名属性，冲突时拦截保存。
- **FR-005**: 系统必须在写入阶段校验原生属性定义符合 JSON Schema Draft 2020-12 规范子集——仅支持 string/number/integer/boolean/array 五类根级属性类型，array 元素仅限上述四类标量（string/number/integer/boolean），禁止 object 类型出现于任意层级，禁止 null 类型。
- **FR-006**: RelationSchema 的人类可读名称（name）字段必填，用于界面展示与用户友好标识。
- **FR-006a**: 系统必须支持在指定 Bundle 版本 + Package 下创建、修改、删除 RelationSchema，以 FQN 为唯一标识。
- **FR-007**: RelationSchema 的描述（description）字段必填。
- **FR-007a**: RelationSchema 必须包含 source_fqn 和 target_fqn 字段，通过 FQN 引用两端 EntitySchema，系统校验引用目标存在且可见（跨 Bundle 时需目标包在导出清单中）。
- **FR-008**: RelationSchema 的 AssociationType 为必填内置枚举字段，仅可选"组成/关联引用/映射对应/依赖影响/流程时序"五个值，不可自定义。
- **FR-009**: RelationSchema 同样支持两种属性定义方式（挂载属性模板组 + 原生属性定义），适用与 EntitySchema 完全一致的属性名唯一性校验规则。
- **FR-010**: Bundle 版本发布时，系统必须自动对版本内每一个 EntitySchema 和 RelationSchema 完成属性平铺合并，生成扁平化 JSON Schema 写入 `json_schema` 字段并永久固化。
- **FR-011**: 已发布版本的 json_schema 字段不可修改，不同 Bundle 版本中 FQN 同名元素的 json_schema 完全独立、互不影响。
- **FR-012**: 已发布版本的 EntitySchema 和 RelationSchema 的属性定义（原生属性 + 属性模板组挂载）全量锁定，不可增删改。

#### 属性模板组复用（对应 User Story 1）

- **FR-013**: AttributeTemplate 的人类可读名称（name）字段必填。
- **FR-013b**: AttributeTemplate 的描述（description）字段可选，MVP 阶段暂不强约束。
- **FR-013a**: 系统必须支持在 Bundle 版本级别创建、修改、删除 AttributeTemplate，以 FQN 为唯一标识，FQN 不含 Package 路径（如 `order:1.0.0.AuditFields`）。
- **FR-014**: AttributeTemplate 的属性约束必须对齐 JSON Schema Draft 2020-12 规范子集，支持 string/number/integer/boolean/array 五类类型及必填、默认值、枚举、取值范围、正则等标准约束。
- **FR-015**: 全 Bundle 内所有 EntitySchema 和 RelationSchema 均可直接引用 AttributeTemplate，无需额外声明。

#### Bundle 版本生命周期与依赖（对应 User Story 2）

- **FR-016**: 系统必须支持创建 Bundle（FQN 即 bundle-code，正则 `[a-z][a-z0-9_-]{2,63}`），Bundle 描述字段必填（需覆盖适用场景、能力边界两个要素）。
- **FR-017**: 系统必须支持 Bundle 版本"草稿 → 已发布"两态生命周期，正向流转不可逆，已发布版本不可回退为草稿态。
- **FR-018**: 草稿态下所有元模型元素 enabled=false（从 BundleVersion.status 推导），仅内部可见，不可被下游消费方使用。
- **FR-019**: 已发布态下所有元模型元素 enabled=true（从 BundleVersion.status 推导），状态不可修改。
- **FR-020**: 系统必须仅允许从`最新已发布版本`创建新草稿——全量原子复制源版本内容，禁止从历史非最新版本创建、禁止版本分叉。
- **FR-021**: 创建草稿时，用户选择目标升级等级（MAJOR/MINOR/PATCH，默认 PATCH），系统基于源版本号与选定等级自动计算新版本号。
- **FR-022**: 发布时系统必须校验变更内容与声明升级等级的匹配性：PATCH 仅允许描述修改、元数据修正等非结构性变更（禁止增删元素、修改字段类型/必填/枚举值）；MINOR 仅允许新增元素/属性，禁止删除和结构性变更；MAJOR 允许所有类型变更。不匹配则阻止发布。
- **FR-023**: 同一 Bundle 下只允许存在一个草稿版本。同一草稿的并发编辑采用"最后写入覆盖"策略，不引入锁机制。
- **FR-024**: Bundle 发布为原子操作——校验不通过时全量回滚至草稿态，不产生中间态脏数据。

#### 跨 Bundle 依赖（对应 User Story 2）

- **FR-025**: 跨 Bundle 依赖声明必须使用目标 BundleVersion 的完整 FQN（含精确版本号），禁止使用版本范围表达式或 `latest` 语义。
- **FR-026**: 传递依赖遵循"范围不放大、版本不自动升级"两项硬约束。
- **FR-027**: 不同 Bundle 可传递依赖同一第三方 Bundle 的不同版本，彼此独立共存。
- **FR-028**: 系统必须在元模型加载和发布时检测 Bundle 依赖图中的循环依赖（任意长度），检测到即拦截并报告完整循环路径。

#### Package 命名空间（对应 User Story 3）

- **FR-029**: 系统必须支持在 Bundle 草稿版本内创建、修改、删除 Package，以 FQN 为唯一标识。
- **FR-030**: Package 描述字段必填，需明确子领域范围。
- **FR-031**: Package 嵌套深度上限为 5 层（根层 + 4 级子层），写入时超限拦截。
- **FR-032**: 同一 Bundle 版本内所有 Package 的元素默认天然互相可见，FQN 引用仅校验目标存在性。
- **FR-033**: 删除 Package 前必须执行前置校验——该 Package 下存在元模型元素时直接拦截，需先清理下属元素。
- **FR-034**: 已发布版本中的 Package 不可删除、不可修改。

#### 导出清单与跨 Bundle 可见性（对应 User Story 4）

- **FR-035**: 系统必须支持在 Bundle 版本维度配置导出清单（ExportManifest），以 Package FQN 列表形式指定对外可见范围。
- **FR-036**: 未列入导出清单的 Package 和元素仅 Bundle 内部可见，外部 Bundle 引用时直接拦截。
- **FR-037**: 导出包含 RelationSchema 的 Package 时，系统必须自动校验其关联两端的 EntitySchema 所在 Package 均已导出或可访问，不满足则拦截发布。
- **FR-038**: 导出清单随 Bundle 发布固化，变更必须通过新建 Bundle 草稿版本实现。
- **FR-039**: 跨 Bundle 引用时 FQN 必须包含目标 Bundle 的版本号，不可省略版本。

#### 批量导入导出（对应 User Story 5）

- **FR-040**: 系统必须支持 YAML/JSON 格式文件的元模型导入导出，支持 Bundle 全量导入导出和 Package 级导入导出两种粒度。
- **FR-041**: 导入解析顺序严格遵循：Bundle → Package → AttributeTemplate → EntitySchema → RelationSchema，任一阶段依赖缺失即终止导入。
- **FR-042**: 导入以 FQN 为幂等基准，重复导入支持"跳过"和"报错"两种策略，但禁止覆盖已发布版本。
- **FR-043**: 导出文件格式与导入格式完全兼容，导出的文件可直接重新导入。
- **FR-044**: Package 级导出时自动包含元素依赖的属性模板组，保证导入后语义完整。
- **FR-045**: 导入仅支持生成/更新草稿版本，禁止自动发布。

#### 预置系统 Bundle（对应 User Story 6）

- **FR-046**: 系统初始化时自动预置 `metaforge` Bundle v1.0.0，标记为系统内置，包含 `agent` 和 `common` 两个预置包。
- **FR-047**: `metaforge` Bundle 禁止删除，其已发布核心结构（元素定义、RelationSchema、AttributeTemplate）禁止修改。
- **FR-048**: 扩展预置元模型仅允许通过新建自定义 Bundle 并依赖 `metaforge` 的方式实现。

#### 校验体系

- **FR-049**: 系统必须在 Bundle 草稿保存时执行写入轻量校验：FQN 全局唯一性、引用完整性、循环依赖、Package 嵌套深度、属性名冲突、命名规范合规性（含 segment 中禁止出现 FQN 保留分隔符 `:` 和 `.`）。
- **FR-050**: 系统必须在 Bundle 发布前执行全量全局校验：跨 Bundle 引用可达性、导出清单一致性、关联Schema 端点合法性、依赖链自洽性、升级等级匹配性、属性名冲突全量校验、JSON Schema 自动生成合规性。
- **FR-051**: 系统必须支持仅校验不落库的预览模式，错误信息精准定位到具体元素的 FQN 与字段名称。

### Key Entities *(include if feature involves data)*

- **Bundle**: 顶层治理单元与最小交付单元，以 bundle-code 为 FQN（如 `order`）。持有名称、描述、负责人、是否为系统内置、向量描述（embedding，JSONB）等属性。版本/依赖/导出的唯一锚点。
- **BundleVersion**: Bundle 的版本化内容容器，FQN 格式为 `order:1.0.0`。持有状态（草稿/已发布）、源版本 FQN、升级等级等属性。发布时触发 JSON Schema 自动生成。
- **Package**: Bundle 版本内的纯分类容器，FQN 格式为 `order:1.0.0.pkg_order`。持有点向量描述（embedding，JSONB）。支持多级嵌套（上限 5 层）。无独立治理能力，仅承载元素归属。
- **EntitySchema**: 核心语义层一等元素，领域概念建模定义，FQN 格式为 `order:1.0.0.pkg_order.Order`。持有人类可读名称（name）、语义描述（description）、向量描述（embedding，JSONB）、原生属性定义（JSONB）、挂载属性模板组 FQN 列表、平铺合并后的 json_schema（JSONB）。
- **RelationSchema**: 核心语义层一等元素，实体间关联建模定义，FQN 格式为 `order:1.0.0.pkg_order.Order_contains_Item`。持有人类可读名称（name）、语义描述（description）、向量描述（embedding，JSONB）、关联类型枚举、源端/目标端 FQN、基数约束、原生属性定义、挂载属性模板组列表、json_schema。
- **AttributeTemplate**: 属性定义层辅助复用单元，FQN 格式为 `order:1.0.0.AuditFields`。直接归属 Bundle 版本，不隶属任何 Package。持有人类可读名称（name）、描述（description）、属性定义集合（JSONB）。
- **BundleDependency**: 记录跨 Bundle 依赖关系，包含源 BundleVersion FQN 和目标 BundleVersion FQN（精确版本号）。
- **ExportManifest**: Bundle 版本维度的导出清单，包含导出的 Package FQN 列表。随版本固化为快照，不可独立修改。

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 领域建模专家能在 2 小时内完成一个包含 5+ EntitySchema、3+ RelationSchema、2+ AttributeTemplate 的完整 Bundle 定义，并通过全量校验成功发布。
- **SC-002**: 单 Bundle 发布时全量全局校验（含属性平铺合并、JSON Schema 生成、循环依赖检测、导出清单一致性、升级等级匹配性）在 100 个核心语义元素规模下耗时不超过 3 秒。
- **SC-003**: FQN 全局唯一校验覆盖所有实体类型，重复 FQN 写入 100% 被拦截，无误漏。
- **SC-004**: Bundle 间循环依赖检测正确率 100%（无误报、无漏报），错误信息包含完整循环路径。
- **SC-005**: 已发布版本的任何修改尝试 100% 被拦截（元素定义、属性定义、json_schema、导出清单、依赖声明）。
- **SC-006**: 属性名冲突在写入阶段和发布阶段双环节检测覆盖率达 100%，无遗漏的同名冲突可通过任一环节。
- **SC-007**: 导入导出格式完全互逆——导出文件直接重新导入后草稿版本结构一致，无信息丢失。
- **SC-008**: 属性平铺合并生成的 JSON Schema 能通过 JSON Schema Draft 2020-12 规范验证器的校验，合法性达 100%。

## Clarifications

### Session 2026-08-01

- Q: name 字段的语义？ → A: 独立的人类可读显示名字段，与 FQN 推导的短名无关。
- Q: 向量字段的名称和类型？ → A: 字段名 `embedding`，类型 JSONB（存储浮点数组）。
- Q: AttributeTemplate 的 description 是否必填？ → A: 可选（MVP 阶段暂不强约束）。
- Q: FQN 统一生成器的实现方式？ → A: 独立工具类 `FqnGenerator`，提供静态生成/解析方法。
- Q: FQN 生成器跨 BC 归属与冲突防止？ → A: 各 BC 在各自包路径内独立实现 FQN 生成器（包路径天然隔离），采用接口+实现分离以预留未来提取公共基类到 metaforge-common 的可能性。公共基类提取为后续优化项，不在当前 MVP 范围。
- Q: FQN 生成器是否需暴露为对外服务？ → A: 不对外暴露。FQN 生成能力作为 BC 内部领域服务，仅生成逻辑内部使用；解析能力通过已有的 `resolveFqn` MCP 工具和 REST 查询接口间接覆盖下游消费方需求。
- Q: FQN 生成器的校验职责边界？ → A: 生成器仅做纯字符串变换，不做业务格式校验。`parse()` 尽最大努力解析（格式完全不兼容时抛异常），`generate()` 不做输入校验。格式合规校验统一在读/写/发布链路的上层校验环节完成。
- Q: EntitySchema / RelationSchema 过滤查询的维度与范围？ → A: 四个维度全覆盖——支持 FQN 前缀模糊查询、按 Bundle 过滤、按 BundleVersion 过滤、按 Package 过滤，各维度均支持单独或组合使用。
- Q: 过滤查询是否需要扩展到其他实体类型？ → A: 仅限 EntitySchema + RelationSchema。AttributeTemplate 和 Package 按现有精确查询方式已足够。
- Q: 过滤查询维度是否需要精简？ → A: 精简为仅 `fqnPrefix` 集合查询（`List<String> fqnPrefixes`），去掉 bundleFqn / bundleVersionFqn / packageFqn 三个冗余维度。FQN 前缀本身已编码 Bundle/版本/Package 层级信息。
- Q: FQN segment 非法字符校验？ → A: FQN 的 segment（bundle-code、Package 段、元素短名）中禁止出现分隔符 `:` 和 `.`（这些是 FQN 保留分隔符），写入阶段校验时拦截。

## Assumptions

- 系统采用单实例部署模式，不涉及分布式事务或集群协调（MVP 阶段假设）。
- 领域建模专家具备基本的声明式配置文件编写能力，无需可视化编辑器（MVP 不做 GUI）。
- 元模型定义仅指建模层的结构化语义约束，不涉及具体业务数据实例的存储与处理。
- 草稿版本的并发编辑冲突由最后写入者覆盖，系统不提供悲观锁或乐观锁的冲突解决机制（MVP 约束）。
- JSON Schema 生成仅支持 string/number/integer/boolean/array 五类类型且不支持 object 嵌套，复杂度足够覆盖 MVP 阶段的业务场景建模需求。
- 属性模板组不支持嵌套引用其他属性模板组（MVP 阶段暂不实现）。
- 元数据消费方的 FQN 版本省略行为依赖"最新已发布版本"的准确定义——系统需维护各 Bundle 的最新已发布版本索引。
- 预置 `metaforge` Bundle 的内容定义由系统设计阶段确定，不在运行时动态变更。
- FQN 生成器公共接口提取至 `metaforge-common` 为后续优化项，当前 MVP 阶段各 BC 在各自包路径内独立实现，包路径天然形成命名空间隔离。
