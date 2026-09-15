CREATE TABLE IF NOT EXISTS social_worker_schema.p_social_worker_matching_results (
    matching_result_id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    social_worker_id UUID,
    status VARCHAR(255) NOT NULL DEFAULT 'REQUESTED',
    requested_at TIMESTAMPTZ NOT NULL,
    assigned_at TIMESTAMPTZ,
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
ALTER TABLE social_worker_schema.p_social_worker_matching_results
    ADD CONSTRAINT ck_p_social_worker_matching_results_status CHECK (status IN ('REQUESTED','ACTIVE','FAILED','ENDED'));