package org.janus.modules.identity.application.userCredentials.service;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.identity.application.userCredentials.dto.UpdateUserCredentialsDTO;
import org.janus.modules.identity.application.userCredentials.mapper.UserCredentialsMapper;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.ports.in.userCredentials.IUpdateUserCredentialsUseCase;
import org.janus.modules.identity.ports.out.UserCredentialRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.enums.PasswordAlgorithmEnum;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.domain.security.PasswordEncoderPort;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class UpdateUserCredentialsUseCase implements IUpdateUserCredentialsUseCase {

    private final UserCredentialRepository repository;
    private final UserCredentialsMapper mapper;
    private final PasswordEncoderPort passwordEncoder;

    @Override
    @ResultTransaction
    public Result<UserCredentialsEntity> execute(UUID userId, UpdateUserCredentialsDTO dto) {
        Optional<UserCredentialsEntity> credentialsOptional = repository.findByUserId(userId);

        if (credentialsOptional.isEmpty()) {
            return Result.notFound("User credentials not found");
        }

        UserCredentialsEntity credentials = credentialsOptional.get();

        String hashedPassword = passwordEncoder.encode(dto.passwordHash());

        mapper.updateEntityFromDto(dto, credentials);

        credentials.setPasswordHash(hashedPassword);

        if (dto.algorithm() != null) {
            credentials.setAlgorithm(dto.algorithm().getValue());
        } else if (credentials.getAlgorithm() == null) {
            credentials.setAlgorithm(PasswordAlgorithmEnum.ARGON2ID.getValue());
        }

        try {
            UserCredentialsEntity updated = repository.save(credentials);
            return Result.success(updated);

        } catch (DataIntegrityViolationException e) {
            return DatabaseConstraintHandler.handle(e);

        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}