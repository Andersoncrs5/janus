package org.janus.modules.identity.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.shared.infrastructure.persistence.jdbc.GenericJdbcRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
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

        String sql = """
                INSERT INTO %s (
                    id, email, username, full_name, is_active,
                    is_email_verified, failed_login_attempts, locked_until,
                    last_login_at, version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """.formatted(getTableName());

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)
        ) {
            int i = 1;

            statement.setObject(i++, entity.getId());
            statement.setString(i++, entity.getEmail());
            statement.setString(i++, entity.getUsername());
            statement.setString(i++, entity.getFullName());
            statement.setBoolean(i++, Boolean.TRUE.equals(entity.getIsActive()));
            statement.setBoolean(i++, Boolean.TRUE.equals(entity.getIsEmailVerified()));
            statement.setInt(i++, entity.getFailedLoginAttempts() != null ? entity.getFailedLoginAttempts() : 0);
            statement.setObject(i++, entity.getLockedUntil());
            statement.setObject(i++, entity.getLastLoginAt());

            statement.executeUpdate();

            entity.setVersion(0L);
            return entity;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error inserting User entity with ID: " + entity.getId(),
                    e
            );
        }
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

    public Optional<UserEntity> findByEmail(String email) {
        String sql = """
                SELECT *
                FROM %s
                WHERE LOWER(email) = LOWER(?)
                  AND deleted_at IS NULL
                LIMIT 1
                """.formatted(getTableName());

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, email);

            try (var rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error finding User by email: " + email, e);
        }
    }

    public Optional<UserEntity> findByUsername(String username) {
        String sql = """
                SELECT *
                FROM %s
                WHERE LOWER(username) = LOWER(?)
                  AND deleted_at IS NULL
                LIMIT 1
                """.formatted(getTableName());

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, username);

            try (var rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error finding User by username: " + username, e);
        }
    }

    public boolean existsByEmail(String email) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM %s
                    WHERE LOWER(email) = LOWER(?)
                      AND deleted_at IS NULL
                )
                """.formatted(getTableName());

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, email);

            try (var rs = statement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error checking User email existence: " + email, e);
        }
    }

    public boolean existsByUsername(String username) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM %s
                    WHERE LOWER(username) = LOWER(?)
                      AND deleted_at IS NULL
                )
                """.formatted(getTableName());

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, username);

            try (var rs = statement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error checking User username existence: " + username, e);
        }
    }

    public boolean updateOptimistic(UserEntity entity) {
        String sql = """
                UPDATE %s
                SET
                    email = ?,
                    username = ?,
                    full_name = ?,
                    is_active = ?,
                    is_email_verified = ?,
                    failed_login_attempts = ?,
                    locked_until = ?,
                    last_login_at = ?,
                    version = version + 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND version = ?
                  AND deleted_at IS NULL
                """.formatted(getTableName());

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)
        ) {
            int i = 1;

            statement.setString(i++, entity.getEmail());
            statement.setString(i++, entity.getUsername());
            statement.setString(i++, entity.getFullName());
            statement.setBoolean(i++, Boolean.TRUE.equals(entity.getIsActive()));
            statement.setBoolean(i++, Boolean.TRUE.equals(entity.getIsEmailVerified()));
            statement.setInt(i++, entity.getFailedLoginAttempts() != null ? entity.getFailedLoginAttempts() : 0);
            statement.setObject(i++, entity.getLockedUntil());
            statement.setObject(i++, entity.getLastLoginAt());

            statement.setObject(i++, entity.getId());
            statement.setLong(i, entity.getVersion() != null ? entity.getVersion() : 0L);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Error updating User with ID: " + entity.getId(), e);
        }
    }
}