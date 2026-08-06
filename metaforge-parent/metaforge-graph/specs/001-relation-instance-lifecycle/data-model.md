# 数据模型设计：语义关系实例全生命周期管理

**Feature**: 001-relation-instance-lifecycle | **Date**: 2026-08-01

## 概述

本文档定义 `metaforge-graph` BC 的三表正交存储数据模型、领域模型层次结构、值对象定义、状态流转规则与关键约束。所有数据模型严格遵循 BC 宪法中声明的「主表 + 草稿表 + 历史表」三层架构。

---

## 1. 数据库表设计（基础设施层）

### 1.1 主表 `relation_instance`

存储当前生效的唯一正式版本，是对外拓扑查询的唯一权威数据源。

```sql
CREATE SCHEMA IF NOT EXISTS semantic_relation_network;

CREATE TABLE semantic_relation_network.relation_instance (
    id                      BIGSERIAL       PRIMARY KEY,
    fqn                     VARCHAR(1536)   NOT NULL,          -- 关系 FQN: {源实体FQN}#{关系类型FQN}#{目标实体FQN}
    name                    VARCHAR(512)    NOT NULL,          -- 人类可读名称
    description             TEXT,                              -- 关系描述
    source_entity_fqn       VARCHAR(512)    NOT NULL,          -- 源实体 FQN
    target_entity_fqn       VARCHAR(512)    NOT NULL,          -- 目标实体 FQN
    relation_type            VARCHAR(64)     NOT NULL,          -- 关系类型枚举值
    relation_schema_fqn     VARCHAR(256)    NOT NULL,          -- 绑定的 RelationSchema FQN（含版本号）
    content                 JSONB           NOT NULL,          -- 属性内容（遵循 Schema 的 JSON Schema）
    embedding               JSONB,                             -- 向量嵌入（List<Float>，MVP 仅占位）
    current_version         INTEGER         NOT NULL DEFAULT 1,-- 当前版本号
    created_by              VARCHAR(128),                      -- 创建人
    created_time            TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by              VARCHAR(128),                      -- 更新人
    updated_time            TIMESTAMP       NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_ri_fqn UNIQUE (fqn)
);

CREATE INDEX idx_ri_source_fqn ON semantic_relation_network.relation_instance(source_entity_fqn);
CREATE INDEX idx_ri_target_fqn ON semantic_relation_network.relation_instance(target_entity_fqn);
CREATE INDEX idx_ri_relation_type ON semantic_relation_network.relation_instance(relation_type);
CREATE INDEX idx_ri_schema_fqn ON semantic_relation_network.relation_instance(relation_schema_fqn);
CREATE INDEX idx_ri_updated_time ON semantic_relation_network.relation_instance(updated_time);
CREATE INDEX idx_ri_name_trgm ON semantic_relation_network.relation_instance USING gin (name gin_trgm_ops);
CREATE INDEX idx_ri_description_trgm ON semantic_relation_network.relation_instance USING gin (description gin_trgm_ops);
CREATE INDEX idx_ri_source_type ON semantic_relation_network.relation_instance(relation_type, source_entity_fqn);
CREATE INDEX idx_ri_target_type ON semantic_relation_network.relation_instance(relation_type, target_entity_fqn);
```

### 1.2 草稿表 `relation_instance_draft`

存储未发布的编辑态副本，与主表物理隔离，对外完全不可见。

```sql
CREATE TABLE semantic_relation_network.relation_instance_draft (
    id                      BIGSERIAL       PRIMARY KEY,
    fqn                     VARCHAR(1536)   NOT NULL,          -- 关系 FQN（与主表格式一致）
    name                    VARCHAR(512)    NOT NULL,
    description             TEXT,
    source_entity_fqn       VARCHAR(512)    NOT NULL,
    target_entity_fqn       VARCHAR(512)    NOT NULL,
    relation_type            VARCHAR(64)     NOT NULL,
    relation_schema_fqn     VARCHAR(256)    NOT NULL,
    content                 JSONB           NOT NULL,
    embedding               JSONB,
    base_version            INTEGER,                           -- 基于哪个版本创建的草稿（null 表示全新创建）
    created_by              VARCHAR(128),
    created_time            TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by              VARCHAR(128),
    updated_time            TIMESTAMP       NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_rid_fqn UNIQUE (fqn)                         -- 同一 FQN 仅允许一条草稿
);

CREATE INDEX idx_rid_updated_time ON semantic_relation_network.relation_instance_draft(updated_time);
```

### 1.3 历史表 `relation_version`

只读归档库，仅支持 INSERT，禁止 UPDATE/DELETE。

```sql
CREATE TABLE semantic_relation_network.relation_version (
    id                      BIGSERIAL       PRIMARY KEY,
    fqn                     VARCHAR(1536)   NOT NULL,          -- 关系 FQN
    name                    VARCHAR(512)    NOT NULL,          -- 归档时的 name 快照
    description             TEXT,                              -- 归档时的 description 快照
    source_entity_fqn       VARCHAR(512)    NOT NULL,
    target_entity_fqn       VARCHAR(512)    NOT NULL,
    relation_type            VARCHAR(64)     NOT NULL,
    relation_schema_fqn     VARCHAR(256)    NOT NULL,
    content                 JSONB           NOT NULL,          -- 归档时的 content 全量快照
    embedding               JSONB,                             -- 归档时的 embedding 快照
    version                 INTEGER         NOT NULL,          -- 版本号（从 1 开始递增）
    activated_by            VARCHAR(128),                      -- 生效操作人
    activated_time          TIMESTAMP       NOT NULL DEFAULT NOW(), -- 生效时间
    
    CONSTRAINT uq_rv_fqn_version UNIQUE (fqn, version)         -- 同一 FQN 下版本号唯一
);

CREATE INDEX idx_rv_fqn ON semantic_relation_network.relation_version(fqn, version DESC);
CREATE INDEX idx_rv_activated_time ON semantic_relation_network.relation_version(activated_time);
```

### 1.4 双向索引表 `entity_relation_index`

为每个实体维护出边/入边关系列表，与关系生命周期同步更新。

```sql
CREATE TABLE semantic_relation_network.entity_relation_index (
    id              BIGSERIAL       PRIMARY KEY,
    entity_fqn      VARCHAR(512)    NOT NULL,       -- 实体 FQN
    direction       VARCHAR(8)      NOT NULL,       -- 'OUTBOUND' | 'INBOUND'
    relation_fqn    VARCHAR(1536)   NOT NULL,       -- 关联的关系 FQN
    created_time    TIMESTAMP       NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_ei_entity_direction_relation 
        UNIQUE (entity_fqn, direction, relation_fqn)
);

CREATE INDEX idx_ei_entity_direction 
    ON semantic_relation_network.entity_relation_index(entity_fqn, direction);
CREATE INDEX idx_ei_relation_fqn 
    ON semantic_relation_network.entity_relation_index(relation_fqn);
```

---

## 2. 领域模型层次（领域层）

### 2.1 聚合根

#### RelationInstance（生效态关系聚合根）

| 属性 | 类型 | 描述 |
|------|------|------|
| id | `Long` | 持久化主键（数据库自增） |
| fqn | `FQN` (值对象) | 关系 FQN 全局唯一标识 |
| name | `RelationName` (值对象) | 人类可读名称 |
| description | `RelationDescription` (值对象) | 关系描述 |
| sourceEntityFqn | `EntityFQN` (值对象) | 源实体 FQN |
| targetEntityFqn | `EntityFQN` (值对象) | 目标实体 FQN |
| relationType | `RelationType` (枚举) | 关系类型 |
| relationSchemaFqn | `RelationSchemaFQN` (值对象) | 绑定的 RelationSchema FQN（含版本号） |
| content | `Map<String, Object>` | 属性内容（结构化数据） |
| embedding | `List<Float>` | 向量嵌入（MVP 占位） |
| currentVersion | `VersionNumber` (值对象) | 当前版本号 |
| createdTime | `LocalDateTime` | 创建时间 |
| updatedTime | `LocalDateTime` | 更新时间 |

**核心业务方法**：
- `deprecate(DependencyCheckService)` — 执行下线（需依赖校验通过）
- 聚合根外部通过 `FQN` 标识引用，内部元素通过聚合根的业务方法操作

#### RelationInstanceDraft（草稿态关系聚合根）

| 属性 | 类型 | 描述 |
|------|------|------|
| id | `Long` | 持久化主键 |
| fqn | `FQN` (值对象) | 关系 FQN |
| name | `RelationName` (值对象) | 人类可读名称 |
| description | `RelationDescription` (值对象) | 关系描述 |
| sourceEntityFqn | `EntityFQN` (值对象) | 源实体 FQN |
| targetEntityFqn | `EntityFQN` (值对象) | 目标实体 FQN |
| relationType | `RelationType` (枚举) | 关系类型 |
| relationSchemaFqn | `RelationSchemaFQN` (值对象) | 绑定的 RelationSchema FQN |
| content | `Map<String, Object>` | 属性内容 |
| embedding | `List<Float>` | 向量嵌入（MVP 占位） |
| baseVersion | `Integer` | 基于哪个版本创建（null = 全新创建） |

**核心业务方法**：
- `updateContent(Map<String, Object> newContent, RelationSchemaValidationService)` — 更新内容并实时校验
- `activate(RelationInstanceRepository, ...)` — 生成生效版本聚合根并执行生效
- 创建后 `fqn`、`relationSchemaFqn`、`sourceEntityFqn`、`targetEntityFqn` 不可变更

**生命周期状态**：`DRAFT`（编辑中，仅此一种状态，生效后整体迁移至主表并删除草稿）

### 2.2 值对象

| 值对象 | 字段 | 约束 |
|--------|------|------|
| `FQN` | `value: String` | 格式 `{源实体FQN}#{关系类型FQN}#{目标实体FQN}`，最大长度 1536 |
| `RelationSchemaFQN` | `value: String` | 上游元模型 FQN 格式，含版本号 |
| `EntityFQN` | `value: String` | 上游实体 FQN 格式 |
| `VersionNumber` | `value: Integer` | ≥ 1，单调递增 |
| `RelationName` | `name: String` | 最大长度 512，不可为空 |
| `RelationDescription` | `description: String` | 文本类型，最大长度可配置 |
| `CardinalityRule` | `sourceCardinality: String`, `targetCardinality: String` | 1:1 / 1:N / N:M |
| `ContentSnapshot` | `content: Map<String, Object>` | JSONB 动态结构 |

### 2.3 枚举

**RelationType（关系类型）**：
| 枚举值 | 说明 |
|--------|------|
| `COMPOSITION` | 组成——整体与部分的结构化关系 |
| `ASSOCIATION_REFERENCE` | 关联引用——实体间的通用引用关系 |
| `MAPPING_CORRESPONDENCE` | 映射对应——不同领域实体间的映射 |
| `DEPENDENCY_INFLUENCE` | 依赖影响——依赖与影响链路 |
| `PROCESS_SEQUENCE` | 流程时序——流程中的先后顺序 |

---

## 3. 状态流转规则

### 3.1 关系实例生命周期

```
                    ┌──────────────────────┐
                    │     [不存在]           │
                    └──────────┬───────────┘
                               │ 创建草稿
                               ▼
                    ┌──────────────────────┐
                    │   DRAFT（草稿表）      │  ← 可编辑、可删除
                    │   对外不可见           │
                    └──────────┬───────────┘
                               │ 执行生效（原子四步事务）
                               │ - 主表写入/覆盖
                               │ - 历史表归档
                               │ - 草稿表删除
                               │ - 双向索引更新
                               ▼
                    ┌──────────────────────┐
                    │   ACTIVE（主表）       │  ← 对外默认可见
                    │   唯一生效版本         │     发布 ACTIVATED 事件
                    └──────────┬───────────┘
                               │ 执行下线
                               │ - 依赖校验
                               │ - 主表删除
                               │ - 历史表不变
                               ▼
                    ┌──────────────────────┐
                    │ DEPRECATED（历史表有档案）│  ← 历史表仅 INSERT
                    │   对外不可见             │     发布 DEPRECATED 事件
                    └──────────┬───────────┘
                               │ 重新生效（基于历史版本）
                               │ - 历史表 → 主表恢复
                               │ - 版本号不变
                               │ - 发布 ACTIVATED 事件
                               ▼
                    ┌──────────────────────┐
                    │   ACTIVE（主表）       │  ← 循环回活跃态
                    └──────────────────────┘
```

### 3.2 原子生效事务四步操作（不可分割）

1. **主表写入/覆盖**：INSERT ... ON CONFLICT (fqn) DO UPDATE（版本号递增）
2. **历史表归档**：INSERT INTO relation_version（全量快照）
3. **草稿表删除**：DELETE FROM relation_instance_draft WHERE fqn = ?
4. **双向索引更新**：INSERT INTO entity_relation_index（源实体 OUTBOUND + 目标实体 INBOUND）

任一步失败全量回滚（`@Transactional`），不存在中间脏状态。

### 3.3 原子下线操作

1. **主表删除**：DELETE FROM relation_instance WHERE fqn = ?
2. **双向索引删除**：DELETE FROM entity_relation_index WHERE relation_fqn = ?
3. **仅当无下游强依赖时**才执行（依赖校验前置）

---

## 4. 核心约束

| # | 约束 | 类型 | 描述 |
|---|------|------|------|
| C-1 | FQN 全局唯一 | 唯一索引 | 主表 + 草稿表 FQN 联合唯一，一个 FQN 全局至多存在一条生效或一条草稿 |
| C-2 | 元模型版本已发布 | 业务校验 | 创建时校验 `relation_schema_fqn` 指向已发布的 RelationSchema 版本 |
| C-3 | 端点实体有效性 | 业务校验 | 生效时源端/目标端实体必须处于 ACTIVE 状态（通过上游 api 查询） |
| C-4 | 基数约束 | 业务校验 | 基于 RelationSchema 定义的源端/目标端基数实时校验 |
| C-5 | 单草稿限制 | 唯一索引 | 同一 FQN 至多存在一条草稿 |
| C-6 | 同源同目标唯一 | 业务校验 | 同一 RelationSchema 下同源同目标的重复关系创建拦截 |
| C-7 | 端点类型匹配 | 业务校验 | 源/目标实体的 EntitySchema 类型与 RelationSchema 定义的端点类型匹配 |
| C-8 | 历史表只写 | 数据库权限 | REVOKE UPDATE, DELETE ON relation_version FROM app_user |
| C-9 | 跨域关系授权 | 业务校验 | 跨 Bundle 关系须符合上游导出清单与依赖规则 |
| C-10 | content 大小限制 | 应用层拦截 | 单条 content 字段不超过 10MB |

---

## 5. MapStruct 转换映射

### DTO ↔ 领域对象 ↔ JPO 转换矩阵

```
  api/dto (CreateDraftRequest, RelationInstanceDto, ...)
      ↕ MapStruct (infrastructure/converter/)
  domain (RelationInstance, RelationInstanceDraft, RelationVersion)
      ↕ MapStruct (infrastructure/converter/)
  jpa (RelationInstanceJpo, RelationInstanceDraftJpo, RelationVersionJpo)
```

转换器统一定义在 `metaforge-graph-core` 的 `infrastructure/converter/` 包下，禁止在领域层引入转换逻辑。

---

## 6. 跨 BC 关联

| 上游 BC | 消费方式 | 数据流方向 | 用于 |
|---------|----------|------------|------|
| `metamodel-governance` | `ElementDefinitionService.getRelationSchema(fqn)` | 读 | 获取 RelationSchema JSON Schema 与基数约束 |
| `metadata-management` | `MetadataQueryService.getByFqn(fqn)` | 读 | 校验端点实体存在性与生效状态 |
| `metadata-management` | `MetadataChangeEvent` | 事件消费 | 监听实体变更触发关系自动构建/同步 |

**严禁**: 跨 BC 直接写表、绕过 api 模块访问上游 `core` 模块、反向依赖上游 BC。
