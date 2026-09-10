package org.janus.modules.authorization.application.service.userRole;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.application.dto.userRole.request.CreateUserRoleDTO;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.authorization.infrastructure.in.userRole.IDeleteRoleByIdUseCase;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class DeleteRoleByIdUseCase implements IDeleteRoleByIdUseCase {

    private final UserRoleRepository repository;

    @Override
    @ResultTransaction
    public Result<Void> execute(UUID id) {

        try {
            int deleted = repository.deleteById(id);

            if (deleted <= 0) {
                return Result.notFound("User Role not found");
            }

            if (deleted > 1) {
                return Result.notFound("More of one user role deleted");
            }

            return Result.success();
        } catch (DataIntegrityViolationException e) {
            String message = e.getMessage();

            if (message == null) {
                return DatabaseConstraintHandler.handle(e);
            }

            return DatabaseConstraintHandler.handle(e);
        }  catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }

    }

}
