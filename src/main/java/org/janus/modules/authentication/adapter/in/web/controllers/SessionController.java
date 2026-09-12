package org.janus.modules.authentication.adapter.in.web.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authentication.application.dto.session.request.CreateSessionDTO;
import org.janus.modules.authentication.application.dto.session.request.RefreshSessionDTO;
import org.janus.modules.authentication.application.dto.session.response.SessionDTO;
import org.janus.modules.authentication.application.mapper.SessionMapper;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.modules.authentication.port.in.session.*;
import org.janus.shared.domain.result.Result;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@RequiredArgsConstructor
public class SessionController {
    private final ICreateSessionUseCase createSessionUseCase;
    private final IFindActiveSessionsByUserIdUseCase findActiveSessionsByUserIdUseCase;
    private final IRevokeSessionByIdUseCase revokeSessionByIdUseCase;
    private final IRevokeAllUserSessionsUseCase revokeAllUserSessionsUseCase;
    private final IRefreshSessionUseCase refreshSessionUseCase;
    private final SessionMapper sessionMapper;

    @POST
    public Response createSession(CreateSessionDTO dto) {
        Result<SessionEntity> result = createSessionUseCase.execute(dto);
        if (!result.isSuccess()) {
            return Response.status(result.getStatusCode()).entity(result.getErrors()).build();
        }
        SessionDTO responseDto = sessionMapper.toDTO(result.getValue());
        return Response.status(Response.Status.CREATED).entity(responseDto).build();
    }

    @GET
    @Path("/users/{userId}")
    public Response getActiveSessions(@PathParam("userId") UUID userId) {
        Result<List<SessionEntity>> result = findActiveSessionsByUserIdUseCase.execute(userId);
        if (!result.isSuccess()) {
            return Response.status(result.getStatusCode()).entity(result.getErrors()).build();
        }
        List<SessionDTO> responseDtos = sessionMapper.toDTO(result.getValue());
        return Response.ok(responseDtos).build();
    }

    @DELETE
    @Path("/{sessionId}/users/{userId}")
    public Response revokeSession(@PathParam("sessionId") UUID sessionId, @PathParam("userId") UUID userId) {
        Result<Void> result = revokeSessionByIdUseCase.execute(sessionId, userId);
        if (!result.isSuccess()) {
            return Response.status(result.getStatusCode()).entity(result.getErrors()).build();
        }
        return Response.noContent().build();
    }

    @DELETE
    @Path("/users/{userId}/all")
    public Response revokeAllSessions(@PathParam("userId") UUID userId) {
        Result<Integer> result = revokeAllUserSessionsUseCase.execute(userId);
        if (!result.isSuccess()) {
            return Response.status(result.getStatusCode()).entity(result.getErrors()).build();
        }
        return Response.ok(result.getValue()).build();
    }

    @PUT
    @Path("/refresh")
    public Response refreshSession(RefreshSessionDTO dto) {
        Result<SessionEntity> result = refreshSessionUseCase.execute(dto);
        if (!result.isSuccess()) {
            return Response.status(result.getStatusCode()).entity(result.getErrors()).build();
        }
        SessionDTO responseDto = sessionMapper.toDTO(result.getValue());
        return Response.ok(responseDto).build();
    }
}
