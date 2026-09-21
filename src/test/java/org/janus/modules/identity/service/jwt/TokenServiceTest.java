package org.janus.modules.identity.service.jwt;

import io.quarkus.security.UnauthorizedException;
import io.smallrye.jwt.auth.principal.JWTParser;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.janus.modules.identity.application.jwt.TokenService;
import org.janus.modules.identity.application.user.dto.response.UserDTO;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.shared.domain.api.TokenResponse;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {
    @Mock
    private JWTParser parser;
    @Mock
    private JwtProperties properties;
    @Mock
    private JwtProperties.Exp expProperties;
    @Mock
    private JsonWebToken currentJwt;
    @Mock
    private JsonWebToken parsedJwt;
    private TokenService tokenService;
    private UserEntity activeUser;
    private UserEntity inactiveUser;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(parser, properties, currentJwt);
        activeUser = new UserEntity();
        activeUser.setId(UUID.randomUUID());
        activeUser.setUsername("janus_user");
        activeUser.setEmail("master@janus.io");
        activeUser.setIsActive(true);
        inactiveUser = new UserEntity();
        inactiveUser.setId(UUID.randomUUID());
        inactiveUser.setUsername("inactive_user");
        inactiveUser.setEmail("inactive@janus.io");
        inactiveUser.setIsActive(false);
    }

    @Nested
    @DisplayName("1. Generate Token")
    class GenerateToken {
        @Test
        @DisplayName("Should return forbidden when user is inactive")
        void shouldReturnForbiddenWhenUserIsInactive() {
            Result<String> result = tokenService.generateToken(inactiveUser, List.of("ADMIN"), List.of("READ"));
            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(403);
            assertThat(result.getFirstError()).isEqualTo("User is unactive");
        }

        @Test
        @DisplayName("Should throw exception when user is null")
        void shouldThrowExceptionWhenUserIsNull() {
            assertThatThrownBy(() -> tokenService.generateToken(null, List.of("ADMIN"), List.of("READ"))).isInstanceOf(IllegalArgumentException.class).hasMessage("User and ID are required to set the claims.");
        }

        @Test
        @DisplayName("Should throw exception when user ID is null")
        void shouldThrowExceptionWhenUserIdIsNull() {
            UserEntity userWithoutId = new UserEntity();
            userWithoutId.setUsername("janus_user");
            userWithoutId.setEmail("master@janus.io");
            userWithoutId.setIsActive(true);
            assertThatThrownBy(() -> tokenService.generateToken(userWithoutId, List.of("ADMIN"), List.of("READ"))).isInstanceOf(IllegalArgumentException.class).hasMessage("User and ID are required to set the claims.");
        }

        @Test
        @DisplayName("Should generate token successfully")
        void shouldGenerateTokenSuccessfully() {
            when(properties.exp()).thenReturn(expProperties);
            when(expProperties.token()).thenReturn(24L);
            Result<String> result = tokenService.generateToken(activeUser, List.of("ADMIN"), List.of("READ", "WRITE"));
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotBlank();
            String token = result.getData();
            assertThat(token).contains(".").hasSizeGreaterThan(100);
            assertThat(token.split("\\.")).hasSize(3);
            verify(properties).exp();
            verify(expProperties).token();
        }

        @Test
        @DisplayName("Should generate token when roles are null")
        void shouldGenerateTokenWhenRolesAreNull() {
            when(properties.exp()).thenReturn(expProperties);
            when(expProperties.token()).thenReturn(24L);
            Result<String> result = tokenService.generateToken(activeUser, null, List.of("READ"));
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotBlank();
        }

        @Test
        @DisplayName("Should generate token when permissions are null")
        void shouldGenerateTokenWhenPermissionsAreNull() {
            when(properties.exp()).thenReturn(expProperties);
            when(expProperties.token()).thenReturn(24L);
            Result<String> result = tokenService.generateToken(activeUser, List.of("ADMIN"), null);
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotBlank();
        }

        @Test
        @DisplayName("Should generate token when roles and permissions are null")
        void shouldGenerateTokenWhenRolesAndPermissionsAreNull() {
            when(properties.exp()).thenReturn(expProperties);
            when(expProperties.token()).thenReturn(24L);
            Result<String> result = tokenService.generateToken(activeUser, null, null);
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("2. Validate Token")
    class ValidateToken {
        @Test
        @DisplayName("Should throw UnauthorizedException when token is null")
        void shouldThrowExceptionWhenTokenIsNull() {
            assertThatThrownBy(() -> tokenService.validateToken(null)).isInstanceOf(UnauthorizedException.class).hasMessage("Token missing");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when token is blank")
        void shouldThrowExceptionWhenTokenIsBlank() {
            assertThatThrownBy(() -> tokenService.validateToken(" ")).isInstanceOf(UnauthorizedException.class).hasMessage("Token missing");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when parser fails")
        void shouldThrowExceptionWhenParserFails() throws Exception {
            String invalidToken = "invalid.token.str";
            when(parser.parse(invalidToken)).thenThrow(new RuntimeException("Expired token"));
            assertThatThrownBy(() -> tokenService.validateToken(invalidToken)).isInstanceOf(UnauthorizedException.class).hasMessage("Invalid token: Expired token");
        }

        @Test
        @DisplayName("Should return subject when token is valid")
        void shouldReturnSubjectWhenTokenIsValid() throws Exception {
            String validToken = "valid.jwt.token";
            String expectedSubject = activeUser.getId().toString();
            when(parser.parse(validToken)).thenReturn(parsedJwt);
            when(parsedJwt.getSubject()).thenReturn(expectedSubject);
            Result<String> result = tokenService.validateToken(validToken);
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(expectedSubject);
            verify(parser).parse(validToken);
            verify(parsedJwt).getSubject();
        }
    }

    @Nested
    @DisplayName("3. Extract All Claims")
    class ExtractAllClaims {
        @Test
        @DisplayName("Should extract all claims successfully")
        void shouldExtractAllClaimsSuccessfully() throws Exception {
            String validToken = "valid.jwt.token";
            String expectedSubject = activeUser.getId().toString();
            when(parser.parse(validToken)).thenReturn(parsedJwt);
            when(parsedJwt.getClaimNames()).thenReturn(Set.of("sub", "email", "nickname"));
            when(parsedJwt.getClaim("sub")).thenReturn(expectedSubject);
            when(parsedJwt.getClaim("email")).thenReturn(activeUser.getEmail());
            when(parsedJwt.getClaim("nickname")).thenReturn(activeUser.getUsername());
            Result<Map<String, Object>> result = tokenService.extractAllClaims(validToken);
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            Map<String, Object> claims = result.getData();
            assertThat(claims).hasSize(3).containsEntry("sub", expectedSubject).containsEntry("email", activeUser.getEmail()).containsEntry("nickname", activeUser.getUsername());
            verify(parser).parse(validToken);
            verify(parsedJwt).getClaimNames();
        }

        @Test
        @DisplayName("Should return empty map when token has no claims")
        void shouldReturnEmptyMapWhenTokenHasNoClaims() throws Exception {
            String validToken = "valid.jwt.token";
            when(parser.parse(validToken)).thenReturn(parsedJwt);
            when(parsedJwt.getClaimNames()).thenReturn(Set.of());
            Result<Map<String, Object>> result = tokenService.extractAllClaims(validToken);
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when token is invalid")
        void shouldThrowUnauthorizedExceptionWhenTokenIsInvalid() throws Exception {
            String invalidToken = "invalid.token";
            when(parser.parse(invalidToken)).thenThrow(new RuntimeException("Malformed token"));
            assertThatThrownBy(() -> tokenService.extractAllClaims(invalidToken)).isInstanceOf(UnauthorizedException.class).hasMessage("Invalid token: Malformed token");
        }
    }

    @Nested
    @DisplayName("4. Make Tokens")
    class MakeTokens {
        @Test
        @DisplayName("Should return null while method is not implemented")
        void shouldReturnNull() {
            Result<TokenResponse> result = tokenService.makeTokens(activeUser, List.of("ADMIN"), new UserDTO());
            assertThat(result).isNull();
        }
    }
}