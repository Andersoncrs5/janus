package org.janus.modules.authentication.application.service.session;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.modules.authentication.port.in.session.IRevokeSessionByIdUseCase;
import org.janus.modules.authentication.port.out.SessionRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
public class RevokeSessionByIdUseCase implements IRevokeSessionByIdUseCase {

    @Inject
    private SessionRepository repository;

    @Override
    @ResultTransaction
    public Result<Void> execute(UUID sessionId, UUID userId) {
        if (sessionId == null)
            return Result.badRequest("Session ID cannot be null");

        if (userId == null)
            return Result.badRequest("User ID cannot be null");

        SessionEntity session = repository.findBySessionIdAndUserId(sessionId, userId).orElse(null);

        if (session == null)
            return Result.notFound("Session not found");

        if (Boolean.TRUE.equals(session.getIsRevoked())) {
            return Result.badRequest("Session is already revoked");
        }

        session.setIsRevoked(true);

        try {
            repository.save(session);
            return Result.noContent();
        } catch (DataIntegrityViolationException e) {
            return DatabaseConstraintHandler.handle(e);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}