package org.janus.shared.domain.queries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

@SuppressWarnings("unchecked")
public abstract class QueryBuilder<T extends QueryBuilder<T>> {

    protected final String table;
    protected final List<String> conditions = new ArrayList<>();
    protected final List<Object> parameters = new ArrayList<>();
    protected final List<String> joins = new ArrayList<>();
    protected String orderByClause = "";

    public QueryBuilder(String table) {
        this.table = table;
    }

    // =========================================================
    // CONDITIONS GERAIS (Movidos para cá!)
    // =========================================================

    public T where(String column, Object value) {
        if (value != null) {
            this.conditions.add(column + " = ?");
            this.parameters.add(value);
        }
        return (T) this;
    }

    public T whereIgnoreCase(String column, Object value) {
        if (value != null) {
            this.conditions.add(column.toLowerCase() + " = LOWER(?)");
            this.parameters.add(value);
        }
        return (T) this;
    }

    public T andCustom(String customCondition) {
        this.conditions.add(customCondition);
        return (T) this;
    }

    // =========================================================
    // JOINS
    // =========================================================
    public T innerJoin(String joinTable, String condition) {
        joins.add("INNER JOIN " + joinTable + " ON " + condition);
        return (T) this;
    }

    public T leftJoin(String joinTable, String condition) {
        joins.add("LEFT JOIN " + joinTable + " ON " + condition);
        return (T) this;
    }

    // =========================================================
    // OUTRAS CONDIÇÕES (AND / OR)
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

    public T and(String condition, Object... values) {
        conditions.add(condition);
        parameters.addAll(Arrays.asList(values));
        return (T) this;
    }

    public T or(String condition, Object... values) {
        if (conditions.isEmpty()) {
            conditions.add(condition);
        } else {
            String previous = conditions.removeLast();
            conditions.add("(" + previous + " OR " + condition + ")");
        }
        parameters.addAll(Arrays.asList(values));
        return (T) this;
    }

    public T andEqual(String column, Object value) {
        if (value == null) return (T) this;
        return and(column + " = ?", value);
    }

    public T andSoftDelete() {
        return and("deleted_at IS NULL");
    }

    // =========================================================
    // HELPERS PARA CONSTRUIR SQL
    // =========================================================
    protected String buildWhereClause() {
        return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
    }

    protected String buildJoinsClause() {
        return joins.isEmpty() ? "" : String.join(" ", joins) + " ";
    }


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
    // ORDER BY
    // =========================================================

    public T orderBy(String column, String direction) {
        if (column != null && !column.isBlank()) {
            this.orderByClause = " ORDER BY " + column + " " + (direction != null ? direction : "ASC");
        }
        return (T) this;
    }

    public T orderByDesc(String column) {
        return orderBy(column, "DESC");
    }

    public T orderByAsc(String column) {
        return orderBy(column, "ASC");
    }

    // Atualize o helper para concatenar o ORDER BY ao SQL
    protected String buildOrderByClause() {
        return orderByClause;
    }


}