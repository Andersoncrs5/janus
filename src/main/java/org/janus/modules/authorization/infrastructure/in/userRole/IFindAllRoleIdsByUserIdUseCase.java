package org.janus.modules.authorization.infrastructure.in.userRole;

import org.janus.shared.domain.result.Result;

import java.util.List;
import java.util.UUID;

public interface IFindAllRoleIdsByUserIdUseCase {
    Result<List<UUID>> execute(UUID userId);
}
