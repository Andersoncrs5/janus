package org.janus.modules.authentication.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.model.BaseEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SessionEntity extends BaseEntity {

    private UUID userId;
    private String ipAddress;
    private String userAgent;
    private Boolean isRevoked = false;
    private OffsetDateTime expiresAt;

}
