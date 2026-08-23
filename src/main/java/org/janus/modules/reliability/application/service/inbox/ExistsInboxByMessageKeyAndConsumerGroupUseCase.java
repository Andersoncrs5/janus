package org.janus.modules.reliability.application.service.inbox;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.reliability.port.in.inbox.IExistsInboxByMessageKeyAndConsumerGroupUseCase;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;

@ApplicationScoped
@RequiredArgsConstructor
public class ExistsInboxByMessageKeyAndConsumerGroupUseCase implements IExistsInboxByMessageKeyAndConsumerGroupUseCase {

    private final InboxRepository repository;

    @Override
    public boolean execute(String messageKey, String consumerGroup) {
        if (messageKey == null || messageKey.isBlank() || consumerGroup == null || consumerGroup.isBlank()) {
            return false;
        }

        try {
            return repository.existsByMessageKeyAndConsumerGroup(messageKey, consumerGroup);
        } catch (Exception e) {
            throw new InternalServerErrorException(
                    "Error checking if inbox record exists by messageKey and consumerGroup", e
            );
        }
    }
}