package org.janus.modules.authentication.application.service.session;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authentication.application.dto.session.request.RefreshSessionDTO;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.modules.authentication.port.in.session.IRefreshSessionUseCase;
import org.janus.modules.authentication.port.out.SessionRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

@ApplicationScoped
@RequiredArgsConstructor
public class RefreshSessionUseCase implements IRefreshSessionUseCase {

    private final SessionRepository repository;

    @Override
    @ResultTransaction
    public Result<SessionEntity> execute(RefreshSessionDTO dto) {
        if (dto == null) return Result.badRequest("Session DTO cannot be null");
        if (dto.sessionId() == null) return Result.badRequest("Session ID cannot be null");
        if (dto.userId() == null) return Result.badRequest("User ID cannot be null");

        if (dto.durationInMinutes() == null || dto.durationInMinutes() <= 0) {
            return Result.badRequest("Duration in minutes must be greater than zero");
        }

        SessionEntity session = repository.findBySessionIdAndUserId(dto.sessionId(), dto.userId()).orElse(null);

        if (session == null) {
            return Result.notFound("Session not found");
        }

        if (Boolean.TRUE.equals(session.getIsRevoked())) {
            return Result.badRequest("Session is already revoked");
        }

        session.addTime(dto.durationInMinutes());

        try {
            SessionEntity saved = repository.save(session);
            return Result.ok(saved);
        } catch (DataIntegrityViolationException e) {
            return DatabaseConstraintHandler.handle(e);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}