CREATE TABLE login_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID,
    email_attempted VARCHAR(320),
    ip_address VARCHAR(45),
    user_agent TEXT,

    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(100),

    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_login_attempts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT ck_login_attempts_identifier_required
        CHECK (
            user_id IS NOT NULL
            OR email_attempted IS NOT NULL
        ),

    CONSTRAINT ck_login_attempts_email_not_empty
        CHECK (
            email_attempted IS NULL
            OR TRIM(email_attempted) <> ''
        ),

    CONSTRAINT ck_login_attempts_email_lowercase
        CHECK (
            email_attempted IS NULL
            OR email_attempted = LOWER(email_attempted)
        ),

    CONSTRAINT ck_login_attempts_failure_reason
        CHECK (
            (success = TRUE AND failure_reason IS NULL)
            OR (success = FALSE AND failure_reason IS NOT NULL AND TRIM(failure_reason) <> '')
        ),

    CONSTRAINT ck_login_attempts_ip_not_empty
        CHECK (
            ip_address IS NULL
            OR TRIM(ip_address) <> ''
        ),

    CONSTRAINT ck_login_attempts_user_agent_not_empty
        CHECK (
            user_agent IS NULL
            OR TRIM(user_agent) <> ''
        )
);

CREATE INDEX idx_login_attempts_user_id
    ON login_attempts(user_id);

CREATE INDEX idx_login_attempts_email_attempted
    ON login_attempts(email_attempted);

CREATE INDEX idx_login_attempts_ip_address
    ON login_attempts(ip_address);

CREATE INDEX idx_login_attempts_created_at
    ON login_attempts(created_at DESC);

CREATE INDEX idx_login_attempts_failed
    ON login_attempts(created_at DESC)
    WHERE success = FALSE;