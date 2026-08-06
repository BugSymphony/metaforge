# Data Model: 元数据全生命周期管理

**Feature**: 001-metadata-full-lifecycle
**Date**: 2026-08-01

## 核心聚合与实体

### 1. MetadataEntity（聚合根 — 主表生效版本）

**对应表**: `metadata_management.metadata_entity`
**职责**: 对外服务的唯一生效版本，每个 FQN 最多一条记录。

| 字段 | 类型 (Domain) | DB 类型 | 约束 | 描述 |
|------|--------------|---------|------|------|
| `id` | `Long` | `BIGINT PK AUTO_INCREMENT` | NOT NULL | 系统主键 |
| `fqn` | `FQN` (值对象) | `VARCHAR(1024)` | NOT NULL, UNIQUE | 全局唯一 FQN，M1 层级路径（点分隔） |
| `name` | `String` | `VARCHAR(255)` | NOT NULL | 展示名称（不要求全局唯一） |
| `description` | `String` | `VARCHAR(2000)` | NULLABLE | 业务含义描述 |
| `parentFqn` | `String` | `VARCHAR(1024)` | NULLABLE | 父实体完整 FQN（顶级实体为 null） |
| `entitySchemaFqn` | `EntitySchemaFQN` (值对象) | `VARCHAR(512)` | NOT NULL | 关联元模型 FQN（M2 层格式，含版本号） |
| `content` | `Map<String, Object>` | `JSONB` | NOT NULL | 属性内容（符合 EntitySchema JSON Schema） |
| `embedding` | `List<Float>` | `JSONB` | NULLABLE | 向量嵌入（浮点数组，JSONB 存储，MVP 占位） |
| `currentVersion` | `VersionNumber` (值对象) | `INT` | NOT NULL, DEFAULT 1 | 当前版本号（递增整数） |
| `createdBy` | `String` | `VARCHAR(255)` | NOT NULL | 创建人 |
| `createdTime` | `LocalDateTime` | `TIMESTAMP` | NOT NULL | 创建时间 |
| `updatedBy` | `String` | `VARCHAR(255)` | NOT NULL | 更新人 |
| `updatedTime` | `LocalDateTime` | `TIMESTAMP` | NOT NULL | 更新时间 |

**唯一约束**: `fqn` UNIQUE
**状态语义**: 表中存在 = 生效态（ACTIVE），不存在 = 已下线（DEPRECATED）或从未存在

---

### 2. MetadataEntityDraft（聚合根 — 草稿表）

**对应表**: `metadata_management.metadata_entity_draft`
**职责**: 编辑态数据，物理隔离于主表，对外完全不可见。

| 字段 | 类型 (Domain) | DB 类型 | 约束 | 描述 |
|------|--------------|---------|------|------|
| `id` | `Long` | `BIGINT PK AUTO_INCREMENT` | NOT NULL | 系统主键 |
| `fqn` | `FQN` (值对象) | `VARCHAR(1024)` | NOT NULL, UNIQUE | 全局唯一 FQN |
| `name` | `String` | `VARCHAR(255)` | NOT NULL | 展示名称 |
| `description` | `String` | `VARCHAR(2000)` | NULLABLE | 业务含义描述 |
| `parentFqn` | `String` | `VARCHAR(1024)` | NULLABLE | 父实体完整 FQN |
| `entitySchemaFqn` | `EntitySchemaFQN` (值对象) | `VARCHAR(512)` | NOT NULL | 关联元模型 FQN |
| `content` | `Map<String, Object>` | `JSONB` | NOT NULL | 属性内容 |
| `embedding` | `List<Float>` | `JSONB` | NULLABLE | 向量嵌入（MVP 占位） |
| `baseVersion` | `Integer` | `INT` | NULLABLE | 基于哪个生效版本创建的草稿（null=全新创建） |
| `createdBy` | `String` | `VARCHAR(255)` | NOT NULL | 创建人 |
| `createdTime` | `LocalDateTime` | `TIMESTAMP` | NOT NULL | 创建时间 |
| `updatedBy` | `String` | `VARCHAR(255)` | NOT NULL | 更新人 |
| `updatedTime` | `LocalDateTime` | `TIMESTAMP` | NOT NULL | 更新时间 |

**唯一约束**: `fqn` UNIQUE
**状态语义**: 表中存在 = 草稿态（DRAFT），通过物理删除清除

---

### 3. EntityVersion（领域实体 — 历史表）

**对应表**: `metadata_management.entity_version`
**职责**: 所有正式发布版本的全量快照，不可变只读归档。

| 字段 | 类型 (Domain) | DB 类型 | 约束 | 描述 |
|------|--------------|---------|------|------|
| `id` | `Long` | `BIGINT PK AUTO_INCREMENT` | NOT NULL | 系统主键 |
| `fqn` | `FQN` (值对象) | `VARCHAR(1024)` | NOT NULL | 元数据 FQN |
| `name` | `String` | `VARCHAR(255)` | NOT NULL | 生效时快照的名称 |
| `description` | `String` | `VARCHAR(2000)` | NULLABLE | 生效时快照的描述 |
| `parentFqn` | `String` | `VARCHAR(1024)` | NULLABLE | 生效时快照的父实体 FQN |
| `version` | `VersionNumber` (值对象) | `INT` | NOT NULL | 递增版本号，同 FQN 下联合唯一 |
| `entitySchemaFqn` | `EntitySchemaFQN` (值对象) | `VARCHAR(512)` | NOT NULL | 生效时关联的元模型 FQN |
| `content` | `Map<String, Object>` | `JSONB` | NOT NULL | 完整属性快照 |
| `embedding` | `List<Float>` | `JSONB` | NULLABLE | 向量嵌入快照（MVP 占位） |
| `createdBy` | `String` | `VARCHAR(255)` | NOT NULL | 生效操作人 |
| `createdTime` | `LocalDateTime` | `TIMESTAMP` | NOT NULL | 生效时间（归档时间） |

**联合唯一约束**: `(fqn, version)` UNIQUE
**读写约束**: 仅 INSERT，禁止 UPDATE / DELETE（数据库层面或应用层强约束）

---

## 值对象定义

### 4. FQN

M1 层元数据实例的全限定名（Fully Qualified Name）。

- **格式**: `segment["." segment]*`，顶层为单个 segment，子实体为父 FQN + "." + segment
- **文法**: `segment ::= [A-Za-z][A-Za-z0-9_-]*`，禁止包含保留分隔符 `.`
- **示例**: `SalesOrder_001`，`SalesOrder_001.OrderItems_005`
- **不变性**: 创建后不可变更（与 FR-005 一致）
- **方法**: `getValue()`, `toString()`, `equals()`, `hashCode()`

### 5. EntitySchemaFQN

M2 层元模型 EntitySchema 的全限定名，携带完整版本号。

- **格式**: `bundleCode ":" version "." packagePath "." schemaName`
- **示例**: `order:1.0.0.pkg_order.Order`
- **约束**: 必须绑定已发布（PUBLISHED）版本的元模型，禁止绑定草稿态（DRAFT）版本
- **方法**: `getValue()`, `getBundleCode()`, `getVersion()`, `toString()`

### 6. VersionNumber

不可变版本号值对象，用于主表 `currentVersion` 和历史表 `version`。

- **类型**: `Integer`，从 1 开始递增
- **方法**: `increment() → VersionNumber`（返回新值对象，不修改自身）
- **不变性**: 与 FR-018 一致——生效时版本号自动递增

### 7. JsonSchemaSnapshot

上游 EntitySchema 编译产出的 JSON Schema 快照缓存值对象。

- **内容**: `Map<String, Object>`（已解析的 JSON Schema 结构）
- **绑定**: 与 `entitySchemaFqn` 一一对应
- **用途**: 传递给 JSON Schema Validator 执行运行时校验
- **缓存策略**: 以 `entitySchemaFqn` 为 key，Caffeine 缓存 TTL 30min

---

## 关系图

```
┌──────────────────────────┐
│    EntitySchemaFQN       │  值对象：关联元模型全限定名
│  (M2 层 FQN 格式)        │  如 "order:1.0.0.pkg_order.Order"
└───────────▲──────────────┘
            │ 引用
            │
┌───────────┴──────────────┐     ┌──────────────────────┐
│   MetadataEntityDraft    │     │    MetadataEntity    │
│   (草稿表 — 编辑隔离)     │────▶│    (主表 — 唯一生效版) │
│                          │ 生效 │                      │
│  - fqn: FQN              │     │  - fqn: FQN           │
│  - name: String          │     │  - name: String       │
│  - content: JSONB        │     │  - content: JSONB     │
│  - baseVersion: Int?     │     │  - currentVersion: Ver│
│  - entitySchemaFqn: ...  │     │  - entitySchemaFqn: ..│
└──────────────────────────┘     └───────────┬──────────┘
            │                                │
            │ 删除草稿                        │ 归档快照 (原子事务)
            ▼                                ▼
       [物理删除]                   ┌──────────────────────┐
                                   │    EntityVersion     │
                                   │    (历史表 — 只读归档) │
                                   │                      │
                                   │  - fqn: FQN          │
                                   │  - version: Int      │
                                   │  - content: JSONB    │
                                   │  - createdTime       │
                                   └──────────────────────┘
```

## 生命周期状态机

```
                    createDraft()
  [不存在] ──────────────────────▶ [DRAFT]
       ▲                                │
       │ reActivate()                   │ activate()
       │ (从历史表恢复)                   │ 主表写入 + 历史归档 + 删除草稿
       │                                ▼
       │                          [ACTIVE]
       │                                │
       │                                │ deactivate()
       │                                │ 主表删除 (历史保留)
       │                                ▼
       └─────────────────────── [DEPRECATED]
    (历史表数据不变)                        (历史表数据不变)
```

**关键规则**:
- DRAFT → ACTIVE：原子事务，版本号递增，主表写入/覆盖
- ACTIVE → DEPRECATED：原子事务，主表删除，下线前校验外部引用与子实体
- DEPRECATED → ACTIVE：无修改直接从历史表恢复（版本号不变），需修改则先创建草稿
- DRAFT → [不存在]：物理删除草稿记录

## 校验规则汇总

### 写入时校验（createDraft / updateDraft）

| 校验项 | 触发时机 | 违规行为 |
|--------|---------|---------|
| FQN segment 文法 | 创建时 | 拒绝，返回 `FQN_INVALID_SEGMENT` |
| FQN 全局唯一性（主表+草稿表） | 创建时 | 拒绝，返回 `FQN_CONFLICT` |
| JSON Schema 全字段结构校验 | 每次保存 | 拒绝，返回结构化错误列表 |
| entity_schema_fqn 版本已发布 | 创建时 | 拒绝，返回 `SCHEMA_VERSION_NOT_PUBLISHED` |
| parent_fqn 父实体已生效 | 创建时（子实体） | 拒绝，返回 `PARENT_NOT_ACTIVE` |
| FQN 不可变更 | 编辑时 | 拒绝（FR-005） |
| parent_fqn 不可变更 | 编辑时 | 拒绝（FR-005） |
| entity_schema_fqn 不可变更 | 编辑时 | 拒绝（FR-005） |

### 生效前校验（activate）

| 校验项 | 描述 |
|--------|------|
| JSON Schema 全字段结构校验 | 最终确认 |
| 组合层级合法性 | 父实体当前状态为 ACTIVE |
| 元模型版本有效性 | entity_schema_fqn 对应版本为 PUBLISHED |

### 下线前校验（deactivate）

| 校验项 | 描述 |
|--------|------|
| 外部活跃引用检查 | 调用语义关系网络模块，存在引用则拦截 |
| 生效子实体检查 | FQN 前缀匹配 `fqn + "."`，存在生效子实体则拦截 |
