package org.janus.modules.reliability.application.dto.inbox.request;

import org.janus.shared.domain.enums.InboxStatusEnum;

public record CreateInboxDTO(
        String messageKey,
        String consumerGroup,
        InboxStatusEnum status,
        String responsePayload,
        Integer responseCode
) {
}