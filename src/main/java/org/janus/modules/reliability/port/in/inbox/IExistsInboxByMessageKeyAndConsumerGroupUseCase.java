package org.janus.modules.reliability.port.in.inbox;

import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

public interface IExistsInboxByMessageKeyAndConsumerGroupUseCase {
    boolean execute(String messageKey, String consumerGroup);
}