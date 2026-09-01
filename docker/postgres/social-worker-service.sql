CREATE SCHEMA IF NOT EXISTS social_worker_schema;

CREATE TYPE social_worker_schema.matching_status AS ENUM (
    'REQUESTED', 'ACTIVE', 'ENDED'
);

CREATE TABLE social_worker_schema.p_social_worker_matching_results (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    social_worker_id UUID NOT NULL,
    status social_worker_schema.matching_status NOT NULL DEFAULT 'REQUESTED',
    requested_at TIMESTAMP NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);
