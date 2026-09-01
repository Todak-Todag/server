CREATE SCHEMA IF NOT EXISTS care_plan_schema;

CREATE TYPE care_plan_schema.care_plan_status AS ENUM (
    'UNDER_REVIEW', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED'
);

CREATE TYPE care_plan_schema.preferred_time_slot AS ENUM (
    'MORNING', 'AFTERNOON'
);

CREATE TABLE care_plan_schema.p_care_plans (
    id UUID PRIMARY KEY,

    -- 논리 FK -> user_schema.p_users(id)
    patient_id UUID NOT NULL,

    -- 논리 FK -> discharge_schema.p_discharge(id)
    discharge_id UUID NOT NULL,

    status care_plan_schema.care_plan_status NOT NULL DEFAULT 'UNDER_REVIEW',
    note TEXT,
    start_date DATE NOT NULL,
    finish_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

CREATE TABLE care_plan_schema.p_care_plan_services (
    id UUID PRIMARY KEY,
    care_plan_id UUID NOT NULL,

    -- 논리 FK -> provider_schema.p_provide_services(id)
    provide_service_id UUID NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,

    CONSTRAINT fk_care_plan_services_plan
        FOREIGN KEY (care_plan_id)
        REFERENCES care_plan_schema.p_care_plans(id)
);

CREATE TABLE care_plan_schema.p_care_plan_service_preferences (
    id UUID PRIMARY KEY,
    plan_service_id UUID NOT NULL,
    preferred_time_slot care_plan_schema.preferred_time_slot NOT NULL,
    preferred_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,

    CONSTRAINT fk_service_preferences_plan_service
        FOREIGN KEY (plan_service_id)
        REFERENCES care_plan_schema.p_care_plan_services(id)
);
