-- =========================================================
-- ENUMS PARA AUDIT LOGS
-- =========================================================

CREATE TYPE audit_status AS ENUM (
    'SUCCESS',
    'FAILURE',
    'ERROR'
);

CREATE TYPE audit_severity AS ENUM (
    'INFO',
    'WARN',
    'ERROR',
    'CRITICAL'
);


-- =========================================================
-- 10. AUDIT_LOGS
-- =========================================================

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    event VARCHAR(150) NOT NULL,
    status audit_status NOT NULL DEFAULT 'SUCCESS',
    severity audit_severity NOT NULL DEFAULT 'INFO',
    resource_type VARCHAR(100),
    resource_id VARCHAR(150),
    failure_reason VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_logs_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT ck_audit_logs_event_not_empty
        CHECK (TRIM(event) <> ''),
    CONSTRAINT ck_audit_logs_resource_type_not_empty
        CHECK (resource_type IS NULL OR TRIM(resource_type) <> '')
);

CREATE INDEX idx_audit_logs_user_id
    ON audit_logs(user_id);

CREATE INDEX idx_audit_logs_event
    ON audit_logs(event);

CREATE INDEX idx_audit_logs_status
    ON audit_logs(status);

CREATE INDEX idx_audit_logs_severity
    ON audit_logs(severity)
    WHERE severity IN ('WARN', 'ERROR', 'CRITICAL');

CREATE INDEX idx_audit_logs_resource
    ON audit_logs(resource_type, resource_id)
    WHERE resource_type IS NOT NULL;

CREATE INDEX idx_audit_logs_created_at
    ON audit_logs(created_at DESC);

ALTER TABLE audit_logs
    ADD COLUMN trace_id VARCHAR(100),
    ADD COLUMN span_id VARCHAR(100),
    ADD COLUMN actor_type VARCHAR(30) NOT NULL DEFAULT 'USER';