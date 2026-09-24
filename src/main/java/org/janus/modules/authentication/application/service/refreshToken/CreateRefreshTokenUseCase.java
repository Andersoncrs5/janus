package org.janus.modules.authentication.application.service.refreshToken;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.janus.modules.authentication.application.dto.refreshToken.request.CreateRefreshTokenDTO;
import org.janus.modules.authentication.application.mapper.RefreshTokenMapper;
import org.janus.modules.authentication.domain.entity.RefreshTokenEntity;
import org.janus.modules.authentication.port.in.refreshToken.ICreateRefreshTokenUseCase;
import org.janus.modules.authentication.port.out.RefreshTokenRepository;
import org.janus.shared.domain.base64.GeneratorBase64;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.JwtProperties;

import java.time.OffsetDateTime;

@ApplicationScoped
public class CreateRefreshTokenUseCase implements ICreateRefreshTokenUseCase {

    private final RefreshTokenRepository repository;
    private final RefreshTokenMapper mapper;
    private final JwtProperties properties;

    @Inject
    public CreateRefreshTokenUseCase(RefreshTokenRepository repository, RefreshTokenMapper mapper, JwtProperties properties) {
        this.repository = repository;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    public Result<RefreshTokenEntity> execute(CreateRefreshTokenDTO dto) {
        if (dto == null) {
            return Result.badRequest("Refresh token data cannot be null");
        }
        if (dto.sessionId() == null) {
            return Result.badRequest("Session ID cannot be null");
        }
        if (dto.userId() == null) {
            return Result.badRequest("User ID cannot be null");
        }

        RefreshTokenEntity entity = buildEntity(dto);

        try {
            RefreshTokenEntity savedEntity = repository.insert(entity);
            return Result.created(savedEntity);
        } catch (DataIntegrityViolationException e) {
            return handleConstraintViolation(e, dto);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

    private RefreshTokenEntity buildEntity(CreateRefreshTokenDTO dto) {
        RefreshTokenEntity entity = mapper.toEntity(dto);
        if (entity == null) {
            entity = new RefreshTokenEntity();
        }

        if (entity.getSessionId() == null) entity.setSessionId(dto.sessionId());
        if (entity.getUserId() == null) entity.setUserId(dto.userId());
        if (entity.getIsUsed() == null) entity.setIsUsed(false);
        if (entity.getIsRevoked() == null) entity.setIsRevoked(false);
        if (entity.getTokenHash() == null || entity.getTokenHash().isBlank()) {
            entity.setTokenHash(GeneratorBase64.generateRandomBase64Token());
        }
        if (entity.getExpiresAt() == null) {
            entity.setExpiresAt(OffsetDateTime.now().plusMinutes(properties.exp().refresh()));
        }

        return entity;
    }

    private Result<RefreshTokenEntity> handleConstraintViolation(
            DataIntegrityViolationException e,
            CreateRefreshTokenDTO dto
    ) {
        String message = e.getMessage();
        if (message == null) {
            return DatabaseConstraintHandler.handle(e);
        }

        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("fk_refresh_tokens_session")) {
            return Result.notFound("Session not found with id: '" + dto.sessionId() + "'");
        }
        if (lowerMessage.contains("fk_refresh_tokens_user")) {
            return Result.notFound("User not found with id: '" + dto.userId() + "'");
        }
        if (lowerMessage.contains("uk_refresh_tokens_token_hash")) {
            return Result.conflict("Refresh token hash already exists");
        }
        if (lowerMessage.contains("ck_refresh_tokens_token_hash_not_empty")) {
            return Result.badRequest("Token hash cannot be empty");
        }
        if (lowerMessage.contains("ck_refresh_tokens_expires_after_created")) {
            return Result.badRequest("Expiration date (expiresAt) must be in the future relative to creation time");
        }
        if (lowerMessage.contains("ck_refresh_tokens_version")) {
            return Result.badRequest("Refresh token version cannot be negative");
        }

        return DatabaseConstraintHandler.handle(e);
    }
}