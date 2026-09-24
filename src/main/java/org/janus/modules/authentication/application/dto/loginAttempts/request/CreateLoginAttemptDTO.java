package org.janus.modules.authentication.application.dto.loginAttempts.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLoginAttemptDTO(
        @NotBlank String emailAttempted,
        @NotBlank String ipAddress,
        @NotBlank String userAgent,
        @NotNull Boolean success,
        @NotBlank String failureReason
) {
    public static CreateLoginAttemptDTO init(
            String emailAttempted,
            String ipAddress,
            String userAgent,
            Boolean success,
            String failureReason
    ) {
        return new CreateLoginAttemptDTO(
                emailAttempted, ipAddress, userAgent, success, failureReason
        );
    }
}
