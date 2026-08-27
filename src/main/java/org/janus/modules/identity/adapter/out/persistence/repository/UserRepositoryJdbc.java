package org.janus.modules.identity.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.shared.domain.queries.Query;
import org.janus.shared.infrastructure.persistence.jdbc.GenericJdbcRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepositoryJdbc
        extends GenericJdbcRepository<UserEntity>
        implements UserRepository {

    @Override
    protected String getTableName() {
        return UserEntity.TABLE_NAME;
    }

    @Override
    protected UserEntity mapRow(ResultSet rs) throws SQLException {
        UserEntity user = new UserEntity();

        mapBaseFields(user, rs);

        user.setEmail(rs.getString("email"));
        user.setUsername(rs.getString("username"));
        user.setFullName(rs.getString("full_name"));
        user.setIsActive(rs.getBoolean("is_active"));
        user.setIsEmailVerified(rs.getBoolean("is_email_verified"));
        user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));

        user.setLockedUntil(
                rs.getObject("locked_until", OffsetDateTime.class)
        );

        user.setLastLoginAt(
                rs.getObject("last_login_at", OffsetDateTime.class)
        );

        return user;
    }

    @Override
    public UserEntity insert(UserEntity entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        return new Query.Insert(getTableName())
                .value("id", entity.getId())
                .value("email", entity.getEmail())
                .value("username", entity.getUsername())
                .value("full_name", entity.getFullName())
                .value("is_active", Boolean.TRUE.equals(entity.getIsActive()))
                .value("is_email_verified", Boolean.TRUE.equals(entity.getIsEmailVerified()))
                .value("failed_login_attempts", entity.getFailedLoginAttempts() != null ? entity.getFailedLoginAttempts() : 0)
                .value("locked_until", entity.getLockedUntil())
                .value("last_login_at", entity.getLastLoginAt())
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
    public UserEntity save(UserEntity entity) {
        if (entity.getId() == null || !existsById(entity.getId())) {
            return insert(entity);
        }

        boolean updated = updateOptimistic(entity);
        if (!updated) {
            throw new IllegalStateException("Failed to update User entity (optimistic lock or deleted): " + entity.getId());
        }
        return entity;
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        return new Query.Select(getTableName())
                .whereIgnoreCase("email", email)
                .andSoftDelete()
                .findFirst(dataSource, this::mapRow);
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return new Query.Select(getTableName())
                .whereIgnoreCase("username", username)
                .andSoftDelete()
                .findFirst(dataSource, this::mapRow);
    }

    @Override
    public boolean existsByEmail(String email) {
        return new Query.Exists(getTableName())
                .whereIgnoreCase("email", email)
                .andSoftDelete()
                .execute(dataSource);
    }

    @Override
    public boolean existsByUsername(String username) {
        return new Query.Exists(getTableName())
                .whereIgnoreCase("username", username)
                .andSoftDelete()
                .execute(dataSource);
    }

    public boolean updateOptimistic(UserEntity entity) {
        return new Query.Update(getTableName())
                .set("email", entity.getEmail())
                .set("username", entity.getUsername())
                .set("full_name", entity.getFullName())
                .set("is_active", Boolean.TRUE.equals(entity.getIsActive()))
                .set("is_email_verified", Boolean.TRUE.equals(entity.getIsEmailVerified()))
                .set("failed_login_attempts", entity.getFailedLoginAttempts() != null ? entity.getFailedLoginAttempts() : 0)
                .set("locked_until", entity.getLockedUntil())
                .set("last_login_at", entity.getLastLoginAt())
                .setExpression("version = version + 1")
                .setExpression("updated_at = CURRENT_TIMESTAMP")
                .where("id", entity.getId())
                .and("version = ?", entity.getVersion() != null ? entity.getVersion() : 0L)
                .andSoftDelete()
                .execute(dataSource);
    }
}