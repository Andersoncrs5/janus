package org.janus.modules.authorization.application.service.role;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.application.dto.role.request.CreateRoleDTO;
import org.janus.modules.authorization.application.mapper.RoleMapper;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.infrastructure.in.role.ICreateRoleUseCase;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

@ApplicationScoped
@RequiredArgsConstructor
public class CreateRoleUseCase implements ICreateRoleUseCase {

    private final RoleRepository repository;
    private final RoleMapper mapper;

    @Override
    @ResultTransaction
    public Result<RoleEntity> execute(CreateRoleDTO dto) {
        RoleEntity role = mapper.toEntity(dto);

        try {
            RoleEntity saved = repository.insert(role);

            return Result.created(saved);
        } catch (DataIntegrityViolationException e) {
            String message = e.getMessage();

            if (message == null) {
                return DatabaseConstraintHandler.handle(e);
            }

            if (message.toLowerCase().contains("uk_roles_slug")) {
                return Result.failure(
                        "Role already exists with slug: '" + dto.slug() + "'" ,
                        409
                );
            }

            if (message.toLowerCase().contains("uk_roles_name")) {
                return Result.failure(
                        "Role already exists with name: '" + dto.name() + "'" ,
                        409
                );
            }

            return DatabaseConstraintHandler.handle(e);
        }  catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }


}
