package org.janus.modules.authentication.application.service.session;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.janus.modules.authentication.application.dto.session.request.CreateSessionDTO;
import org.janus.modules.authentication.application.mapper.SessionMapper;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.modules.authentication.port.in.session.ICreateSessionUseCase;
import org.janus.modules.authentication.port.out.SessionRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

@ApplicationScoped
public class CreateSessionUseCase implements ICreateSessionUseCase {

    @Inject
    private SessionRepository repository;

    @Inject
    private SessionMapper mapper;

    @Override
    @ResultTransaction
    public Result<SessionEntity> execute(CreateSessionDTO dto) {
        if (dto == null)
            return Result.badRequest("Session data cannot be null");

        if (dto.userId() == null)
            return Result.badRequest("User ID cannot be null");

        SessionEntity entity = mapper.toEntity(dto);
        entity.addTime(dto.durationInMinutes());

        if (entity.getIsRevoked() == null) {
            entity.setIsRevoked(false);
        }

        try {
            SessionEntity savedEntity = repository.insert(entity);
            return Result.created(savedEntity);
        } catch (DataIntegrityViolationException e) {
            String message = e.getMessage();

            if (message == null) {
                return DatabaseConstraintHandler.handle(e);
            }

            String lowerMessage = message.toLowerCase();

            if (lowerMessage.contains("fk_sessions_user")) {
                return Result.notFound(
                        "User not found with id: '" + dto.userId() + "'"
                );
            }

            if (lowerMessage.contains("ck_sessions_expires_after_created")) {
                return Result.badRequest(
                        "Expiration date (expiresAt) must be in the future relative to creation time"
                );
            }

            if (lowerMessage.contains("ck_sessions_version")) {
                return Result.badRequest(
                        "Session version cannot be negative"
                );
            }

            return DatabaseConstraintHandler.handle(e);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}