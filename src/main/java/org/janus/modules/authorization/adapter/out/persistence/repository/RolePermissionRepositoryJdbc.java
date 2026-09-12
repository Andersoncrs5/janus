package org.janus.modules.authorization.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.janus.modules.authorization.application.dto.rolePermission.filter.RolePermissionFilterDTO;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.shared.domain.enums.rolePermission.PermissionEffectEnum;
import org.janus.shared.domain.page.Page;
import org.janus.shared.domain.queries.Query;
import org.janus.shared.infrastructure.persistence.jdbc.GenericJdbcRepository;
import org.postgresql.util.PGobject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RolePermissionRepositoryJdbc
        extends GenericJdbcRepository<RolePermissionEntity>
        implements RolePermissionRepository {

    @Override
    public Page<RolePermissionEntity> findAll(RolePermissionFilterDTO filter) {
        Query.Select select = new Query.Select(getTableName());

        if (filter.getRoleId() != null) {
            select.andEqual("role_id", filter.getRoleId());
        }

        if (filter.getPermissionId() != null) {
            select.andEqual("permission_id", filter.getPermissionId());
        }

        if (filter.getAssignedBy() != null) {
            select.andEqual("assigned_by", filter.getAssignedBy());
        }

        if (filter.getEffect() != null && !filter.getEffect().isEmpty()) {
            select.andEnumInCast("effect", filter.getEffect(), "permission_effect_enum");
        }

        if (filter.getIsExpired() != null) {
            select.andExpired("expires_at", filter.getIsExpired());
        }

        if (filter.getExpiresAtFrom() != null) {
            select.andGreaterThanOrEqual("expires_at", filter.getExpiresAtFrom());
        }

        if (filter.getExpiresAtTo() != null) {
            select.andLessThanOrEqual("expires_at", filter.getExpiresAtTo());
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

        List<RolePermissionEntity> content = select.findAll(dataSource, this::mapRow);

        return Page.of(content, totalElements, filter.getPage(), filter.getSize());
    }

    @Override
    public Optional<RolePermissionEntity> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId) {
        return new Query.Select(getTableName())
                .andEqual("role_id", roleId)
                .andEqual("permission_id", permissionId)
                .andSoftDelete()
                .findFirst(dataSource, this::mapRow);
    }

    @Override
    public List<RolePermissionEntity> findAllByRoleId(UUID roleId) {
        return new Query.Select(getTableName())
                .where("role_id", roleId)
                .andSoftDelete()
                .findAll(dataSource, this::mapRow);
    }

    @Override
    public List<RolePermissionEntity> findAllByPermissionId(UUID permissionId) {
        return new Query.Select(getTableName())
                .where("permission_id", permissionId)
                .andSoftDelete()
                .findAll(dataSource, this::mapRow);
    }

    @Override
    public boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId) {
        return new Query.Exists(getTableName())
                .andEqual("role_id", roleId)
                .andEqual("permission_id", permissionId)
                .andSoftDelete()
                .execute(dataSource);
    }

    @Override
    public List<RolePermissionEntity> findAllActiveByRoleId(UUID roleId, OffsetDateTime now) {
        OffsetDateTime refTime = now != null ? now : OffsetDateTime.now();

        return new Query.Select(getTableName())
                .where("role_id", roleId)
                .andSoftDelete()
                .and("(expires_at IS NULL OR expires_at > ?)", refTime)
                .findAll(dataSource, this::mapRow);
    }

    @Override
    public long countByRoleId(UUID roleId) {
        return new Query.Count(getTableName())
                .where("role_id", roleId)
                .andSoftDelete()
                .execute(dataSource);
    }

    @Override
    public RolePermissionEntity insert(RolePermissionEntity entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        OffsetDateTime assignedAt = entity.getAssignedAt() != null
                ? entity.getAssignedAt()
                : OffsetDateTime.now();

        return new Query.Insert(getTableName())
                .value("id", entity.getId())
                .value("role_id", entity.getRoleId())
                .value("permission_id", entity.getPermissionId())
                .value("effect", toPgEnum(entity.getEffect()))
                .value("conditions", postgresJsonb(entity.getConditions()))
                .value("expires_at", entity.getExpiresAt())
                .value("assigned_by", entity.getAssignedBy())
                .value("assigned_at", assignedAt)
                .value("version", 0L)
                .value("created_at", OffsetDateTime.now())
                .value("updated_at", OffsetDateTime.now())
                .executeAndMap(
                        dataSource,
                        List.of("version", "created_at", "updated_at"),
                        rs -> {
                            entity.setVersion(rs.getLong("version"));
                            entity.setCreatedAt(
                                    rs.getObject("created_at", OffsetDateTime.class)
                            );
                            entity.setUpdatedAt(
                                    rs.getObject("updated_at", OffsetDateTime.class)
                            );
                            return entity;
                        }
                );
    }

    @Override
    public RolePermissionEntity save(RolePermissionEntity entity) {
        if (entity.getId() == null || !existsById(entity.getId())) {
            return insert(entity);
        }

        boolean updated = updateOptimistic(entity);

        if (!updated) {
            throw new IllegalStateException(
                    "Failed to update RolePermission entity (optimistic lock or deleted): "
                            + entity.getId()
            );
        }

        return entity;
    }

    public boolean updateOptimistic(RolePermissionEntity entity) {
        return new Query.Update(getTableName())
                .set("role_id", entity.getRoleId())
                .set("permission_id", entity.getPermissionId())
                .set("effect", toPgEnum(entity.getEffect()))
                .set("conditions", postgresJsonb(entity.getConditions()))
                .set("expires_at", entity.getExpiresAt())
                .set("assigned_by", entity.getAssignedBy())
                .set("assigned_at", entity.getAssignedAt())
                .setExpression("version = version + 1")
                .setExpression("updated_at = CURRENT_TIMESTAMP")
                .where("id", entity.getId())
                .and(
                        "version = ?",
                        entity.getVersion() != null
                                ? entity.getVersion()
                                : 0L
                )
                .andSoftDelete()
                .execute(dataSource);
    }

    @Override
    protected String getTableName() {
        return RolePermissionEntity.TABLE_NAME;
    }

    @Override
    protected RolePermissionEntity mapRow(ResultSet rs) throws SQLException {
        RolePermissionEntity entity = new RolePermissionEntity();

        mapBaseFields(entity, rs);

        Object roleIdObj = rs.getObject("role_id");
        if (roleIdObj != null) {
            entity.setRoleId((UUID) roleIdObj);
        }

        Object permissionIdObj = rs.getObject("permission_id");
        if (permissionIdObj != null) {
            entity.setPermissionId((UUID) permissionIdObj);
        }

        String effectStr = rs.getString("effect");
        if (effectStr != null) {
            entity.setEffect(PermissionEffectEnum.valueOf(effectStr));
        }

        entity.setConditions(rs.getString("conditions"));
        entity.setExpiresAt(rs.getObject("expires_at", OffsetDateTime.class));

        Object assignedByObj = rs.getObject("assigned_by");
        if (assignedByObj != null) {
            entity.setAssignedBy((UUID) assignedByObj);
        }

        entity.setAssignedAt(rs.getObject("assigned_at", OffsetDateTime.class));

        return entity;
    }

    private PGobject toPgEnum(PermissionEffectEnum effect) {
        String value = effect != null ? effect.name() : PermissionEffectEnum.ALLOW.name();
        try {
            PGobject pgObject = new PGobject();
            pgObject.setType("permission_effect_enum");
            pgObject.setValue(value);
            return pgObject;
        } catch (SQLException e) {
            throw new IllegalArgumentException("Erro ao converter enum para PGobject: " + value, e);
        }
    }

}