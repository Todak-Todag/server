CREATE TABLE IF NOT EXISTS care_plan_schema.p_care_plans
(
    care_plan_id UUID PRIMARY KEY,

    -- 논리 FK -> user_schema.p_users(user_id)
    patient_id   UUID                              NOT NULL,

    -- 논리 FK -> discharge_schema.p_discharges(discharge_id)
    discharge_id UUID                              NOT NULL,

    status VARCHAR(255) NOT NULL DEFAULT 'UNDER_REVIEW',
    note         TEXT,
    start_date   DATE                              NOT NULL,
    finish_date  DATE                              NOT NULL,
    created_at   TIMESTAMPTZ                       NOT NULL,
    created_by   UUID                              NOT NULL,
    updated_at   TIMESTAMPTZ                       NOT NULL,
    updated_by   UUID                              NOT NULL,
    deleted_at   TIMESTAMPTZ,
    deleted_by   UUID
);

CREATE TABLE IF NOT EXISTS care_plan_schema.p_care_plan_services
(
    plan_service_id    UUID PRIMARY KEY,
    care_plan_id       UUID        NOT NULL,

    -- 논리 FK -> provider_schema.p_provide_services(provide_service_id)
    provide_service_id UUID        NOT NULL,

    created_at         TIMESTAMPTZ NOT NULL,
    created_by         UUID        NOT NULL,
    deleted_at         TIMESTAMPTZ,
    deleted_by         UUID
);

CREATE TABLE IF NOT EXISTS care_plan_schema.p_care_plan_service_preferences
(
    service_preference_id UUID PRIMARY KEY,
    plan_service_id       UUID                                 NOT NULL,
    preferred_time_slot VARCHAR(255) NOT NULL,
    preferred_date        DATE                                 NOT NULL,
    created_at            TIMESTAMPTZ                          NOT NULL,
    created_by            UUID                                 NOT NULL,
    updated_at            TIMESTAMPTZ                          NOT NULL,
    updated_by            UUID                                 NOT NULL,
    deleted_at            TIMESTAMPTZ,
    deleted_by            UUID
);

CREATE TABLE IF NOT EXISTS care_plan_schema.p_care_plan_outbox_events
(
    outbox_event_id    UUID PRIMARY KEY,

    aggregate_id       UUID        NOT NULL,

    event_type VARCHAR(255) NOT NULL,

    payload            TEXT        NOT NULL,

    status VARCHAR(255)
    NOT NULL DEFAULT 'PENDING',

    retry_count        INTEGER     NOT NULL DEFAULT 0,

    last_error_message TEXT,

    published_at       TIMESTAMPTZ,

    -- 다중 인스턴스 환경에서 PENDING -> PROCESSING 선점 시 낙관적 락(@Version)에 사용
    version            BIGINT      NOT NULL DEFAULT 0,

    created_at         TIMESTAMPTZ NOT NULL,
    created_by         UUID        NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,
    updated_by         UUID        NOT NULL
);


-- ─────────────────────────────────────────────
-- 값 검증 (기존 ENUM 타입 대체). validate 는 CHECK 을 검사하지 않아 안전
-- ─────────────────────────────────────────────
ALTER TABLE care_plan_schema.p_care_plan_outbox_events
    ADD CONSTRAINT ck_p_care_plan_outbox_events_event_type CHECK (event_type IN ('CARE_PLAN_CONFIRMED','CARE_PLAN_COMPLETED'));
ALTER TABLE care_plan_schema.p_care_plan_outbox_events
    ADD CONSTRAINT ck_p_care_plan_outbox_events_status CHECK (status IN ('PENDING','PROCESSING','SENT','FAILED'));
ALTER TABLE care_plan_schema.p_care_plan_service_preferences
    ADD CONSTRAINT ck_p_care_plan_service_preferences_preferred_time_slot CHECK (preferred_time_slot IN ('MORNING','AFTERNOON'));
ALTER TABLE care_plan_schema.p_care_plans
    ADD CONSTRAINT ck_p_care_plans_status CHECK (status IN ('UNDER_REVIEW','CONFIRMED','IN_PROGRESS','COMPLETED'));