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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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

    public InboxEntity insert(InboxEntity entity) {

        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        String sql = """
            INSERT INTO %s (
                id,
                message_key,
                consumer_group,
                status,
                response_payload,
                response_code,
                version,
                created_at,
                updated_at
            )
            VALUES (
                ?,
                ?,
                ?,
                ?::inbox_status_enum,
                ?,
                ?,
                0,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            RETURNING
                id,
                version,
                created_at,
                updated_at
            """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            int i = 1;

            statement.setObject(
                    i++,
                    entity.getId()
            );

            statement.setString(
                    i++,
                    entity.getMessageKey()
            );

            statement.setString(
                    i++,
                    entity.getConsumerGroup()
            );

            statement.setString(
                    i++,
                    entity.getStatus() != null
                            ? entity.getStatus().name()
                            : InboxStatusEnum.PROCESSING.name()
            );

            statement.setString(
                    i++,
                    entity.getResponsePayload()
            );

            if (entity.getResponseCode() != null) {
                statement.setInt(
                        i++,
                        entity.getResponseCode()
                );
            } else {
                statement.setNull(
                        i++,
                        Types.INTEGER
                );
            }

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    throw new IllegalStateException(
                            "Insert Inbox did not return generated values: "
                                    + entity.getId()
                    );
                }

                entity.setVersion(
                        rs.getLong("version")
                );

                entity.setCreatedAt(
                        rs.getObject(
                                "created_at",
                                OffsetDateTime.class
                        )
                );

                entity.setUpdatedAt(
                        rs.getObject(
                                "updated_at",
                                OffsetDateTime.class
                        )
                );

                return entity;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error inserting Inbox entity with ID: "
                            + entity.getId(),
                    e
            );
        }
    }

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
        String sql = """
                UPDATE %s
                SET
                    message_key = ?,
                    consumer_group = ?,
                    status = ?::inbox_status_enum,
                    response_payload = ?,
                    response_code = ?,
                    version = version + 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND version = ?
                  AND deleted_at IS NULL
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            int i = 1;

            statement.setString(i++, entity.getMessageKey());
            statement.setString(i++, entity.getConsumerGroup());
            statement.setString(i++, entity.getStatus() != null ? entity.getStatus().name() : InboxStatusEnum.PROCESSING.name());
            statement.setString(i++, entity.getResponsePayload());

            if (entity.getResponseCode() != null) {
                statement.setInt(i++, entity.getResponseCode());
            } else {
                statement.setNull(i++, Types.INTEGER);
            }

            statement.setObject(i++, entity.getId());
            statement.setLong(i, entity.getVersion() != null ? entity.getVersion() : 0L);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Error updating Inbox entity with ID: " + entity.getId(), e);
        }
    }

    @Override
    public Optional<InboxEntity> findByMessageKeyAndConsumerGroup(String messageKey, String consumerGroup) {
        String sql = """
                SELECT *
                FROM %s
                WHERE message_key = ?
                  AND consumer_group = ?
                  AND deleted_at IS NULL
                LIMIT 1
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, messageKey);
            statement.setString(2, consumerGroup);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error finding Inbox entity by message_key and consumer_group", e);
        }
    }

    @Override
    public boolean existsByMessageKeyAndConsumerGroup(String messageKey, String consumerGroup) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM %s
                    WHERE message_key = ?
                      AND consumer_group = ?
                      AND deleted_at IS NULL
                )
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, messageKey);
            statement.setString(2, consumerGroup);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error checking Inbox existence by message_key and consumer_group", e);
        }
    }

    @Override
    public boolean existsByMessageKey(String messageKey) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM %s
                    WHERE message_key = ?
                      AND deleted_at IS NULL
                )
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, messageKey);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error checking Inbox existence by message_key", e);
        }
    }


}