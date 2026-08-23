package org.janus.shared.domain.idempotency;

import java.util.UUID;

public interface IdempotencyService {
    boolean isProcessed(UUID messageId, String consumerSource);
    void registerRequest(UUID messageId, String consumerSource, String payload);
}