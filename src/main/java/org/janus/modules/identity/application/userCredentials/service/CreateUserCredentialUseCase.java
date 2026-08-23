package org.janus.modules.identity.application.userCredentials.service;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.identity.application.userCredentials.dto.CreateUserCredentialsDTO;
import org.janus.modules.identity.application.userCredentials.mapper.UserCredentialsMapper;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.ports.in.userCredentials.ICreateUserCredentialUseCase;
import org.janus.modules.identity.ports.out.UserCredentialRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.enums.PasswordAlgorithmEnum;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.domain.security.PasswordEncoderPort;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class CreateUserCredentialUseCase implements ICreateUserCredentialUseCase {

    private final UserCredentialRepository repository;
    private final UserCredentialsMapper mapper;
    private final PasswordEncoderPort passwordEncoder;

    @Override
    @ResultTransaction
    public Result<UserCredentialsEntity> execute(CreateUserCredentialsDTO dto, UUID userId) {
        UserCredentialsEntity credentials = mapper.toEntity(dto);
        credentials.setUserId(userId);

        String hashedPassword = passwordEncoder.encode(dto.passwordHash());
        credentials.setPasswordHash(hashedPassword);

        if (credentials.getAlgorithm() == null) {
            credentials.setAlgorithm(PasswordAlgorithmEnum.ARGON2ID.getValue());
        }

        try {
            UserCredentialsEntity inserted = repository.insert(credentials);
            return Result.success(inserted);

        } catch (DataIntegrityViolationException e) {

            String message = e.getMessage();

            if (message == null) {
                return DatabaseConstraintHandler.handle(e);
            }

            if (message.toLowerCase().contains("uk_user_id_credentials_user")) {
                return Result.failure(
                        "User already have one credential",
                        409
                );
            }

            if (message.toLowerCase().contains("fk_user_credentials_user")) {
                return Result.failure(
                        "User not found",
                        404
                );
            }

            return DatabaseConstraintHandler.handle(e);

        }  catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}
