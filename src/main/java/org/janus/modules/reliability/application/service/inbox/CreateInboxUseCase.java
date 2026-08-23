package org.janus.modules.reliability.application.service.inbox;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.reliability.application.dto.inbox.request.CreateInboxDTO;
import org.janus.modules.reliability.application.mapper.InboxMapper;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.modules.reliability.port.in.inbox.ICreateInboxUseCase;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

@ApplicationScoped
@RequiredArgsConstructor
public class CreateInboxUseCase implements ICreateInboxUseCase {

    private final InboxRepository repository;
    private final InboxMapper mapper;

    @Override
    @ResultTransaction
    public Result<InboxEntity> execute(CreateInboxDTO dto) {
        InboxEntity inbox = mapper.toEntity(dto);

        try {
            InboxEntity inserted = repository.insert(inbox);

            return Result.success(inserted);
        } catch (DataIntegrityViolationException e) {
            String message = e.getMessage();

            if (message == null) {
                return DatabaseConstraintHandler.handle(e);
            }

            if (message.toLowerCase().contains("uk_inbox_message_group")) {
                return Result.failure(
                        "Inbox already exists with message key: '" + dto.messageKey()
                                +"' and group: '" + dto.consumerGroup() + "'" ,
                        409
                );
            }

            return DatabaseConstraintHandler.handle(e);
        }  catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }

    }

}
