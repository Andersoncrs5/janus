package org.janus.modules.authorization.application.service.role;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.application.dto.role.filter.RoleFilterDTO;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.infrastructure.in.role.IFindAllRoleUseCase;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.shared.domain.page.Page;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

@ApplicationScoped
@RequiredArgsConstructor
public class FindAllRoleUseCase implements IFindAllRoleUseCase {

    private final RoleRepository repository;

    @Override
    @ResultTransaction
    public Page<RoleEntity> execute(RoleFilterDTO dto) {
        return repository.findAll(dto);
    }

}
