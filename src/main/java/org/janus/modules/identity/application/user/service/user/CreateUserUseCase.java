package org.janus.modules.identity.application.user.service.user;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.identity.application.user.dto.CreateUserDTO;
import org.janus.modules.identity.application.user.mapper.UserMapper;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.modules.identity.ports.in.user.ICreateUserUseCase;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

@ApplicationScoped
@RequiredArgsConstructor
public class CreateUserUseCase implements ICreateUserUseCase {

    private final UserRepository repository;
    private final UserMapper mapper;

    @Override
    @ResultTransaction
    public Result<UserEntity> execute(CreateUserDTO dto) {
        UserEntity user = mapper.toEntity(dto);

        try {
            UserEntity inserted = repository.insert(user);

            return Result.success(inserted);
        } catch (DataIntegrityViolationException e) {

            String message = e.getMessage();

            if (message == null) {
                return DatabaseConstraintHandler.handle(e);
            }

            if (message.toLowerCase().contains("uk_email_user")) {
                return Result.failure(
                        "Email: '" + dto.getEmail() + "' already exists",
                        409
                );
            }

            if (message.toLowerCase().contains("uk_username_user")) {
                return Result.failure(
                        "Username: '" + dto.getUsername() + "' already exists",
                        409
                );
            }

            return DatabaseConstraintHandler.handle(e);

        }  catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }


    }
}
