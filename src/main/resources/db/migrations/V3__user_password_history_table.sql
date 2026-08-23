-- =========================================================
-- 2.1. USER_PASSWORD_HISTORY
-- =========================================================

CREATE TABLE user_password_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    algorithm VARCHAR(50) NOT NULL DEFAULT 'argon2id',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_password_history_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_user_password_history_hash_not_empty
        CHECK (TRIM(password_hash) <> ''),
    CONSTRAINT ck_user_password_history_algorithm_not_empty
        CHECK (TRIM(algorithm) <> '')
);

CREATE INDEX idx_user_password_history_user_created
    ON user_password_history(user_id, created_at DESC);