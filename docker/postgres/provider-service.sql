CREATE SCHEMA IF NOT EXISTS provider_schema;

CREATE TABLE IF NOT EXISTS provider_schema.p_provide_services (
    provide_service_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    content VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

CREATE TABLE IF NOT EXISTS provider_schema.p_provide_service_offerings (
    service_offering_id UUID PRIMARY KEY,

    -- 논리 FK -> user_schema.p_users(user_id)
    provider_id UUID NOT NULL,

    -- ERD 참조 -> provider_schema.p_provide_services(provide_service_id)
    provide_service_id UUID NOT NULL,

    -- 논리 FK -> user_schema.p_regions(region_id)
    region_id UUID NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

CREATE TABLE IF NOT EXISTS provider_schema.p_provide_works (
    provide_work_id UUID PRIMARY KEY,
    service_offering_id UUID NOT NULL,
    day INTEGER NOT NULL,
    started_at TIME NOT NULL,
    finished_at TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID

);
