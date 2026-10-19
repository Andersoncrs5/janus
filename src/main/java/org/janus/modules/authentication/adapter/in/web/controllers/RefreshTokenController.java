package org.janus.modules.authentication.adapter.in.web.controllers;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.janus.modules.authentication.port.in.refreshToken.IDeleteRefreshTokenByIdUseCase;
import org.janus.modules.authentication.port.in.refreshToken.IRevokeAllTokenByUserIdUseCase;
import org.janus.shared.domain.api.ResponseHTTP;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.idempotency.annotation.Idempotent;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

@Authenticated
@Path("/api/v1/refresh-token")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class RefreshTokenController {

    @Inject
    private IRevokeAllTokenByUserIdUseCase revokeAllTokens;

    @Inject
    private IDeleteRefreshTokenByIdUseCase deleteRefreshToken;

    @Inject
    private JsonWebToken jwt;

    @DELETE
    @Path("/me")
    @Idempotent
    @RunOnVirtualThread
    @PermissionsAllowed("refresh-tokens:revoke")
    public Response revokeAllByAuthenticatedUser(
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        Result<Integer> result = revokeAllTokens.execute(userId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(
                        result.getData(),
                        idempotencyKey,
                        "All refresh tokens revoked!"
                ))
                .build();
    }

    @DELETE
    @Path("/{id}")
    @Idempotent
    @RunOnVirtualThread
    @PermissionsAllowed("refresh-tokens:delete")
    public Response deleteById(
            @PathParam("id") UUID id,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<Void> result = deleteRefreshToken.execute(id);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(
                        null,
                        idempotencyKey,
                        "Refresh token deleted!"
                ))
                .build();
    }
}