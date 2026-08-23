package org.janus.modules.reliability.application.service.inbox;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.modules.reliability.port.in.inbox.IFindInboxByMessageKeyAndConsumerGroupUseCase;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;

import java.util.Optional;

@ApplicationScoped
@RequiredArgsConstructor
public class FindInboxByMessageKeyAndConsumerGroupUseCase implements IFindInboxByMessageKeyAndConsumerGroupUseCase {

    private final InboxRepository repository;

    @Override
    public Optional<InboxEntity> execute(String messageKey, String consumerGroup) {
        if (messageKey == null || messageKey.isBlank() || consumerGroup == null || consumerGroup.isBlank()) {
            return Optional.empty();
        }

        try {
            return repository.findByMessageKeyAndConsumerGroup(messageKey, consumerGroup);
        } catch (Exception e) {
            throw new InternalServerErrorException(
                    "Error finding inbox record by messageKey and consumerGroup", e
            );
        }
    }
}