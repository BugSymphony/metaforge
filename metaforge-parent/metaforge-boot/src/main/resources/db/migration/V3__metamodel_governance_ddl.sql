-- ============================================================================
-- V1: 元模型治理 Schema DDL
-- 创建 metamodel_governance Schema 及所有实体表
-- 所有 FK 使用 VARCHAR(512) FQN 引用
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS metamodel_governance;

-- ----------------------------------------------------------------------------
-- 1. bundle — 顶层治理单元
-- ----------------------------------------------------------------------------
CREATE TABLE metamodel_governance.bundle (
    id              BIGSERIAL       PRIMARY KEY,
    fqn             VARCHAR(512)    NOT NULL UNIQUE,
    name            VARCHAR(255)    NOT NULL,
    description     TEXT            NOT NULL,
    owner           VARCHAR(128)    NOT NULL,
    is_system       BOOLEAN         NOT NULL DEFAULT FALSE,
    embedding       JSONB           DEFAULT NULL,
    created_time    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 2. bundle_version — Bundle 的版本化内容容器
-- ----------------------------------------------------------------------------
CREATE TABLE metamodel_governance.bundle_version (
    id                      BIGSERIAL       PRIMARY KEY,
    fqn                     VARCHAR(512)    NOT NULL UNIQUE,
    bundle_fqn              VARCHAR(512)    NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    source_version_fqn      VARCHAR(512)    DEFAULT NULL,
    upgrade_level           VARCHAR(20)     DEFAULT NULL,
    created_time            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bundle_version_bundle_fqn_status
    ON metamodel_governance.bundle_version(bundle_fqn, status);

-- ----------------------------------------------------------------------------
-- 3. package — Bundle 版本内的分类容器
-- ----------------------------------------------------------------------------
CREATE TABLE metamodel_governance.package (
    id                      BIGSERIAL       PRIMARY KEY,
    fqn                     VARCHAR(512)    NOT NULL UNIQUE,
    bundle_version_fqn      VARCHAR(512)    NOT NULL,
    parent_package_fqn      VARCHAR(512)    DEFAULT NULL,
    description             TEXT            NOT NULL,
    depth                   INT             NOT NULL DEFAULT 0,
    embedding               JSONB           DEFAULT NULL,
    created_time            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_package_bundle_version_fqn
    ON metamodel_governance.package(bundle_version_fqn);
CREATE INDEX idx_package_parent_package_fqn
    ON metamodel_governance.package(parent_package_fqn);

-- ----------------------------------------------------------------------------
-- 4. entity_schema — 核心语义层：实体定义
-- ----------------------------------------------------------------------------
CREATE TABLE metamodel_governance.entity_schema (
    id                      BIGSERIAL       PRIMARY KEY,
    fqn                     VARCHAR(512)    NOT NULL UNIQUE,
    package_fqn             VARCHAR(512)    NOT NULL,
    bundle_version_fqn      VARCHAR(512)    NOT NULL,
    name                    VARCHAR(255)    NOT NULL,
    description             TEXT            NOT NULL,
    native_attributes       JSONB           DEFAULT NULL,
    mounted_template_fqns   JSONB           DEFAULT NULL,
    json_schema             JSONB           DEFAULT NULL,
    embedding               JSONB           DEFAULT NULL,
    created_time            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_entity_schema_package_fqn
    ON metamodel_governance.entity_schema(package_fqn);
CREATE INDEX idx_entity_schema_bundle_version_fqn
    ON metamodel_governance.entity_schema(bundle_version_fqn);

-- ----------------------------------------------------------------------------
-- 5. relation_schema — 核心语义层：关系定义
-- ----------------------------------------------------------------------------
CREATE TABLE metamodel_governance.relation_schema (
    id                      BIGSERIAL       PRIMARY KEY,
    fqn                     VARCHAR(512)    NOT NULL UNIQUE,
    package_fqn             VARCHAR(512)    NOT NULL,
    bundle_version_fqn      VARCHAR(512)    NOT NULL,
    name                    VARCHAR(255)    NOT NULL,
    description             TEXT            NOT NULL,
    source_fqn              VARCHAR(512)    NOT NULL,
    target_fqn              VARCHAR(512)    NOT NULL,
    association_type        VARCHAR(50)     NOT NULL,
    cardinality_source      VARCHAR(20)     NOT NULL,
    cardinality_target      VARCHAR(20)     NOT NULL,
    native_attributes       JSONB           DEFAULT NULL,
    mounted_template_fqns   JSONB           DEFAULT NULL,
    json_schema             JSONB           DEFAULT NULL,
    embedding               JSONB           DEFAULT NULL,
    created_time            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_relation_schema_source_fqn
    ON metamodel_governance.relation_schema(source_fqn);
CREATE INDEX idx_relation_schema_target_fqn
    ON metamodel_governance.relation_schema(target_fqn);
CREATE INDEX idx_relation_schema_package_fqn
    ON metamodel_governance.relation_schema(package_fqn);

-- ----------------------------------------------------------------------------
-- 6. attribute_template — 属性模板组
-- ----------------------------------------------------------------------------
CREATE TABLE metamodel_governance.attribute_template (
    id                      BIGSERIAL       PRIMARY KEY,
    fqn                     VARCHAR(512)    NOT NULL UNIQUE,
    bundle_version_fqn      VARCHAR(512)    NOT NULL,
    name                    VARCHAR(255)    NOT NULL,
    description             TEXT            DEFAULT NULL,
    attribute_definitions   JSONB           NOT NULL,
    created_time            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_attribute_template_bundle_version_fqn
    ON metamodel_governance.attribute_template(bundle_version_fqn);

-- ----------------------------------------------------------------------------
-- 7. bundle_dependency — 跨 Bundle 版本依赖
-- ----------------------------------------------------------------------------
CREATE TABLE metamodel_governance.bundle_dependency (
    id                      BIGSERIAL       PRIMARY KEY,
    source_version_fqn      VARCHAR(512)    NOT NULL,
    target_version_fqn      VARCHAR(512)    NOT NULL,
    created_time            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (source_version_fqn, target_version_fqn)
);

CREATE INDEX idx_bundle_dependency_source
    ON metamodel_governance.bundle_dependency(source_version_fqn);
CREATE INDEX idx_bundle_dependency_target
    ON metamodel_governance.bundle_dependency(target_version_fqn);

-- ----------------------------------------------------------------------------
-- 8. export_manifest — 导出清单
-- ----------------------------------------------------------------------------
CREATE TABLE metamodel_governance.export_manifest (
    id                      BIGSERIAL       PRIMARY KEY,
    bundle_version_fqn      VARCHAR(512)    NOT NULL UNIQUE,
    exported_package_fqns   JSONB           NOT NULL,
    created_time            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);
