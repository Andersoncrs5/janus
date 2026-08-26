package org.janus.modules.reliability.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.janus.modules.reliability.application.dto.inbox.filter.InboxFilterDTO;
import org.janus.modules.reliability.application.dto.inbox.filter.InboxOrder;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.enums.InboxStatusEnum;
import org.janus.shared.domain.page.Page;
import org.janus.shared.domain.queries.Query;
import org.janus.shared.infrastructure.persistence.jdbc.GenericJdbcRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class InboxRepositoryJdbc
        extends GenericJdbcRepository<InboxEntity>
        implements InboxRepository {

    @Override
    protected String getTableName() {
        return InboxEntity.TABLE_NAME;
    }

    @Override
    protected InboxEntity mapRow(ResultSet rs) throws SQLException {
        InboxEntity entity = new InboxEntity();

        mapBaseFields(entity, rs);

        entity.setMessageKey(rs.getString("message_key"));
        entity.setConsumerGroup(rs.getString("consumer_group"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            entity.setStatus(InboxStatusEnum.valueOf(statusStr));
        }

        entity.setResponsePayload(rs.getString("response_payload"));
        entity.setResponseCode(rs.getObject("response_code") != null ? rs.getInt("response_code") : null);

        return entity;
    }

    @Override
    public InboxEntity insert(InboxEntity entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        String statusValue = entity.getStatus() != null
                ? entity.getStatus().name()
                : InboxStatusEnum.PROCESSING.name();

        return new Query.Insert(getTableName())
                .value("id", entity.getId())
                .value("message_key", entity.getMessageKey())
                .value("consumer_group", entity.getConsumerGroup())
                .value("status", statusValue, "::inbox_status_enum")
                .value("response_payload", entity.getResponsePayload())
                .value("response_code", entity.getResponseCode())
                .value("version", 0L)
                .value("created_at", OffsetDateTime.now())
                .value("updated_at", OffsetDateTime.now())
                .executeAndMap(dataSource, List.of("id", "version", "created_at", "updated_at"), rs -> {
                    entity.setVersion(rs.getLong("version"));
                    entity.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    entity.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                    return entity;
                });
    }

    @Override
    public InboxEntity save(InboxEntity entity) {
        if (entity.getId() == null || !existsById(entity.getId())) {
            return insert(entity);
        }

        boolean updated = updateOptimistic(entity);
        if (!updated) {
            throw new IllegalStateException("Failed to update Inbox entity (optimistic lock or deleted): " + entity.getId());
        }
        return entity;
    }

    public boolean updateOptimistic(InboxEntity entity) {
        String statusValue = entity.getStatus() != null
                ? entity.getStatus().name()
                : InboxStatusEnum.PROCESSING.name();

        return new Query.Update(getTableName())
                .set("message_key", entity.getMessageKey())
                .set("consumer_group", entity.getConsumerGroup())
                .set("status", statusValue, "::inbox_status_enum")
                .set("response_payload", entity.getResponsePayload())
                .set("response_code", entity.getResponseCode())
                .setExpression("version = version + 1")
                .setExpression("updated_at = CURRENT_TIMESTAMP")
                .where("id", entity.getId())
                .and("version = ?", entity.getVersion() != null ? entity.getVersion() : 0L)
                .andSoftDelete()
                .execute(dataSource);
    }

    @Override
    public Optional<InboxEntity> findByMessageKeyAndConsumerGroup(String messageKey, String consumerGroup) {
        return new Query.Select(getTableName())
                .where("message_key", messageKey)
                .where("consumer_group", consumerGroup)
                .andSoftDelete()
                .findFirst(dataSource, this::mapRow);
    }

    @Override
    public boolean existsByMessageKeyAndConsumerGroup(String messageKey, String consumerGroup) {
        return new Query.Exists(getTableName())
                .where("message_key", messageKey)
                .where("consumer_group", consumerGroup)
                .andSoftDelete()
                .execute(dataSource);
    }

    @Override
    public boolean existsByMessageKey(String messageKey) {
        return new Query.Exists(getTableName())
                .where("message_key", messageKey)
                .andSoftDelete()
                .execute(dataSource);
    }

    @Override
    public Page<InboxEntity> findAll(InboxFilterDTO filter) {
        Query query = buildBaseFilter(filter);

        if (filter.getMessageKey() != null && !filter.getMessageKey().isBlank()) {
            query.and(
                    "message_key ILIKE ?",
                    "%" + filter.getMessageKey() + "%"
            );
        }

        if (filter.getConsumerGroup() != null && !filter.getConsumerGroup().isBlank()) {
            query.and(
                    "consumer_group ILIKE ?",
                    "%" + filter.getConsumerGroup() + "%"
            );
        }

        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            List<String> statusValues = filter.getStatus().stream()
                    .map(InboxStatusEnum::getValue)
                    .toList();

            String placeholders = String.join(
                    ", ",
                    statusValues.stream()
                            .map(value -> "?::inbox_status_enum")
                            .toList()
            );

            query.and(
                    "status IN (" + placeholders + ")",
                    statusValues.toArray()
            );
        }

        if (filter.getResponsePayload() != null && !filter.getResponsePayload().isBlank()) {
            query.and(
                    "response_payload ILIKE ?",
                    "%" + filter.getResponsePayload() + "%"
            );
        }

        if (filter.getResponseCodeMin() != null) {
            query.and(
                    "response_code >= ?",
                    filter.getResponseCodeMin()
            );
        }

        if (filter.getResponseCodeMax() != null) {
            query.and(
                    "response_code <= ?",
                    filter.getResponseCodeMax()
            );
        }

        List<InboxOrder> orders = (filter.getOrders() != null && !filter.getOrders().isEmpty())
                ? filter.getOrders()
                : List.of(InboxOrder.CREATED_AT);

        List<String> orderFields = orders.stream()
                .map(InboxOrder::getField)
                .collect(Collectors.toList());

        return findAll(
                query,
                orderFields,
                filter
        );
    }
}