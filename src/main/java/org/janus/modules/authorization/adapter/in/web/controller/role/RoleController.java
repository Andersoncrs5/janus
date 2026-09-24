package org.janus.modules.authorization.adapter.in.web.controller.role;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.janus.modules.authorization.application.dto.role.filter.RoleFilterDTO;
import org.janus.modules.authorization.application.dto.role.request.CreateRoleDTO;
import org.janus.modules.authorization.application.dto.role.request.UpdateRoleDTO;
import org.janus.modules.authorization.application.dto.role.response.RoleDTO;
import org.janus.modules.authorization.application.mapper.RoleMapper;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.infrastructure.in.role.*;
import org.janus.shared.domain.api.ResponseHTTP;
import org.janus.shared.domain.page.Page;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.idempotency.annotation.Idempotent;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

@Authenticated
@Path("/v1/role")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoleController {

    @Inject
    private IExistsRoleByNameUseCase existsRoleByName;

    @Inject
    private IFindAllRoleUseCase findAllRole;

    @Inject
    private IDeleteRoleByIdUseCase roleByIdUseCase;

    @Inject
    private IFindRoleByIdUseCase findRoleById;

    @Inject
    private ICreateRoleUseCase createRole;

    @Inject
    private IUpdateRoleUseCase updateRole;

    @Inject
    private RoleMapper roleMapper;

    @Inject
    JsonWebToken jwt;

    @POST
    @Idempotent
    @RunOnVirtualThread
    @PermissionsAllowed("roles:create")
    public Response create(
            CreateRoleDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<RoleEntity> result = createRole.execute(dto);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        var role = roleMapper.toDTO(result.getData());

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(role, idempotencyKey, "Role created!"))
                .build();
    }

    @GET
    @Idempotent
    @Path("{id}")
    @RunOnVirtualThread
    public Response get(
            UUID id,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<RoleEntity> result = findRoleById.execute(id);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        var role = roleMapper.toDTO(result.getData());

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(role, idempotencyKey, "OK!"))
                .build();
    }

    @DELETE
    @Idempotent
    @Path("{id}")
    @RunOnVirtualThread
    @PermissionsAllowed("roles:delete")
    public Response delete(
            UUID id,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<Void> result = roleByIdUseCase.execute(id);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(null, idempotencyKey, "OK!"))
                .build();
    }

    @PATCH
    @Idempotent
    @Path("{id}")
    @RunOnVirtualThread
    @PermissionsAllowed("roles:update")
    public Response update(
            UUID id,
            UpdateRoleDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<RoleEntity> result = updateRole.execute(id, dto);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        var role = roleMapper.toDTO(result.getData());

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(role, idempotencyKey, "OK!"))
                .build();
    }

    @GET
    @Idempotent
    @RunOnVirtualThread
    @PermissionsAllowed("roles:read")
    public Response findAll(
            @BeanParam RoleFilterDTO dto
    ) {
        Page<RoleEntity> result = findAllRole.execute(dto);

        Page<RoleDTO> responsePage = result
                .map(roleMapper::toDTO);

        return Response.status(200)
                .entity(responsePage)
                .build();
    }

    @GET
    @Path("/exists/name")
    @RunOnVirtualThread
    @PermissionsAllowed("roles:read")
    public Response existsByName(
            @QueryParam("name") String name,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<Boolean> result = existsRoleByName.execute(name);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), idempotencyKey, "OK!"))
                .build();
    }
}
