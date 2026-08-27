package org.janus.modules.identity.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.ports.out.UserCredentialRepository;
import org.janus.shared.domain.queries.Query;
import org.janus.shared.infrastructure.persistence.jdbc.GenericJdbcRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserCredentialRepositoryJdbc
        extends GenericJdbcRepository<UserCredentialsEntity>
        implements UserCredentialRepository {

    @Override
    protected String getTableName() {
        return UserCredentialsEntity.TABLE_NAME;
    }

    @Override
    protected UserCredentialsEntity mapRow(ResultSet rs) throws SQLException {
        UserCredentialsEntity credentials = new UserCredentialsEntity();

        mapBaseFields(credentials, rs);

        credentials.setUserId(rs.getObject("user_id", UUID.class));
        credentials.setPasswordHash(rs.getString("password_hash"));
        credentials.setAlgorithm(rs.getString("algorithm"));

        return credentials;
    }

    @Override
    public UserCredentialsEntity insert(UserCredentialsEntity entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        String algorithmValue = entity.getAlgorithm() != null ? entity.getAlgorithm() : "argon2id";

        return new Query.Insert(getTableName())
                .value("id", entity.getId())
                .value("user_id", entity.getUserId())
                .value("password_hash", entity.getPasswordHash())
                .value("algorithm", algorithmValue)
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
    public UserCredentialsEntity save(UserCredentialsEntity entity) {
        if (entity.getId() == null || !existsById(entity.getId())) {
            return insert(entity);
        }

        boolean updated = updateOptimistic(entity);
        if (!updated) {
            throw new IllegalStateException("Failed to update UserCredentials entity (optimistic lock or deleted): " + entity.getId());
        }
        return entity;
    }

    @Override
    public Optional<UserCredentialsEntity> findByUserId(UUID userId) {
        return new Query.Select(getTableName())
                .where("user_id", userId)
                .andSoftDelete()
                .findFirst(dataSource, this::mapRow);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return new Query.Exists(getTableName())
                .where("user_id", userId)
                .andSoftDelete()
                .execute(dataSource);
    }

    @Override
    public boolean deleteByUserId(UUID userId) {
        // Mantém a consistência de soft delete com atualização de versão
        return new Query.Update(getTableName())
                .setExpression("deleted_at = CURRENT_TIMESTAMP")
                .setExpression("version = version + 1")
                .where("user_id", userId)
                .andSoftDelete()
                .execute(dataSource);
    }

    public boolean updateOptimistic(UserCredentialsEntity entity) {
        return new Query.Update(getTableName())
                .set("user_id", entity.getUserId())
                .set("password_hash", entity.getPasswordHash())
                .set("algorithm", entity.getAlgorithm())
                .setExpression("version = version + 1")
                .setExpression("updated_at = CURRENT_TIMESTAMP")
                .where("id", entity.getId())
                .and("version = ?", entity.getVersion() != null ? entity.getVersion() : 0L)
                .andSoftDelete()
                .execute(dataSource);
    }
}