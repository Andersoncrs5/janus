package org.janus.modules.reliability.adapter.in.idempotency;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.janus.modules.reliability.application.dto.inbox.request.CreateInboxDTO;
import org.janus.modules.reliability.port.in.inbox.ICreateInboxUseCase;
import org.janus.modules.reliability.port.in.inbox.IExistsInboxByMessageKeyAndConsumerGroupUseCase;
import org.janus.shared.domain.enums.InboxStatusEnum;
import org.janus.shared.domain.idempotency.IdempotencyService;

import java.util.UUID;

@ApplicationScoped
public class InboxIdempotencyServiceAdapter implements IdempotencyService {

    @Inject
    ICreateInboxUseCase createInboxUseCase;

    @Inject
    IExistsInboxByMessageKeyAndConsumerGroupUseCase existsInboxUseCase;

    @Override
    public boolean isProcessed(UUID messageId, String consumerSource) {
        return existsInboxUseCase.execute(messageId.toString(), consumerSource);
    }

    @Override
    public void registerRequest(UUID messageId, String consumerSource, String payload) {
        CreateInboxDTO dto = new CreateInboxDTO(
                messageId.toString(),
                consumerSource,
                InboxStatusEnum.PROCESSING,
                payload,
                200
        );
        createInboxUseCase.execute(dto);
    }
}
