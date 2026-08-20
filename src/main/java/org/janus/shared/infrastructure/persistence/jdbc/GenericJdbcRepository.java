package org.janus.shared.infrastructure.persistence.jdbc;

import jakarta.inject.Inject;
import org.janus.shared.domain.base.model.BaseEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.*;

public abstract class GenericJdbcRepository<T extends BaseEntity> {

    @Inject
    protected DataSource dataSource;

    // =========================================================
    // REQUIRED
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
     *
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
     *
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
     *
     * Retorna 1 quando a entidade foi removida.
     * Retorna 0 quando:
     *
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
     *
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
     *
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
}
