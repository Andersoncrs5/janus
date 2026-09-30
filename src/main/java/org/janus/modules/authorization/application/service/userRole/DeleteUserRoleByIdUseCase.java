package org.janus.modules.authorization.application.service.userRole;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.janus.modules.authorization.infrastructure.in.userRole.IDeleteUserRoleByIdUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.IFindRoleNameByIdUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.IFindRolesOnlyNameByUserIdUseCase;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DeleteUserRoleByIdUseCase implements IDeleteUserRoleByIdUseCase {

    @Inject
    private UserRoleRepository repository;
    @Inject
    private IFindRoleNameByIdUseCase findRoleNameById;
    @Inject
    private IFindRolesOnlyNameByUserIdUseCase findRolesOnlyNameByUserId;

    @Override
    @ResultTransaction
    public Result<Void> execute(UUID id, UUID userId) {
        if (id == null || userId == null) {
            return Result.badRequest("UserID and Id are required");
        }

        try {
            Result<List<String>> operatorRolesResult = findRolesOnlyNameByUserId.execute(userId);
            if (operatorRolesResult.isFailure()) {
                return Result.failure(operatorRolesResult.getFirstError(), operatorRolesResult.getStatus());
            }

            Result<String> roleNameResult = findRoleNameById.execute(id);
            if (roleNameResult.isFailure()) {
                return Result.failure(roleNameResult.getFirstError(), roleNameResult.getStatus());
            }

            String roleName = roleNameResult.getData();
            List<String> operatorRoles = operatorRolesResult.getData();

            if ("MASTER".equalsIgnoreCase(roleName)) {
                return Result.failure("MASTER role cannot be deleted", 403);
            }

            boolean isOperatorMaster = operatorRoles.stream()
                    .anyMatch("MASTER"::equalsIgnoreCase);

            if ("ADMINISTRADOR".equalsIgnoreCase(roleName) && !isOperatorMaster) {
                return Result.failure("Only MASTER users can revoke the ADMINISTRADOR role", 403);
            }

            int deleted = repository.deleteById(id);

            if (deleted <= 0) {
                return Result.notFound("User Role not found");
            }

            if (deleted > 1) {
                return Result.failure("Unexpected state: multiple user roles were deleted", 500);
            }

            return Result.success();
        } catch (DataIntegrityViolationException e) {
            return DatabaseConstraintHandler.handle(e);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}