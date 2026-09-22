package org.janus.modules.identity.ports.in.jwt;

import org.janus.modules.identity.application.user.dto.response.UserDTO;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.shared.domain.api.TokenResponse;
import org.janus.shared.domain.result.Result;

import java.util.List;
import java.util.Map;

public interface ITokenService {
    Result<String> generateToken(UserEntity user, List<String> roles, List<String> permissions);

    Result<Map<String, Object>> extractAllClaims(String token);

    Result<String> validateToken(String token);

    Result<TokenResponse> makeTokens(UserEntity user, List<String> roles, UserDTO userDTO);
}