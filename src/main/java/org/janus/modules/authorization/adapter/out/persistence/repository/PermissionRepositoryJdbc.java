package org.janus.modules.authorization.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.janus.modules.authorization.application.dto.permission.filter.PermissionFilterDTO;
import org.janus.modules.authorization.application.dto.permission.filter.PermissionOrder;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.enums.permission.PermissionModule;
import org.janus.shared.domain.enums.permission.PermissionResource;
import org.janus.shared.domain.enums.permission.PermissionRiskLevel;
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
public class PermissionRepositoryJdbc
        extends GenericJdbcRepository<PermissionEntity>
        implements PermissionRepository {

    @Override
    public Page<PermissionEntity> findAll(PermissionFilterDTO filter) {
        Query query = buildBaseFilter(filter);

        query.andILike("name", filter.getName());
        query.andILike("slug", filter.getSlug());
        query.andILike("action", filter.getAction());


        query.andEnumInCast(
                "module",
                filter.getModule(),
                "permission_module"
        );

        query.andEnumInCast(
                "resource",
                filter.getResource(),
                "permission_resource"
        );

        query.andEnumInCast(
                "risk_level",
                filter.getRiskLevel(),
                "permission_risk_level"
        );

        query.andEqual("is_active", filter.getIsActive());
        query.andEqual("is_system", filter.getIsSystem());
        query.andEqual("created_by", filter.getCreatedBy());

        List<PermissionOrder> orders =
                filter.getOrders() != null && !filter.getOrders().isEmpty()
                        ? filter.getOrders()
                        : List.of(PermissionOrder.CREATED_AT);

        List<String> orderFields = orders.stream()
                .map(PermissionOrder::getField)
                .toList();

        return findAll(
                query,
                orderFields,
                filter
        );
    }

    @Override
    public Optional<PermissionEntity> findByName(String name) {
        return new Query.Select(getTableName())
                .where("name", name)
                .andSoftDelete()
                .findFirst(dataSource, this::mapRow);
    }

    @Override
    public boolean existsByName(String name) {
        return new Query.Exists(getTableName())
                .where("name", name)
                .andSoftDelete()
                .execute(dataSource);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return new Query.Exists(getTableName())
                .where("slug", slug)
                .andSoftDelete()
                .execute(dataSource);
    }

    @Override
    public PermissionEntity insert(PermissionEntity entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        return new Query.Insert(getTableName())
                .value("id", entity.getId())
                .value("name", entity.getName())
                .value("slug", entity.getSlug())
                .value("description", entity.getDescription())
                .value(
                        "module",
                        postgresEnum(
                                "permission_module",
                                entity.getModule()
                        )
                )
                .value(
                        "resource",
                        postgresEnum(
                                "permission_resource",
                                entity.getResource()
                        )
                )
                .value("action", entity.getAction())
                .value(
                        "risk_level",
                        entity.getRiskLevel() != null
                                ? postgresEnum(
                                "permission_risk_level",
                                entity.getRiskLevel()
                        )
                                : postgresEnum(
                                "permission_risk_level",
                                PermissionRiskLevel.LOW
                        )
                )
                .value(
                        "is_active",
                        entity.getIsActive() != null
                                ? entity.getIsActive()
                                : Boolean.TRUE
                )
                .value(
                        "is_system",
                        entity.getIsSystem() != null
                                ? entity.getIsSystem()
                                : Boolean.FALSE
                )
                .value("metadata", postgresJsonb(entity.getMetadata()))
                .value("created_by", entity.getCreatedBy())
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
    public PermissionEntity save(PermissionEntity entity) {
        if (entity.getId() == null || !existsById(entity.getId())) {
            return insert(entity);
        }

        boolean updated = updateOptimistic(entity);

        if (!updated) {
            throw new IllegalStateException(
                    "Failed to update Permission entity (optimistic lock or deleted): "
                            + entity.getId()
            );
        }

        return entity;
    }

    public boolean updateOptimistic(PermissionEntity entity) {
        return new Query.Update(getTableName())
                .set("name", entity.getName())
                .set("slug", entity.getSlug())
                .set("description", entity.getDescription())
                .set(
                        "module",
                        postgresEnum(
                                "permission_module",
                                entity.getModule()
                        )
                )
                .set(
                        "resource",
                        postgresEnum(
                                "permission_resource",
                                entity.getResource()
                        )
                )
                .set("action", entity.getAction())
                .set(
                        "risk_level",
                        entity.getRiskLevel() != null
                                ? postgresEnum(
                                "permission_risk_level",
                                entity.getRiskLevel()
                        )
                                : postgresEnum(
                                "permission_risk_level",
                                PermissionRiskLevel.LOW
                        )
                )
                .set(
                        "is_active",
                        entity.getIsActive() != null
                                ? entity.getIsActive()
                                : Boolean.TRUE
                )
                .set(
                        "is_system",
                        entity.getIsSystem() != null
                                ? entity.getIsSystem()
                                : Boolean.FALSE
                )
                .set("metadata", postgresJsonb(entity.getMetadata()))
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
        return "permissions";
    }

    @Override
    protected PermissionEntity mapRow(ResultSet rs) throws SQLException {
        PermissionEntity entity = new PermissionEntity();

        mapBaseFields(entity, rs);

        entity.setName(rs.getString("name"));
        entity.setSlug(rs.getString("slug"));
        entity.setDescription(rs.getString("description"));

        String moduleStr = rs.getString("module");
        if (moduleStr != null) {
            entity.setModule(PermissionModule.valueOf(moduleStr));
        }

        String resourceStr = rs.getString("resource");
        if (resourceStr != null) {
            entity.setResource(PermissionResource.valueOf(resourceStr));
        }

        entity.setAction(rs.getString("action"));

        String riskLevelStr = rs.getString("risk_level");
        if (riskLevelStr != null) {
            entity.setRiskLevel(PermissionRiskLevel.valueOf(riskLevelStr));
        }

        entity.setIsActive(rs.getBoolean("is_active"));
        entity.setIsSystem(rs.getBoolean("is_system"));
        entity.setMetadata(rs.getString("metadata"));

        Object createdByObj = rs.getObject("created_by");
        if (createdByObj != null) {
            entity.setCreatedBy((UUID) createdByObj);
        }

        return entity;
    }
}