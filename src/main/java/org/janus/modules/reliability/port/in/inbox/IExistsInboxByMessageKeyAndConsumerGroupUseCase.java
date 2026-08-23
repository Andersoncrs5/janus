package org.janus.modules.reliability.port.in.inbox;

public interface IExistsInboxByMessageKeyAndConsumerGroupUseCase {
    boolean execute(String messageKey, String consumerGroup);
}