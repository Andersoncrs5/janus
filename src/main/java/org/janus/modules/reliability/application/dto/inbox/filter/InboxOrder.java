package org.janus.modules.reliability.application.dto.inbox.filter;

import lombok.Getter;

@Getter
public enum InboxOrder {
    ID("id"),
    MESSAGE_KEY("message_key"),
    CONSUMER_GROUP("consumer_group"),
    STATUS("status"),
    CREATED_AT("created_at"),
    UPDATED_AT("updated_at");

    private final String field;

    InboxOrder(String field) {
        this.field = field;
    }
}