package org.janus.modules.reliability.application.dto.inbox.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.dto.BaseDTO;
import org.janus.shared.domain.enums.InboxStatusEnum;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InboxDTO extends BaseDTO {

    private String messageKey;
    private String consumerGroup;
    private InboxStatusEnum status;
    private String responsePayload;
    private Integer responseCode;
}