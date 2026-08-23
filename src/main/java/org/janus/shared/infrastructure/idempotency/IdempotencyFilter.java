package org.janus.shared.infrastructure.idempotency;

import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.janus.shared.domain.api.ResponseHTTP;
import org.janus.shared.domain.idempotency.IdempotencyService;
import org.janus.shared.infrastructure.idempotency.annotation.Idempotent;

import java.util.List;
import java.util.UUID;

@Provider
@Idempotent
@Priority(Priorities.HEADER_DECORATOR)
public class IdempotencyFilter implements ContainerRequestFilter {

    private static final String IDEMPOTENCY_HEADER = "idempotency-key";

    @Inject
    IdempotencyService idempotencyService;

    @Inject
    RoutingContext routingContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        List<String> headers = requestContext.getHeaders().get(IDEMPOTENCY_HEADER);

        if (headers == null || headers.isEmpty() || headers.getFirst().isBlank()) {
            abortWithResponse(requestContext, Response.Status.BAD_REQUEST.getStatusCode(), "Idempotency-Key header is required.");
            return;
        }

        if (headers.size() > 1) {
            abortWithResponse(requestContext, Response.Status.BAD_REQUEST.getStatusCode(), "Idempotency-Key header must contain a single value.");
            return;
        }

        UUID messageId;
        try {
            messageId = UUID.fromString(headers.getFirst());
        } catch (IllegalArgumentException e) {
            abortWithResponse(requestContext, Response.Status.BAD_REQUEST.getStatusCode(), "Idempotency-Key must be a valid UUID.");
            return;
        }

        String consumerSource = requestContext.getMethod() + ":" + requestContext.getUriInfo().getPath();

        if (idempotencyService.isProcessed(messageId, consumerSource)) {
            abortWithResponse(requestContext, Response.Status.CONFLICT.getStatusCode(), "Request with this Idempotency-Key has already been processed.");
            return;
        }

        String payload = (routingContext.body() != null) ? routingContext.body().asString() : null;
        try {
            idempotencyService.registerRequest(messageId, consumerSource, payload);
        } catch (Exception e) {
            abortWithResponse(requestContext, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage());
        }
    }

    private void abortWithResponse(ContainerRequestContext requestContext, int statusCode, String message) {
        requestContext.abortWith(
                Response.status(statusCode)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(ResponseHTTP.error(message))
                        .build()
        );
    }
}