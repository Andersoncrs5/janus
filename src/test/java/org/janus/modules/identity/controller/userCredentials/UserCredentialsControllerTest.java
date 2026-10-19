package org.janus.modules.identity.controller.userCredentials;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.janus.help.BaseTest;
import org.janus.modules.identity.application.userCredentials.dto.UpdateUserCredentialsDTO;
import org.janus.modules.identity.application.userCredentials.dto.UserCredentialsDTO;
import org.janus.shared.domain.api.TokenResponse;
import org.janus.shared.domain.enums.PasswordAlgorithmEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("UserCredentialsController")
public class UserCredentialsControllerTest extends BaseTest {

    private final String url = "/v1/user-credentials";

    @Nested
    @DisplayName("PATCH /v1/user-credentials/{userId}")
    public class Update {

        @Test
        @DisplayName("Should update user credentials successfully (200)")
        void success() {
            TokenResponse masterToken = loginMasterHTTP();
            TokenResponse userToken = createUserHTTP();

            UUID userId = userToken.user().getId();

            UpdateUserCredentialsDTO updateDTO = new UpdateUserCredentialsDTO(
                    "NewSecurePassword123!",
                    PasswordAlgorithmEnum.ARGON2ID
            );

            UserCredentialsDTO result = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(updateDTO)
                    .when()
                    .patch(url + "/" + userId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getObject("data", UserCredentialsDTO.class);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getAlgorithm())
                    .isEqualTo(PasswordAlgorithmEnum.ARGON2ID.getValue());
        }

        @Test
        @DisplayName("Should return 404 when user credentials do not exist")
        void failCredentialsNotFound() {
            TokenResponse masterToken = loginMasterHTTP();

            UpdateUserCredentialsDTO updateDTO = new UpdateUserCredentialsDTO(
                    "NewSecurePassword123!",
                    PasswordAlgorithmEnum.ARGON2ID
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(updateDTO)
                    .when()
                    .patch(url + "/" + UUID.randomUUID())
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Should return 401 when JWT token is missing")
        void failTokenMissing() {
            UpdateUserCredentialsDTO updateDTO = new UpdateUserCredentialsDTO(
                    "NewSecurePassword123!",
                    PasswordAlgorithmEnum.ARGON2ID
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(updateDTO)
                    .when()
                    .patch(url + "/" + UUID.randomUUID())
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Should return 400 when Idempotency-Key header is missing")
        void failIdempotency() {
            TokenResponse masterToken = loginMasterHTTP();
            TokenResponse userToken = createUserHTTP();

            UpdateUserCredentialsDTO updateDTO = new UpdateUserCredentialsDTO(
                    "NewSecurePassword123!",
                    PasswordAlgorithmEnum.ARGON2ID
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .body(updateDTO)
                    .when()
                    .patch(url + "/" + userToken.user().getId())
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Should return 400 when password is blank")
        void failBlankPassword() {
            TokenResponse masterToken = loginMasterHTTP();
            TokenResponse userToken = createUserHTTP();

            UpdateUserCredentialsDTO updateDTO = new UpdateUserCredentialsDTO(
                    " ",
                    PasswordAlgorithmEnum.ARGON2ID
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(updateDTO)
                    .when()
                    .patch(url + "/" + userToken.user().getId())
                    .then()
                    .statusCode(400);
        }
    }
}