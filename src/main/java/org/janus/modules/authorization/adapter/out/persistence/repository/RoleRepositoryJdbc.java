package org.janus.modules.authorization.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.janus.modules.authorization.application.dto.role.filter.RoleFilterDTO;
import org.janus.modules.authorization.application.dto.role.filter.RoleOrder;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
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
public class RoleRepositoryJdbc
        extends GenericJdbcRepository<RoleEntity>
        implements RoleRepository {

    @Override
    public Page<RoleEntity> findAll(RoleFilterDTO filter) {
        Query query = buildBaseFilter(filter);

        if (filter.getName() != null && !filter.getName().isBlank()) {
            query.andILike("name", filter.getName());
        }

        if (filter.getSlug() != null && !filter.getSlug().isBlank()) {
            query.andILike("slug", filter.getSlug());
        }

        if (filter.getDescription() != null && !filter.getDescription().isBlank()) {
            query.andILike("description", filter.getDescription());
        }

        if (filter.getIsActive() != null) {
            query.andEqual("is_active", filter.getIsActive());
        }

        if (filter.getIsSystem() != null) {
            query.andEqual("is_system", filter.getIsSystem());
        }

        List<RoleOrder> orders = (filter.getOrders() != null && !filter.getOrders().isEmpty())
                ? filter.getOrders()
                : List.of(RoleOrder.NAME);

        List<String> orderFields = orders.stream()
                .map(RoleOrder::getField)
                .collect(Collectors.toList());

        return findAll(
                query,
                orderFields,
                filter
        );
    }

    @Override
    protected String getTableName() {
        return RoleEntity.TABLE_NAME;
    }

    @Override
    protected RoleEntity mapRow(ResultSet rs) throws SQLException {
        RoleEntity entity = new RoleEntity();

        mapBaseFields(entity, rs);

        entity.setName(rs.getString("name"));
        entity.setSlug(rs.getString("slug"));
        entity.setDescription(rs.getString("description"));
        entity.setIsActive(rs.getBoolean("is_active"));
        entity.setIsSystem(rs.getBoolean("is_system"));

        return entity;
    }

    public RoleEntity insert(RoleEntity entity) {

        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        String sql = """
            INSERT INTO %s (
                id,
                slug,
                name,
                description,
                is_active,
                is_system,
                version,
                created_at,
                updated_at
            )
            VALUES (
                ?,
                ?,
                ?,
                ?,
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
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            int i = 1;

            statement.setObject(i++, entity.getId());
            statement.setString(i++, entity.getSlug());
            statement.setString(i++, entity.getName());

            if (entity.getDescription() != null) {
                statement.setString(i++, entity.getDescription());
            } else {
                statement.setNull(i++, Types.VARCHAR);
            }

            statement.setBoolean(
                    i++,
                    entity.getIsActive() != null ? entity.getIsActive() : Boolean.TRUE
            );

            statement.setBoolean(
                    i++,
                    entity.getIsSystem() != null ? entity.getIsSystem() : Boolean.FALSE
            );

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    throw new IllegalStateException(
                            "Insert Role did not return generated values: "
                                    + entity.getId()
                    );
                }

                entity.setVersion(rs.getLong("version"));
                entity.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                entity.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));

                return entity;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error inserting Role entity with ID: " + entity.getId(),
                    e
            );
        }
    }

    public RoleEntity save(RoleEntity entity) {
        if (entity.getId() == null || !existsById(entity.getId())) {
            return insert(entity);
        }

        boolean updated = updateOptimistic(entity);
        if (!updated) {
            throw new IllegalStateException(
                    "Failed to update Role entity (optimistic lock or deleted): " + entity.getId()
            );
        }
        return entity;
    }

    public boolean updateOptimistic(RoleEntity entity) {
        String sql = """
                UPDATE %s
                SET
                    slug = ?,
                    name = ?,
                    description = ?,
                    is_active = ?,
                    is_system = ?,
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

            statement.setString(i++, entity.getSlug());
            statement.setString(i++, entity.getName());

            if (entity.getDescription() != null) {
                statement.setString(i++, entity.getDescription());
            } else {
                statement.setNull(i++, Types.VARCHAR);
            }

            statement.setBoolean(
                    i++,
                    entity.getIsActive() != null ? entity.getIsActive() : Boolean.TRUE
            );

            statement.setBoolean(
                    i++,
                    entity.getIsSystem() != null ? entity.getIsSystem() : Boolean.FALSE
            );

            statement.setObject(i++, entity.getId());
            statement.setLong(i, entity.getVersion() != null ? entity.getVersion() : 0L);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Error updating Role entity with ID: " + entity.getId(), e);
        }
    }

    @Override
    public Optional<RoleEntity> findByName(String name) {
        String sql = """
                SELECT *
                FROM %s
                WHERE name = ?
                  AND deleted_at IS NULL
                LIMIT 1
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, name);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error finding Role entity by name: " + name, e);
        }
    }

    @Override
    public boolean existsByName(String name) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM %s
                    WHERE name = ?
                      AND deleted_at IS NULL
                )
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, name);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error checking Role existence by name: " + name, e);
        }
    }
}