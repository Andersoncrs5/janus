package org.janus.modules.reliability.application.service.inbox;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.modules.reliability.port.in.inbox.IFindInboxByIdUseCase;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class FindInboxByIdUseCase implements IFindInboxByIdUseCase {

    private final InboxRepository repository;

    @Override
    public Result<InboxEntity> execute(UUID id) {
        if (id == null) {
            return Result.failure("ID cannot be null", 400);
        }

        try {
            return repository.findById(id)
                    .map(Result::success)
                    .orElseGet(() -> Result.notFound("Inbox record not found with ID: " + id));
        } catch (Exception e) {
            throw new InternalServerErrorException("Error searching inbox record by ID: " + id, e);
        }
    }
}