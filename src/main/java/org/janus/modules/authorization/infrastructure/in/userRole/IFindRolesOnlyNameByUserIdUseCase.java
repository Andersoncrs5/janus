package org.janus.modules.authorization.infrastructure.in.userRole;

import org.janus.shared.domain.result.Result;

import java.util.List;
import java.util.UUID;

public interface IFindRolesOnlyNameByUserIdUseCase {
    Result<List<String>> execute(UUID userId);
}
