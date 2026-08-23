package org.janus.modules.reliability.port.out;

import org.janus.modules.reliability.application.dto.inbox.filter.InboxFilterDTO;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.shared.domain.base.repository.GenericRepository;
import org.janus.shared.domain.page.Page;

import java.util.Optional;
import java.util.UUID;

public interface InboxRepository extends GenericRepository<InboxEntity, UUID> {
    Optional<InboxEntity> findByMessageKeyAndConsumerGroup(String messageKey, String consumerGroup);
    boolean existsByMessageKeyAndConsumerGroup(String messageKey, String consumerGroup);
    boolean existsByMessageKey(String messageKey);
    Page<InboxEntity> findAll(InboxFilterDTO filter);
}