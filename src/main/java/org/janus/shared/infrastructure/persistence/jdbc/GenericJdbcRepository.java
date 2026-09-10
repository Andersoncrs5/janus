package org.janus.shared.infrastructure.persistence.jdbc;

import jakarta.inject.Inject;
import org.janus.shared.domain.base.filter.FilterBaseDTO;
import org.janus.shared.domain.base.model.BaseEntity;
import org.janus.shared.domain.page.Page;
import org.janus.shared.domain.queries.Query;
import org.postgresql.util.PGobject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

public abstract class GenericJdbcRepository<T extends BaseEntity> {

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
    // REQUIRED
    // =========================================================


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


    /**
     * Permite que repositories específicos definam como
     * obter o UUID caso o tipo não seja diretamente suportado
     * por ResultSet#getObject(...).
     */
    @SuppressWarnings("unchecked")
    protected UUID getId(
            ResultSet rs
    ) throws SQLException {

        return (UUID) rs.getObject("id");
    }


    // =========================================================
    // FIND BY UUID
    // =========================================================

    public Optional<T> findById(UUID id) {

        String sql = """
                SELECT *
                FROM %s
                WHERE id = ?
                  AND deleted_at IS NULL
                LIMIT 1
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
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


    // =========================================================
    // EXISTS BY UUID
    // =========================================================

    public boolean existsById(UUID id) {

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
                PreparedStatement statement =
                        connection.prepareStatement(sql)
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


    // =========================================================
    // DELETE BY UUID
    // =========================================================

    /**
     * Soft delete.
     * <p>
     * Incrementa a versão e preenche deleted_at.
     */
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
                PreparedStatement statement =
                        connection.prepareStatement(sql)
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


    // =========================================================
    // DELETE BY UUID + OPTIMISTIC LOCK
    // =========================================================

    /**
     * Soft delete usando optimistic locking.
     * <p>
     * Retorna 1 quando a entidade foi removida.
     * Retorna 0 quando:
     * <p>
     * - não existe;
     * - já foi removida;
     * - version está desatualizada.
     */
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
                PreparedStatement statement =
                        connection.prepareStatement(sql)
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


    // =========================================================
    // DELETE ALL
    // =========================================================

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
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Error deleting all entities.",
                    e
            );
        }
    }


    // =========================================================
    // DELETE ALL BY UUIDS
    // =========================================================

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
                PreparedStatement statement =
                        connection.prepareStatement(sql)
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
    // INSERT
    // =========================================================

    /**
     * O insert genérico não é implementado automaticamente
     * porque cada entidade possui colunas diferentes.
     * <p>
     * Cada repository específico deve implementar o INSERT.
     */
    public T insert(T entity) {
        throw new UnsupportedOperationException(
                "Insert must be implemented by the concrete repository: "
                        + getClass().getSimpleName()
        );
    }


    // =========================================================
    // SAVE
    // =========================================================

    /**
     * Save é deixado para o repository específico porque
     * INSERT/UPDATE dependem das colunas da entidade.
     */
    public T save(T entity) {
        throw new UnsupportedOperationException(
                "Save must be implemented by the concrete repository: "
                        + getClass().getSimpleName()
        );
    }


    // =========================================================
    // UPDATE WITH OPTIMISTIC LOCK
    // =========================================================

    /**
     * Helper para repositories específicos construírem
     * UPDATEs com versionamento.
     * <p>
     * O repository concreto fornece apenas o SET e os
     * parâmetros adicionais.
     */
    protected int executeOptimisticUpdate(
            Connection connection,
            String sql,
            UUID id,
            long expectedVersion,
            Object... parameters
    ) throws SQLException {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
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
    // RESTORE BY UUID
    // =========================================================

    /**
     * Restaura uma entidade removida por soft delete (`deleted_at = NULL`).
     * Incrementa a versão.
     */
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
                PreparedStatement statement =
                        connection.prepareStatement(sql)
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


    // =========================================================
    // RESTORE ALL BY UUIDS
    // =========================================================

    /**
     * Restaura múltiplas entidades removidas por soft delete (`deleted_at = NULL`).
     * Incrementa a versão de cada registro afetado.
     */
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
                PreparedStatement statement =
                        connection.prepareStatement(sql)
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

    /**
     * Remove fisicamente a linha do banco de dados (Hard Delete).
     */
    public int deleteForceById(UUID id) {

        String sql = """
                DELETE FROM %s
                WHERE id = ?
                """.formatted(getTableName());

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
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


    // =========================================================
    // HARD DELETE ALL BY UUIDS (FORCE DELETE ALL)
    // =========================================================

    /**
     * Remove fisicamente múltiplas linhas do banco de dados (Hard Delete).
     */
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
                PreparedStatement statement =
                        connection.prepareStatement(sql)
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
    // JDBC PARAMETER
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


    // =========================================================
    // HELPERS
    // =========================================================

    protected String placeholders(
            int count
    ) {

        StringJoiner joiner =
                new StringJoiner(", ");

        for (int i = 0; i < count; i++) {
            joiner.add("?");
        }

        return joiner.toString();
    }

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
                PreparedStatement statement =
                        connection.prepareStatement(sql)
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


    // =========================================================
    // SELECT PAGINATED
    // =========================================================

    private List<T> executeSelect(
            Connection connection,
            String sql,
            List<Object> parameters,
            FilterBaseDTO filter
    ) throws SQLException {

        List<T> result = new ArrayList<>();

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
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
