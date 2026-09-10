-- =========================================================
-- JANUS IAM
-- V1.0.0
-- =========================================================

-- =========================================================
-- 1. USERS
-- =========================================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL,
    username VARCHAR(100),
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
    CONSTRAINT ck_users_email_lowercase
        CHECK (email = LOWER(email)),
    CONSTRAINT ck_users_username_not_empty
        CHECK (username IS NULL OR TRIM(username) <> ''),
    CONSTRAINT ck_users_username_no_spaces
        CHECK (username IS NULL OR username !~ '\s'),
    CONSTRAINT ck_users_full_name_not_empty
        CHECK (full_name IS NULL OR TRIM(full_name) <> ''),
    CONSTRAINT ck_users_failed_login_attempts
        CHECK (failed_login_attempts >= 0),
    CONSTRAINT ck_users_version
        CHECK (version >= 0),

    CONSTRAINT uk_email_user UNIQUE (email),
    CONSTRAINT uk_username_user UNIQUE (username)
);

CREATE UNIQUE INDEX uk_users_active_email
    ON users (email)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_users_active_username
    ON users (username)
    WHERE deleted_at IS NULL AND username IS NOT NULL;

CREATE INDEX idx_users_locked_until
    ON users (locked_until)
    WHERE locked_until IS NOT NULL;

CREATE INDEX idx_users_created_at
    ON users (created_at DESC);


-- =========================================================
-- 2. USER_CREDENTIALS
-- =========================================================

CREATE TABLE user_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    algorithm VARCHAR(50) NOT NULL DEFAULT 'argon2id',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT uk_user_id_credentials_user
        UNIQUE (user_id),
    CONSTRAINT fk_user_credentials_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_user_credentials_password_hash_not_empty
        CHECK (TRIM(password_hash) <> ''),
    CONSTRAINT ck_user_credentials_algorithm_not_empty
        CHECK (TRIM(algorithm) <> ''),
    CONSTRAINT ck_user_credentials_version
        CHECK (version >= 0)
);

CREATE INDEX idx_user_credentials_user_id
    ON user_credentials(user_id);


-- =========================================================
-- 3. SESSIONS
-- =========================================================

CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_sessions_expires_after_created
        CHECK (expires_at > created_at),
    CONSTRAINT ck_sessions_version
        CHECK (version >= 0)
);

CREATE INDEX idx_sessions_user_id
    ON sessions(user_id);

CREATE INDEX idx_sessions_user_active
    ON sessions(user_id)
    WHERE is_revoked = FALSE;

CREATE INDEX idx_sessions_expires_at
    ON sessions(expires_at);

CREATE INDEX idx_sessions_created_at
    ON sessions(created_at DESC);


-- =========================================================
-- 4. REFRESH_TOKENS
-- =========================================================

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by_token_id UUID,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT uk_refresh_tokens_token_hash
        UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_session
        FOREIGN KEY (session_id)
        REFERENCES sessions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_refresh_tokens_token_hash_not_empty
        CHECK (TRIM(token_hash) <> ''),
    CONSTRAINT ck_refresh_tokens_expires_after_created
        CHECK (expires_at > created_at),
    CONSTRAINT ck_refresh_tokens_version
        CHECK (version >= 0)
);

CREATE INDEX idx_refresh_tokens_session_id
    ON refresh_tokens(session_id);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);

CREATE INDEX idx_refresh_tokens_expires_at
    ON refresh_tokens(expires_at);

CREATE INDEX idx_refresh_tokens_active
    ON refresh_tokens(user_id)
    WHERE is_used = FALSE
      AND is_revoked = FALSE;


-- =========================================================
-- 5. ROLES
-- =========================================================

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT uk_roles_slug UNIQUE (slug),
    CONSTRAINT uk_roles_name UNIQUE (name),
    CONSTRAINT ck_roles_name_not_empty CHECK (TRIM(name) <> ''),
    CONSTRAINT ck_roles_version CHECK (version >= 0)
);


-- =========================================================
-- 6. PERMISSIONS
-- =========================================================

CREATE TYPE permission_risk_level AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');
CREATE TYPE permission_module AS ENUM ('AUTHENTICATION', 'AUTHORIZATION', 'IDENTITY', 'AUDIT', 'MFA', 'RELIABILITY');
CREATE TYPE permission_resource AS ENUM ('USER', 'SESSION', 'ROLE', 'PERMISSION', 'AUDIT_LOG', 'SYSTEM_SETTING');

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(100) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    description TEXT,

    module permission_module NOT NULL,
    resource permission_resource NOT NULL,
    action VARCHAR(50) NOT NULL,
    risk_level permission_risk_level NOT NULL DEFAULT 'LOW',

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,

    metadata JSONB,
    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    created_by UUID,

    CONSTRAINT uk_permissions_slug UNIQUE (slug),
    CONSTRAINT uk_permissions_name UNIQUE (name),
    CONSTRAINT ck_permissions_slug_not_empty CHECK (TRIM(slug) <> ''),
    CONSTRAINT ck_permissions_version CHECK (version >= 0),

    CONSTRAINT fk_permissions_created_by
            FOREIGN KEY (created_by)
            REFERENCES users(id)
            ON DELETE SET NULL
);


CREATE INDEX idx_permissions_module ON permissions (module);
CREATE INDEX idx_permissions_resource ON permissions (resource);
CREATE INDEX idx_permissions_active ON permissions (is_active) WHERE deleted_at IS NULL;

-- =========================================================
-- 7. USER_ROLES
-- =========================================================

CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    expires_at TIMESTAMPTZ,
    assigned_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_assigned_by
        FOREIGN KEY (assigned_by)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT uk_user_role_ids_user_roles UNIQUE (user_id, role_id),
    CONSTRAINT ck_user_roles_version CHECK (version >= 0)
);

CREATE INDEX idx_user_roles_role_id
    ON user_roles(role_id);


-- =========================================================
-- 8. ROLE_PERMISSIONS
-- =========================================================

CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ,
    assigned_by UUID,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_role_permission_ids UNIQUE (permission_id, role_id),
    CONSTRAINT ck_role_permissions_version CHECK (version >= 0)
);

CREATE INDEX idx_role_permissions_permission_id
    ON role_permissions(permission_id);


-- =========================================================
-- 9. MFA_FACTORS
-- =========================================================

CREATE TABLE mfa_factors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    secret VARCHAR(1024),
    friendly_name VARCHAR(100),
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT uk_mfa_factors_user_type UNIQUE (user_id, type),
    CONSTRAINT fk_mfa_factors_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_mfa_factors_type_not_empty
        CHECK (TRIM(type) <> ''),
    CONSTRAINT ck_mfa_factors_version
        CHECK (version >= 0)
);

CREATE INDEX idx_mfa_factors_user_id
    ON mfa_factors(user_id);

CREATE INDEX idx_mfa_factors_enabled
    ON mfa_factors(user_id)
    WHERE is_enabled = TRUE
      AND deleted_at IS NULL;


-- =========================================================
-- UPDATED_AT TRIGGER FUNCTION
-- =========================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


-- =========================================================
-- TRIGGERS DE UPDATED_AT PARA ENTIDADES MUTÁVEIS
-- =========================================================

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_user_credentials_updated_at
BEFORE UPDATE ON user_credentials
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_sessions_updated_at
BEFORE UPDATE ON sessions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_refresh_tokens_updated_at
BEFORE UPDATE ON refresh_tokens
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_roles_updated_at
BEFORE UPDATE ON roles
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_permissions_updated_at
BEFORE UPDATE ON permissions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_user_roles_updated_at
BEFORE UPDATE ON user_roles
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_role_permissions_updated_at
BEFORE UPDATE ON role_permissions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_mfa_factors_updated_at
BEFORE UPDATE ON mfa_factors
FOR EACH ROW EXECUTE FUNCTION set_updated_at();