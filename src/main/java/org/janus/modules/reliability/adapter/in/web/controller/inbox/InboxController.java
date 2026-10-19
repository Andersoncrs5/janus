package org.janus.modules.reliability.adapter.in.web.controller.inbox;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.janus.modules.reliability.application.dto.inbox.filter.InboxFilterDTO;
import org.janus.modules.reliability.application.dto.inbox.request.UpdateInboxDTO;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.modules.reliability.port.in.inbox.*;
import org.janus.shared.domain.api.ResponseHTTP;
import org.janus.shared.domain.page.Page;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.idempotency.annotation.Idempotent;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.Optional;
import java.util.UUID;

@Authenticated
@Path("/v1/inbox")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InboxController {

    @Inject
    private IFindAllInboxUseCase findAllInbox;

    @Inject
    private IDeleteInboxByIdUseCase deleteInboxById;

    @Inject
    private IExistsInboxByMessageKeyAndConsumerGroupUseCase existsInboxByMessageKeyAndConsumerGroup;

    @Inject
    private IExistsInboxByMessageKeyUseCase existsInboxByMessageKey;

    @Inject
    private IFindInboxByIdUseCase findInboxById;

    @Inject
    private IFindInboxByMessageKeyAndConsumerGroupUseCase findInboxByMessageKeyAndConsumerGroup;

    @Inject
    private IUpdateInboxUseCase updateInbox;

    @GET
    @Idempotent
    @RunOnVirtualThread
    public Response findAll(
            @BeanParam InboxFilterDTO dto
    ) {
        Page<InboxEntity> result = findAllInbox.execute(dto);

        return Response.status(Response.Status.OK)
                .entity(result)
                .build();
    }

    @GET
    @Path("/{id}")
    @Idempotent
    @RunOnVirtualThread
    public Response findById(
            @PathParam("id") UUID id,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<InboxEntity> result = findInboxById.execute(id);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(
                        result.getData(),
                        idempotencyKey,
                        "OK!"
                ))
                .build();
    }

    @DELETE
    @Path("/{id}")
    @Idempotent
    @RunOnVirtualThread
    public Response delete(
            @PathParam("id") UUID id,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<Void> result = deleteInboxById.execute(id);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(
                        null,
                        idempotencyKey,
                        "Inbox record deleted!"
                ))
                .build();
    }

    @GET
    @Path("/exists/message-key")
    @Idempotent
    @RunOnVirtualThread
    public Response existsByMessageKey(
            @QueryParam("messageKey") String messageKey,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<Boolean> result =
                existsInboxByMessageKey.execute(messageKey);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(
                        result.getData(),
                        idempotencyKey,
                        "OK!"
                ))
                .build();
    }

    @GET
    @Path("/exists/message-key-consumer-group")
    @Idempotent
    @RunOnVirtualThread
    public Response existsByMessageKeyAndConsumerGroup(
            @QueryParam("messageKey") String messageKey,
            @QueryParam("consumerGroup") String consumerGroup,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        boolean exists =
                existsInboxByMessageKeyAndConsumerGroup.execute(
                        messageKey,
                        consumerGroup
                );

        return Response.ok(
                ResponseHTTP.ok(exists, idempotencyKey, "OK!")
        ).build();
    }

    @GET
    @Path("/by-message-key-and-consumer-group")
    @Idempotent
    @RunOnVirtualThread
    @PermissionsAllowed("reliability:inbox:read")
    public Response findByMessageKeyAndConsumerGroup(
            @QueryParam("messageKey") String messageKey,
            @QueryParam("consumerGroup") String consumerGroup,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Optional<InboxEntity> result =
                findInboxByMessageKeyAndConsumerGroup.execute(
                        messageKey,
                        consumerGroup
                );

        if (result.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ResponseHTTP.error("Inbox record not found"))
                    .build();
        }

        return Response.ok(
                ResponseHTTP.ok(
                        result.get(),
                        idempotencyKey,
                        "OK!"
                )
        ).build();
    }

    @PATCH
    @Path("/{id}")
    @Idempotent
    @RunOnVirtualThread
    public Response update(
            @PathParam("id") UUID id,
            UpdateInboxDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<InboxEntity> result = updateInbox.execute(id, dto);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(
                        result.getData(),
                        idempotencyKey,
                        "Inbox record updated!"
                ))
                .build();
    }
}