package org.janus.modules.identity.application.userCredentials.dto;

import jakarta.validation.constraints.NotBlank;
import org.janus.shared.domain.enums.PasswordAlgorithmEnum;

public record UpdateUserCredentialsDTO(

        @NotBlank(message = "The password/hash is required")
        String passwordHash,

        PasswordAlgorithmEnum algorithm
) {
}