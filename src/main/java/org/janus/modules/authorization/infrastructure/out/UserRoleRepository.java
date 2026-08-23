package org.janus.modules.authorization.infrastructure.out;

import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.shared.domain.base.repository.GenericRepository;

import java.util.UUID;

public interface UserRoleRepository extends GenericRepository<UserRoleEntity, UUID> {

}
