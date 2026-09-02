CREATE SCHEMA IF NOT EXISTS schedule_schema;

CREATE TYPE schedule_schema.service_schedule_status AS ENUM (
    'SCHEDULED', 'RESCHEDULING', 'CHANGED',
    'COMPLETED', 'CANCELED', 'NO_SHOW'
);

CREATE TABLE IF NOT EXISTS schedule_schema.p_service_schedules (
    service_schedule_id UUID PRIMARY KEY,

    -- 논리 FK -> care_plan_schema.p_care_plan_service_preferences(service_preference_id)
    service_preference_id UUID NOT NULL,

    -- 논리 FK -> provider_schema.p_provide_service_offerings(service_offering_id)
    service_offering_id UUID NOT NULL,

    status schedule_schema.service_schedule_status NOT NULL DEFAULT 'SCHEDULED',
    date DATE NOT NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP NOT NULL,
    cancel_reason TEXT,
    canceled_at TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

CREATE TABLE IF NOT EXISTS schedule_schema.p_care_plan_service_results (
    service_result_id UUID PRIMARY KEY,
    service_schedule_id UUID NOT NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID

);
