package org.janus.shared.domain.queries;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ExistsQueryBuilder {

    private final String tableName;
    private final List<String> conditions = new ArrayList<>();
    private final List<Object> parameters = new ArrayList<>();
    private boolean includeSoftDeleteCheck = true;

    private ExistsQueryBuilder(String tableName) {
        this.tableName = tableName;
    }

    public static ExistsQueryBuilder from(String tableName) {
        return new ExistsQueryBuilder(tableName);
    }

    public ExistsQueryBuilder where(String condition, Object value) {
        this.conditions.add(condition);
        this.parameters.add(value);
        return this;
    }

    public ExistsQueryBuilder andWhere(String condition, Object value) {
        return where(condition, value);
    }

    public ExistsQueryBuilder ignoreSoftDelete() {
        this.includeSoftDeleteCheck = false;
        return this;
    }

    public boolean execute(DataSource dataSource) {
        if (includeSoftDeleteCheck) {
            conditions.add("deleted_at IS NULL");
        }

        String whereClause = conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);

        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM %s
                    %s
                )
                """.formatted(tableName, whereClause);

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error executing EXISTS query for table: " + tableName, e);
        }
    }
}