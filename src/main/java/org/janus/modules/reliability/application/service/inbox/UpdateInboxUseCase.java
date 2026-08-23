package org.janus.modules.reliability.application.service.inbox;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.reliability.application.dto.inbox.request.UpdateInboxDTO;
import org.janus.modules.reliability.application.mapper.InboxMapper;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.modules.reliability.port.in.inbox.IUpdateInboxUseCase;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class UpdateInboxUseCase implements IUpdateInboxUseCase {

    private final InboxRepository repository;
    private final InboxMapper mapper;

    @Override
    @ResultTransaction
    public Result<InboxEntity> execute(UUID id, UpdateInboxDTO dto) {
        InboxEntity inbox = this.repository.findById(id).orElse(null);

        if (inbox == null) return Result.notFound("Inbox not found");

        mapper.updateEntityFromDto(dto, inbox);

        try {
            InboxEntity inserted = repository.save(inbox);

            return Result.success(inserted);
        } catch (DataIntegrityViolationException e) {
            String message = e.getMessage();

            if (message == null) {
                return DatabaseConstraintHandler.handle(e);
            }

            return DatabaseConstraintHandler.handle(e);
        }  catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }

    }

}
