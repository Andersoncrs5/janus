package org.janus.modules.identity.application.user.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.janus.modules.identity.application.user.dto.filter.UserFilterDTO;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.in.user.IFindAllUsersUseCase;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.shared.domain.base.service.BaseUseCase;
import org.janus.shared.domain.page.Page;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

@ApplicationScoped
public class FindAllUsersUseCase extends BaseUseCase<Page<UserEntity>> implements IFindAllUsersUseCase {

    private final UserRepository userRepository;

    @Inject
    public FindAllUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @ResultTransaction
    @PermissionsAllowed("users:read")
    public Result<Page<UserEntity>> execute(UserFilterDTO filter) {
        return executeSafely(() -> {
            Page<UserEntity> usersPage = userRepository.findAll(filter);
            return Result.success(usersPage);
        });
    }
}