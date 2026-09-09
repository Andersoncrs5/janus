package org.janus.modules.authentication.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.janus.modules.authentication.application.dto.session.filter.SessionFilterDTO;
import org.janus.modules.authentication.application.dto.session.filter.SessionOrder;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.modules.authentication.port.out.SessionRepository;
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
public class SessionRepositoryJdbc
        extends GenericJdbcRepository<SessionEntity>
        implements SessionRepository {

    @Override
    protected String getTableName() {
        return "sessions";
    }

    @Override
    public Page<SessionEntity> findAll(SessionFilterDTO filter) {
        Query query = buildBaseFilter(filter);

        if (filter.getUserId() != null) {
            query.andEqual("user_id", filter.getUserId());
        }

        if (filter.getIpAddress() != null && !filter.getIpAddress().isBlank()) {
            query.andILike("ip_address", filter.getIpAddress());
        }

        if (filter.getUserAgent() != null && !filter.getUserAgent().isBlank()) {
            query.andILike("user_agent", filter.getUserAgent());
        }

        if (filter.getIsRevoked() != null) {
            query.andEqual("is_revoked", filter.getIsRevoked());
        }

        if (Boolean.FALSE.equals(filter.getIncludeExpired())) {
            query.and("expires_at > CURRENT_TIMESTAMP");
        }

        if (filter.getExpiresAtFrom() != null) {
            query.and("expires_at >= ?", filter.getExpiresAtFrom());
        }

        if (filter.getExpiresAtTo() != null) {
            query.and("expires_at <= ?", filter.getExpiresAtTo());
        }

        List<SessionOrder> orders = (filter.getOrders() != null && !filter.getOrders().isEmpty())
                ? filter.getOrders()
                : List.of(SessionOrder.CREATED_AT);

        List<String> orderFields = orders.stream()
                .map(SessionOrder::getField)
                .collect(Collectors.toList());

        return findAll(
                query,
                orderFields,
                filter
        );
    }

    @Override
    public Optional<SessionEntity> findBySessionIdAndUserId(UUID sessionId, UUID userId) {
        return new Query.Select(getTableName())
                .where("session_id", sessionId)
                .where("user_id", userId)
                .andSoftDelete()
                .orderByDesc("created_at")
                .limit(1)
                .findFirst(dataSource, this::mapRow);
    }

    @Override
    protected SessionEntity mapRow(ResultSet rs) throws SQLException {
        SessionEntity entity = new SessionEntity();

        mapBaseFields(entity, rs);

        entity.setUserId(rs.getObject("user_id", UUID.class));
        entity.setIpAddress(rs.getString("ip_address"));
        entity.setUserAgent(rs.getString("user_agent"));
        entity.setIsRevoked(rs.getBoolean("is_revoked"));
        entity.setExpiresAt(rs.getObject("expires_at", OffsetDateTime.class));

        return entity;
    }

    @Override
    public SessionEntity insert(SessionEntity entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime createdAt = entity.getCreatedAt() != null ? entity.getCreatedAt() : now;

        return new Query.Insert(getTableName())
                .value("id", entity.getId())
                .value("user_id", entity.getUserId())
                .value("ip_address", entity.getIpAddress())
                .value("user_agent", entity.getUserAgent())
                .value("is_revoked", entity.getIsRevoked() != null ? entity.getIsRevoked() : Boolean.FALSE)
                .value("expires_at", entity.getExpiresAt())
                .value("version", 0L)
                .value("created_at", createdAt)
                .value("updated_at", createdAt)
                .executeAndMap(dataSource, List.of("version", "created_at", "updated_at"), rs -> {
                    entity.setVersion(rs.getLong("version"));
                    entity.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    entity.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                    return entity;
                });
    }

    @Override
    public SessionEntity save(SessionEntity entity) {
        if (entity.getId() == null || !existsById(entity.getId())) {
            return insert(entity);
        }

        boolean updated = updateOptimistic(entity);
        if (!updated) {
            throw new IllegalStateException(
                    "Failed to update Session entity (optimistic lock or deleted): " + entity.getId()
            );
        }

        entity.setVersion((entity.getVersion() != null ? entity.getVersion() : 0L) + 1);
        entity.setUpdatedAt(OffsetDateTime.now());

        return entity;
    }

    public boolean updateOptimistic(SessionEntity entity) {
        return new Query.Update(getTableName())
                .set("ip_address", entity.getIpAddress())
                .set("user_agent", entity.getUserAgent())
                .set("is_revoked", entity.getIsRevoked() != null ? entity.getIsRevoked() : Boolean.FALSE)
                .set("expires_at", entity.getExpiresAt())
                .setExpression("version = version + 1")
                .setExpression("updated_at = CURRENT_TIMESTAMP")
                .where("id", entity.getId())
                .and("version = ?", entity.getVersion() != null ? entity.getVersion() : 0L)
                .andSoftDelete()
                .execute(dataSource);
    }

    @Override
    public List<SessionEntity> findActiveByUserId(UUID userId) {
        return new Query.Select(getTableName())
                .where("user_id", userId)
                .andEqual("is_revoked", false)
                .andCustom("expires_at > CURRENT_TIMESTAMP")
                .andSoftDelete()
                .orderByDesc("created_at")
                .findAll(dataSource, this::mapRow);
    }

    @Override
    public int revokeById(UUID id) {
        return new Query.Update(getTableName())
                .set("is_revoked", true)
                .setExpression("updated_at = CURRENT_TIMESTAMP")
                .where("id", id)
                .andEqual("is_revoked", false)
                .andSoftDelete()
                .executeCount(dataSource);
    }

    @Override
    public int revokeAllByUserId(UUID userId) {
        return new Query.Update(getTableName())
                .set("is_revoked", true)
                .setExpression("updated_at = CURRENT_TIMESTAMP")
                .where("user_id", userId)
                .andEqual("is_revoked", false)
                .andSoftDelete()
                .executeCount(dataSource);
    }


}