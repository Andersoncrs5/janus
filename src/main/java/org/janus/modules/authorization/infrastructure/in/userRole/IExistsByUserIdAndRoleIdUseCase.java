package org.janus.modules.authorization.infrastructure.in.userRole;

import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IExistsByUserIdAndRoleIdUseCase {
    Result<Boolean> execute(UUID userId, UUID roleId);
}
