-- ============================================================
-- V006: metaforge-graph BC DDL (语义关系网络)
-- Schema: semantic_relation_network
-- Tables: relation_instance (主表), relation_instance_draft (草稿表),
--         relation_version (历史表), entity_relation_index (双向索引表)
-- ============================================================

-- 启用 pg_trgm 扩展（GIN 索引加速 ILIKE 模糊匹配）
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 创建 Schema
CREATE SCHEMA IF NOT EXISTS semantic_relation_network;

-- ============================================================
-- 1. relation_instance — 关系实例主表（当前生效的唯一正式版本）
-- ============================================================
CREATE TABLE semantic_relation_network.relation_instance (
    id                      BIGSERIAL       PRIMARY KEY,
    fqn                     VARCHAR(1536)   NOT NULL,
    name                    VARCHAR(512)    NOT NULL,
    description             TEXT,
    source_entity_fqn       VARCHAR(512)    NOT NULL,
    target_entity_fqn       VARCHAR(512)    NOT NULL,
    relation_type            VARCHAR(64)     NOT NULL,
    relation_schema_fqn     VARCHAR(256)    NOT NULL,
    content                 JSONB           NOT NULL,
    embedding               JSONB,
    current_version         INTEGER         NOT NULL DEFAULT 1,
    created_by              VARCHAR(128),
    created_time            TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by              VARCHAR(128),
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

-- ============================================================
-- 2. relation_instance_draft — 关系实例草稿表（编辑态隔离容器）
-- ============================================================
CREATE TABLE semantic_relation_network.relation_instance_draft (
    id                      BIGSERIAL       PRIMARY KEY,
    fqn                     VARCHAR(1536)   NOT NULL,
    name                    VARCHAR(512)    NOT NULL,
    description             TEXT,
    source_entity_fqn       VARCHAR(512)    NOT NULL,
    target_entity_fqn       VARCHAR(512)    NOT NULL,
    relation_type            VARCHAR(64)     NOT NULL,
    relation_schema_fqn     VARCHAR(256)    NOT NULL,
    content                 JSONB           NOT NULL,
    embedding               JSONB,
    base_version            INTEGER,
    created_by              VARCHAR(128),
    created_time            TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by              VARCHAR(128),
    updated_time            TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_rid_fqn UNIQUE (fqn)
);

CREATE INDEX idx_rid_updated_time ON semantic_relation_network.relation_instance_draft(updated_time);

-- ============================================================
-- 3. relation_version — 关系版本历史表（只读归档库，仅允许 INSERT）
-- ============================================================
CREATE TABLE semantic_relation_network.relation_version (
    id                      BIGSERIAL       PRIMARY KEY,
    fqn                     VARCHAR(1536)   NOT NULL,
    name                    VARCHAR(512)    NOT NULL,
    description             TEXT,
    source_entity_fqn       VARCHAR(512)    NOT NULL,
    target_entity_fqn       VARCHAR(512)    NOT NULL,
    relation_type            VARCHAR(64)     NOT NULL,
    relation_schema_fqn     VARCHAR(256)    NOT NULL,
    content                 JSONB           NOT NULL,
    embedding               JSONB,
    version                 INTEGER         NOT NULL,
    activated_by            VARCHAR(128),
    activated_time          TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_rv_fqn_version UNIQUE (fqn, version)
);

CREATE INDEX idx_rv_fqn ON semantic_relation_network.relation_version(fqn, version DESC);
CREATE INDEX idx_rv_activated_time ON semantic_relation_network.relation_version(activated_time);

-- ============================================================
-- 4. entity_relation_index — 实体双向引用索引表
-- ============================================================
CREATE TABLE semantic_relation_network.entity_relation_index (
    id              BIGSERIAL       PRIMARY KEY,
    entity_fqn      VARCHAR(512)    NOT NULL,
    direction       VARCHAR(8)      NOT NULL,
    relation_fqn    VARCHAR(1536)   NOT NULL,
    created_time    TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_ei_entity_direction_relation
        UNIQUE (entity_fqn, direction, relation_fqn)
);

CREATE INDEX idx_ei_entity_direction
    ON semantic_relation_network.entity_relation_index(entity_fqn, direction);
CREATE INDEX idx_ei_relation_fqn
    ON semantic_relation_network.entity_relation_index(relation_fqn);
