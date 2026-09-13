CREATE SCHEMA IF NOT EXISTS schedule_schema;

CREATE TYPE schedule_schema.service_schedule_status AS ENUM (
    'SCHEDULED', 'RESCHEDULING', 'CHANGED',
    'COMPLETED', 'CANCELED', 'NO_SHOW'
);

CREATE TABLE IF NOT EXISTS schedule_schema.p_service_schedules (
    service_schedule_id UUID PRIMARY KEY,

    -- 논리 FK -> care_plan_schema.p_care_plans(care_plan_id)
    care_plan_id UUID NOT NULL,

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

    -- 논리 FK -> schedule_schema.p_service_schedules(service_schedule_id)
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

CREATE TYPE schedule_schema.schedule_outbox_event_status AS ENUM (
    'PENDING', 'SENT', 'FAILED'
);

CREATE TABLE IF NOT EXISTS schedule_schema.p_schedule_outbox_events (
    outbox_event_id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload TEXT NOT NULL,
    status schedule_schema.schedule_outbox_event_status NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL,
    last_error_message TEXT,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL
);

-- CarePlanCompleted는 케어플랜당 한 번만 적재되어야 함
-- 애플리케이션은 케어플랜 단위 advisory lock으로 판정~적재 구간을 직렬화하며, 이 인덱스는 그 바깥에서
-- 들어온 중복을 막는 DB 불변식
-- 전체 (event_type, aggregate_id)에 걸지 않는 이유: ProviderReMatched는 일정 변경이
-- aggregate_id = service_schedule_id로 적재하는데, "변경 -> 재매칭 실패 -> SCHEDULED 복구 -> 재변경"에서
-- 같은 키로 두 번 적재되는 것이 정상 흐름이라 테이블 전체 유니크는 그 경로를 깨뜨림
CREATE UNIQUE INDEX IF NOT EXISTS ux_schedule_outbox_events_care_plan_completed
    ON schedule_schema.p_schedule_outbox_events (aggregate_id)
    WHERE event_type = 'CarePlanCompleted';

CREATE TYPE schedule_schema.service_matching_attempts_status AS ENUM (
    'MATCHED', 'FAILED'
);

CREATE TYPE schedule_schema.service_matching_attempts_preferred_time_slot AS ENUM (
    'MORNING', 'AFTERNOON'
);

CREATE TABLE schedule_schema.p_service_matching_attempts (
    matching_attempt_id UUID PRIMARY KEY,

    -- 논리 FK -> care_plan_schema.p_care_plans(care_plan_id)
    care_plan_id UUID NOT NULL,

    -- 논리 FK -> user_schema.p_regions(region_id)
    region_id UUID NOT NULL,

    -- 논리 FK -> provider_schema.p_provide_services(provide_service_id)
    provide_service_id UUID NOT NULL,

    -- 논리 FK -> care_plan_schema.p_care_plan_service_preferences(service_preference_id)
    service_preference_id UUID NOT NULL,

    -- 논리 FK -> provider_schema.p_provide_service_offerings(service_offering_id)
    service_offering_id UUID,

    date DATE NOT NULL,
    preferred_time_slot schedule_schema.service_matching_attempts_preferred_time_slot,
    status schedule_schema.service_matching_attempts_status NOT NULL,
    failure_reason TEXT,
    matched_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);
