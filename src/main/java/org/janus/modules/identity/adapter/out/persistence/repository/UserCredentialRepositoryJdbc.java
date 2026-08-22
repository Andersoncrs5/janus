package org.janus.modules.identity.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.ports.out.UserCredentialRepository;
import org.janus.shared.infrastructure.persistence.jdbc.GenericJdbcRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

        String sql = """
                INSERT INTO %s (
                    id, user_id, password_hash, algorithm, version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """.formatted(getTableName());

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)
        ) {
            int i = 1;

            statement.setObject(i++, entity.getId());
            statement.setObject(i++, entity.getUserId());
            statement.setString(i++, entity.getPasswordHash());
            statement.setString(i++, entity.getAlgorithm() != null ? entity.getAlgorithm() : "argon2id");

            statement.executeUpdate();

            entity.setVersion(0L);
            return entity;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error inserting UserCredentials entity with ID: " + entity.getId(),
                    e
            );
        }
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
        String sql = """
                SELECT *
                FROM %s
                WHERE user_id = ?
                  AND deleted_at IS NULL
                LIMIT 1
                """.formatted(getTableName());

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)
        ) {
            statement.setObject(1, userId);

            try (var rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error finding UserCredentials by user_id: " + userId, e);
        }
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM %s
                    WHERE user_id = ?
                      AND deleted_at IS NULL
                )
                """.formatted(getTableName());

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)
        ) {
            statement.setObject(1, userId);

            try (var rs = statement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error checking UserCredentials existence by user_id: " + userId, e);
        }
    }

    @Override
    public boolean deleteByUserId(UUID userId) {
        String sql = """
                UPDATE %s
                SET
                    deleted_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE user_id = ?
                  AND deleted_at IS NULL
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setObject(1, userId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error deleting entity by UUID: " + userId,
                    e
            );
        }
    }

    public boolean updateOptimistic(UserCredentialsEntity entity) {
        String sql = """
                UPDATE %s
                SET
                    user_id = ?,
                    password_hash = ?,
                    algorithm = ?,
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

            statement.setObject(i++, entity.getUserId());
            statement.setString(i++, entity.getPasswordHash());
            statement.setString(i++, entity.getAlgorithm());

            statement.setObject(i++, entity.getId());
            statement.setLong(i, entity.getVersion() != null ? entity.getVersion() : 0L);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Error updating UserCredentials with ID: " + entity.getId(), e);
        }
    }
}