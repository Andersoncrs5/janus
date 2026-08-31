package org.janus.modules.authentication.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.janus.modules.authentication.domain.entity.RefreshTokenEntity;
import org.janus.modules.authentication.port.out.RefreshTokenRepository;
import org.janus.shared.domain.queries.Query;
import org.janus.shared.infrastructure.persistence.jdbc.GenericJdbcRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RefreshTokenRepositoryJdbc
        extends GenericJdbcRepository<RefreshTokenEntity>
        implements RefreshTokenRepository {

    @Override
    protected String getTableName() {
        return RefreshTokenEntity.TABLE_NAME;
    }

    @Override
    protected RefreshTokenEntity mapRow(ResultSet rs) throws SQLException {
        RefreshTokenEntity entity = new RefreshTokenEntity();

        mapBaseFields(entity, rs);

        entity.setSessionId(rs.getObject("session_id", UUID.class));
        entity.setUserId(rs.getObject("user_id", UUID.class));
        entity.setTokenHash(rs.getString("token_hash"));
        entity.setIsUsed(rs.getBoolean("is_used"));
        entity.setReplacedByTokenId(rs.getObject("replaced_by_token_id", UUID.class));
        entity.setIsRevoked(rs.getBoolean("is_revoked"));
        entity.setExpiresAt(rs.getObject("expires_at", OffsetDateTime.class));

        return entity;
    }

    @Override
    public RefreshTokenEntity insert(RefreshTokenEntity entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        return new Query.Insert(getTableName())
                .value("id", entity.getId())
                .value("session_id", entity.getSessionId())
                .value("user_id", entity.getUserId())
                .value("token_hash", entity.getTokenHash())
                .value("is_used", entity.getIsUsed() != null ? entity.getIsUsed() : Boolean.FALSE)
                .value("replaced_by_token_id", entity.getReplacedByTokenId())
                .value("is_revoked", entity.getIsRevoked() != null ? entity.getIsRevoked() : Boolean.FALSE)
                .value("expires_at", entity.getExpiresAt())
                .value("version", 0L)
                .value("created_at", OffsetDateTime.now())
                .value("updated_at", OffsetDateTime.now())
                .executeAndMap(dataSource, List.of("version", "created_at", "updated_at"), rs -> {
                    entity.setVersion(rs.getLong("version"));
                    entity.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    entity.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                    return entity;
                });
    }

    @Override
    public RefreshTokenEntity save(RefreshTokenEntity entity) {
        if (entity.getId() == null || !existsById(entity.getId())) {
            return insert(entity);
        }

        boolean updated = updateOptimistic(entity);
        if (!updated) {
            throw new IllegalStateException(
                    "Failed to update RefreshToken entity (optimistic lock or deleted): " + entity.getId()
            );
        }
        return entity;
    }

    public boolean updateOptimistic(RefreshTokenEntity entity) {
        return new Query.Update(getTableName())
                .set("session_id", entity.getSessionId())
                .set("user_id", entity.getUserId())
                .set("token_hash", entity.getTokenHash())
                .set("is_used", entity.getIsUsed() != null ? entity.getIsUsed() : Boolean.FALSE)
                .set("replaced_by_token_id", entity.getReplacedByTokenId())
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
    public Optional<RefreshTokenEntity> findByTokenHash(String tokenHash) {
        return new Query.Select(getTableName())
                .where("token_hash", tokenHash)
                .andSoftDelete()
                .findFirst(dataSource, this::mapRow);
    }

    @Override
    public int revokeById(UUID id) {
        return new Query.Update(getTableName())
                .set("is_revoked", true)
                .setExpression("updated_at = CURRENT_TIMESTAMP")
                .where("id", id)
                .and("is_revoked", false)
                .andSoftDelete()
                .executeCount(dataSource);
    }

    @Override
    public int revokeAllBySessionId(UUID sessionId) {
        return new Query.Update(getTableName())
                .set("is_revoked", true)
                .setExpression("updated_at = CURRENT_TIMESTAMP")
                .where("session_id", sessionId)
                .and("is_revoked", false)
                .andSoftDelete()
                .executeCount(dataSource);
    }

    @Override
    public int revokeAllByUserId(UUID userId) {
        return new Query.Update(getTableName())
                .set("is_revoked", true)
                .setExpression("updated_at = CURRENT_TIMESTAMP")
                .where("user_id", userId)
                .and("is_revoked", false)
                .andSoftDelete()
                .executeCount(dataSource);
    }

    @Override
    public int revokeFamilyBySessionId(UUID sessionId) {
        return revokeAllBySessionId(sessionId);
    }

    @Override
    public boolean existsActiveBySessionId(UUID sessionId) {
        return new Query.Exists(getTableName())
                .where("session_id", sessionId)
                .and("is_revoked", false)
                .and("is_used", false)
                .and("expires_at > CURRENT_TIMESTAMP")
                .andSoftDelete()
                .execute(dataSource);
    }

    @Override
    public Optional<RefreshTokenEntity> findLatestBySessionId(UUID sessionId) {
        return new Query.Select(getTableName())
                .where("session_id", sessionId)
                .andSoftDelete()
                .orderByDesc("created_at")
                .limit(1)
                .findFirst(dataSource, this::mapRow);
    }
}
