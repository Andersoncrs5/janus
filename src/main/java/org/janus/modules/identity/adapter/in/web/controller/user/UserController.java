package org.janus.modules.identity.adapter.in.web.controller.user;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.janus.modules.identity.application.user.dto.filter.UserFilterDTO;
import org.janus.modules.identity.application.user.dto.request.UpdateUserDTO;
import org.janus.modules.identity.application.user.dto.response.UserDTO;
import org.janus.modules.identity.application.user.mapper.UserMapper;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.in.user.IDeleteUserByIdUseCase;
import org.janus.modules.identity.ports.in.user.IFindAllUsersUseCase;
import org.janus.modules.identity.ports.in.user.IFindUserByIdUseCase;
import org.janus.modules.identity.ports.in.user.IUpdateUserUseCase;
import org.janus.shared.domain.api.ResponseHTTP;
import org.janus.shared.domain.page.Page;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.idempotency.annotation.Idempotent;

import java.util.UUID;

@Path("/v1/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class UserController {

    @Inject
    private IFindAllUsersUseCase findAllUsers;

    @Inject
    private UserMapper userMapper;

    @Inject
    private IDeleteUserByIdUseCase deleteUserById;

    @Inject
    private IUpdateUserUseCase updateUser;

    @Inject
    private IFindUserByIdUseCase findUserById;

    @Inject
    JsonWebToken jwt;

    @GET
    @Idempotent
    @RunOnVirtualThread
    public Response findAll(@BeanParam UserFilterDTO dto) {
        Result<Page<UserEntity>> result = findAllUsers.execute(dto);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        Page<UserDTO> responsePage = result.getData()
                .map(userMapper::toDTO);

        return Response.status(result.getStatus())
                .entity(responsePage)
                .build();
    }

    @GET
    @Idempotent
    @Path("/me")
    @RunOnVirtualThread
    public Response getMe(
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        var userId = UUID.fromString(jwt.getSubject());

        Result<UserEntity> result = findUserById.execute(userId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        UserDTO user = userMapper.toDTO(result.getData());

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(user, idempotencyKey, "User found!"))
                .build();
    }

    @DELETE
    @Idempotent
    @RunOnVirtualThread
    public Response delete(
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        var userId = UUID.fromString(jwt.getSubject());

        var result = deleteUserById.execute(userId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(null, idempotencyKey, "User found!"))
                .build();
    }

    @PATCH
    @Idempotent
    @RunOnVirtualThread
    public Response update(
            UpdateUserDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        var userId = UUID.fromString(jwt.getSubject());

        Result<UserEntity> result = updateUser.execute(userId, dto);
        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        UserDTO user = userMapper.toDTO(result.getData());

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(user, idempotencyKey, "User updated!"))
                .build();
    }

}
