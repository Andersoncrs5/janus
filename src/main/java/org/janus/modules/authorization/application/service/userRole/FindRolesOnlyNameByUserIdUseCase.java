package org.janus.modules.authorization.application.service.userRole;

import jakarta.enterprise.context.ApplicationScoped;
import org.janus.modules.authorization.infrastructure.in.userRole.IFindRolesOnlyNameByUserIdUseCase;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FindRolesOnlyNameByUserIdUseCase implements IFindRolesOnlyNameByUserIdUseCase {

    private final UserRoleRepository repository;

    public FindRolesOnlyNameByUserIdUseCase(UserRoleRepository repository) {
        this.repository = repository;
    }

    public Result<List<String>> execute(UUID userId) {
        if (userId == null) {
            return Result.failure("User ID cannot be null", 400);
        }

        try {
            List<String> roleNames = repository.findRolesOnlyNameByUserId(userId);
            return Result.success(roleNames, 200);
        } catch (Exception e) {
            throw new InternalServerErrorException(
                    "Error retrieving role names for userId: " + userId,
                    e
            );
        }
    }
}