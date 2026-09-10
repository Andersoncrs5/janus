package org.janus.modules.authorization.application.service.permission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.infrastructure.in.permission.IExistsPermissionBySlugUseCase;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.result.Result;

@ApplicationScoped
@RequiredArgsConstructor
public class ExistsPermissionBySlugUseCase implements IExistsPermissionBySlugUseCase {

    private final PermissionRepository repository;

    @Override
    public Result<Boolean> execute(String slug) {
        if (slug == null || slug.isBlank()) {
            return Result.badRequest("Permission slug should be defined");
        }

        boolean exists = repository.existsBySlug(slug);
        return Result.success(exists);
    }
}