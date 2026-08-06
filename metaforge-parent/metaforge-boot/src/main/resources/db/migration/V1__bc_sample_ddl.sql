CREATE SCHEMA IF NOT EXISTS bc_sample;

CREATE TABLE IF NOT EXISTS bc_sample.sample_entity (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    created_time TIMESTAMP,
    updated_time TIMESTAMP
);
