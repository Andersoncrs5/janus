package org.janus.modules.authorization.adapter.in.web.controller.permission;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.janus.modules.authorization.application.dto.permission.request.CreatePermissionDTO;
import org.janus.modules.authorization.application.dto.permission.request.UpdatePermissionDTO;
import org.janus.modules.authorization.application.dto.permission.filter.PermissionFilterDTO;
import org.janus.modules.authorization.application.dto.permission.response.PermissionDTO;
import org.janus.modules.authorization.application.mapper.PermissionMapper;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.infrastructure.in.permission.*;
import org.janus.shared.domain.api.ResponseHTTP;
import org.janus.shared.domain.page.Page;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.idempotency.annotation.Idempotent;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

@Authenticated
@Path("/v1/permission")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PermissionController {

    @Inject
    private IExistsPermissionByNameUseCase existsPermissionByName;

    @Inject
    private IExistsPermissionBySlugUseCase existsPermissionBySlug;

    @Inject
    private IFindAllPermissionUseCase findAllPermission;

    @Inject
    private IDeletePermissionByIdUseCase permissionByIdUseCase;

    @Inject
    private IFindPermissionByIdUseCase findPermissionById;

    @Inject
    private ICreatePermissionUseCase createPermission;

    @Inject
    private IUpdatePermissionUseCase updatePermission;

    @Inject
    private PermissionMapper permissionMapper;

    @Inject
    private JsonWebToken jwt;

    @POST
    @Idempotent
    @RunOnVirtualThread
    @PermissionsAllowed("permissions:create")
    public Response create(
            CreatePermissionDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        var userId = UUID.fromString(jwt.getSubject());

        Result<PermissionEntity> result = createPermission.execute(dto, userId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        var permission = permissionMapper.toDTO(result.getData());

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(permission, idempotencyKey, "Permission created!"))
                .build();
    }

    @GET
    @Idempotent
    @Path("{id}")
    @RunOnVirtualThread
    public Response get(
            @PathParam("id") UUID id,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<PermissionEntity> result = findPermissionById.execute(id);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        var permission = permissionMapper.toDTO(result.getData());

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(permission, idempotencyKey, "OK!"))
                .build();
    }

    @DELETE
    @Idempotent
    @Path("{id}")
    @RunOnVirtualThread
    @PermissionsAllowed("permissions:delete")
    public Response delete(
            @PathParam("id") UUID id,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<Void> result = permissionByIdUseCase.execute(id);

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
    @PermissionsAllowed("permissions:update")
    public Response update(
            @PathParam("id") UUID id,
            UpdatePermissionDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<PermissionEntity> result = updatePermission.execute(dto, id);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        var permission = permissionMapper.toDTO(result.getData());

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(permission, idempotencyKey, "OK!"))
                .build();
    }

    @GET
    @Idempotent
    @RunOnVirtualThread
    @PermissionsAllowed("permissions:read")
    public Response findAll(
            @BeanParam PermissionFilterDTO dto
    ) {
        Page<PermissionEntity> result = findAllPermission.execute(dto);

        Page<PermissionDTO> responsePage = result
                .map(permissionMapper::toDTO);

        return Response.status(200)
                .entity(responsePage)
                .build();
    }

    @GET
    @Path("/exists/name")
    @RunOnVirtualThread
    @PermissionsAllowed("permissions:read")
    public Response existsByName(
            @QueryParam("name") String name,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<Boolean> result = existsPermissionByName.execute(name);

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

    @GET
    @Path("/exists/slug")
    @RunOnVirtualThread
    @PermissionsAllowed("permissions:read")
    public Response existsBySlug(
            @QueryParam("slug") String slug,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<Boolean> result = existsPermissionBySlug.execute(slug);

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