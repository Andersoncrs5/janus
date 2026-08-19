CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

-- =========================================================
-- JANUS IAM
-- V1.0.0
-- =========================================================


-- =========================================================
-- 1. USERS
-- =========================================================

CREATE TABLE public.users (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    email VARCHAR(320) NOT NULL,

    username VARCHAR(50),

    full_name VARCHAR(150),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,

    failed_login_attempts INTEGER NOT NULL DEFAULT 0,

    locked_until TIMESTAMPTZ,

    last_login_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMPTZ,

    CONSTRAINT ck_users_email_not_empty
        CHECK (TRIM(email) <> ''),

    CONSTRAINT ck_users_username_not_empty
        CHECK (username IS NULL OR TRIM(username) <> ''),

    CONSTRAINT ck_users_full_name_not_empty
        CHECK (full_name IS NULL OR TRIM(full_name) <> ''),

    CONSTRAINT ck_users_failed_login_attempts
        CHECK (failed_login_attempts >= 0),

    CONSTRAINT ck_users_version
        CHECK (version >= 0)

);


CREATE UNIQUE INDEX ux_users_tenant_email
    ON public.users (tenant_id, LOWER(email))
    WHERE deleted_at IS NULL;


CREATE UNIQUE INDEX ux_users_tenant_username
    ON public.users (tenant_id, LOWER(username))
    WHERE username IS NOT NULL
      AND deleted_at IS NULL;


CREATE INDEX idx_users_tenant_id
    ON public.users (tenant_id);


CREATE INDEX idx_users_locked_until
    ON public.users (locked_until)
    WHERE locked_until IS NOT NULL;


CREATE INDEX idx_users_created_at
    ON public.users (created_at DESC);


-- =========================================================
-- 2. USER_CREDENTIALS
-- =========================================================

CREATE TABLE public.user_credentials (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,

    password_hash VARCHAR(255) NOT NULL,

    algorithm VARCHAR(50) NOT NULL DEFAULT 'argon2id',

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_credentials_user
        UNIQUE (user_id),

    CONSTRAINT fk_user_credentials_user
        FOREIGN KEY (user_id)
        REFERENCES public.users(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_user_credentials_password_hash_not_empty
        CHECK (TRIM(password_hash) <> ''),

    CONSTRAINT ck_user_credentials_algorithm_not_empty
        CHECK (TRIM(algorithm) <> ''),

    CONSTRAINT ck_user_credentials_version
        CHECK (version >= 0)

);


CREATE INDEX idx_user_credentials_tenant_id
    ON public.user_credentials(tenant_id);


CREATE INDEX idx_user_credentials_user_id
    ON public.user_credentials(user_id);


-- =========================================================
-- 3. SESSIONS
-- =========================================================

CREATE TABLE public.sessions (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,

    ip_address VARCHAR(45),

    user_agent TEXT,

    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,

    expires_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES public.users(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_sessions_version
        CHECK (version >= 0)

);


CREATE INDEX idx_sessions_tenant_id
    ON public.sessions(tenant_id);


CREATE INDEX idx_sessions_user_id
    ON public.sessions(user_id);


CREATE INDEX idx_sessions_user_active
    ON public.sessions(user_id)
    WHERE is_revoked = FALSE;


CREATE INDEX idx_sessions_expires_at
    ON public.sessions(expires_at);


CREATE INDEX idx_sessions_created_at
    ON public.sessions(created_at DESC);


-- =========================================================
-- 4. REFRESH_TOKENS
-- =========================================================

CREATE TABLE public.refresh_tokens (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    session_id UUID NOT NULL,

    user_id UUID NOT NULL,

    token_hash VARCHAR(255) NOT NULL,

    is_used BOOLEAN NOT NULL DEFAULT FALSE,

    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,

    expires_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_refresh_tokens_token_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_refresh_tokens_session
        FOREIGN KEY (session_id)
        REFERENCES public.sessions(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES public.users(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_refresh_tokens_token_hash_not_empty
        CHECK (TRIM(token_hash) <> ''),

    CONSTRAINT ck_refresh_tokens_version
        CHECK (version >= 0)

);


CREATE INDEX idx_refresh_tokens_tenant_id
    ON public.refresh_tokens(tenant_id);


CREATE INDEX idx_refresh_tokens_session_id
    ON public.refresh_tokens(session_id);


CREATE INDEX idx_refresh_tokens_user_id
    ON public.refresh_tokens(user_id);


CREATE INDEX idx_refresh_tokens_expires_at
    ON public.refresh_tokens(expires_at);


CREATE INDEX idx_refresh_tokens_active
    ON public.refresh_tokens(user_id)
    WHERE is_used = FALSE
      AND is_revoked = FALSE;


-- =========================================================
-- 5. ROLES
-- =========================================================

CREATE TABLE public.roles (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(100) NOT NULL,

    description TEXT,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMPTZ,

    CONSTRAINT ck_roles_name_not_empty
        CHECK (TRIM(name) <> ''),

    CONSTRAINT ck_roles_version
        CHECK (version >= 0)

);


CREATE UNIQUE INDEX ux_roles_tenant_name
    ON public.roles(tenant_id, LOWER(name))
    WHERE deleted_at IS NULL;


CREATE INDEX idx_roles_tenant_id
    ON public.roles(tenant_id);


CREATE INDEX idx_roles_active
    ON public.roles(tenant_id)
    WHERE is_active = TRUE
      AND deleted_at IS NULL;


-- =========================================================
-- 6. PERMISSIONS
-- =========================================================

CREATE TABLE public.permissions (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    slug VARCHAR(150) NOT NULL,

    description TEXT,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMPTZ,

    CONSTRAINT ck_permissions_slug_not_empty
        CHECK (TRIM(slug) <> ''),

    CONSTRAINT ck_permissions_version
        CHECK (version >= 0)

);


CREATE UNIQUE INDEX ux_permissions_tenant_slug
    ON public.permissions(tenant_id, LOWER(slug))
    WHERE deleted_at IS NULL;


CREATE INDEX idx_permissions_tenant_id
    ON public.permissions(tenant_id);


CREATE INDEX idx_permissions_active
    ON public.permissions(tenant_id)
    WHERE is_active = TRUE
      AND deleted_at IS NULL;


-- =========================================================
-- 7. USER_ROLES
-- =========================================================

CREATE TABLE public.user_roles (

    user_id UUID NOT NULL,

    role_id UUID NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES public.users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES public.roles(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_user_roles_version
        CHECK (version >= 0)

);


CREATE INDEX idx_user_roles_tenant_id
    ON public.user_roles(tenant_id);


CREATE INDEX idx_user_roles_role_id
    ON public.user_roles(role_id);


-- =========================================================
-- 8. ROLE_PERMISSIONS
-- =========================================================

CREATE TABLE public.role_permissions (

    role_id UUID NOT NULL,

    permission_id UUID NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
        REFERENCES public.roles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id)
        REFERENCES public.permissions(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_role_permissions_version
        CHECK (version >= 0)

);


CREATE INDEX idx_role_permissions_tenant_id
    ON public.role_permissions(tenant_id);


CREATE INDEX idx_role_permissions_permission_id
    ON public.role_permissions(permission_id);


-- =========================================================
-- 9. MFA_FACTORS
-- =========================================================

CREATE TABLE public.mfa_factors (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,

    type VARCHAR(30) NOT NULL,

    secret VARCHAR(1024),

    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_mfa_factors_user
        FOREIGN KEY (user_id)
        REFERENCES public.users(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_mfa_factors_type_not_empty
        CHECK (TRIM(type) <> ''),

    CONSTRAINT ck_mfa_factors_version
        CHECK (version >= 0)

);


CREATE INDEX idx_mfa_factors_tenant_id
    ON public.mfa_factors(tenant_id);


CREATE INDEX idx_mfa_factors_user_id
    ON public.mfa_factors(user_id);


CREATE INDEX idx_mfa_factors_enabled
    ON public.mfa_factors(user_id)
    WHERE is_enabled = TRUE
      AND deleted_at IS NULL;


-- =========================================================
-- 10. AUDIT_LOGS
-- =========================================================

CREATE TABLE public.audit_logs (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID,

    event VARCHAR(150) NOT NULL,

    ip_address VARCHAR(45),

    user_agent TEXT,

    metadata JSONB,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_logs_user
        FOREIGN KEY (user_id)
        REFERENCES public.users(id)
        ON DELETE SET NULL,

    CONSTRAINT ck_audit_logs_event_not_empty
        CHECK (TRIM(event) <> ''),

    CONSTRAINT ck_audit_logs_version
        CHECK (version >= 0)

);


CREATE INDEX idx_audit_logs_tenant_id
    ON public.audit_logs(tenant_id);


CREATE INDEX idx_audit_logs_user_id
    ON public.audit_logs(user_id);


CREATE INDEX idx_audit_logs_event
    ON public.audit_logs(tenant_id, event);


CREATE INDEX idx_audit_logs_created_at
    ON public.audit_logs(tenant_id, created_at DESC);


-- =========================================================
-- UPDATED_AT TRIGGER
-- =========================================================

CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


-- =========================================================
-- USERS
-- =========================================================

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON public.users
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();


-- =========================================================
-- USER_CREDENTIALS
-- =========================================================

CREATE TRIGGER trg_user_credentials_updated_at
BEFORE UPDATE ON public.user_credentials
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();


-- =========================================================
-- SESSIONS
-- =========================================================

CREATE TRIGGER trg_sessions_updated_at
BEFORE UPDATE ON public.sessions
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();


-- =========================================================
-- REFRESH_TOKENS
-- =========================================================

CREATE TRIGGER trg_refresh_tokens_updated_at
BEFORE UPDATE ON public.refresh_tokens
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();


-- =========================================================
-- ROLES
-- =========================================================

CREATE TRIGGER trg_roles_updated_at
BEFORE UPDATE ON public.roles
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();


-- =========================================================
-- PERMISSIONS
-- =========================================================

CREATE TRIGGER trg_permissions_updated_at
BEFORE UPDATE ON public.permissions
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();


-- =========================================================
-- MFA_FACTORS
-- =========================================================

CREATE TRIGGER trg_mfa_factors_updated_at
BEFORE UPDATE ON public.mfa_factors
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();
