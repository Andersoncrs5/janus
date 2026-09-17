package org.janus.modules.identity.controller.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.janus.help.BaseTest;
import org.janus.modules.identity.application.auth.dto.LoginUserDTO;
import org.janus.shared.domain.api.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class AuthControllerTest extends BaseTest {

    private final String url = "/v1/auth";

    @Nested
    @DisplayName("POST /v1/auth/login")
    public class Login {

        @Test
        @DisplayName("Should log user master in with success (200)")
        void loginMaster() {
            TokenResponse tokenResponse = loginMasterHTTP();
            assertThat(tokenResponse).isNotNull();
        }

        @Test
        @DisplayName("Should log user in with success (200)")
        void shouldSuccess() {
            TokenResponse tokenResponse = createUserHTTP();
            assertThat(tokenResponse).isNotNull();

            LoginUserDTO dto = new LoginUserDTO(
                    tokenResponse.user().getEmail(),
                    "12345678"
            );

            TokenResponse result = given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(dto)
                    .when()
                    .post(url + "/login")
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getObject("data", TokenResponse.class);

            assertThat(result.user().getId()).isNotNull();
            assertThat(result.user().getEmail()).isEqualTo(dto.email());

            assertThat(result.token()).isNotBlank();
            assertThat(result.refreshToken()).isNotBlank();

            assertThat(result.expToken()).isInTheFuture();
            assertThat(result.expRefreshToken()).isInTheFuture();
        }

        @Test
        @DisplayName("Should fail login when user is not found (401)")
        void shouldFailUserNotFound() {
            LoginUserDTO dto = new LoginUserDTO(
                    "nonexistent.user." + UUID.randomUUID() + "@example.com",
                    "12345678"
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(dto)
                    .when()
                    .post(url + "/login")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Should fail login with invalid password (401)")
        void shouldFailInvalidPassword() {
            TokenResponse tokenResponse = createUserHTTP();

            LoginUserDTO dto = new LoginUserDTO(
                    tokenResponse.user().getEmail(),
                    "wrong_password"
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(dto)
                    .when()
                    .post(url + "/login")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Should lock user account after 3 failed password attempts (423)")
        void shouldLockUserAfterMaxFailedAttempts() {
            TokenResponse tokenResponse = createUserHTTP();
            String email = tokenResponse.user().getEmail();
            LoginUserDTO wrongPasswordDto = new LoginUserDTO(email, "wrong_password");

            for (int i = 0; i < 4; i++) {
                given()
                        .contentType(ContentType.JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("X-Forwarded-For", "10.0.0." + (i + 1))
                        .body(wrongPasswordDto)
                        .when()
                        .post(url + "/login")
                        .then()
                        .statusCode(401);
            }

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("X-Forwarded-For", "10.0.0.99")
                    .body(wrongPasswordDto)
                    .when()
                    .post(url + "/login")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Should block IP after 3 failed login attempts from same IP (429)")
        void shouldBlockIpAfterMaxFailedAttempts() {
            String targetIp = "192.168.10.50";

            for (int i = 0; i < 4; i++) {
                LoginUserDTO dto = new LoginUserDTO(
                        "invalid.user." + i + "." + UUID.randomUUID() + "@example.com",
                        "wrong_password"
                );

                given()
                        .contentType(ContentType.JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("X-Forwarded-For", targetIp)
                        .body(dto)
                        .when()
                        .post(url + "/login")
                        .then()
                        .statusCode(401);
            }

            LoginUserDTO dto = new LoginUserDTO("any.user@example.com", "12345678");

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("X-Forwarded-For", targetIp)
                    .body(dto)
                    .when()
                    .post(url + "/login")
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    public class Create {

        @Test
        @DisplayName("Should to create user with success (201)")
        void shouldCreateSuccess() {
            TokenResponse tokenResponse = createUserHTTP();
            assertThat(tokenResponse).isNotNull();
        }
    }

    @Nested
    @DisplayName("POST /v1/auth/logout-all")
    public class LogoutAll {

        @Test
        @DisplayName("Should revoke all user sessions and tokens with success (200)")
        void shouldLogoutAllSuccess() {
            TokenResponse tokenResponse = createUserHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + tokenResponse.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .when()
                    .post(url + "/logout-all")
                    .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Should fail when request is unauthorized without JWT (401)")
        void shouldFailUnauthorized() {
            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .when()
                    .post(url + "/logout-all")
                    .then()
                    .statusCode(401);
        }
    }

}