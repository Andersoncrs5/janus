package org.janus.modules.authentication.application.dto.session.filter;

import jakarta.ws.rs.QueryParam;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.filter.FilterBaseDTO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SessionFilterDTO extends FilterBaseDTO {

    @QueryParam("userId")
    private UUID userId;

    @QueryParam("ipAddress")
    private String ipAddress;

    @QueryParam("userAgent")
    private String userAgent;

    @QueryParam("isRevoked")
    private Boolean isRevoked;

    @QueryParam("includeExpired")
    private Boolean includeExpired;

    @QueryParam("expiresAtFrom")
    private OffsetDateTime expiresAtFrom;

    @QueryParam("expiresAtTo")
    private OffsetDateTime expiresAtTo;

    @QueryParam("orders")
    @Builder.Default
    private List<SessionOrder> orders = List.of(SessionOrder.CREATED_AT);
}