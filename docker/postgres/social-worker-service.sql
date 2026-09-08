CREATE SCHEMA IF NOT EXISTS social_worker_schema;

CREATE TYPE social_worker_schema.matching_status AS ENUM (
    'REQUESTED',
    'ACTIVE',
    'FAILED',
    'ENDED'
);

CREATE TABLE IF NOT EXISTS social_worker_schema.p_social_worker_matching_results (
    matching_result_id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    social_worker_id UUID,
    status social_worker_schema.matching_status NOT NULL DEFAULT 'REQUESTED',
    requested_at TIMESTAMPTZ NOT NULL,
    assigned_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_social_worker_matching_active_patient
    ON social_worker_schema.p_social_worker_matching_results (patient_id)
    WHERE status IN ('REQUESTED', 'ACTIVE')
      AND deleted_at IS NULL;
