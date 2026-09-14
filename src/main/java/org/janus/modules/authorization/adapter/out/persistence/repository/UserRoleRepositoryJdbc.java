package org.janus.modules.authorization.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.shared.domain.queries.Query;
import org.janus.shared.infrastructure.persistence.jdbc.GenericJdbcRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRoleRepositoryJdbc
        extends GenericJdbcRepository<UserRoleEntity>
        implements UserRoleRepository {

    @Override
    protected String getTableName() {
        return UserRoleEntity.TABLE_NAME;
    }

    @Override
    public UserRoleEntity insert(UserRoleEntity entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        return new Query.Insert(getTableName())
                .value("id", entity.getId())
                .value("user_id", entity.getUserId())
                .value("role_id", entity.getRoleId())
                .value("expires_at", entity.getExpiresAt())
                .value("assigned_by", entity.getAssignedById())
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
    public UserRoleEntity save(UserRoleEntity entity) {
        if (entity.getId() == null || !existsById(entity.getId())) {
            return insert(entity);
        }

        boolean updated = updateOptimistic(entity);
        if (!updated) {
            throw new IllegalStateException(
                    "Failed to update UserRole entity (optimistic lock or deleted): " + entity.getId()
            );
        }
        return entity;
    }

    public boolean updateOptimistic(UserRoleEntity entity) {
        return new Query.Update(getTableName())
                .set("user_id", entity.getUserId())
                .set("role_id", entity.getRoleId())
                .set("expires_at", entity.getExpiresAt())
                .set("assigned_by", entity.getAssignedById())
                .setExpression("version = version + 1")
                .setExpression("updated_at = CURRENT_TIMESTAMP")
                .where("id", entity.getId())
                .and("version = ?", entity.getVersion() != null ? entity.getVersion() : 0L)
                .andSoftDelete()
                .execute(dataSource);
    }

    @Override
    protected UserRoleEntity mapRow(ResultSet rs) throws SQLException {
        UserRoleEntity entity = new UserRoleEntity();
        mapBaseFields(entity, rs);
        entity.setUserId(rs.getObject("user_id", UUID.class));
        entity.setRoleId(rs.getObject("role_id", UUID.class));
        entity.setExpiresAt(rs.getObject("expires_at", OffsetDateTime.class));
        entity.setAssignedById(rs.getObject("assigned_by", UUID.class));
        return entity;
    }

    @Override
    public boolean existsByUserIdAndRoleId(UUID userId, UUID roleId) {
        return new Query.Exists(getTableName())
                .where("user_id", userId)
                .where("role_id", roleId)
                .andSoftDelete()
                .execute(dataSource);
    }

    @Override
    public List<UUID> findAllRoleIdsByUserId(UUID userId) {
        return new Query.Select(getTableName())
                .where("user_id", userId)
                .andSoftDelete()
                .findAll(dataSource, rs -> rs.getObject("role_id", UUID.class));
    }

    @Override
    public int deleteByUserIdAndRoleId(UUID userId, UUID roleId) {
        return new Query.Delete(getTableName())
                .where("user_id", userId)
                .where("role_id", roleId)
                .execute(dataSource);
    }

    @Override
    public int deleteAllByUserId(UUID userId) {
        return new Query.Delete(getTableName())
                .where("user_id", userId)
                .execute(dataSource);
    }

    @Override
    public boolean existsActiveByUserIdAndRoleId(UUID userId, UUID roleId) {
        return new Query.Exists(getTableName())
                .where("user_id", userId)
                .where("role_id", roleId)
                .andSoftDelete()
                .andCustom("(expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)")
                .execute(dataSource);
    }

    @Override
    public Optional<UserRoleEntity> findByUserIdAndRoleId(UUID userId, UUID roleId) {
        return new Query.Select(getTableName())
                .where("user_id", userId)
                .where("role_id", roleId)
                .andSoftDelete()
                .findFirst(dataSource, this::mapRow);
    }

    @Override
    public List<String> findRolesOnlyNameByUserId(UUID userId) {
        String sql = """
                    SELECT r.name 
                    FROM user_roles ur
                    INNER JOIN roles r ON ur.role_id = r.id
                    WHERE ur.user_id = ?
                      AND ur.deleted_at IS NULL
                      AND r.deleted_at IS NULL
                      AND r.is_active = TRUE
                      AND (ur.expires_at IS NULL OR ur.expires_at > CURRENT_TIMESTAMP)
                """;

        List<String> roles = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roles.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching role names for userId: " + userId, e);
        }

        return roles;
    }

    @Override
    public List<RoleEntity> findRolesByUserId(UUID userId) {
        String sql = """
                    SELECT r.id, r.name, r.slug, r.description, r.is_active, r.is_system, 
                           r.version, r.created_at, r.updated_at, r.deleted_at
                    FROM user_roles ur
                    INNER JOIN roles r ON ur.role_id = r.id
                    WHERE ur.user_id = ?
                      AND ur.deleted_at IS NULL
                      AND r.deleted_at IS NULL
                      AND r.is_active = TRUE
                      AND (ur.expires_at IS NULL OR ur.expires_at > CURRENT_TIMESTAMP)
                """;

        List<RoleEntity> roles = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RoleEntity role = new RoleEntity();
                    role.setId(rs.getObject("id", UUID.class));
                    role.setName(rs.getString("name"));
                    role.setSlug(rs.getString("slug"));
                    role.setDescription(rs.getString("description"));
                    role.setIsActive(rs.getBoolean("is_active"));
                    role.setIsSystem(rs.getBoolean("is_system"));
                    role.setVersion(rs.getLong("version"));
                    role.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    role.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                    role.setDeletedAt(rs.getObject("deleted_at", OffsetDateTime.class));

                    roles.add(role);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching roles for userId: " + userId, e);
        }

        return roles;
    }
}