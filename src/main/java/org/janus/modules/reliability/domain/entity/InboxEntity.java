package org.janus.modules.reliability.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.model.BaseEntity;
import org.janus.shared.domain.enums.InboxStatusEnum;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InboxEntity extends BaseEntity {
    public static final String TABLE_NAME = "inbox";

    private String messageKey;
    private String consumerGroup;
    private InboxStatusEnum status;
    private String responsePayload;
    private Integer responseCode;
}