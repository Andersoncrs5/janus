package org.janus.modules.identity.application.userCredentials.dto;

import jakarta.validation.constraints.NotBlank;
import org.janus.shared.domain.enums.PasswordAlgorithmEnum;

public record CreateUserCredentialsDTO(
        @NotBlank(message = "A senha/hash é obrigatória")
        String passwordHash,

        PasswordAlgorithmEnum algorithm
) {

    public static CreateUserCredentialsDTO init(String passwordHash, PasswordAlgorithmEnum algorithm) {
        return new CreateUserCredentialsDTO(
                passwordHash,
                algorithm
        );
    }

}