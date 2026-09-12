package org.janus.modules.authorization.application.service.permission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.application.dto.permission.filter.PermissionFilterDTO;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.infrastructure.in.permission.IFindAllPermissionUseCase;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.page.Page;

@ApplicationScoped
@RequiredArgsConstructor
public class FindAllPermissionUseCase implements IFindAllPermissionUseCase {

    private final PermissionRepository repository;

    @Override
    public Page<PermissionEntity> execute(PermissionFilterDTO dto) {
        return repository.findAll(dto);
    }
}
