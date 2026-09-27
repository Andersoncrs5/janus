package org.janus.modules.identity.service.auth;

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
import org.janus.modules.identity.application.auth.service.LoginUserCase;
import org.janus.modules.identity.application.user.dto.response.UserDTO;
import org.janus.modules.identity.application.user.mapper.UserMapper;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.in.jwt.ITokenService;
import org.janus.modules.identity.ports.in.user.IFindUserByEmailUseCase;
import org.janus.modules.identity.ports.in.userCredentials.IGetUserCredentialsByUserIdService;
import org.janus.shared.domain.api.TokenResponse;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.domain.security.PasswordEncoderPort;
import org.janus.shared.infrastructure.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginUserCaseTest {

    @Mock
    private IFindUserByEmailUseCase findUserByEmail;

    @Mock
    private IGetUserCredentialsByUserIdService getUserCredentialsByUserId;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private ICreateLoginAttemptsUseCase createLoginAttempts;

    @Mock
    private ICountFailedLoginAttemptsByUserIdUseCase countFailedAttemptsByUserId;

    @Mock
    private ICountFailedLoginAttemptsByIpUseCase countFailedAttemptsByIp;

    @Mock
    private IFindRolesOnlyNameByUserIdUseCase findRolesOnlyNameByUserId;

    @Mock
    private IFindPermissionSlugsByRoleNameUseCase findPermissionSlugsByRoleName;

    @Mock
    private IRevokeAllUserSessionsUseCase revokeAllUserSessions;

    @Mock
    private ICreateSessionUseCase createSession;

    @Mock
    private ICreateRefreshTokenUseCase createRefreshToken;

    @Mock
    private IRevokeAllTokenByUserIdUseCase revokeAllTokenByUserId;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private JwtProperties.Exp expProperties;

    @Mock
    private ITokenService tokenService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private LoginUserCase loginUserCase;

    private LoginUserDTO loginDTO;
    private String ipAddress;
    private String userAgent;
    private UUID userId;
    private UserEntity userEntity;
    private UserCredentialsEntity credentialsEntity;
    private LoginAttemptEntity loginAttemptEntity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        loginDTO = new LoginUserDTO(
                "john.doe@example.com",
                "Secret123!"
        );

        ipAddress = "192.168.1.100";
        userAgent = "Mozilla/5.0";

        userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail(loginDTO.email());

        credentialsEntity = new UserCredentialsEntity();
        credentialsEntity.setPasswordHash("$2a$10$hashedpassword");

        loginAttemptEntity = new LoginAttemptEntity();
    }

    @Nested
    @DisplayName("Lockout & Attempt Limit Scenarios")
    class LockoutScenarios {

        @Test
        @DisplayName("Should return HTTP 429 when IP address exceeds maximum failed attempts")
        void shouldReturn429WhenIpIsBlocked() {
            when(countFailedAttemptsByIp.execute(
                    eq(ipAddress),
                    any()
            )).thenReturn(Result.success(3L));

            when(createLoginAttempts.execute(
                    any(CreateLoginAttemptDTO.class),
                    eq(null)
            )).thenReturn(Result.success(loginAttemptEntity));

            Result<TokenResponse> result =
                    loginUserCase.execute(
                            loginDTO,
                            ipAddress,
                            userAgent
                    );

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(429);
            assertThat(result.getFirstError())
                    .contains("IP temporariamente bloqueado");

            verify(createLoginAttempts)
                    .execute(
                            any(CreateLoginAttemptDTO.class),
                            eq(null)
                    );

            verifyNoInteractions(
                    findUserByEmail,
                    getUserCredentialsByUserId
            );
        }

        @Test
        @DisplayName("Should return HTTP 423 when User exceeds maximum failed attempts")
        void shouldReturn423WhenUserIsLocked() {
            when(countFailedAttemptsByIp.execute(
                    eq(ipAddress),
                    any()
            )).thenReturn(Result.success(0L));

            when(findUserByEmail.execute(loginDTO.email()))
                    .thenReturn(Result.success(userEntity));

            when(countFailedAttemptsByUserId.execute(
                    eq(userId),
                    any()
            )).thenReturn(Result.success(3L));

            when(createLoginAttempts.execute(
                    any(CreateLoginAttemptDTO.class),
                    eq(userId)
            )).thenReturn(Result.success(loginAttemptEntity));

            Result<TokenResponse> result =
                    loginUserCase.execute(
                            loginDTO,
                            ipAddress,
                            userAgent
                    );

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(423);
            assertThat(result.getFirstError())
                    .contains("Conta temporariamente bloqueada");

            verify(createLoginAttempts)
                    .execute(
                            any(CreateLoginAttemptDTO.class),
                            eq(userId)
                    );

            verifyNoInteractions(
                    getUserCredentialsByUserId,
                    passwordEncoder
            );
        }
    }

    @Nested
    @DisplayName("Authentication Failure Scenarios")
    class AuthenticationFailureScenarios {

        @Test
        @DisplayName("Should return HTTP 401 when user email is not found")
        void shouldReturn401WhenEmailNotFound() {
            when(countFailedAttemptsByIp.execute(
                    eq(ipAddress),
                    any()
            )).thenReturn(Result.success(0L));

            when(findUserByEmail.execute(loginDTO.email()))
                    .thenReturn(
                            Result.failure(
                                    "User not found",
                                    404
                            )
                    );

            when(createLoginAttempts.execute(
                    any(CreateLoginAttemptDTO.class),
                    eq(null)
            )).thenReturn(Result.success(loginAttemptEntity));

            Result<TokenResponse> result =
                    loginUserCase.execute(
                            loginDTO,
                            ipAddress,
                            userAgent
                    );

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(401);
            assertThat(result.getFirstError())
                    .isEqualTo("Credenciais inválidas");

            verify(createLoginAttempts)
                    .execute(
                            any(CreateLoginAttemptDTO.class),
                            eq(null)
                    );
        }

        @Test
        @DisplayName("Should return HTTP 401 when user credentials record is missing")
        void shouldReturn401WhenCredentialsNotFound() {
            when(countFailedAttemptsByIp.execute(
                    eq(ipAddress),
                    any()
            )).thenReturn(Result.success(0L));

            when(findUserByEmail.execute(loginDTO.email()))
                    .thenReturn(Result.success(userEntity));

            when(countFailedAttemptsByUserId.execute(
                    eq(userId),
                    any()
            )).thenReturn(Result.success(0L));

            when(getUserCredentialsByUserId.execute(userId))
                    .thenReturn(
                            Result.failure(
                                    "Credentials missing",
                                    404
                            )
                    );

            when(createLoginAttempts.execute(
                    any(CreateLoginAttemptDTO.class),
                    eq(userId)
            )).thenReturn(Result.success(loginAttemptEntity));

            Result<TokenResponse> result =
                    loginUserCase.execute(
                            loginDTO,
                            ipAddress,
                            userAgent
                    );

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(401);

            verify(createLoginAttempts)
                    .execute(
                            any(CreateLoginAttemptDTO.class),
                            eq(userId)
                    );
        }

        @Test
        @DisplayName("Should return HTTP 401 when password does not match")
        void shouldReturn401WhenPasswordMismatch() {
            when(countFailedAttemptsByIp.execute(
                    eq(ipAddress),
                    any()
            )).thenReturn(Result.success(0L));

            when(findUserByEmail.execute(loginDTO.email()))
                    .thenReturn(Result.success(userEntity));

            when(countFailedAttemptsByUserId.execute(
                    eq(userId),
                    any()
            )).thenReturn(Result.success(0L));

            when(getUserCredentialsByUserId.execute(userId))
                    .thenReturn(Result.success(credentialsEntity));

            when(passwordEncoder.matches(
                    loginDTO.password(),
                    credentialsEntity.getPasswordHash()
            )).thenReturn(false);

            when(createLoginAttempts.execute(
                    any(CreateLoginAttemptDTO.class),
                    eq(userId)
            )).thenReturn(Result.success(loginAttemptEntity));

            Result<TokenResponse> result =
                    loginUserCase.execute(
                            loginDTO,
                            ipAddress,
                            userAgent
                    );

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(401);

            verify(createLoginAttempts)
                    .execute(
                            any(CreateLoginAttemptDTO.class),
                            eq(userId)
                    );
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException when recording login attempt fails")
        void shouldThrowExceptionWhenRecordingAttemptFails() {
            when(countFailedAttemptsByIp.execute(
                    eq(ipAddress),
                    any()
            )).thenReturn(Result.success(0L));

            when(findUserByEmail.execute(loginDTO.email()))
                    .thenReturn(
                            Result.failure(
                                    "User not found",
                                    404
                            )
                    );

            when(createLoginAttempts.execute(
                    any(CreateLoginAttemptDTO.class),
                    eq(null)
            )).thenReturn(
                    Result.failure(
                            "Database error",
                            500
                    )
            );

            assertThatThrownBy(() ->
                    loginUserCase.execute(
                            loginDTO,
                            ipAddress,
                            userAgent
                    )
            )
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining("Database error");
        }
    }

    @Nested
    @DisplayName("Token and Session Generation Failure Scenarios")
    class GenerationFailureScenarios {

        @BeforeEach
        void setUpValidCredentials() {
            when(countFailedAttemptsByIp.execute(
                    eq(ipAddress),
                    any()
            )).thenReturn(Result.success(0L));

            when(findUserByEmail.execute(loginDTO.email()))
                    .thenReturn(Result.success(userEntity));

            when(countFailedAttemptsByUserId.execute(
                    eq(userId),
                    any()
            )).thenReturn(Result.success(0L));

            when(getUserCredentialsByUserId.execute(userId))
                    .thenReturn(Result.success(credentialsEntity));

            when(passwordEncoder.matches(
                    loginDTO.password(),
                    credentialsEntity.getPasswordHash()
            )).thenReturn(true);

            when(createLoginAttempts.execute(
                    any(CreateLoginAttemptDTO.class),
                    eq(userId)
            )).thenReturn(Result.success(loginAttemptEntity));
        }

        @Test
        @DisplayName("Should fail when finding user roles returns an error")
        void shouldFailWhenFindRolesFails() {
            when(findRolesOnlyNameByUserId.execute(userId))
                    .thenReturn(
                            Result.failure(
                                    "Roles error",
                                    500
                            )
                    );

            Result<TokenResponse> result =
                    loginUserCase.execute(
                            loginDTO,
                            ipAddress,
                            userAgent
                    );

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getFirstError())
                    .isEqualTo("Roles error");
        }

        @Test
        @DisplayName("Should fail when finding permission slugs returns an error")
        void shouldFailWhenFindPermissionSlugsFails() {
            List<String> roles = List.of("ROLE_USER");

            when(findRolesOnlyNameByUserId.execute(userId))
                    .thenReturn(Result.success(roles));

            when(findPermissionSlugsByRoleName.execute(roles))
                    .thenReturn(
                            Result.failure(
                                    "Permissions error",
                                    500
                            )
                    );

            Result<TokenResponse> result =
                    loginUserCase.execute(
                            loginDTO,
                            ipAddress,
                            userAgent
                    );

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getFirstError())
                    .isEqualTo("Permissions error");
        }

        @Test
        @DisplayName("Should fail when token generation service returns an error")
        void shouldFailWhenTokenServiceFails() {
            List<String> roles = List.of("ROLE_USER");
            List<String> permissions = List.of("users:read");

            when(findRolesOnlyNameByUserId.execute(userId))
                    .thenReturn(Result.success(roles));

            when(findPermissionSlugsByRoleName.execute(roles))
                    .thenReturn(Result.success(permissions));

            when(tokenService.generateToken(
                    userEntity,
                    roles,
                    permissions
            )).thenReturn(
                    Result.failure(
                            "JWT failure",
                            500
                    )
            );

            Result<TokenResponse> result =
                    loginUserCase.execute(
                            loginDTO,
                            ipAddress,
                            userAgent
                    );

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getFirstError())
                    .isEqualTo("JWT failure");
        }

        @Test
        @DisplayName("Should fail when revoking user sessions returns an error")
        void shouldFailWhenRevokingSessionsFails() {
            List<String> roles = List.of("ROLE_USER");
            List<String> permissions = List.of("users:read");

            when(findRolesOnlyNameByUserId.execute(userId))
                    .thenReturn(Result.success(roles));

            when(findPermissionSlugsByRoleName.execute(roles))
                    .thenReturn(Result.success(permissions));

            when(tokenService.generateToken(
                    userEntity,
                    roles,
                    permissions
            )).thenReturn(Result.success("jwt_token"));

            when(revokeAllUserSessions.execute(userId))
                    .thenReturn(
                            Result.failure(
                                    "Revoke sessions failed",
                                    500
                            )
                    );

            Result<TokenResponse> result =
                    loginUserCase.execute(
                            loginDTO,
                            ipAddress,
                            userAgent
                    );

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getFirstError())
                    .isEqualTo("Revoke sessions failed");
        }

        @Test
        @DisplayName("Should fail when session creation returns an error")
        void shouldFailWhenSessionCreationFails() {
            mockSessionExpiration();

            List<String> roles = List.of("ROLE_USER");
            List<String> permissions = List.of("users:read");

            when(findRolesOnlyNameByUserId.execute(userId))
                    .thenReturn(Result.success(roles));

            when(findPermissionSlugsByRoleName.execute(roles))
                    .thenReturn(Result.success(permissions));

            when(tokenService.generateToken(
                    userEntity,
                    roles,
                    permissions
            )).thenReturn(Result.success("jwt_token"));

            when(revokeAllUserSessions.execute(userId))
                    .thenReturn(Result.success(1));

            when(createSession.execute(
                    any(CreateSessionDTO.class)
            )).thenReturn(
                    Result.failure(
                            "Create session failed",
                            500
                    )
            );

            Result<TokenResponse> result =
                    loginUserCase.execute(
                            loginDTO,
                            ipAddress,
                            userAgent
                    );

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getFirstError())
                    .isEqualTo("Create session failed");
        }

        @Test
        @DisplayName("Should fail when revoking refresh tokens returns an error")
        void shouldFailWhenRevokingRefreshTokensFails() {
            mockSessionExpiration();

            SessionEntity sessionEntity =
                    SessionEntity.builder()
                            .userId(userId)
                            .build();

            sessionEntity.setId(UUID.randomUUID());

            List<String> roles = List.of("ROLE_USER");
            List<String> permissions = List.of("users:read");

            when(findRolesOnlyNameByUserId.execute(userId))
                    .thenReturn(Result.success(roles));

            when(findPermissionSlugsByRoleName.execute(roles))
                    .thenReturn(Result.success(permissions));

            when(tokenService.generateToken(
                    userEntity,
                    roles,
                    permissions
            )).thenReturn(Result.success("jwt_token"));

            when(revokeAllUserSessions.execute(userId))
                    .thenReturn(Result.success(1));

            when(createSession.execute(
                    any(CreateSessionDTO.class)
            )).thenReturn(Result.success(sessionEntity));

            when(revokeAllTokenByUserId.execute(userId))
                    .thenReturn(
                            Result.failure(
                                    "Revoke tokens failed",
                                    500
                            )
                    );

            Result<TokenResponse> result =
                    loginUserCase.execute(
                            loginDTO,
                            ipAddress,
                            userAgent
                    );

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getFirstError())
                    .isEqualTo("Revoke tokens failed");
        }

        @Test
        @DisplayName("Should fail when refresh token creation returns an error")
        void shouldFailWhenCreatingRefreshTokenFails() {
            mockSessionExpiration();

            SessionEntity sessionEntity =
                    SessionEntity.builder()
                            .userId(userId)
                            .build();

            sessionEntity.setId(UUID.randomUUID());

            List<String> roles = List.of("ROLE_USER");
            List<String> permissions = List.of("users:read");

            when(findRolesOnlyNameByUserId.execute(userId))
                    .thenReturn(Result.success(roles));

            when(findPermissionSlugsByRoleName.execute(roles))
                    .thenReturn(Result.success(permissions));

            when(tokenService.generateToken(
                    userEntity,
                    roles,
                    permissions
            )).thenReturn(Result.success("jwt_token"));

            when(revokeAllUserSessions.execute(userId))
                    .thenReturn(Result.success(1));

            when(createSession.execute(
                    any(CreateSessionDTO.class)
            )).thenReturn(Result.success(sessionEntity));

            when(revokeAllTokenByUserId.execute(userId))
                    .thenReturn(Result.success(2));

            when(createRefreshToken.execute(
                    any(CreateRefreshTokenDTO.class)
            )).thenReturn(
                    Result.failure(
                            "Create refresh token failed",
                            500
                    )
            );

            Result<TokenResponse> result =
                    loginUserCase.execute(
                            loginDTO,
                            ipAddress,
                            userAgent
                    );

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getFirstError())
                    .isEqualTo("Create refresh token failed");
        }

        private void mockSessionExpiration() {
            when(jwtProperties.exp()).thenReturn(expProperties);
            when(expProperties.session()).thenReturn(60L);
        }
    }

    @Nested
    @DisplayName("Successful Login Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should perform full login flow successfully and return TokenResponse")
        void shouldExecuteLoginSuccessfully() {
            mockJwtProperties();

            SessionEntity sessionEntity =
                    SessionEntity.builder()
                            .userId(userId)
                            .build();

            UUID sessionId = UUID.randomUUID();
            sessionEntity.setId(sessionId);

            RefreshTokenEntity refreshTokenEntity =
                    RefreshTokenEntity.builder()
                            .tokenHash("sample_refresh_token_hash")
                            .expiresAt(OffsetDateTime.now().plusDays(7))
                            .build();

            UserDTO userDTO = new UserDTO();
            List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");
            List<String> permissions = List.of("users:read", "users:write");

            when(countFailedAttemptsByIp.execute(
                    eq(ipAddress),
                    any()
            )).thenReturn(Result.success(0L));

            when(findUserByEmail.execute(loginDTO.email()))
                    .thenReturn(Result.success(userEntity));

            when(countFailedAttemptsByUserId.execute(
                    eq(userId),
                    any()
            )).thenReturn(Result.success(0L));

            when(getUserCredentialsByUserId.execute(userId))
                    .thenReturn(Result.success(credentialsEntity));

            when(passwordEncoder.matches(
                    loginDTO.password(),
                    credentialsEntity.getPasswordHash()
            )).thenReturn(true);

            when(createLoginAttempts.execute(
                    any(CreateLoginAttemptDTO.class),
                    eq(userId)
            )).thenReturn(Result.success(loginAttemptEntity));

            when(findRolesOnlyNameByUserId.execute(userId))
                    .thenReturn(Result.success(roles));

            when(findPermissionSlugsByRoleName.execute(roles))
                    .thenReturn(Result.success(permissions));

            when(tokenService.generateToken(
                    userEntity,
                    roles,
                    permissions
            )).thenReturn(Result.success("generated_jwt_token"));

            when(revokeAllUserSessions.execute(userId))
                    .thenReturn(Result.success(1));

            when(createSession.execute(
                    any(CreateSessionDTO.class)
            )).thenReturn(Result.success(sessionEntity));

            when(revokeAllTokenByUserId.execute(userId))
                    .thenReturn(Result.success(2));

            when(createRefreshToken.execute(
                    any(CreateRefreshTokenDTO.class)
            )).thenReturn(Result.success(refreshTokenEntity));

            when(userMapper.toDTO(userEntity))
                    .thenReturn(userDTO);

            Result<TokenResponse> result =
                    loginUserCase.execute(
                            loginDTO,
                            ipAddress,
                            userAgent
                    );

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();

            TokenResponse tokenResponse = result.getData();

            assertThat(tokenResponse).isNotNull();
            assertThat(tokenResponse.token()).isEqualTo("generated_jwt_token");
            assertThat(tokenResponse.refreshToken()).isEqualTo("sample_refresh_token_hash");
            assertThat(tokenResponse.user()).isEqualTo(userDTO);
            assertThat(tokenResponse.roles()).containsExactly("ROLE_USER", "ROLE_ADMIN");
        }

        private void mockJwtProperties() {
            when(jwtProperties.exp()).thenReturn(expProperties);
            when(expProperties.session()).thenReturn(60L);
            when(expProperties.token()).thenReturn(15L);
        }
    }
}
