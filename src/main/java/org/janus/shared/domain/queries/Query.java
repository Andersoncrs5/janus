package org.janus.shared.domain.queries;

import jakarta.enterprise.context.ApplicationScoped;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class Query {

    private final List<String> conditions = new ArrayList<>();
    private final List<Object> parameters = new ArrayList<>();

    public void and(String condition, Object value) {
        conditions.add(condition);
        parameters.add(value);
    }

    public String whereClause() {
        if (conditions.isEmpty()) {
            return "";
        }

        return "WHERE " + String.join(" AND ", conditions);
    }

    public List<Object> parameters() {
        return parameters;
    }

    public void setNullableString(
            PreparedStatement statement,
            int index,
            String value
    ) throws SQLException {

        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
            return;
        }

        statement.setString(index, value);
    }


    public void setNullableOffsetDateTime(
            PreparedStatement statement,
            int index,
            OffsetDateTime value
    ) throws SQLException {

        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP_WITH_TIMEZONE);
            return;
        }

        statement.setObject(index, value);
    }


    public OffsetDateTime getOffsetDateTime(
            ResultSet resultSet,
            String column
    ) throws SQLException {

        return resultSet.getObject(
                column,
                OffsetDateTime.class
        );
    }
}
