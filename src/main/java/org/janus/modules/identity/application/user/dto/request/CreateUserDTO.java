package org.janus.modules.identity.application.user.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateUserDTO {
    private String email;
    private String username;
    private String fullName;

    private String password;

    public CreateUserDTO() {
    }

    public CreateUserDTO(String email, String username, String fullName, String password) {
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.password = password;
    }
}