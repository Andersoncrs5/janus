package org.janus.modules.reliability.application.service.inbox;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.reliability.port.in.inbox.IDeleteInboxByIdUseCase;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class DeleteInboxByIdUseCase implements IDeleteInboxByIdUseCase {

    private final InboxRepository repository;

    @Override
    public Result<Void> execute(UUID id) {
        if (id == null) {
            return Result.failure("ID cannot be null", 400);
        }

        try {
            int rowsDeleted = repository.deleteById(id);
            if (rowsDeleted == 0) {
                return Result.notFound("Inbox record not found for deletion with ID: " + id);
            }
            return Result.success();
        } catch (Exception e) {
            throw new InternalServerErrorException("Error deleting inbox record by ID: " + id, e);
        }
    }
}