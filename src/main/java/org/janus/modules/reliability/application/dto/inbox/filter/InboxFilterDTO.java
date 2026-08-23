package org.janus.modules.reliability.application.dto.inbox.filter;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.filter.FilterBaseDTO;
import org.janus.shared.domain.enums.InboxStatusEnum;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InboxFilterDTO extends FilterBaseDTO {

    private String messageKey;
    private String consumerGroup;
    private List<InboxStatusEnum> status;
    private String responsePayload;
    private Integer responseCodeMin;
    private Integer responseCodeMax;

    private List<InboxOrder> orders = List.of(InboxOrder.CREATED_AT);
}
