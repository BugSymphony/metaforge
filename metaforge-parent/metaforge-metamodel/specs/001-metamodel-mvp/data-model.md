# 数据模型: 元模型治理核心能力 MVP

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Research**: [research.md](./research.md)

## 实体关系概览

```
Bundle 1──* BundleVersion
BundleVersion 1──* Package (树形嵌套, 深度 ≤ 5)
BundleVersion 1──* AttributeTemplate
BundleVersion 1──* EntitySchema (通过 Package 间接归属)
BundleVersion 1──* RelationSchema (通过 Package 间接归属)
BundleVersion 1──* BundleDependency (作为 source)
EntitySchema *──* AttributeTemplate (多对多挂载)
RelationSchema *──* AttributeTemplate (多对多挂载)
RelationSchema *──1 EntitySchema (source_fqn)
RelationSchema *──1 EntitySchema (target_fqn)
BundleVersion 1──1 ExportManifest
```

### FQN 命名约束

所有 FQN 的 segment 组成部分禁止包含保留分隔符：
- bundle-code: 禁止 `:` 和 `.`（已由正则 `[a-z][a-z0-9_-]{2,63}` 保证）
- Package 段: 禁止 `:` 和 `.`
- EntitySchema / RelationSchema / AttributeTemplate 短名（segment）: 禁止 `:` 和 `.`

写入阶段校验时拦截非法字符，提示 `"segment 不允许包含保留分隔符 : 和 ."`。

---

## 1. Bundle (聚合根)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 自增主键 |
| fqn | VARCHAR(512) | UNIQUE, NOT NULL | 全局唯一标识，格式 `{bundle-code}`，如 `order` |
| name | VARCHAR(255) | NOT NULL | 人类可读名称 |
| description | TEXT | NOT NULL | 描述（覆盖适用场景、能力边界） |
| owner | VARCHAR(128) | NOT NULL | 负责人 |
| is_system | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否为系统内置 Bundle |
| embedding | JSONB | NULLABLE | 向量描述（MVP 阶段仅占位） |
| created_time | TIMESTAMP | NOT NULL | 创建时间 |
| updated_time | TIMESTAMP | NOT NULL | 更新时间 |

### 校验规则

- `fqn` 匹配正则 `[a-z][a-z0-9_-]{2,63}`
- `fqn` 全局唯一
- `description` 必填
- `is_system=true` 的 Bundle（如 `metaforge`）禁止删除

---

## 2. BundleVersion (实体)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 自增主键 |
| fqn | VARCHAR(512) | UNIQUE, NOT NULL | 格式 `{bundle-code}:{version}`，如 `order:1.0.0` |
| bundle_fqn | VARCHAR(512) | FK → Bundle.fqn, NOT NULL | 所属 Bundle 的 FQN（如 `order`） |
| status | VARCHAR(20) | NOT NULL | DRAFT 或 PUBLISHED |
| source_version_fqn | VARCHAR(512) | NULLABLE | 源版本 FQN（复制自哪个版本） |
| upgrade_level | VARCHAR(20) | NULLABLE | MAJOR / MINOR / PATCH |
| created_time | TIMESTAMP | NOT NULL | 创建时间 |
| updated_time | TIMESTAMP | NOT NULL | 更新时间 |

### 状态转换

```
[DRAFT] ──发布──▶ [PUBLISHED] (不可逆)
```

### 校验规则

- 同一 Bundle 下只能有一个 DRAFT 状态的版本
- 仅允许从最新 PUBLISHED 版本创建新草稿
- 发布时执行升级等级匹配校验
- 已发布版本不可修改（ORM 层拦截 UPDATE/DELETE）

---

## 3. Package (实体)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 自增主键 |
| fqn | VARCHAR(512) | UNIQUE, NOT NULL | 格式 `{bundle-code}:{version}.{path}`，如 `order:1.0.0.pkg_order` |
| bundle_version_fqn | VARCHAR(512) | FK → BundleVersion.fqn, NOT NULL | 所属 Bundle 版本 FQN |
| parent_package_fqn | VARCHAR(512) | FK → Package.fqn, NULLABLE | 父 Package FQN（树形嵌套） |
| description | TEXT | NOT NULL | 描述（明确子领域范围） |
| depth | INT | NOT NULL, DEFAULT 0 | 嵌套深度（根层=0） |
| embedding | JSONB | NULLABLE | 向量描述（MVP 阶段仅占位） |
| created_time | TIMESTAMP | NOT NULL | 创建时间 |
| updated_time | TIMESTAMP | NOT NULL | 更新时间 |

### 校验规则

- `fqn` 全局唯一
- `depth` ≤ 4（根层 + 4 级子层 = 5 层上限）
- `description` 必填
- 删除前校验：Package 下存在元模型元素时拦截
- 已发布版本中的 Package 不可删除、不可修改

---

## 4. EntitySchema (实体)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 自增主键 |
| fqn | VARCHAR(512) | UNIQUE, NOT NULL | 格式 `{bundle}:{version}.{pkg}.{name}`，如 `order:1.0.0.pkg_order.Order` |
| package_fqn | VARCHAR(512) | FK → Package.fqn, NOT NULL | 所属 Package FQN |
| bundle_version_fqn | VARCHAR(512) | FK → BundleVersion.fqn, NOT NULL | 所属 Bundle 版本 FQN（冗余） |
| name | VARCHAR(255) | NOT NULL | 人类可读显示名 |
| description | TEXT | NOT NULL | 语义描述（业务含义 + 适用场景） |
| native_attributes | JSONB | NULLABLE | 原生属性定义列表 |
| mounted_template_fqns | JSONB | NULLABLE | 挂载属性模板组 FQN 列表 `["fqn1", "fqn2"]` |
| json_schema | JSONB | NULLABLE | 发布时生成的扁平 JSON Schema（PUBLISHED 后固化） |
| embedding | JSONB | NULLABLE | 向量描述（MVP 阶段仅占位） |
| created_time | TIMESTAMP | NOT NULL | 创建时间 |
| updated_time | TIMESTAMP | NOT NULL | 更新时间 |

### 校验规则

- `fqn` 全局唯一
- `name` 必填
- `description` 必填
- 属性名唯一性：挂载模板组之间、模板组与原生属性之间无同名 → 写入阶段 + 发布阶段双环节校验
- 原生属性定义符合 JSON Schema Draft 2020-12 规范子集（仅 string/number/integer/boolean/array，不支持 object）
- 已发布版本：所有字段只读，`json_schema` 不可修改

### 派生字段（不存储）

- 短名: `fqn.rsplit(".", 1)[-1]`
- bundle-code: `fqn.split(":")[0]`
- version: `fqn` 中 `:` 后前三个数字 segment
- parent-fqn: `fqn.rsplit(".", 1)[0]`
- enabled: BundleVersion.status == PUBLISHED

---

## 5. RelationSchema (实体)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 自增主键 |
| fqn | VARCHAR(512) | UNIQUE, NOT NULL | 格式 `{bundle}:{version}.{pkg}.{name}` |
| package_fqn | VARCHAR(512) | FK → Package.fqn, NOT NULL | 所属 Package FQN |
| bundle_version_fqn | VARCHAR(512) | FK → BundleVersion.fqn, NOT NULL | 所属 Bundle 版本 FQN（冗余） |
| name | VARCHAR(255) | NOT NULL | 人类可读显示名 |
| description | TEXT | NOT NULL | 语义描述 |
| source_fqn | VARCHAR(512) | NOT NULL | 源端 EntitySchema 的 FQN |
| target_fqn | VARCHAR(512) | NOT NULL | 目标端 EntitySchema 的 FQN |
| association_type | VARCHAR(50) | NOT NULL | 关联类型枚举：组成/关联引用/映射对应/依赖影响/流程时序 |
| cardinality_source | VARCHAR(20) | NOT NULL | 源端基数（如 1、0..1、0..*、1..*） |
| cardinality_target | VARCHAR(20) | NOT NULL | 目标端基数 |
| native_attributes | JSONB | NULLABLE | 原生属性定义列表 |
| mounted_template_fqns | JSONB | NULLABLE | 挂载属性模板组 FQN 列表 |
| json_schema | JSONB | NULLABLE | 发布时生成的扁平 JSON Schema |
| embedding | JSONB | NULLABLE | 向量描述（MVP 阶段仅占位） |
| created_time | TIMESTAMP | NOT NULL | 创建时间 |
| updated_time | TIMESTAMP | NOT NULL | 更新时间 |

### 校验规则

- `fqn` 全局唯一
- `name` 必填
- `description` 必填
- `source_fqn` 和 `target_fqn` 引用目标 EntitySchema 存在且可见（跨 Bundle 时需在导出清单中）
- `association_type` 限定为内置枚举值（不可自定义）
- 属性名唯一性：与 EntitySchema 一致
- 已发布版本只读

---

## 6. AttributeTemplate (实体)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 自增主键 |
| fqn | VARCHAR(512) | UNIQUE, NOT NULL | 格式 `{bundle-code}:{version}.{name}`，如 `order:1.0.0.AuditFields` |
| bundle_version_fqn | VARCHAR(512) | FK → BundleVersion.fqn, NOT NULL | 直属 Bundle 版本 FQN（不隶属 Package） |
| name | VARCHAR(255) | NOT NULL | 人类可读显示名 |
| description | TEXT | NULLABLE | 描述（MVP 阶段可选） |
| attribute_definitions | JSONB | NOT NULL | 属性定义集合（JSON Schema Draft 2020-12 子集） |
| created_time | TIMESTAMP | NOT NULL | 创建时间 |
| updated_time | TIMESTAMP | NOT NULL | 更新时间 |

### 校验规则

- `fqn` 全局唯一，不含 Package 路径
- `name` 必填
- `description` 可选
- 属性约束符合 JSON Schema Draft 2020-12 规范子集
- 已发布版本的 `attribute_definitions` 不可修改

---

## 7. BundleDependency (实体)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 自增主键 |
| source_version_fqn | VARCHAR(512) | NOT NULL | 当前 BundleVersion 的 FQN |
| target_version_fqn | VARCHAR(512) | NOT NULL | 目标 BundleVersion 的 FQN（精确版本） |
| created_time | TIMESTAMP | NOT NULL | 创建时间 |

### 校验规则

- `target_version_fqn` 必须使用精确版本号
- 目标 Bundle 版本必须存在且已发布
- 循环依赖零容忍
- (source_version_fqn, target_version_fqn) 联合唯一

---

## 8. ExportManifest (值对象，随 BundleVersion 固化)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 自增主键 |
| bundle_version_fqn | VARCHAR(512) | FK → BundleVersion.fqn, UNIQUE, NOT NULL | 所属 Bundle 版本 FQN（一对一） |
| exported_package_fqns | JSONB | NOT NULL | 导出 Package FQN 列表 `["fqn1", "fqn2"]` |
| created_time | TIMESTAMP | NOT NULL | 创建时间 |
| updated_time | TIMESTAMP | NOT NULL | 更新时间 |

### 校验规则

- 导出 Package FQN 均存在于当前 Bundle 版本中
- 导出包含 RelationSchema 的 Package 时，自动校验关联两端 EntitySchema 所在包均已导出
- 随 Bundle 发布固化，不可独立修改

---

## JSONB 字段结构定义

### native_attributes (EntitySchema / RelationSchema)

```json
[
  {
    "name": "orderAmount",
    "type": "number",
    "required": true,
    "description": "订单金额",
    "minimum": 0
  },
  {
    "name": "status",
    "type": "string",
    "required": true,
    "enum": ["pending", "confirmed", "shipped"]
  }
]
```

### mounted_template_fqns (EntitySchema / RelationSchema)

```json
["order:1.0.0.AuditFields", "order:1.0.0.VersionFields"]
```

### attribute_definitions (AttributeTemplate)

```json
[
  {
    "name": "createdBy",
    "type": "string",
    "required": true,
    "description": "创建人"
  },
  {
    "name": "createdAt",
    "type": "string",
    "required": true,
    "format": "date-time",
    "description": "创建时间"
  }
]
```

### json_schema（发布时生成，扁平化）

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "orderAmount": { "type": "number", "minimum": 0 },
    "status": { "type": "string", "enum": ["pending", "confirmed", "shipped"] },
    "createdBy": { "type": "string" },
    "createdAt": { "type": "string", "format": "date-time" }
  },
  "required": ["orderAmount", "status", "createdBy", "createdAt"]
}
```

---

## Schema: metamodel_governance

所有表属于 `metamodel_governance` Schema，通过 Hibernate `@Table(schema = "metamodel_governance")` 声明。

---

## 索引设计

| 表 | 索引列 | 类型 | 目的 |
|----|--------|------|------|
| `entity_schema` | `fqn` | UNIQUE B-tree | 精确查询 + FQN 前缀集合过滤（`LIKE 'prefix%'` 走索引） |
| `relation_schema` | `fqn` | UNIQUE B-tree | 精确查询 + FQN 前缀集合过滤 |
| `relation_schema` | `source_fqn` | B-tree | 关系源端查询 |
| `relation_schema` | `target_fqn` | B-tree | 关系目标端查询 |
| `bundle_version` | `bundle_fqn` + `status` | B-tree | 按 Bundle FQN 查询版本 + 状态过滤 |
| `bundle_version` | `fqn` | UNIQUE B-tree | 版本 FQN 精确查询 |
| `package` | `parent_package_fqn` | B-tree | Package 树形层级查询 |
| `bundle_dependency` | `source_version_fqn` | B-tree | 依赖源查询与循环检测 |
| `bundle_dependency` | `target_version_fqn` | B-tree | 依赖目标查询 |

> 注：所有外键统一使用 FQN（VARCHAR 512），不再使用数字 ID。FQN 字段自身的 UNIQUE 约束已创建 B-tree 索引，同时满足前缀查询需求。MV 规模（≤1000 实体）下索引开销极小。
