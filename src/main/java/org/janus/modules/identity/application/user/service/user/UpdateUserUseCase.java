package org.janus.modules.identity.application.user.service.user;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.identity.application.user.dto.UpdateUserDTO;
import org.janus.modules.identity.application.user.mapper.UserMapper;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.modules.identity.ports.in.user.IUpdateUserUseCase;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class UpdateUserUseCase implements IUpdateUserUseCase {

    private final UserRepository repository;
    private final UserMapper mapper;

    @Override
    @ResultTransaction
    public Result<UserEntity> execute(UUID id, UpdateUserDTO dto) {
        Optional<UserEntity> userOptional = repository.findById(id);

        if (userOptional.isEmpty()) {
            return Result.notFound("User not found");
        }

        UserEntity user = userOptional.get();
        mapper.updateEntityFromDto(dto, user);

        try {
            UserEntity updated = repository.save(user);

            return Result.success(updated);
        } catch (DataIntegrityViolationException e) {

            String message = e.getMessage();

            if (message == null) {
                return DatabaseConstraintHandler.handle(e);
            }

            if (message.toLowerCase().contains("uk_email_user")) {
                return Result.failure(
                        "Email already exists",
                        409
                );
            }

            if (message.toLowerCase().contains("uk_username_user")) {
                return Result.failure(
                        "Username already exists",
                        409
                );
            }

            return DatabaseConstraintHandler.handle(e);

        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}