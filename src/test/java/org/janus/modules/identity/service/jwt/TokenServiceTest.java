package org.janus.modules.identity.service.jwt;

import io.quarkus.security.UnauthorizedException;
import io.smallrye.jwt.auth.principal.JWTParser;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.janus.modules.identity.application.jwt.TokenService;
import org.janus.modules.identity.application.user.dto.UserDTO;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.shared.domain.api.TokenResponse;
import org.janus.shared.domain.exception.InternalServerErrorException;
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
import static org.mockito.Mockito.*;

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
        lenient().when(properties.exp()).thenReturn(expProperties);
        lenient().when(expProperties.token()).thenReturn(24L);

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
    @DisplayName("1. Generate Token Tests")
    class GenerateToken {

        @Test
        @DisplayName("Should return 403 Forbidden when user is inactive")
        void shouldReturnForbiddenWhenUserIsInactive() {
            Result<String> result = tokenService.generateToken(inactiveUser, List.of("ADMIN"));

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(403);
            assertThat(result.getFirstError()).isEqualTo("User is unactive");
        }

    }

    @Nested
    @DisplayName("2. Validate Token Tests")
    class ValidateToken {

        @Test
        @DisplayName("Should throw UnauthorizedException when token is null or blank")
        void shouldThrowExceptionWhenTokenIsBlank() {
            assertThatThrownBy(() -> tokenService.validateToken(null))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Token missing");

            assertThatThrownBy(() -> tokenService.validateToken("   "))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Token missing");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when parser fails to parse token")
        void shouldThrowExceptionWhenParserFails() throws Exception {
            String invalidToken = "invalid.token.str";
            when(parser.parse(invalidToken)).thenThrow(new RuntimeException("Expired token"));

            assertThatThrownBy(() -> tokenService.validateToken(invalidToken))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid token: Expired token");
        }

        @Test
        @DisplayName("Should return subject when token is valid")
        void shouldReturnSubjectWhenTokenIsValid() throws Exception {
            String validToken = "valid.jwt.token";
            String expectedSubject = activeUser.getId().toString();

            when(parser.parse(validToken)).thenReturn(parsedJwt);
            when(parsedJwt.getSubject()).thenReturn(expectedSubject);

            Result<String> result = tokenService.validateToken(validToken);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(expectedSubject);
        }
    }

    @Nested
    @DisplayName("3. Extract All Claims Tests")
    class ExtractAllClaims {

        @Test
        @DisplayName("Should extract all claims into Map successfully")
        void shouldExtractAllClaimsSuccessfully() throws Exception {
            String validToken = "valid.jwt.token";

            when(parser.parse(validToken)).thenReturn(parsedJwt);
            when(parsedJwt.getClaimNames()).thenReturn(Set.of("sub", "email", "nickname"));
            when(parsedJwt.getClaim("sub")).thenReturn(activeUser.getId().toString());
            when(parsedJwt.getClaim("email")).thenReturn(activeUser.getEmail());
            when(parsedJwt.getClaim("nickname")).thenReturn(activeUser.getUsername());

            Result<Map<String, Object>> result = tokenService.extractAllClaims(validToken);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData())
                    .hasSize(3)
                    .containsEntry("sub", activeUser.getId().toString())
                    .containsEntry("email", activeUser.getEmail())
                    .containsEntry("nickname", activeUser.getUsername());
        }
    }

    @Nested
    @DisplayName("4. Make Tokens Tests")
    class MakeTokens {

        @Test
        @DisplayName("Should return null as currently implemented")
        void shouldReturnNull() {
            Result<TokenResponse> result = tokenService.makeTokens(activeUser, List.of("ADMIN"), new UserDTO());
            assertThat(result).isNull();
        }
    }
}