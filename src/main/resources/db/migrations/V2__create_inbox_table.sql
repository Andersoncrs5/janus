CREATE TYPE inbox_status_enum AS ENUM ('PROCESSING', 'COMPLETED', 'FAILED');

CREATE TABLE inbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_key VARCHAR(255) NOT NULL,
    consumer_group VARCHAR(100) NOT NULL,
    status inbox_status_enum NOT NULL DEFAULT 'PROCESSING',
    response_payload TEXT,
    response_code INT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT uk_inbox_message_group UNIQUE (message_key, consumer_group)
);

CREATE INDEX idx_inbox_status ON inbox(status);
CREATE INDEX idx_inbox_created_at ON inbox(created_at);