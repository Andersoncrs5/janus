package org.janus.modules.authentication.adapter.in.web.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

@Path("/api/v1/refresh-token")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@RequiredArgsConstructor
public class RefreshTokenController {

}
