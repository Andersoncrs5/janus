package org.janus.modules.authentication.application.service.loginAttempts;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authentication.application.dto.loginAttempts.request.CreateLoginAttemptDTO;
import org.janus.modules.authentication.application.mapper.LoginAttemptsMapper;
import org.janus.modules.authentication.domain.entity.LoginAttemptEntity;
import org.janus.modules.authentication.port.in.loginAttempts.ICreateLoginAttemptsUseCase;
import org.janus.modules.authentication.port.out.LoginAttemptRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class CreateLoginAttemptsUseCase implements ICreateLoginAttemptsUseCase {

    private final LoginAttemptRepository repository;
    private final LoginAttemptsMapper mapper;

    @Override
    @ResultTransaction
    public Result<LoginAttemptEntity> execute(CreateLoginAttemptDTO dto, UUID userId) {
        if (dto == null) {
            return Result.badRequest("Login Attempt data cannot be null");
        }

        LoginAttemptEntity attempt = mapper.toEntity(dto);
        attempt.setUserId(userId);

        if (attempt.getEmailAttempted() != null) {
            attempt.setEmailAttempted(attempt.getEmailAttempted().toLowerCase().trim());
        }

        try {
            LoginAttemptEntity inserted = repository.insert(attempt);
            return Result.created(inserted);
        } catch (DataIntegrityViolationException e) {
            return handleConstraintViolation(e, dto, userId);
        } catch (RuntimeException e) {
            throw new InternalServerErrorException("Error executing INSERT for table: login_attempts", e);
        }
    }

    private Result<LoginAttemptEntity> handleConstraintViolation(
            DataIntegrityViolationException e,
            CreateLoginAttemptDTO dto,
            UUID userId
    ) {
        String message = e.getMessage();
        if (message == null) {
            return DatabaseConstraintHandler.handle(e);
        }

        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("fk_login_attempts_user")) {
            return Result.notFound("User not found with id: '" + userId + "'");
        }
        if (lowerMessage.contains("ck_login_attempts_email_not_empty")) {
            return Result.badRequest("Email attempted cannot be empty or blank");
        }
        if (lowerMessage.contains("ck_login_attempts_email_lowercase")) {
            return Result.badRequest("Email attempted must be in lowercase");
        }
        if (lowerMessage.contains("ck_login_attempts_identifier_required")) {
            return Result.badRequest("At least user ID or email attempted must be provided");
        }
        if (lowerMessage.contains("ck_login_attempts_failure_reason")) {
            return Result.badRequest("Failure reason is required for failed attempts and must be null for successful ones");
        }
        if (lowerMessage.contains("ck_login_attempts_ip_not_empty")) {
            return Result.badRequest("IP address cannot be empty or blank");
        }
        if (lowerMessage.contains("ck_login_attempts_user_agent_not_empty")) {
            return Result.badRequest("User agent cannot be empty or blank");
        }

        return DatabaseConstraintHandler.handle(e);
    }
}