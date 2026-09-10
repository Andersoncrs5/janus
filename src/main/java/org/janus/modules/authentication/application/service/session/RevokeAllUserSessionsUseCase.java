package org.janus.modules.authentication.application.service.session;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.janus.modules.authentication.port.in.session.IRevokeAllUserSessionsUseCase;
import org.janus.modules.authentication.port.out.SessionRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped

public class RevokeAllUserSessionsUseCase implements IRevokeAllUserSessionsUseCase {

    @Inject
    private SessionRepository repository;

    @Override
    @ResultTransaction
    public Result<Integer> execute(UUID userId) {
        if (userId == null)
            return Result.badRequest("User ID cannot be null");

        try {
            int revoked = this.repository.revokeAllByUserId(userId);

            return Result.success(revoked, 200);
        } catch (DataIntegrityViolationException e) {
            return DatabaseConstraintHandler.handle(e);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

}
