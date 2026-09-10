package org.janus.modules.authorization.application.service.userRole;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.application.dto.userRole.request.CreateUserRoleDTO;
import org.janus.modules.authorization.application.mapper.UserRoleMapper;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.authorization.infrastructure.in.userRole.ICreateUserRoleUseCase;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

@ApplicationScoped
@RequiredArgsConstructor
public class CreateUserRoleUseCase implements ICreateUserRoleUseCase {

    private final UserRoleRepository repository;
    private final UserRoleMapper mapper;

    @Override
    @ResultTransaction
    public Result<UserRoleEntity> execute(CreateUserRoleDTO dto) {
        UserRoleEntity entity = mapper.toEntity(dto);

        try {
            UserRoleEntity inserted = repository.insert(entity);

            return Result.created(inserted);
        } catch (DataIntegrityViolationException e) {
            String message = e.getMessage();

            if (message == null) {
                return DatabaseConstraintHandler.handle(e);
            }

            if (message.toLowerCase().contains("fk_user_roles_user")) {
                return Result.failure(
                        "User not found with id: '" + dto.getUserId() + "'" ,
                        409
                );
            }

            if (message.toLowerCase().contains("fk_user_roles_role")) {
                return Result.failure(
                        "Role not found with id: '" + dto.getRoleId() + "'" ,
                        409
                );
            }

            if (message.toLowerCase().contains("fk_user_roles_assigned_by")) {
                return Result.failure(
                        "User not found with id: '" + dto.getAssignedById() + "'" ,
                        409
                );
            }

            return DatabaseConstraintHandler.handle(e);
        }  catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }

    }

}
