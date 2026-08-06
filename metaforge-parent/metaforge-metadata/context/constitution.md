<!--
================================================================================
  Sync Impact Report
================================================================================
  BC Constitution Version: N/A (initial skeleton) → 1.0.0
  Parent Global Constitution Version: 1.0.0

  BC-Specific Principles:
    ✅ Added: I. 三表正交存储架构 (MUST)
    ✅ Added: II. FQN 全局唯一标识 (MUST)
    ✅ Added: III. 版本全生命周期 — 草稿→生效→下线 (MUST)
    ✅ Added: IV. 强结构校验前置 (MUST)
    ✅ Added: V. 单正式版本原则 (MUST)
    ✅ Added: VI. 纯元数据边界 (MUST)
    ✅ Added: VII. 历史版本追溯与差异对比 (SHOULD)
    ✅ Added: VIII. 多维度查询检索 (SHOULD)
    ✅ Added: IX. 批量导入导出 (SHOULD)

  Custom Sections:
    ✅ Added: 数据模型与存储规范
    ✅ Added: 校验与生效工作流
    ✅ Added: 变更事件通知规范
    ✅ Added: 性能与一致性基线

  BC Overrides:
    ⬚ None — inherit all global SHOULD/MAY principles as-is

  Deferred TODOs: None

  Rationale: Initial comprehensive constitution for metadata-management BC,
  defining entity metadata instance management aligned with MOF M1 layer.
  All 9 BC principles and 4 custom sections derived from the Metaforge
  metadata management requirements document V1.0.
================================================================================
-->

# metadata-management Bounded Context Constitution
<!-- BC-level governance constitution. Inherits all rules from the global system constitution as read-only baseline. -->
<!-- OVERRIDE RULE: Only SHOULD/MAY level principles from the parent global constitution can be overridden. MUST level principles CANNOT be modified or removed. -->

**BC Name**: metadata-management — Entity 元数据实例化管理，对应 MOF M1 层

**Parent Version**: 1.0.0
<!-- Human-readable traceability marker only. No version alignment validation during AI merge. Records the global constitution version referenced when this BC constitution was created/last updated. -->

---

## BC-Specific Principles
<!-- Exclusive core principles for this BC only, not inherited from the global constitution. Follow MUST/SHOULD/MAY level specification. Principle names must not duplicate global constitution principles. -->

### I. 三表正交存储架构 (MUST)

元数据采用「主表(metadata_entity) + 草稿表(metadata_entity_draft) + 历史表(entity_version)」三层物理隔离存储。主表仅存唯一生效版本，是对外服务的唯一数据源；草稿表隔离编辑态数据，对外完全不可见；历史表永久只读归档所有正式发布版本，仅支持 INSERT 禁止 UPDATE/DELETE。三表职责严格正交，不混合存储、不交叉状态，从存储层面杜绝状态混淆。

### II. FQN 全局唯一标识 (MUST)

FQN 是元数据实例的唯一业务标识，文法与元模型体系完全对齐：`<segment> ::= [A-Za-z][A-Za-z0-9_-]*`（segment 内禁止出现保留分隔符 `.`），以点分隔层级路径。顶级实体由创建者指定全局唯一编码；组合子实体 FQN 自动按「父 FQN + . + 当前编码」生成。FQN 生命周期内不可变更，元模型类型与版本通过独立字段 entity_schema_fqn 关联，实现业务标识稳定与语义规则可升级的解耦。元数据自身 FQN 不携带元模型版本与类型信息。

### III. 版本全生命周期 — 草稿→生效→下线 (MUST)

元数据严格遵循「草稿→生效→下线」三态流转。草稿态完全可编辑且与正式版本物理隔离，生效态是唯一对外版本，下线态从主表移除但历史表永久保留归档。生效操作为原子事务：同一事务内完成主表写入/覆盖、历史表归档、草稿表删除三步，任意一步失败全量回滚。下线前须校验外部引用与组合子实体状态，存在活跃引用或生效子实体则拦截。支持从历史版本重新生效，无修改时保持版本号不变。

### IV. 强结构校验前置 (MUST)

所有写入操作（创建、编辑、生效、导入）必须实时调用对应版本 EntitySchema 的 JSON Schema 执行全字段结构校验，校验通过后方可继续。校验覆盖字段类型、必填项、枚举值、取值范围、正则格式、数组元素约束等全部规则。校验失败时返回结构化错误信息，包含违规字段路径、违规类型、违反的元模型规则引用。校验逻辑与元模型版本严格绑定，不允许绕过校验写入，不合规数据零容忍。

### V. 单正式版本原则 (MUST)

任意 FQN 在同一时刻最多仅存在一条生效记录，对外服务仅以主表生效版本为唯一基准。不支持多版本并行发布、不支持版本回滚。同一 FQN 最多仅允许存在一条草稿，不支持多草稿并行编辑，并发编辑采用「最后写入覆盖」策略。

### VI. 纯元数据边界 (MUST)

本模块仅管理 EntitySchema 对应的实体元数据实例，不涉及 RelationSchema 关系实例。仅存储业务概念与规则的描述性元数据，禁止存储具体业务交易数据、生产流水数据。不引入业务域、分类、标签等元模型之外的治理概念，所有语义分类与领域归属均由关联的元模型定义。权限控制完全继承元模型的导入授权体系，不做独立的细粒度权限管控。

### VII. 历史版本追溯与差异对比 (SHOULD)

所有正式发布版本全量归档至历史表，支持按 FQN 查询全版本列表（按版本号倒序）、按 FQN+版本号查询单版本完整详情、以及任意两历史版本的字段级差异对比。差异按「新增字段、修改字段、删除字段」三类分类展示。历史版本仅可查看，不可修改或删除。

### VIII. 多维度查询检索 (SHOULD)

支持四种查询模式：FQN 精准查询（主表读取）、FQN 前缀范围查询（天然覆盖组合层级子树）、元模型类型查询、属性条件组合查询（精准/模糊匹配）。默认返回生效版本，支持分页与排序。管理端专属查询可跨主表/草稿表/历史表聚合全状态数据，仅管理员可调用。

### IX. 批量导入导出 (SHOULD)

支持 YAML/JSON 标准格式批量导入导出。导入以 FQN 为唯一标识，幂等支持「跳过/报错」两种策略，逐条执行结构校验，成功后仅写入草稿表，需手动生效。导出默认从主表读取生效版本，支持按 FQN 前缀范围、元模型类型、指定 FQN 列表三种粒度，导出格式与导入完全兼容可复现。

---

## 数据模型与存储规范

<!-- BC-specific data model and storage constraint section. -->

— metadata_entity 主表：id(BIGINT PK), fqn(VARCHAR UNIQUE), name(VARCHAR NOT NULL), description(VARCHAR), parent_fqn(VARCHAR), entity_schema_fqn(VARCHAR), content(JSONB), embedding(JSONB), current_version(INT), created_by/created_time/updated_by/updated_time
— metadata_entity_draft 草稿表：id(BIGINT PK), fqn(VARCHAR UNIQUE), name(VARCHAR NOT NULL), description(VARCHAR), parent_fqn(VARCHAR), entity_schema_fqn(VARCHAR), content(JSONB), embedding(JSONB), base_version(INT nullable), created_by/created_time/updated_by/updated_time
— entity_version 历史表：id(BIGINT PK), fqn(VARCHAR), name(VARCHAR NOT NULL), description(VARCHAR), parent_fqn(VARCHAR), version(INT, fqn+version UNIQUE), entity_schema_fqn(VARCHAR), content(JSONB), embedding(JSONB), created_by(VARCHAR), created_time(BIGINT)
— 三表均归属 metadata_management 数据库 Schema
— 审计字段约束：所有表统一保留创建人、创建时间、更新人、更新时间
— FQN 全局唯一约束：主表与草稿表各自建立唯一索引，全局联合保证一个 FQN 最多一条有效记录
— 元模型绑定约束：entity_schema_fqn 必须携带完整版本号，禁止绑定草稿态元模型
— 历史只读约束：数据库层面禁止 UPDATE/DELETE

---

## 校验与生效工作流

<!-- BC-specific validation and activation workflow section. -->

— 写入校验清单（每次写入触发）：① JSON Schema 全字段结构校验 ② FQN 全局唯一性 ③ 元模型存在性与已发布状态 ④ 组合父实体存在性与状态 ⑤ 组合子实体 FQN 自动生成规则
— 生效前校验清单：① 结构合规校验 ② 组合层级合法性校验 ③ 元模型版本有效性校验 ④ 所有校验通过后方可执行
— 生效原子事务三步：主表写入/覆盖 + 历史表归档（版本号递增）+ 草稿表删除
— 下线前校验：① 外部引用校验（调用语义关系网络模块）② 组合子实体状态校验 ③ 存在活跃引用或生效子实体则拦截
— 重新生效：从历史表读取最新归档版本写入主表（无修改保持版本号）；需修改则先创建草稿再走标准生效流程
— 批量合规校验：支持按元模型类型/FQN 前缀范围批量校验，输出校验报告（通过率+违规清单+详情）

---

## 变更事件通知规范

<!-- BC-specific change event notification specification section. -->

— 元数据生效时自动发布变更事件
— 元数据下线时自动发布变更事件
— 事件内容包含 FQN、操作类型（生效/下线）、版本号、时间戳
— 驱动下游关系网络、查询引擎等模块同步数据

---

## 性能与一致性基线

<!-- BC-specific performance and consistency baseline section. -->

— 单条草稿创建（含校验）≤ 50ms
— 主表 FQN 精准查询 ≤ 20ms
— FQN 前缀范围查询（百级结果）≤ 100ms
— 单批次 500 条批量导入 ≤ 5s
— 草稿生效原子操作 ≤ 100ms
— 全历史版本列表查询 ≤ 100ms
— 写入结构校验通过率 100%，版本生效原子性 100%，主表与历史表数据一致性 100%，FQN 全局唯一性保障准确率 100%

---

## BC Overrides
<!-- Selective override of parent global constitution SHOULD/MAY level principles only. Each entry must reference the exact parent principle name + explicit override rationale. MUST level principles are forbidden to be overridden here. -->

暂无覆盖项 — 全局宪法的所有 SHOULD/MAY 级原则原样继承。本 BC 的领域专属原则为对全局宪法的外部扩展，不构成对任何全局原则的覆盖或弱化。

---

**BC Constitution Version**: 1.0.0 | **Created**: 2026-08-01 | **Last Amended**: 2026-08-01
<!-- BC constitution has independent semantic versioning, decoupled from global constitution version. -->
