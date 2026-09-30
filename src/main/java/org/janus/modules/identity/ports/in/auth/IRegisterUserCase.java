package org.janus.modules.identity.ports.in.auth;

import org.janus.modules.identity.application.user.dto.request.CreateUserDTO;
import org.janus.shared.domain.api.TokenResponse;
import org.janus.shared.domain.result.Result;

public interface IRegisterUserCase {
    Result<TokenResponse> execute(CreateUserDTO dto, String ipAddress, String userAgent);
}
