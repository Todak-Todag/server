CREATE SCHEMA IF NOT EXISTS provider_schema;

CREATE TABLE IF NOT EXISTS provider_schema.p_provide_services (
    provide_service_id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    content VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_provide_services_name
    ON provider_schema.p_provide_services (name)
    WHERE deleted_at IS NULL;

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

CREATE TABLE IF NOT EXISTS provider_schema.p_provider_outbox_events (
    outbox_event_id UUID PRIMARY KEY,

    -- PROVIDER_MATCHED / PROVIDER_MATCH_FAILED
    event_type VARCHAR(50) NOT NULL,

    -- 논리 FK -> care_plan_schema 의 service_preference_id
    aggregate_id UUID NOT NULL,

    payload TEXT NOT NULL,

    -- 발행에 성공한 시각. null 이면 아직 발행되지 않은 건이다
    published_at TIMESTAMPTZ,

    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error_message TEXT,

    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL
);

-- 릴레이는 미발행 건만 생성 순서로 폴링한다
CREATE INDEX IF NOT EXISTS ix_provider_outbox_events_pending
    ON provider_schema.p_provider_outbox_events (created_at)
    WHERE published_at IS NULL;

-- 같은 제공자가 같은 서비스 종류를 두 번 등록할 수 없다
-- 논리 삭제된 건은 제외해야 지웠던 조합을 다시 등록할 수 있다
CREATE UNIQUE INDEX IF NOT EXISTS uq_service_offerings_provider_service
    ON provider_schema.p_provide_service_offerings (provider_id, provide_service_id)
    WHERE deleted_at IS NULL;