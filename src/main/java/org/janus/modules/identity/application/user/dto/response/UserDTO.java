package org.janus.modules.identity.application.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.dto.BaseDTO;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO extends BaseDTO {
    private String email;
    private String username;
    private String fullName;
    private Boolean isActive;
    private Boolean isEmailVerified;
}