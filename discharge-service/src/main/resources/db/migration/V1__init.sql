CREATE TABLE IF NOT EXISTS discharge_schema.p_discharges (
    discharge_id UUID PRIMARY KEY,

    -- 논리 FK -> user_schema.p_users(user_id)
    patient_id UUID NOT NULL,

    -- 논리 FK -> user_schema.p_users(user_id)
    hospital_staff_id UUID NOT NULL,

    hospital_name VARCHAR(50) NOT NULL,
    scheduled_date DATE,
    actual_date DATE,

    status VARCHAR(255) NOT NULL DEFAULT 'SCHEDULED',

    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);


-- ─────────────────────────────────────────────
-- 값 검증 (기존 ENUM 타입 대체). validate 는 CHECK 을 검사하지 않아 안전
-- ─────────────────────────────────────────────
ALTER TABLE discharge_schema.p_discharges
    ADD CONSTRAINT ck_p_discharges_status CHECK (status IN ('SCHEDULED','POSTPONED','COMPLETED','CANCELED'));