package org.janus.modules.identity.application.user.service;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.identity.application.user.dto.request.CreateUserDTO;
import org.janus.modules.identity.application.user.mapper.UserMapper;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.in.user.ICreateUserUseCase;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.shared.domain.base.service.BaseUseCase;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.Map;
import java.util.function.Supplier;

@ApplicationScoped
@RequiredArgsConstructor
public class CreateUserUseCase extends BaseUseCase<UserEntity> implements ICreateUserUseCase {

    private final UserRepository repository;
    private final UserMapper mapper;

    private Map<String, Supplier<Result<UserEntity>>> buildConstraintHandlers(CreateUserDTO dto) {
        return Map.ofEntries(
                Map.entry("uk_email_user", () -> Result.failure("Email: '" + dto.getEmail() + "' already exists", 409)),
                Map.entry("uk_users_active_email", () -> Result.failure("Email: '" + dto.getEmail() + "' already exists", 409)),
                Map.entry("uk_username_user", () -> Result.failure("Username: '" + dto.getUsername() + "' already exists", 409)),
                Map.entry("uk_users_active_username", () -> Result.failure("Username: '" + dto.getUsername() + "' already exists", 409)),
                Map.entry("ck_users_email_lowercase", () -> Result.badRequest("Email should be lowercase")),
                Map.entry("ck_users_email_not_empty", () -> Result.badRequest("Email should be defined")),
                Map.entry("ck_users_username_not_empty", () -> Result.badRequest("Username should be defined")),
                Map.entry("ck_users_username_no_spaces", () -> Result.badRequest("Username cannot contain spaces")),
                Map.entry("ck_users_full_name_not_empty", () -> Result.badRequest("Full name should be defined")),
                Map.entry("ck_users_failed_login_attempts", () -> Result.badRequest("Failed login attempts cannot be negative")),
                Map.entry("ck_users_version", () -> Result.badRequest("Version cannot be negative"))
        );
    }

    @Override
    @ResultTransaction
    public Result<UserEntity> execute(CreateUserDTO dto) {
        if (dto == null) {
            return Result.badRequest("User payload cannot be null");
        }

        UserEntity user = mapper.toEntity(dto);
        normalizeEmail(user);

        try {
            UserEntity insertedUser = repository.insert(user);
            return Result.success(insertedUser);
        } catch (DataIntegrityViolationException e) {
            return handleConstraintException(e, buildConstraintHandlers(dto));
        } catch (Exception e) {
            DataIntegrityViolationException integrityException = findDataIntegrityException(e);
            if (integrityException != null) {
                return handleConstraintException(integrityException, buildConstraintHandlers(dto));
            }
            throw new InternalServerErrorException("Error executing INSERT for table: users", e);
        }
    }

    private void normalizeEmail(UserEntity user) {
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim().toLowerCase());
        }
    }

    private DataIntegrityViolationException findDataIntegrityException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DataIntegrityViolationException integrityException) {
                return integrityException;
            }
            current = current.getCause();
        }
        return null;
    }
}