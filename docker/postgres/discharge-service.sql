CREATE SCHEMA IF NOT EXISTS discharge_schema;

CREATE TYPE discharge_schema.discharge_status AS ENUM (
    'SCHEDULED', 'POSTPONED', 'COMPLETED', 'CANCELED'
);

CREATE TABLE IF NOT EXISTS discharge_schema.p_discharge (
    discharge_id UUID PRIMARY KEY,
    -- 논리 FK -> user_schema.p_users(user_id)
    patient_id UUID NOT NULL,
    -- 논리 FK -> user_schema.p_users(user_id)
    hospital_staff_id UUID NOT NULL,
    hospital_name VARCHAR(50) NOT NULL,
    schedule_date DATE NOT NULL,
    actual_date DATE,
    status discharge_schema.discharge_status NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);
