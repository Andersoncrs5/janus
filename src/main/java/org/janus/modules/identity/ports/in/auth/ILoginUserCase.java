package org.janus.modules.identity.ports.in.auth;

import org.janus.modules.identity.application.auth.dto.LoginUserDTO;
import org.janus.shared.domain.api.TokenResponse;
import org.janus.shared.domain.result.Result;

public interface ILoginUserCase {
    Result<TokenResponse> execute(LoginUserDTO dto, String ipAddress, String userAgent);
}
