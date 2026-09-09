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

    public boolean isExpired() {
        if (this.expiresAt == null) {
            return false;
        }
        return OffsetDateTime.now().isAfter(this.expiresAt);
    }

    public void addTime(long minutes) {
        this.expiresAt.plusMinutes(minutes);
    }

    public boolean isActive() {
        return Boolean.FALSE.equals(this.isRevoked) && !isExpired();
    }

    public void revoke() {
        this.isRevoked = true;
    }

    public void extendDuration(long durationInMinutes) {
        if (this.expiresAt == null) {
            this.expiresAt = OffsetDateTime.now().plusMinutes(durationInMinutes);
        } else {
            this.expiresAt = this.expiresAt.plusMinutes(durationInMinutes);
        }
    }

    public boolean isSameUser(UUID userId) {
        return this.userId != null && this.userId.equals(userId);
    }

}
