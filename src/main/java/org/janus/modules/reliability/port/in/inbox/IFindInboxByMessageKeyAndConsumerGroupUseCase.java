package org.janus.modules.reliability.port.in.inbox;

import org.janus.modules.reliability.domain.entity.InboxEntity;

import java.util.Optional;

public interface IFindInboxByMessageKeyAndConsumerGroupUseCase {
    Optional<InboxEntity> execute(String messageKey, String consumerGroup);
}