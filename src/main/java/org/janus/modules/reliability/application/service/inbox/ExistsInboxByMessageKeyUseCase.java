package org.janus.modules.reliability.application.service.inbox;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.reliability.port.in.inbox.IExistsInboxByMessageKeyUseCase;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;

@ApplicationScoped
@RequiredArgsConstructor
public class ExistsInboxByMessageKeyUseCase implements IExistsInboxByMessageKeyUseCase {

    private final InboxRepository repository;

    @Override
    public Result<Boolean> execute(String messageKey) {
        if (messageKey == null || messageKey.isBlank()) {
            return Result.failure("Message key cannot be null or empty", 400);
        }

        try {
            boolean exists = repository.existsByMessageKey(messageKey);
            return Result.success(exists);
        } catch (Exception e) {
            throw new InternalServerErrorException(
                    "Error checking if inbox record exists by messageKey: '" + messageKey + "'", e
            );
        }
    }
}