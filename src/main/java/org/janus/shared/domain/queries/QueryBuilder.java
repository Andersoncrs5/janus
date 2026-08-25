package org.janus.shared.domain.queries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unchecked")
public abstract class QueryBuilder<T extends QueryBuilder<T>> {

    protected final String table;
    protected final List<String> conditions = new ArrayList<>();
    protected final List<Object> parameters = new ArrayList<>();
    protected final List<String> joins = new ArrayList<>();

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
}