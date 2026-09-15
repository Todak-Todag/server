CREATE TABLE IF NOT EXISTS user_schema.p_regions (
    region_id UUID PRIMARY KEY,
    province VARCHAR(20) NOT NULL,
    district VARCHAR(20) NOT NULL,
    region_code VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

CREATE TABLE IF NOT EXISTS user_schema.p_users (
    user_id UUID PRIMARY KEY,
    region_id UUID,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    status_change_reason VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    address VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID

);

CREATE TABLE IF NOT EXISTS user_schema.p_auths (
    auth_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    refresh_token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    logout_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_schema.p_consent_documents (
    consent_document_id UUID PRIMARY KEY,
    consent_type VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    is_required BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

CREATE TABLE IF NOT EXISTS user_schema.p_consent_document_versions (
    consent_document_version_id UUID PRIMARY KEY,
    consent_document_id UUID NOT NULL,
    version VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    effective_at TIMESTAMP NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL

);

CREATE TABLE IF NOT EXISTS user_schema.p_consents (
    consent_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    consent_document_version_id UUID NOT NULL,
    status VARCHAR(255) NOT NULL,
    agreed_at TIMESTAMP NOT NULL,
    withdrawn_at TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL

);

CREATE UNIQUE INDEX IF NOT EXISTS ux_p_users_username_active
    ON user_schema.p_users (username)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_p_auths_user_active
    ON user_schema.p_auths (user_id)
    WHERE logout_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_p_auths_refresh_token_hash
    ON user_schema.p_auths (refresh_token_hash)
    WHERE logout_at IS NULL;


-- ─────────────────────────────────────────────
-- 값 검증 (기존 ENUM 타입 대체). validate 는 CHECK 을 검사하지 않아 안전
-- ─────────────────────────────────────────────
ALTER TABLE user_schema.p_consent_documents
    ADD CONSTRAINT ck_p_consent_documents_consent_type CHECK (consent_type IN ('PERSONAL_INFORMATION','SENSITIVE_INFORMATION','MARKETING_INFORMATION'));
ALTER TABLE user_schema.p_consents
    ADD CONSTRAINT ck_p_consents_status CHECK (status IN ('AGREED','WITHDRAWN'));
ALTER TABLE user_schema.p_users
    ADD CONSTRAINT ck_p_users_role CHECK (role IN ('PATIENT','HOSPITAL_STAFF','SOCIAL_WORKER','SERVICE_PROVIDER','ADMIN','MASTER'));
ALTER TABLE user_schema.p_users
    ADD CONSTRAINT ck_p_users_status CHECK (status IN ('PENDING','APPROVED','REJECTED','SUSPENDED','WITHDRAWN'));