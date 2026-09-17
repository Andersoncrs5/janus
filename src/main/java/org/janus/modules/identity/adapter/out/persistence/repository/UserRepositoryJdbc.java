package org.janus.modules.identity.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.janus.modules.identity.application.user.dto.filter.UserFilterDTO;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.shared.domain.page.Page;
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
    public Page<UserEntity> findAll(UserFilterDTO filter) {
        Query.Select select = new Query.Select(getTableName());

        if (filter.getId() != null) {
            select.andEqual("id", filter.getId());
        }

        if (filter.getEmail() != null) {
            select.andEqual("email", filter.getEmail());
        }

        if (filter.getUsername() != null) {
            select.andEqual("username", filter.getUsername());
        }

        if (filter.getFullName() != null) {
            select.andEqual("full_name", filter.getFullName());
        }

        if (filter.getEmailVerified() != null) {
            select.andEqual("is_email_verified", filter.getEmailVerified());
        }

        if (filter.getActive() != null) {
            select.andEqual("is_active", filter.getActive());
        }

        if (filter.getLastLoginAtFrom() != null) {
            select.andGreaterThanOrEqual("last_login_at", filter.getLastLoginAtFrom());
        }

        if (filter.getLastLoginAtTo() != null) {
            select.andLessThanOrEqual("last_login_at", filter.getLastLoginAtTo());
        }

        if (filter.getCreatedAtMin() != null) {
            select.andGreaterThanOrEqual("created_at", filter.getCreatedAtMin());
        }

        if (filter.getCreatedAtMax() != null) {
            select.andLessThanOrEqual("created_at", filter.getCreatedAtMax());
        }

        if (filter.getUpdatedAtMin() != null) {
            select.andGreaterThanOrEqual("updated_at", filter.getUpdatedAtMin());
        }

        if (filter.getUpdatedAtMax() != null) {
            select.andLessThanOrEqual("updated_at", filter.getUpdatedAtMax());
        }

        select.andSoftDelete();

        long totalElements = select.count(dataSource);

        if (totalElements == 0) {
            return Page.empty(filter.getPage(), filter.getSize());
        }

        if (filter.getOrders() != null && !filter.getOrders().isEmpty()) {
            filter.getOrders().forEach(order -> select.orderBy(order.getColumn(), order.getDirection()));
        }

        select.limit(filter.getSize());
        select.offset(filter.getOffset());

        List<UserEntity> content = select.findAll(dataSource, this::mapRow);

        return Page.of(content, totalElements, filter.getPage(), filter.getSize());
    }

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