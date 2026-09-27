package org.janus.modules.identity.application.auth.service;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authentication.application.dto.loginAttempts.request.CreateLoginAttemptDTO;
import org.janus.modules.authentication.application.dto.refreshToken.request.CreateRefreshTokenDTO;
import org.janus.modules.authentication.application.dto.session.request.CreateSessionDTO;
import org.janus.modules.authentication.domain.entity.LoginAttemptEntity;
import org.janus.modules.authentication.domain.entity.RefreshTokenEntity;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.modules.authentication.port.in.loginAttempts.ICountFailedLoginAttemptsByIpUseCase;
import org.janus.modules.authentication.port.in.loginAttempts.ICountFailedLoginAttemptsByUserIdUseCase;
import org.janus.modules.authentication.port.in.loginAttempts.ICreateLoginAttemptsUseCase;
import org.janus.modules.authentication.port.in.refreshToken.ICreateRefreshTokenUseCase;
import org.janus.modules.authentication.port.in.refreshToken.IRevokeAllTokenByUserIdUseCase;
import org.janus.modules.authentication.port.in.session.ICreateSessionUseCase;
import org.janus.modules.authentication.port.in.session.IRevokeAllUserSessionsUseCase;
import org.janus.modules.authorization.infrastructure.in.permission.IFindPermissionSlugsByRoleNameUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.IFindRolesOnlyNameByUserIdUseCase;
import org.janus.modules.identity.application.auth.dto.LoginUserDTO;
import org.janus.modules.identity.application.user.mapper.UserMapper;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.in.auth.ILoginUserCase;
import org.janus.modules.identity.ports.in.jwt.ITokenService;
import org.janus.modules.identity.ports.in.user.IFindUserByEmailUseCase;
import org.janus.modules.identity.ports.in.userCredentials.IGetUserCredentialsByUserIdService;
import org.janus.shared.domain.api.TokenResponse;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.domain.security.PasswordEncoderPort;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;
import org.janus.shared.infrastructure.properties.JwtProperties;

import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor
public class LoginUserCase implements ILoginUserCase {

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCKOUT_MINUTES = 15;

    private final IFindUserByEmailUseCase findUserByEmail;
    private final IGetUserCredentialsByUserIdService getUserCredentialsByUserId;
    private final PasswordEncoderPort passwordEncoder;
    private final ICreateLoginAttemptsUseCase createLoginAttempts;
    private final ICountFailedLoginAttemptsByUserIdUseCase countFailedAttemptsByUserId;
    private final ICountFailedLoginAttemptsByIpUseCase countFailedAttemptsByIp;
    private final IFindRolesOnlyNameByUserIdUseCase findRolesOnlyNameByUserId;
    private final IRevokeAllUserSessionsUseCase revokeAllUserSessions;
    private final IRevokeAllTokenByUserIdUseCase revokeAllTokenByUserId;
    private final ICreateSessionUseCase createSession;
    private final ICreateRefreshTokenUseCase createRefreshToken;
    private final IFindPermissionSlugsByRoleNameUseCase findPermissionSlugsByRoleName;

    private final JwtProperties jwtProperties;
    private final ITokenService tokenService;
    private final UserMapper userMapper;


    @Override
    @ResultTransaction
    public Result<TokenResponse> execute(LoginUserDTO dto, String ipAddress, String userAgent) {
        OffsetDateTime windowStart = OffsetDateTime.now().minusMinutes(LOCKOUT_MINUTES);

        Result<Long> ipFailedCountResult = countFailedAttemptsByIp.execute(ipAddress, windowStart);
        if (ipFailedCountResult.isSuccess() && ipFailedCountResult.getValue() >= MAX_FAILED_ATTEMPTS) {
            recordFailedAttempt(dto.email(), null, ipAddress, userAgent, "IP_TEMPORARILY_BLOCKED");
            return Result.failure("IP temporariamente bloqueado devido a múltiplas tentativas falhas", 429);
        }

        Result<UserEntity> userResult = findUserByEmail.execute(dto.email());
        if (userResult.isFailure()) {
            recordFailedAttempt(dto.email(), null, ipAddress, userAgent, "USER_NOT_FOUND");
            return Result.unauthorized("Credenciais inválidas");
        }

        UserEntity user = userResult.getData();

        Result<Long> userFailedCountResult = countFailedAttemptsByUserId.execute(user.getId(), windowStart);
        if (userFailedCountResult.isSuccess() && userFailedCountResult.getValue() >= MAX_FAILED_ATTEMPTS) {
            recordFailedAttempt(dto.email(), user.getId(), ipAddress, userAgent, "USER_TEMPORARILY_LOCKED");
            return Result.failure("Conta temporariamente bloqueada. Tente novamente mais tarde.", 423);
        }

        Result<UserCredentialsEntity> credentialsResult = getUserCredentialsByUserId.execute(user.getId());
        if (credentialsResult.isFailure()) {
            recordFailedAttempt(dto.email(), user.getId(), ipAddress, userAgent, "CREDENTIALS_NOT_FOUND");
            return Result.unauthorized("Credenciais inválidas");
        }

        UserCredentialsEntity credentials = credentialsResult.getData();

        boolean matches = passwordEncoder.matches(dto.password(), credentials.getPasswordHash());
        if (!matches) {
            recordFailedAttempt(dto.email(), user.getId(), ipAddress, userAgent, "INVALID_PASSWORD");
            return Result.unauthorized("Credenciais inválidas");
        }

        recordSuccessfulAttempt(dto.email(), user.getId(), ipAddress, userAgent);

        Result<TokenResponse> tokensResult = generateToken(user, ipAddress, userAgent);
        if (tokensResult.isFailure()) return Result.failure(tokensResult.getFirstError(), tokensResult.getStatus());

        return Result.success(tokensResult.getData());
    }

    private void recordFailedAttempt(String email, java.util.UUID userId, String ip, String userAgent, String reason) {
        CreateLoginAttemptDTO attemptDTO = CreateLoginAttemptDTO.init(
                email,
                ip,
                userAgent,
                false,
                reason
        );

        Result<LoginAttemptEntity> executed = createLoginAttempts.execute(attemptDTO, userId);

        if (executed.isFailure())
            throw new InternalServerErrorException(executed.getFirstError());
    }

    private void recordSuccessfulAttempt(String email, java.util.UUID userId, String ip, String userAgent) {
        CreateLoginAttemptDTO attemptDTO = CreateLoginAttemptDTO.init(
                email, ip, userAgent, true, null
        );

        Result<LoginAttemptEntity> executed = createLoginAttempts.execute(attemptDTO, userId);

        if (executed.isFailure())
            throw new InternalServerErrorException(executed.getFirstError());
    }

    private Result<TokenResponse> generateToken(UserEntity user, String ipAddress, String userAgent) {

        Result<List<String>> findRoleNameResult = findRolesOnlyNameByUserId.execute(user.getId());
        if (findRoleNameResult.isFailure())
            return Result.failure(findRoleNameResult.getFirstError(), findRoleNameResult.getStatus());
        var rolesName = findRoleNameResult.getData();

        Result<List<String>> listResult = findPermissionSlugsByRoleName.execute(rolesName);
        if (listResult.isFailure())
            return Result.failure(listResult.getFirstError(), listResult.getStatus());

        Result<String> tokenResult = tokenService.generateToken(user, rolesName, listResult.getData());
        if (tokenResult.isFailure()) return Result.failure(tokenResult.getFirstError(), tokenResult.getStatus());

        Result<Integer> revokeResult = revokeAllUserSessions.execute(user.getId());
        if (revokeResult.isFailure()) return Result.failure(revokeResult.getFirstError(), revokeResult.getStatus());

        Result<SessionEntity> sessionResult = createSession.execute(new CreateSessionDTO(
                user.getId(),
                ipAddress,
                userAgent,
                jwtProperties.exp().session()
        ));
        if (sessionResult.isFailure())
            return Result.failure(sessionResult.getFirstError(), sessionResult.getStatus());
        SessionEntity session = sessionResult.getData();

        Result<Integer> revokeAllRefreshTokenResult = revokeAllTokenByUserId.execute(user.getId());
        if (revokeAllRefreshTokenResult.isFailure())
            return Result.failure(revokeAllRefreshTokenResult.getFirstError(), revokeAllRefreshTokenResult.getStatus());

        Result<RefreshTokenEntity> refreshTokenResult = createRefreshToken.execute(new CreateRefreshTokenDTO(
                session.getId(),
                user.getId()
        ));
        if (refreshTokenResult.isFailure())
            return Result.failure(refreshTokenResult.getFirstError(), refreshTokenResult.getStatus());
        RefreshTokenEntity refreshToken = refreshTokenResult.getData();

        TokenResponse tokens = new TokenResponse(
                tokenResult.getData(),
                OffsetDateTime.now().plusDays(jwtProperties.exp().token() * 60),
                refreshToken.getTokenHash(),
                refreshToken.getExpiresAt(),
                userMapper.toDTO(user),
                rolesName
        );

        return Result.success(tokens);
    }
}