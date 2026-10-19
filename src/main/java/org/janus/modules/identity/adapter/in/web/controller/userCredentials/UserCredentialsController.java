package org.janus.modules.identity.adapter.in.web.controller.userCredentials;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.janus.modules.identity.application.userCredentials.dto.UpdateUserCredentialsDTO;
import org.janus.modules.identity.application.userCredentials.dto.UserCredentialsDTO;
import org.janus.modules.identity.application.userCredentials.mapper.UserCredentialsMapper;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.ports.in.userCredentials.IUpdateUserCredentialsUseCase;
import org.janus.shared.domain.api.ResponseHTTP;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.idempotency.annotation.Idempotent;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

@Authenticated
@Path("/v1/user-credentials")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class UserCredentialsController {

    private final IUpdateUserCredentialsUseCase updateUserCredentials;
    private final UserCredentialsMapper mapper;

    @PATCH
    @Path("/{userId}")
    @Idempotent
    @RunOnVirtualThread
    public Response update(
            @PathParam("userId") UUID userId,
            @Valid UpdateUserCredentialsDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<UserCredentialsEntity> result =
                updateUserCredentials.execute(userId, dto);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        UserCredentialsDTO credentialsDTO = mapper.toDTO(result.getData());

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(
                        credentialsDTO,
                        idempotencyKey,
                        "User credentials updated!"
                ))
                .build();
    }
}