package org.janus.modules.reliability.application.dto.inbox.request;

import org.janus.shared.domain.enums.InboxStatusEnum;

public record UpdateInboxDTO(
        InboxStatusEnum status,
        Integer responseCode
) {
}