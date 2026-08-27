package org.janus.modules.authentication.domain.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.model.BaseEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenEntity extends BaseEntity {
    public static final String TABLE_NAME = "refresh_tokens";

    private UUID sessionId;
    private UUID userId;
    private String tokenHash;
    private Boolean isUsed;
    private UUID replacedByTokenId;
    private Boolean isRevoked;
    private OffsetDateTime expiresAt;

    public boolean isValid() {
        boolean notExpired = expiresAt != null && expiresAt.isAfter(OffsetDateTime.now());
        boolean active = Boolean.FALSE.equals(isUsed) && Boolean.FALSE.equals(isRevoked);
        boolean notDeleted = deletedAt == null;
        return notExpired && active && notDeleted;
    }
}