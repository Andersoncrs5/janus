package org.janus.modules.authentication.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import org.janus.modules.authentication.domain.entity.LoginAttemptEntity;
import org.janus.modules.authentication.port.out.LoginAttemptRepository;
import org.janus.shared.domain.queries.Query;
import org.janus.shared.infrastructure.persistence.jdbc.GenericJdbcRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LoginAttemptRepositoryJDBC
        extends GenericJdbcRepository<LoginAttemptEntity>
        implements LoginAttemptRepository {

    @Override
    public LoginAttemptEntity save(LoginAttemptEntity entity) {
        return insert(entity);
    }

    @Override
    public LoginAttemptEntity insert(LoginAttemptEntity entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        OffsetDateTime createdAt =
                entity.getCreatedAt() != null
                        ? entity.getCreatedAt()
                        : OffsetDateTime.now();

        return new Query.Insert(getTableName())
                .value("id", entity.getId())
                .value("user_id", entity.getUserId())
                .value("email_attempted", entity.getEmailAttempted())
                .value("ip_address", entity.getIpAddress())
                .value("user_agent", entity.getUserAgent())
                .value(
                        "success",
                        entity.getSuccess() != null
                                ? entity.getSuccess()
                                : Boolean.FALSE
                )
                .value("failure_reason", entity.getFailureReason())
                .value("created_at", createdAt)
                .executeAndMap(
                        dataSource,
                        List.of("created_at"),
                        rs -> {
                            entity.setCreatedAt(
                                    rs.getObject(
                                            "created_at",
                                            OffsetDateTime.class
                                    )
                            );
                            return entity;
                        }
                );
    }

    @Override
    public long countFailedAttemptsByUserId(
            UUID userId,
            OffsetDateTime since
    ) {
        return new Query.Count(getTableName())
                .andEqual("user_id", userId)
                .andEqual("success", false)
                .and("created_at >= ?", since)
                .execute(dataSource);
    }

    @Override
    public long countFailedAttemptsByIpAddress(
            String ipAddress,
            OffsetDateTime since
    ) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return 0L;
        }

        return new Query.Count(getTableName())
                .andEqual("ip_address", ipAddress)
                .andEqual("success", false)
                .and("created_at >= ?", since)
                .execute(dataSource);
    }

    @Override
    public long countFailedAttemptsByEmail(
            String email,
            OffsetDateTime since
    ) {
        if (email == null || email.isBlank()) {
            return 0L;
        }

        return new Query.Count(getTableName())
                .whereIgnoreCase("email_attempted", email)
                .andEqual("success", false)
                .and("created_at >= ?", since)
                .execute(dataSource);
    }

    @Override
    public List<LoginAttemptEntity> findRecentByUserId(
            UUID userId,
            int limit
    ) {
        return new Query.Select(getTableName())
                .where("user_id", userId)
                .orderByDesc("created_at")
                .limit(limit)
                .findAll(dataSource, this::mapRow);
    }

    @Override
    public List<LoginAttemptEntity> findRecentByIpAddress(
            String ipAddress,
            int limit
    ) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return List.of();
        }

        return new Query.Select(getTableName())
                .where("ip_address", ipAddress)
                .orderByDesc("created_at")
                .limit(limit)
                .findAll(dataSource, this::mapRow);
    }

    @Override
    public List<LoginAttemptEntity> findRecentByEmail(
            String email,
            int limit
    ) {
        if (email == null || email.isBlank()) {
            return List.of();
        }

        return new Query.Select(getTableName())
                .whereIgnoreCase("email_attempted", email)
                .orderByDesc("created_at")
                .limit(limit)
                .findAll(dataSource, this::mapRow);
    }

    @Override
    public boolean existsSuccessfulAttemptByUserIdSince(
            UUID userId,
            OffsetDateTime since
    ) {
        return new Query.Exists(getTableName())
                .andEqual("user_id", userId)
                .andEqual("success", true)
                .and("created_at >= ?", since)
                .execute(dataSource);
    }

    @Override
    protected String getTableName() {
        return LoginAttemptEntity.TABLE_NAME;
    }

    @Override
    protected LoginAttemptEntity mapRow(ResultSet rs)
            throws SQLException {

        LoginAttemptEntity entity = new LoginAttemptEntity();

        mapBaseFields(entity, rs);

        entity.setUserId(
                rs.getObject("user_id", UUID.class)
        );

        entity.setEmailAttempted(
                rs.getString("email_attempted")
        );

        entity.setIpAddress(
                rs.getString("ip_address")
        );

        entity.setUserAgent(
                rs.getString("user_agent")
        );

        entity.setSuccess(
                rs.getBoolean("success")
        );

        entity.setFailureReason(
                rs.getString("failure_reason")
        );

        return entity;
    }
}