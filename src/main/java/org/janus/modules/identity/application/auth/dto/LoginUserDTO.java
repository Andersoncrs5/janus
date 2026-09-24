package org.janus.modules.identity.application.auth.dto;

public record LoginUserDTO(
        String email,
        String password
) {
}
