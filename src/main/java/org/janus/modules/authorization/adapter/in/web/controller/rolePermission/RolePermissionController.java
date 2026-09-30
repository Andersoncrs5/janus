package org.janus.modules.authorization.adapter.in.web.controller.rolePermission;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.janus.modules.authorization.application.dto.rolePermission.filter.RolePermissionFilterDTO;
import org.janus.modules.authorization.application.dto.rolePermission.request.CreateRolePermissionDTO;
import org.janus.modules.authorization.application.dto.rolePermission.request.UpdateRolePermissionDTO;
import org.janus.modules.authorization.application.dto.rolePermission.response.RolePermissionDTO;
import org.janus.modules.authorization.application.mapper.RolePermissionMapper;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.in.rolePermission.*;
import org.janus.shared.domain.api.ResponseHTTP;
import org.janus.shared.domain.page.Page;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.idempotency.annotation.Idempotent;

import java.util.UUID;

@Authenticated
@Path("v1/role-permission")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RolePermissionController {

    @Inject
    private IFindAllRolePermissionsUseCase findAllRolePermissionsByRoleIdUseCase;
    @Inject
    private ICreateRolePermissionUseCase createRolePermissionUse;
    @Inject
    private IDeleteRolePermissionByIdUseCase deleteRolePermissionById;
    @Inject
    private IExistsRolePermissionByRoleIdAndPermissionIdUseCase existsRolePermissionByRoleIdAndPermissionId;
    @Inject
    private IFindAllRolePermissionsUseCase findAllRolePermissions;
    @Inject
    private IUpdateRolePermissionUseCase updateRolePermission;

    @Inject
    private JsonWebToken jwt;
    @Inject
    private RolePermissionMapper mapper;

    @GET
    @Idempotent
    @RunOnVirtualThread
    public Response findAll(
            @BeanParam RolePermissionFilterDTO dto
    ) {
        Result<Page<RolePermissionEntity>> result = findAllRolePermissionsByRoleIdUseCase.execute(dto);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        Page<RolePermissionDTO> responsePage = result.getData().map(mapper::toDTO);

        return Response.status(200)
                .entity(responsePage)
                .build();
    }

    @GET
    @Idempotent
    @RunOnVirtualThread
    @Path("/exists/role/permission/{roleId}/{permissionId}")
    public Response exists(
            UUID roleId,
            UUID permissionId,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<Boolean> result = existsRolePermissionByRoleIdAndPermissionId.execute(roleId, permissionId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), idempotencyKey, "OK"))
                .build();
    }

    @POST
    @Idempotent
    @RunOnVirtualThread
    public Response create(
            CreateRolePermissionDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        Result<RolePermissionEntity> result = createRolePermissionUse.execute(dto, userId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        var role = mapper.toDTO(result.getData());

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(role, idempotencyKey, "Permission added to role!"))
                .build();
    }

    @PATCH
    @Idempotent
    @Path("/{id}")
    @RunOnVirtualThread
    public Response update(
            UUID id,
            UpdateRolePermissionDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<RolePermissionEntity> result = this.updateRolePermission.execute(dto, id);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        var role = mapper.toDTO(result.getData());

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(role, idempotencyKey, "OK"))
                .build();
    }

    @DELETE
    @Idempotent
    @Path("/{id}")
    @RunOnVirtualThread
    public Response delete(
            UUID id,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        var result = deleteRolePermissionById.execute(id);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(null, idempotencyKey, "Removed!!"))
                .build();
    }

}
