package org.janus.shared.domain.queries;

import jakarta.enterprise.context.ApplicationScoped;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@ApplicationScoped
public class Query {

    private final List<String> conditions =
            new ArrayList<>();

    private final List<Object> parameters =
            new ArrayList<>();


    // =========================================================
    // AND
    // =========================================================

    public void and(
            String condition,
            Object... values
    ) {

        conditions.add(condition);

        for (Object value : values) {
            parameters.add(value);
        }
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
                    conditions.remove(
                            conditions.size() - 1
                    );

            conditions.add(
                    "(" +
                            previous +
                            " OR " +
                            condition +
                            ")"
            );
        }

        for (Object value : values) {
            parameters.add(value);
        }
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
}