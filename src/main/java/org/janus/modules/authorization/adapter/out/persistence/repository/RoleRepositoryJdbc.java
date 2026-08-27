package org.janus.modules.authorization.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.janus.modules.authorization.application.dto.role.filter.RoleFilterDTO;
import org.janus.modules.authorization.application.dto.role.filter.RoleOrder;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.shared.domain.page.Page;
import org.janus.shared.domain.queries.Query;
import org.janus.shared.infrastructure.persistence.jdbc.GenericJdbcRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
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

    @Override
    public RoleEntity insert(RoleEntity entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        return new Query.Insert(getTableName())
                .value("id", entity.getId())
                .value("slug", entity.getSlug())
                .value("name", entity.getName())
                .value("description", entity.getDescription())
                .value("is_active", entity.getIsActive() != null ? entity.getIsActive() : Boolean.TRUE)
                .value("is_system", entity.getIsSystem() != null ? entity.getIsSystem() : Boolean.FALSE)
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
        return new Query.Update(getTableName())
                .set("slug", entity.getSlug())
                .set("name", entity.getName())
                .set("description", entity.getDescription())
                .set("is_active", entity.getIsActive() != null ? entity.getIsActive() : Boolean.TRUE)
                .set("is_system", entity.getIsSystem() != null ? entity.getIsSystem() : Boolean.FALSE)
                .setExpression("version = version + 1")
                .setExpression("updated_at = CURRENT_TIMESTAMP")
                .where("id", entity.getId())
                .and("version = ?", entity.getVersion() != null ? entity.getVersion() : 0L)
                .andSoftDelete()
                .execute(dataSource);
    }

    @Override
    public Optional<RoleEntity> findByName(String name) {
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
}