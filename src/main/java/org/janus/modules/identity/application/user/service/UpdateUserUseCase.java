package org.janus.modules.identity.application.user.service;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.identity.application.user.dto.request.UpdateUserDTO;
import org.janus.modules.identity.application.user.mapper.UserMapper;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.in.user.IUpdateUserUseCase;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.shared.domain.base.service.BaseUseCase;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@ApplicationScoped
@RequiredArgsConstructor
public class UpdateUserUseCase
        extends BaseUseCase<UserEntity>
        implements IUpdateUserUseCase {

    private final UserRepository repository;
    private final UserMapper mapper;

    private static final Map<String, Supplier<Result<UserEntity>>> CONSTRAINT_HANDLERS = Map.of(
            "uk_email_user", () -> Result.conflict("Email already exists"),
            "uk_username_user", () -> Result.conflict("Username already exists"),
            "ck_users_email_not_empty", () -> Result.badRequest("Email cannot be empty"),
            "ck_users_email_lowercase", () -> Result.badRequest("Email must be lowercase"),
            "ck_users_username_not_empty", () -> Result.badRequest("Username cannot be empty"),
            "ck_users_username_no_spaces", () -> Result.badRequest("Username cannot contain spaces"),
            "ck_users_full_name_not_empty", () -> Result.badRequest("Full name cannot be empty"),
            "ck_users_failed_login_attempts", () -> Result.badRequest("Invalid login attempts counter"),
            "ck_users_version", () -> Result.badRequest("Invalid record version")
    );

    @Override
    protected Map<String, Supplier<Result<UserEntity>>> getConstraintHandlers() {
        return CONSTRAINT_HANDLERS;
    }

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
            return handleConstraintException(e);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}