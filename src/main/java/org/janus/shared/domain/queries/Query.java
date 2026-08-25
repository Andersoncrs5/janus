package org.janus.shared.domain.queries;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;

import javax.sql.DataSource;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

@ApplicationScoped
public class Query {

    private final List<String> conditions =
            new ArrayList<>();

    private final List<Object> parameters =
            new ArrayList<>();

    private final List<String> joins = new ArrayList<>();

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public static class Insert extends QueryBuilder<Insert> {
        private final Map<String, Object> values = new LinkedHashMap<>();

        public Insert(String table) {
            super(table);
        }

        public Insert value(String column, Object value) {
            if (value != null) {
                values.put(column, value);
            }
            return this;
        }

        public String buildSql(List<String> returningColumns) {
            String columns = String.join(", ", values.keySet());
            String placeholders = String.join(", ", values.keySet().stream().map(k -> "?").toList());
            String returning = returningColumns.isEmpty() ? "" : " RETURNING " + String.join(", ", returningColumns);

            return "INSERT INTO %s (%s) VALUES (%s)%s".formatted(table, columns, placeholders, returning).trim();
        }

        public <T> T executeAndMap(DataSource dataSource, List<String> returningColumns, RowMapper<T> mapper) {
            String sql = buildSql(returningColumns);
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                int index = 1;
                for (Object val : values.values()) {
                    statement.setObject(index++, val);
                }

                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return mapper.map(rs);
                    }
                    throw new IllegalStateException("Insert did not return any rows for table: " + table);
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Error executing INSERT for table: " + table, e);
            }
        }
    }

    // =========================================================
    // UPDATE
    // =========================================================
    public static class Update extends QueryBuilder<Update> {
        private final List<String> setClauses = new ArrayList<>();

        public Update(String table) {
            super(table);
        }

        public Update set(String column, Object value) {
            setClauses.add(column + " = ?");
            parameters.add(value);
            return this;
        }

        public Update setExpression(String columnExpression) {
            setClauses.add(columnExpression);
            return this;
        }

        public String buildSql() {
            String setClause = String.join(", ", setClauses);
            return "UPDATE %s SET %s %s".formatted(table, setClause, buildWhereClause()).trim();
        }

        public boolean execute(DataSource dataSource) {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(buildSql())) {

                for (int i = 0; i < parameters.size(); i++) {
                    statement.setObject(i + 1, parameters.get(i));
                }
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new IllegalStateException("Error executing UPDATE for table: " + table, e);
            }
        }
    }

    public static class Delete {
        private final String table;
        private final List<String> conditions = new ArrayList<>();
        private final List<Object> params = new ArrayList<>();

        public Delete(String table) {
            this.table = table;
        }

        public Delete where(String column, Object value) {
            if (value != null) {
                conditions.add(column + " = ?");
                params.add(value);
            }
            return this;
        }

        public String buildSql() {
            String whereClause = conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
            return "DELETE FROM %s %s".formatted(table, whereClause).trim();
        }

        public int execute(DataSource dataSource) {
            try (
                    Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(buildSql())
            ) {
                for (int i = 0; i < params.size(); i++) {
                    statement.setObject(i + 1, params.get(i));
                }
                return statement.executeUpdate();
            } catch (SQLException e) {
                throw new IllegalStateException("Error executing DELETE query for table: " + table, e);
            }
        }
    }

    @Getter
    public static class Select extends QueryBuilder<Select> {
        private Integer limit;

        public Select(String table) {
            super(table);
        }

        public Select where(String column, Object value) {
            if (value != null) {
                this.conditions.add(column + " = ?");
                this.parameters.add(value);
            }
            return this;
        }

        public Select limit(int limit) {
            this.limit = limit;
            return this;
        }

        public String buildSql() {
            String limitClause = limit != null ? " LIMIT " + limit : "";

            return "SELECT * FROM %s %s%s%s"
                    .formatted(table, buildJoinsClause(), buildWhereClause(), limitClause)
                    .trim();
        }

        public <T> Optional<T> findFirst(DataSource dataSource, RowMapper<T> mapper) {
            this.limit = 1;

            try (
                    Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(buildSql())
            ) {
                for (int i = 0; i < parameters.size(); i++) {
                    statement.setObject(i + 1, parameters.get(i));
                }

                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapper.map(rs));
                    }
                    return Optional.empty();
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Error executing SELECT (findFirst) query for table: " + table, e);
            }
        }

        public <T> List<T> findAll(DataSource dataSource, RowMapper<T> mapper) {
            List<T> results = new ArrayList<>();
            try (
                    Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(buildSql())
            ) {
                for (int i = 0; i < parameters.size(); i++) {
                    statement.setObject(i + 1, parameters.get(i));
                }

                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        results.add(mapper.map(rs));
                    }
                    return results;
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Error executing SELECT (findAll) query for table: " + table, e);
            }
        }
    }

    @Getter
    public static class Exists extends QueryBuilder<Exists> {

        public Exists(String table) {
            super(table);
        }

        public String buildSql() {
            return "SELECT EXISTS (SELECT 1 FROM %s %s%s)".formatted(
                    table,
                    buildJoinsClause(),
                    buildWhereClause()
            ).trim();
        }

        public boolean execute(DataSource dataSource) {
            try (
                    Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(buildSql())
            ) {
                for (int i = 0; i < parameters.size(); i++) {
                    statement.setObject(i + 1, parameters.get(i));
                }
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() && rs.getBoolean(1);
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Error executing EXISTS query for table: " + table, e);
            }
        }
    }

    // =========================================================
    // AND
    // =========================================================

    public void and(
            String condition,
            Object... values
    ) {

        conditions.add(condition);

        parameters.addAll(Arrays.asList(values));
    }

    // =========================================================
    // OR
    // =========================================================

    public void or(
            String condition,
            Object... values
    ) {

        if (conditions.isEmpty()) {
            conditions.add(condition);
        } else {
            String previous =
                    conditions.removeLast(
                    );

            conditions.add(
                    "(" +
                            previous +
                            " OR " +
                            condition +
                            ")"
            );
        }

        parameters.addAll(Arrays.asList(values));
    }

    // =========================================================
    // IN
    // =========================================================

    public void andIn(
            String column,
            List<?> values
    ) {

        if (values == null || values.isEmpty()) {
            return;
        }

        String placeholders =
                placeholders(values.size());

        conditions.add(
                column +
                        " IN (" +
                        placeholders +
                        ")"
        );

        parameters.addAll(values);
    }


    // =========================================================
    // NOT IN
    // =========================================================

    public void andNotIn(
            String column,
            List<?> values
    ) {

        if (values == null || values.isEmpty()) {
            return;
        }

        String placeholders =
                placeholders(values.size());

        conditions.add(
                column +
                        " NOT IN (" +
                        placeholders +
                        ")"
        );

        parameters.addAll(values);
    }


    // =========================================================
    // BETWEEN
    // =========================================================

    public void andBetween(
            String column,
            Object min,
            Object max
    ) {

        if (min != null) {
            and(
                    column + " >= ?",
                    min
            );
        }

        if (max != null) {
            and(
                    column + " <= ?",
                    max
            );
        }
    }


    // =========================================================
    // IS NULL
    // =========================================================

    public void andIsNull(
            String column
    ) {

        conditions.add(
                column + " IS NULL"
        );
    }


    // =========================================================
    // IS NOT NULL
    // =========================================================

    public void andIsNotNull(
            String column
    ) {

        conditions.add(
                column + " IS NOT NULL"
        );
    }


    // =========================================================
    // LIKE
    // =========================================================

    public void andLike(
            String column,
            String value
    ) {

        if (value == null || value.isBlank()) {
            return;
        }

        and(
                column + " LIKE ?",
                "%" + value + "%"
        );
    }


    // =========================================================
    // ILIKE - POSTGRESQL
    // =========================================================

    public void andILike(
            String column,
            String value
    ) {

        if (value == null || value.isBlank()) {
            return;
        }

        and(
                column + " ILIKE ?",
                "%" + value + "%"
        );
    }


    // =========================================================
    // EQUAL
    // =========================================================

    public void andEqual(
            String column,
            Object value
    ) {

        if (value == null) {
            return;
        }

        and(
                column + " = ?",
                value
        );
    }


    // =========================================================
    // NOT EQUAL
    // =========================================================

    public void andNotEqual(
            String column,
            Object value
    ) {

        if (value == null) {
            return;
        }

        and(
                column + " <> ?",
                value
        );
    }


    // =========================================================
    // GREATER THAN
    // =========================================================

    public void andGreaterThan(
            String column,
            Object value
    ) {

        if (value == null) {
            return;
        }

        and(
                column + " > ?",
                value
        );
    }


    // =========================================================
    // GREATER THAN OR EQUAL
    // =========================================================

    public void andGreaterThanOrEqual(
            String column,
            Object value
    ) {

        if (value == null) {
            return;
        }

        and(
                column + " >= ?",
                value
        );
    }

    public void andInCast(
            String column,
            List<?> values,
            String sqlType
    ) {

        if (values == null || values.isEmpty()) {
            return;
        }

        String placeholders = String.join(
                ", ",
                values.stream()
                        .map(value -> "?::" + sqlType)
                        .toList()
        );

        conditions.add(
                column +
                        " IN (" +
                        placeholders +
                        ")"
        );

        parameters.addAll(values);
    }

    // =========================================================
    // LESS THAN
    // =========================================================

    public void andLessThan(
            String column,
            Object value
    ) {

        if (value == null) {
            return;
        }

        and(
                column + " < ?",
                value
        );
    }


    // =========================================================
    // LESS THAN OR EQUAL
    // =========================================================

    public void andLessThanOrEqual(
            String column,
            Object value
    ) {

        if (value == null) {
            return;
        }

        and(
                column + " <= ?",
                value
        );
    }


    // =========================================================
    // WHERE
    // =========================================================

    public String whereClause() {

        if (conditions.isEmpty()) {
            return "";
        }

        return "WHERE " +
                String.join(
                        " AND ",
                        conditions
                );
    }


    // =========================================================
    // PARAMETERS
    // =========================================================

    public List<Object> parameters() {
        return parameters;
    }


    // =========================================================
    // STATE
    // =========================================================

    public boolean hasConditions() {
        return !conditions.isEmpty();
    }


    public boolean hasParameters() {
        return !parameters.isEmpty();
    }


    public void clear() {

        conditions.clear();
        parameters.clear();
    }


    // =========================================================
    // PLACEHOLDERS
    // =========================================================

    private String placeholders(
            int count
    ) {

        StringJoiner joiner =
                new StringJoiner(", ");

        for (int i = 0; i < count; i++) {
            joiner.add("?");
        }

        return joiner.toString();
    }


    // =========================================================
    // JDBC - STRING
    // =========================================================

    public void setNullableString(
            PreparedStatement statement,
            int index,
            String value
    ) throws SQLException {

        if (value == null) {

            statement.setNull(
                    index,
                    Types.VARCHAR
            );

            return;
        }

        statement.setString(
                index,
                value
        );
    }


    // =========================================================
    // JDBC - INTEGER
    // =========================================================

    public void setNullableInteger(
            PreparedStatement statement,
            int index,
            Integer value
    ) throws SQLException {

        if (value == null) {

            statement.setNull(
                    index,
                    Types.INTEGER
            );

            return;
        }

        statement.setInt(
                index,
                value
        );
    }


    // =========================================================
    // JDBC - LONG
    // =========================================================

    public void setNullableLong(
            PreparedStatement statement,
            int index,
            Long value
    ) throws SQLException {

        if (value == null) {

            statement.setNull(
                    index,
                    Types.BIGINT
            );

            return;
        }

        statement.setLong(
                index,
                value
        );
    }


    // =========================================================
    // JDBC - BOOLEAN
    // =========================================================

    public void setNullableBoolean(
            PreparedStatement statement,
            int index,
            Boolean value
    ) throws SQLException {

        if (value == null) {

            statement.setNull(
                    index,
                    Types.BOOLEAN
            );

            return;
        }

        statement.setBoolean(
                index,
                value
        );
    }


    // =========================================================
    // JDBC - OFFSET DATETIME
    // =========================================================

    public void setNullableOffsetDateTime(
            PreparedStatement statement,
            int index,
            OffsetDateTime value
    ) throws SQLException {

        if (value == null) {

            statement.setNull(
                    index,
                    Types.TIMESTAMP_WITH_TIMEZONE
            );

            return;
        }

        statement.setObject(
                index,
                value
        );
    }


    // =========================================================
    // RESULT SET - OFFSET DATETIME
    // =========================================================

    public OffsetDateTime getOffsetDateTime(
            ResultSet resultSet,
            String column
    ) throws SQLException {

        return resultSet.getObject(
                column,
                OffsetDateTime.class
        );
    }


    // =========================================================
    // RESULT SET - INTEGER
    // =========================================================

    public Integer getNullableInteger(
            ResultSet resultSet,
            String column
    ) throws SQLException {

        Object value =
                resultSet.getObject(column);

        if (value == null) {
            return null;
        }

        return ((Number) value)
                .intValue();
    }


    // =========================================================
    // RESULT SET - LONG
    // =========================================================

    public Long getNullableLong(
            ResultSet resultSet,
            String column
    ) throws SQLException {

        Object value =
                resultSet.getObject(column);

        if (value == null) {
            return null;
        }

        return ((Number) value)
                .longValue();
    }


    // =========================================================
    // RESULT SET - BOOLEAN
    // =========================================================

    public Boolean getNullableBoolean(
            ResultSet resultSet,
            String column
    ) throws SQLException {

        Object value =
                resultSet.getObject(column);

        if (value == null) {
            return null;
        }

        return (Boolean) value;
    }

    // =========================================================
// INNER JOIN
// =========================================================

    public void innerJoin(
            String table,
            String condition
    ) {
        joins.add(
                "INNER JOIN " + table + " ON " + condition
        );
    }


// =========================================================
// LEFT JOIN
// =========================================================

    public void leftJoin(
            String table,
            String condition
    ) {
        joins.add(
                "LEFT JOIN " + table + " ON " + condition
        );
    }


// =========================================================
// RIGHT JOIN
// =========================================================

    public void rightJoin(
            String table,
            String condition
    ) {
        joins.add(
                "RIGHT JOIN " + table + " ON " + condition
        );
    }


// =========================================================
// FULL JOIN
// =========================================================

    public void fullJoin(
            String table,
            String condition
    ) {
        joins.add(
                "FULL JOIN " + table + " ON " + condition
        );
    }


// =========================================================
// CROSS JOIN
// =========================================================

    public void crossJoin(
            String table
    ) {
        joins.add(
                "CROSS JOIN " + table
        );
    }

}