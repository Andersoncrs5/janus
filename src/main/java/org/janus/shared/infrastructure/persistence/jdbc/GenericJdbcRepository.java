package org.janus.shared.infrastructure.persistence.jdbc;

import jakarta.inject.Inject;
import org.janus.shared.domain.base.filter.FilterBaseDTO;
import org.janus.shared.domain.base.model.BaseEntity;
import org.janus.shared.domain.base.repository.GenericRepository;
import org.janus.shared.domain.page.Page;
import org.janus.shared.domain.queries.Query;
import org.postgresql.util.PGobject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

public abstract class GenericJdbcRepository<T extends BaseEntity> implements GenericRepository<T, UUID> {

    @Inject
    protected DataSource dataSource;

    protected PGobject postgresJsonb(String value) {
        if (value == null) {
            return null;
        }

        try {
            PGobject object = new PGobject();
            object.setType("jsonb");
            object.setValue(value);
            return object;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to create PostgreSQL JSONB value",
                    e
            );
        }
    }

    protected PGobject postgresEnum(String type, Enum<?> value) {
        if (value == null) {
            return null;
        }

        try {
            PGobject object = new PGobject();
            object.setType(type);
            object.setValue(value.name());
            return object;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to create PostgreSQL enum value: " + type,
                    e
            );
        }
    }

    // =========================================================
    // REQUIRED & MAPPER
    // =========================================================

    /**
     * Nome da tabela usada pelo repository.
     */
    protected abstract String getTableName();

    /**
     * Converte uma linha do ResultSet para a entidade.
     */
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    // =========================================================
    // BASE MAPPING
    // =========================================================

    /**
     * Mapeia os campos existentes em BaseEntity.
     * <p>
     * As entidades específicas devem chamar este método
     * antes de mapear seus próprios campos.
     */
    protected void mapBaseFields(
            T entity,
            ResultSet rs
    ) throws SQLException {

        entity.setId(
                getId(rs)
        );

        entity.setVersion(
                rs.getLong("version")
        );

        entity.setCreatedAt(
                rs.getObject(
                        "created_at",
                        OffsetDateTime.class
                )
        );

        entity.setUpdatedAt(
                rs.getObject(
                        "updated_at",
                        OffsetDateTime.class
                )
        );

        entity.setDeletedAt(
                rs.getObject(
                        "deleted_at",
                        OffsetDateTime.class
                )
        );
    }

    @SuppressWarnings("unchecked")
    protected UUID getId(
            ResultSet rs
    ) throws SQLException {

        return (UUID) rs.getObject("id");
    }

    // =========================================================
    // READ
    // =========================================================

    @Override
    public List<T> findAll() {
        String sql = String.format("SELECT * FROM %s WHERE deleted_at IS NULL", getTableName());
        List<T> result = new ArrayList<>();

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar registros de " + getTableName(), e);
        }
        return result;
    }

    @Override
    public List<T> findAllDeleted() {
        String sql = String.format("SELECT * FROM %s WHERE deleted_at IS NOT NULL", getTableName());
        List<T> result = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error finding deleted entities from: " + getTableName(), e);
        }

        return result;
    }

    @Override
    public Optional<T> findById(UUID id) {
        if (id == null)
            return Optional.empty();

        String sql = """
                SELECT *
                FROM %s
                WHERE id = ?
                  AND deleted_at IS NULL
                LIMIT 1
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setObject(1, id);

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error finding entity by UUID: " + id,
                    e
            );
        }
    }

    @Override
    public Optional<T> findByIdWithDeleted(UUID id) {
        if (id == null)
            return Optional.empty();

        String sql = """
                SELECT *
                FROM %s
                WHERE id = ?
                LIMIT 1
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setObject(1, id);

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error finding entity (including deleted) by UUID: " + id,
                    e
            );
        }
    }

    @Override
    public Optional<T> findByIdForUpdate(UUID id) {
        if (id == null)
            return Optional.empty();

        String sql = """
                SELECT *
                FROM %s
                WHERE id = ?
                  AND deleted_at IS NULL
                FOR UPDATE
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setObject(1, id);

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error finding entity for update by UUID: " + id,
                    e
            );
        }
    }

    @Override
    public boolean existsById(UUID id) {
        if (id == null) return false;

        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM %s
                    WHERE id = ?
                      AND deleted_at IS NULL
                )
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setObject(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error checking entity existence: " + id,
                    e
            );
        }
    }

    @Override
    public boolean existsAllByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return true;

        Set<UUID> uniqueIds = new HashSet<>(ids);
        String placeholders = placeholders(uniqueIds.size());

        String sql = """
                SELECT COUNT(DISTINCT id)
                FROM %s
                WHERE id IN (%s)
                  AND deleted_at IS NULL
                """.formatted(getTableName(), placeholders);

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            int i = 1;
            for (UUID id : uniqueIds) {
                statement.setObject(i++, id);
            }

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) == uniqueIds.size();
                }
                return false;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error checking if all entities exist by UUIDs",
                    e
            );
        }
    }

    @Override
    public long count() {
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE deleted_at IS NULL", getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Error counting active entities from: " + getTableName(), e);
        }
    }

    @Override
    public long countWithDeleted() {
        String sql = String.format("SELECT COUNT(*) FROM %s", getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Error counting all entities (including deleted) from: " + getTableName(), e);
        }
    }

    // =========================================================
    // INSERT & SAVE
    // =========================================================

    @Override
    public T insert(T entity) {
        throw new UnsupportedOperationException(
                "Insert must be implemented by the concrete repository: "
                        + getClass().getSimpleName()
        );
    }

    @Override
    public List<T> insertAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(entities.size());
        for (T entity : entities) {
            result.add(insert(entity));
        }
        return result;
    }

    @Override
    public T save(T entity) {
        throw new UnsupportedOperationException(
                "Save must be implemented by the concrete repository: "
                        + getClass().getSimpleName()
        );
    }

    @Override
    public List<T> saveAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(entities.size());
        for (T entity : entities) {
            result.add(save(entity));
        }
        return result;
    }

    // =========================================================
    // DELETE BY UUID
    // =========================================================

    @Override
    public int deleteById(UUID id) {

        String sql = """
                UPDATE %s
                SET
                    deleted_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = ?
                  AND deleted_at IS NULL
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setObject(1, id);

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error deleting entity by UUID: " + id,
                    e
            );
        }
    }

    @Override
    public int deleteById(
            UUID id,
            long expectedVersion
    ) {

        String sql = """
                UPDATE %s
                SET
                    deleted_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = ?
                  AND version = ?
                  AND deleted_at IS NULL
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setObject(1, id);
            statement.setLong(2, expectedVersion);

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error deleting entity with optimistic lock. UUID: " + id,
                    e
            );
        }
    }

    @Override
    public int deleteAll() {

        String sql = """
                UPDATE %s
                SET
                    deleted_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE deleted_at IS NULL
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error deleting all entities.",
                    e
            );
        }
    }

    @Override
    public int deleteAllById(List<UUID> ids) {

        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        String placeholders = placeholders(ids.size());

        String sql = """
                UPDATE %s
                SET
                    deleted_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id IN (%s)
                  AND deleted_at IS NULL
                """.formatted(
                getTableName(),
                placeholders
        );

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            for (int i = 0; i < ids.size(); i++) {
                statement.setObject(i + 1, ids.get(i));
            }

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error deleting entities by UUIDs.",
                    e
            );
        }
    }

    // =========================================================
    // RESTORE BY UUID
    // =========================================================

    @Override
    public int restoreById(UUID id) {

        String sql = """
                UPDATE %s
                SET
                    deleted_at = NULL,
                    version = version + 1
                WHERE id = ?
                  AND deleted_at IS NOT NULL
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setObject(1, id);

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error restoring entity by UUID: " + id,
                    e
            );
        }
    }

    @Override
    public int restoreAllByIds(List<UUID> ids) {

        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        String placeholders = placeholders(ids.size());

        String sql = """
                UPDATE %s
                SET
                    deleted_at = NULL,
                    version = version + 1
                WHERE id IN (%s)
                  AND deleted_at IS NOT NULL
                """.formatted(
                getTableName(),
                placeholders
        );

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            for (int i = 0; i < ids.size(); i++) {
                statement.setObject(i + 1, ids.get(i));
            }

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error restoring entities by UUIDs.",
                    e
            );
        }
    }

    // =========================================================
    // HARD DELETE BY UUID (FORCE DELETE)
    // =========================================================

    @Override
    public int deleteForceById(UUID id) {

        String sql = """
                DELETE FROM %s
                WHERE id = ?
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setObject(1, id);

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error force deleting entity by UUID: " + id,
                    e
            );
        }
    }

    @Override
    public int deleteAllForceById(List<UUID> ids) {

        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        String placeholders = placeholders(ids.size());

        String sql = """
                DELETE FROM %s
                WHERE id IN (%s)
                """.formatted(
                getTableName(),
                placeholders
        );

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            for (int i = 0; i < ids.size(); i++) {
                statement.setObject(i + 1, ids.get(i));
            }

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error force deleting entities by UUIDs.",
                    e
            );
        }
    }

    // =========================================================
    // UPDATE WITH OPTIMISTIC LOCK
    // =========================================================

    protected int executeOptimisticUpdate(
            Connection connection,
            String sql,
            UUID id,
            long expectedVersion,
            Object... parameters
    ) throws SQLException {

        try (
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            int index = 1;

            for (Object parameter : parameters) {
                setParameter(
                        statement,
                        index++,
                        parameter
                );
            }

            statement.setObject(index++, id);
            statement.setLong(index, expectedVersion);

            return statement.executeUpdate();
        }
    }

    // =========================================================
    // JDBC PARAMETER & HELPERS
    // =========================================================

    protected void setParameter(
            PreparedStatement statement,
            int index,
            Object value
    ) throws SQLException {

        if (value == null) {
            statement.setNull(
                    index,
                    Types.NULL
            );
            return;
        }

        statement.setObject(
                index,
                value
        );
    }

    protected String placeholders(
            int count
    ) {

        StringJoiner joiner = new StringJoiner(", ");

        for (int i = 0; i < count; i++) {
            joiner.add("?");
        }

        return joiner.toString();
    }

    // =========================================================
    // PAGINATION & QUERY BUILDER
    // =========================================================

    protected Page<T> findAll(
            Query query,
            List<String> orderBy,
            FilterBaseDTO filter
    ) {

        String where = query.whereClause();

        String finalWhere;

        if (where.isBlank()) {
            finalWhere = "WHERE deleted_at IS NULL";
        } else {
            finalWhere = where + " AND deleted_at IS NULL";
        }

        String orderClause = String.join(
                ", ",
                orderBy
        );

        String countSql = """
                SELECT COUNT(*)
                FROM %s
                %s
                """.formatted(
                getTableName(),
                finalWhere
        );

        String selectSql = """
                SELECT *
                FROM %s
                %s
                ORDER BY %s
                LIMIT ?
                OFFSET ?
                """.formatted(
                getTableName(),
                finalWhere,
                orderClause
        );

        try (Connection connection = dataSource.getConnection()) {

            long total = executeCount(
                    connection,
                    countSql,
                    query.parameters()
            );

            List<T> content = executeSelect(
                    connection,
                    selectSql,
                    query.parameters(),
                    filter
            );

            return new Page<>(
                    content,
                    total,
                    filter.getPage(),
                    filter.getSize()
            );

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error finding paginated entities from: "
                            + getTableName(),
                    e
            );
        }
    }

    protected Query buildBaseFilter(FilterBaseDTO filter) {

        Query query = new Query();

        if (filter.getId() != null) {
            query.and(
                    "id = ?",
                    filter.getId()
            );
        }

        if (filter.getVersionMin() != null) {
            query.and(
                    "version >= ?",
                    filter.getVersionMin()
            );
        }

        if (filter.getVersionMax() != null) {
            query.and(
                    "version <= ?",
                    filter.getVersionMax()
            );
        }

        if (filter.getCreatedAtMin() != null) {
            query.and(
                    "created_at >= ?",
                    filter.getCreatedAtMin()
            );
        }

        if (filter.getCreatedAtMax() != null) {
            query.and(
                    "created_at <= ?",
                    filter.getCreatedAtMax()
            );
        }

        if (filter.getUpdatedAtMin() != null) {
            query.and(
                    "updated_at >= ?",
                    filter.getUpdatedAtMin()
            );
        }

        if (filter.getUpdatedAtMax() != null) {
            query.and(
                    "updated_at <= ?",
                    filter.getUpdatedAtMax()
            );
        }

        return query;
    }

    private long executeCount(
            Connection connection,
            String sql,
            List<Object> parameters
    ) throws SQLException {

        try (
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            bindParameters(
                    statement,
                    parameters
            );

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    return 0;
                }

                return rs.getLong(1);
            }
        }
    }

    private List<T> executeSelect(
            Connection connection,
            String sql,
            List<Object> parameters,
            FilterBaseDTO filter
    ) throws SQLException {

        List<T> result = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            int index = bindParameters(
                    statement,
                    parameters
            );

            statement.setInt(
                    index++,
                    filter.getSize()
            );

            statement.setInt(
                    index,
                    filter.getOffset()
            );

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }

        return result;
    }

    private int bindParameters(
            PreparedStatement statement,
            List<Object> parameters
    ) throws SQLException {

        int index = 1;

        for (Object parameter : parameters) {

            setParameter(
                    statement,
                    index++,
                    parameter
            );
        }

        return index;
    }
}