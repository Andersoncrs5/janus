package org.janus.modules.identity.adapter.in.web.controller.auth;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.janus.modules.identity.application.user.dto.CreateUserDTO;
import org.janus.modules.identity.application.userCredentials.dto.CreateUserCredentialsDTO;
import org.janus.shared.infrastructure.idempotency.annotation.Idempotent;

@Path("/v1/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

//    @POST
//    @Idempotent
//    @Path("/v1/register")
//    public Response register(CreateUserDTO dto) {
//
//    }
//

}
