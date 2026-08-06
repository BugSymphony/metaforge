-- ============================================================
-- V005: metadata-management BC DDL
-- Schema: metadata_management
-- Tables: metadata_entity (主表), metadata_entity_draft (草稿表), entity_version (历史表)
-- ============================================================

-- 1. Schema
CREATE SCHEMA IF NOT EXISTS metadata_management;

-- 2. 主表：唯一生效版本
CREATE TABLE IF NOT EXISTS metadata_management.metadata_entity (
    id              BIGSERIAL       PRIMARY KEY,
    fqn             VARCHAR(1024)   NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    description     VARCHAR(2000),
    parent_fqn      VARCHAR(1024),
    entity_schema_fqn VARCHAR(512)  NOT NULL,
    content         JSONB           NOT NULL,
    embedding       JSONB,
    current_version INTEGER        NOT NULL DEFAULT 1,
    created_by      VARCHAR(255)    NOT NULL,
    created_time    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(255)    NOT NULL,
    updated_time    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_metadata_entity_fqn UNIQUE (fqn)
);

CREATE INDEX IF NOT EXISTS idx_metadata_entity_fqn ON metadata_management.metadata_entity (fqn);
CREATE INDEX IF NOT EXISTS idx_metadata_entity_fqn_prefix ON metadata_management.metadata_entity (fqn text_pattern_ops);
CREATE INDEX IF NOT EXISTS idx_metadata_entity_schema_fqn ON metadata_management.metadata_entity (entity_schema_fqn);
CREATE INDEX IF NOT EXISTS idx_metadata_entity_content ON metadata_management.metadata_entity USING GIN (content jsonb_path_ops);

-- 3. 草稿表：编辑态数据物理隔离
CREATE TABLE IF NOT EXISTS metadata_management.metadata_entity_draft (
    id                  BIGSERIAL       PRIMARY KEY,
    fqn                 VARCHAR(1024)   NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    description         VARCHAR(2000),
    parent_fqn          VARCHAR(1024),
    entity_schema_fqn   VARCHAR(512)    NOT NULL,
    content             JSONB           NOT NULL,
    embedding           JSONB,
    base_version        INTEGER,
    created_by          VARCHAR(255)    NOT NULL,
    created_time        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(255)    NOT NULL,
    updated_time        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_metadata_entity_draft_fqn UNIQUE (fqn)
);

CREATE INDEX IF NOT EXISTS idx_metadata_entity_draft_fqn ON metadata_management.metadata_entity_draft (fqn);

-- 4. 历史表：只读归档，仅 INSERT
CREATE TABLE IF NOT EXISTS metadata_management.entity_version (
    id                  BIGSERIAL       PRIMARY KEY,
    fqn                 VARCHAR(1024)   NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    description         VARCHAR(2000),
    parent_fqn          VARCHAR(1024),
    version             INTEGER        NOT NULL,
    entity_schema_fqn   VARCHAR(512)    NOT NULL,
    content             JSONB           NOT NULL,
    embedding           JSONB,
    created_by          VARCHAR(255)    NOT NULL,
    created_time        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_entity_version_fqn_version UNIQUE (fqn, version)
);

CREATE INDEX IF NOT EXISTS idx_entity_version_fqn ON metadata_management.entity_version (fqn);
CREATE INDEX IF NOT EXISTS idx_entity_version_fqn_version ON metadata_management.entity_version (fqn, version);
