package org.janus.modules.authentication.application.dto.loginAttempts.response;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.dto.BaseDTO;

import java.util.UUID;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttemptDTO extends BaseDTO {
    private UUID userId;
    private String emailAttempted;
    private String ipAddress;
    private String userAgent;
    private Boolean success;
    private String failureReason;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmailAttempted() {
        return emailAttempted;
    }

    public void setEmailAttempted(String emailAttempted) {
        this.emailAttempted = emailAttempted;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
