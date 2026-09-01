CREATE SCHEMA IF NOT EXISTS user_schema;

-- PostgreSQL ENUM types
CREATE TYPE user_schema.user_status AS ENUM (
    'PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED', 'WITHDRAWN'
);

CREATE TYPE user_schema.user_role AS ENUM (
    'PATIENT', 'HOSPITAL_STAFF', 'SOCIAL_WORKER',
    'SERVICE_PROVIDER', 'ADMIN', 'MASTER'
);

CREATE TYPE user_schema.consent_status AS ENUM (
    'AGREED', 'WITHDRAWN'
);

CREATE TABLE user_schema.p_regions (
    id UUID PRIMARY KEY,
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

CREATE TABLE user_schema.p_users (
    id UUID PRIMARY KEY,
    region_id UUID,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    status user_schema.user_status NOT NULL DEFAULT 'PENDING',
    status_change_reason VARCHAR(255),
    role user_schema.user_role NOT NULL,
    address VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,

    CONSTRAINT fk_users_region
        FOREIGN KEY (region_id)
        REFERENCES user_schema.p_regions(id)
);

CREATE TABLE user_schema.p_auths (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    refresh_token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    logout_at TIMESTAMP,

    CONSTRAINT fk_auths_user
        FOREIGN KEY (user_id)
        REFERENCES user_schema.p_users(id)
);

CREATE TABLE user_schema.p_consent_documents (
    id UUID PRIMARY KEY,
    consent_type VARCHAR(30) NOT NULL,
    title VARCHAR(255) NOT NULL,
    is_required BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

CREATE TABLE user_schema.p_consent_document_versions (
    id UUID PRIMARY KEY,
    consent_document_id UUID NOT NULL,
    version VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    effective_at TIMESTAMP NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,

    CONSTRAINT fk_consent_document_versions_document
        FOREIGN KEY (consent_document_id)
        REFERENCES user_schema.p_consent_documents(id)
);

CREATE TABLE user_schema.p_consents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    consent_document_version_id UUID NOT NULL,
    status user_schema.consent_status NOT NULL,
    agreed_at TIMESTAMP NOT NULL,
    withdrawn_at TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,

    CONSTRAINT fk_consents_user
        FOREIGN KEY (user_id)
        REFERENCES user_schema.p_users(id),

    CONSTRAINT fk_consents_document_version
        FOREIGN KEY (consent_document_version_id)
        REFERENCES user_schema.p_consent_document_versions(id)
);

CREATE UNIQUE INDEX ux_p_users_username_active
    ON user_schema.p_users (username)
    WHERE deleted_at IS NULL;
