CREATE SCHEMA IF NOT EXISTS care_plan_service_schema;

CREATE TABLE IF NOT EXISTS care_plan_service_schema.p_care_plans
(
    care_plan_id
    UUID
    PRIMARY
    KEY,
    patient_id
    UUID
    NOT
    NULL,
    discharge_id
    UUID
    NOT
    NULL,
    status
    VARCHAR
(
    30
) NOT NULL DEFAULT 'UNDER_REVIEW',
    start_date DATE NOT NULL,
    finish_date DATE NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
    );

CREATE TABLE IF NOT EXISTS care_plan_service_schema.p_care_plan_services
(
    plan_service_id
    UUID
    PRIMARY
    KEY,
    care_plan_id
    UUID
    NOT
    NULL,
    provide_service_id
    UUID
    NOT
    NULL,
    created_at
    TIMESTAMPTZ
    NOT
    NULL,
    created_by
    UUID
    NOT
    NULL,
    deleted_at
    TIMESTAMPTZ,
    deleted_by
    UUID
);

CREATE TABLE IF NOT EXISTS care_plan_service_schema.p_care_plan_service_preferences
(
    service_preference_id
    UUID
    PRIMARY
    KEY,
    plan_service_id
    UUID
    NOT
    NULL,
    preferred_time_slot
    VARCHAR
(
    20
) NOT NULL,
    preferred_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
    );