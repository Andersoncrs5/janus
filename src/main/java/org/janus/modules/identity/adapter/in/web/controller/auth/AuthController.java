package org.janus.modules.identity.adapter.in.web.controller.auth;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.janus.modules.authentication.port.in.session.IRevokeAllUserAccessUseCase;
import org.janus.modules.identity.application.auth.dto.LoginUserDTO;
import org.janus.modules.identity.application.user.dto.request.CreateUserDTO;
import org.janus.modules.identity.ports.in.auth.ILoginUserCase;
import org.janus.modules.identity.ports.in.auth.IRegisterUserCase;
import org.janus.shared.domain.api.ResponseHTTP;
import org.janus.shared.domain.api.TokenResponse;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.idempotency.annotation.Idempotent;

import java.util.UUID;

@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    @Inject
    private ILoginUserCase loginUserCase;

    @Inject
    JsonWebToken jwt;

    @Inject
    private IRevokeAllUserAccessUseCase revokeAllUserAccess;

    @Inject
    private IRegisterUserCase registerUserCase;

    @POST
    @Idempotent
    @Path("/register")
    @RunOnVirtualThread
    public Response register(
            CreateUserDTO dto,
            @HeaderParam("X-Forwarded-For") String ipAddress,
            @HeaderParam("User-Agent") String userAgent,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {

        Result<TokenResponse> result =
                registerUserCase.execute(
                        dto,
                        ipAddress,
                        userAgent
                );

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), idempotencyKey, "User created!"))
                .build();
    }

    @POST
    @Idempotent
    @Path("/login")
    @RunOnVirtualThread
    public Response login(
            LoginUserDTO dto,
            @HeaderParam("X-Forwarded-For") String ipAddress,
            @HeaderParam("User-Agent") String userAgent,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {

        Result<TokenResponse> result =
                loginUserCase.execute(
                        dto,
                        ipAddress,
                        userAgent
                );

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), idempotencyKey, "User logged!"))
                .build();
    }

    @POST
    @Idempotent
    @Path("/logout-all")
    @RunOnVirtualThread
    @Authenticated
    public Response logoutAll(
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        Result<Void> result = revokeAllUserAccess.execute(userId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response
                .status(result.getStatus())
                .entity(ResponseHTTP.ok(null, idempotencyKey, "All sessions and tokens revoked successfully!"))
                .build();
    }


}