package org.janus.shared.domain.api;

import jakarta.validation.constraints.NotBlank;
import org.janus.modules.identity.application.user.dto.response.UserDTO;

import java.time.OffsetDateTime;
import java.util.List;

public record TokenResponse(
        @NotBlank
        String token,

        OffsetDateTime expToken,

        @NotBlank
        String refreshToken,

        OffsetDateTime expRefreshToken,

        UserDTO user,

        List<String> roles
) {
}