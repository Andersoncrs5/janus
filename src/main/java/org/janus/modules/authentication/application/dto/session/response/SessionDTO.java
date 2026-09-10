package org.janus.modules.authentication.application.dto.session.response;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.dto.BaseDTO;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SessionDTO extends BaseDTO {
    private UUID userId;
    private String ipAddress;
    private String userAgent;
    private Boolean isRevoked;
    private OffsetDateTime expiresAt;
}