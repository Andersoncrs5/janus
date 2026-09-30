package org.janus.modules.authorization.adapter.in.web.controller.userRole;

import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.janus.modules.authorization.application.dto.userRole.request.CreateUserRoleDTO;
import org.janus.modules.authorization.application.dto.userRole.response.UserRoleDTO;
import org.janus.modules.authorization.application.mapper.UserRoleMapper;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.authorization.infrastructure.in.userRole.ICreateUserRoleUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.IDeleteUserRoleByIdUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.IExistsActiveByUserIdAndRoleIdUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.IExistsByUserIdAndRoleIdUseCase;
import org.janus.shared.domain.api.ResponseHTTP;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.idempotency.annotation.Idempotent;

import java.util.UUID;

@Authenticated
@Path("/v1/user-role")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserRoleController {
    @Inject
    private UserRoleMapper mapper;
    @Inject
    private JsonWebToken jwt;

    @Inject
    private ICreateUserRoleUseCase createUserRole;
    @Inject
    private IExistsActiveByUserIdAndRoleIdUseCase existsActiveByUserIdAndRoleId;
    @Inject
    private IExistsByUserIdAndRoleIdUseCase existsByUserIdAndRoleId;
    @Inject
    private IDeleteUserRoleByIdUseCase deleteRoleById;

    @GET
    @Path("/exists/{userId}/{roleId}")
    @RunOnVirtualThread
    public Response exists(
            @PathParam("userId") UUID userId,
            @PathParam("roleId") UUID roleId,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<Boolean> result =
                existsByUserIdAndRoleId.execute(userId, roleId);

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
    @Path("/exists/active/{userId}/{roleId}")
    @RunOnVirtualThread
    public Response existsActive(
            @PathParam("userId") UUID userId,
            @PathParam("roleId") UUID roleId,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<Boolean> result =
                existsActiveByUserIdAndRoleId.execute(userId, roleId);

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

    @DELETE
    @Path("/{id}")
    @Idempotent
    @RunOnVirtualThread
    public Response delete(
            UUID id,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        Result<Void> result = deleteRoleById.execute(id, userId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(null, idempotencyKey, "Role removed of user"))
                .build();
    }

    @POST
    @Idempotent
    @RunOnVirtualThread
    public Response create(
            CreateUserRoleDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        Result<UserRoleEntity> result = createUserRole.execute(dto, userId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        UserRoleDTO role = mapper.toDTO(result.getData());

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(role, idempotencyKey, "Role to user!"))
                .build();
    }


}
