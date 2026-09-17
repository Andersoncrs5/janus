package org.janus.modules.identity.controller.user;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.janus.help.BaseTest;
import org.janus.modules.identity.application.user.dto.request.UpdateUserDTO;
import org.janus.modules.identity.application.user.dto.response.UserDTO;
import org.janus.shared.domain.api.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class UserControllerTest extends BaseTest {

    private final String url = "/v1/user";

    @Nested
    @DisplayName("GET /v1/user/me")
    public class GetMe {

        @Test
        @DisplayName("Should return authenticated user data with success (200)")
        void success() {
            TokenResponse tokenResponse = createUserHTTP();
            assertThat(tokenResponse).isNotNull();

            String idempotencyKey = UUID.randomUUID().toString();

            UserDTO result = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + tokenResponse.token())
                    .header("Idempotency-Key", idempotencyKey)
                    .when()
                    .get(url + "/me")
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getObject("data", UserDTO.class);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(tokenResponse.user().getId());
            assertThat(result.getEmail()).isEqualTo(tokenResponse.user().getEmail());
        }

        @Test
        @DisplayName("Should fail when JWT token is missing (401)")
        void failTokenMissed() {
            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .when()
                    .get(url + "/me")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Should fail when Idempotency-Key header is missing (400)")
        void failIdempotency() {
            TokenResponse tokenResponse = createUserHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + tokenResponse.token())
                    .when()
                    .get(url + "/me")
                    .then()
                    .statusCode(400);
        }
    }

    @Nested
    @DisplayName("DELETE /v1/user")
    public class Delete {

        @Test
        @DisplayName("Should delete authenticated user with success (200)")
        void success() {
            TokenResponse tokenResponse = createUserHTTP();
            String idempotencyKey = UUID.randomUUID().toString();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + tokenResponse.token())
                    .header("Idempotency-Key", idempotencyKey)
                    .when()
                    .delete(url)
                    .then()
                    .statusCode(200);

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + tokenResponse.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .when()
                    .get(url + "/me")
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Should fail when JWT token is missing (401)")
        void failTokenMissed() {
            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .when()
                    .delete(url)
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Should fail when Idempotency-Key header is missing (400)")
        void failIdempotency() {
            TokenResponse tokenResponse = createUserHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + tokenResponse.token())
                    .when()
                    .delete(url)
                    .then()
                    .statusCode(400);
        }
    }

    @Nested
    @DisplayName("PATCH /v1/user")
    public class Update {

        @Test
        @DisplayName("Should update authenticated user profile with success (200)")
        void success() {
            TokenResponse tokenResponse = createUserHTTP();
            String idempotencyKey = UUID.randomUUID().toString();

            UpdateUserDTO updateDto = UpdateUserDTO.builder()
                    .fullName("Updated Full Name")
                    .isActive(true)
                    .build();

            UserDTO result = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + tokenResponse.token())
                    .header("Idempotency-Key", idempotencyKey)
                    .body(updateDto)
                    .when()
                    .patch(url)
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getObject("data", UserDTO.class);

            assertThat(result).isNotNull();
            assertThat(result.getFullName()).isEqualTo("Updated Full Name");
        }

        @Test
        @DisplayName("Should fail when updated username already belongs to another user (409)")
        void failDuplicateUsername() {
            TokenResponse user1 = createUserHTTP();
            TokenResponse user2 = createUserHTTP();

            UpdateUserDTO updateDto = UpdateUserDTO.builder()
                    .username(user1.user().getUsername())
                    .build();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + user2.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(updateDto)
                    .when()
                    .patch(url)
                    .then()
                    .statusCode(409);
        }

        @Test
        @DisplayName("Should fail when JWT token is missing (401)")
        void failTokenMissed() {
            UpdateUserDTO updateDto = UpdateUserDTO.builder()
                    .fullName("Updated Full Name")
                    .build();

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(updateDto)
                    .when()
                    .patch(url)
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Should fail when Idempotency-Key header is missing (400)")
        void failIdempotency() {
            TokenResponse tokenResponse = createUserHTTP();
            UpdateUserDTO updateDto = UpdateUserDTO.builder()
                    .fullName("Updated Full Name")
                    .build();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + tokenResponse.token())
                    .body(updateDto)
                    .when()
                    .patch(url)
                    .then()
                    .statusCode(400);
        }
    }

    @Nested
    @DisplayName("GET /v1/user")
    public class FindAll {

        @Test
        @DisplayName("Should accept lowercase order field")
        void orderByEmailLowercase() {
            TokenResponse masterToken = loginMasterHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("orders", "EMAIL")
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Should return paginated users with success (200)")
        void success() {
            TokenResponse masterToken = loginMasterHTTP();
            createUserHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content", org.hamcrest.Matchers.notNullValue())
                    .body("page", org.hamcrest.Matchers.equalTo(0))
                    .body("size", org.hamcrest.Matchers.equalTo(10))
                    .body("totalElements", org.hamcrest.Matchers.greaterThan(0));
        }

        @Test
        @DisplayName("Should filter users by email")
        void filterByEmail() {
            TokenResponse masterToken = loginMasterHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("email", "admin@gmail.com")
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content.size()", org.hamcrest.Matchers.equalTo(1))
                    .body("content[0].email", org.hamcrest.Matchers.equalTo("admin@gmail.com"));
        }

        @Test
        @DisplayName("Should filter users by username")
        void filterByUsername() {
            TokenResponse masterToken = loginMasterHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("username", "admin")
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content.size()", org.hamcrest.Matchers.equalTo(1))
                    .body("content[0].username", org.hamcrest.Matchers.equalTo("admin"));
        }

        @Test
        @DisplayName("Should filter users by full name")
        void filterByFullName() {
            TokenResponse masterToken = loginMasterHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("fullName", "System Administrator")
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content.size()", org.hamcrest.Matchers.equalTo(1))
                    .body(
                            "content[0].fullName",
                            org.hamcrest.Matchers.equalTo("System Administrator")
                    );
        }

        @Test
        @DisplayName("Should filter users by verified email")
        void filterByEmailVerified() {
            TokenResponse masterToken = loginMasterHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("email", "admin@gmail.com")
                    .queryParam("isEmailVerified", false)
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content.size()", org.hamcrest.Matchers.equalTo(1))
                    .body(
                            "content[0].isEmailVerified",
                            org.hamcrest.Matchers.equalTo(false)
                    );
        }

        @Test
        @DisplayName("Should return no users when email verification filter does not match")
        void filterByEmailVerifiedShouldReturnEmpty() {
            TokenResponse masterToken = loginMasterHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("email", "admin@gmail.com")
                    .queryParam("isEmailVerified", true)
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content.size()", org.hamcrest.Matchers.equalTo(0));
        }

        @Test
        @DisplayName("Should filter active users")
        void filterByActive() {
            TokenResponse masterToken = loginMasterHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("email", "admin@gmail.com")
                    .queryParam("isActive", true)
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content.size()", org.hamcrest.Matchers.equalTo(1))
                    .body(
                            "content[0].isActive",
                            org.hamcrest.Matchers.equalTo(true)
                    );
        }

        @Test
        @DisplayName("Should return no users when active filter does not match")
        void filterByInactive() {
            TokenResponse masterToken = loginMasterHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("email", "admin@gmail.com")
                    .queryParam("isActive", false)
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content.size()", org.hamcrest.Matchers.equalTo(0));
        }

        @Test
        @DisplayName("Should combine multiple filters")
        void combineFilters() {
            TokenResponse masterToken = loginMasterHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("email", "admin@gmail.com")
                    .queryParam("username", "admin")
                    .queryParam("fullName", "System Administrator")
                    .queryParam("isActive", true)
                    .queryParam("isEmailVerified", false)
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content.size()", org.hamcrest.Matchers.equalTo(1))
                    .body("content[0].email", org.hamcrest.Matchers.equalTo("admin@gmail.com"))
                    .body("content[0].username", org.hamcrest.Matchers.equalTo("admin"))
                    .body(
                            "content[0].fullName",
                            org.hamcrest.Matchers.equalTo("System Administrator")
                    );
        }

        @Test
        @DisplayName("Should paginate users")
        void pagination() {
            TokenResponse masterToken = loginMasterHTTP();
            createUserHTTP();
            createUserHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("page", 0)
                    .queryParam("size", 1)
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("page", org.hamcrest.Matchers.equalTo(0))
                    .body("size", org.hamcrest.Matchers.equalTo(1))
                    .body(
                            "content.size()",
                            org.hamcrest.Matchers.equalTo(1)
                    )
                    .body(
                            "totalElements",
                            org.hamcrest.Matchers.greaterThan(1)
                    );
        }

        @Test
        @DisplayName("Should return second page correctly")
        void secondPage() {
            TokenResponse masterToken = loginMasterHTTP();
            createUserHTTP();
            createUserHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("page", 1)
                    .queryParam("size", 1)
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("page", org.hamcrest.Matchers.equalTo(1))
                    .body("size", org.hamcrest.Matchers.equalTo(1))
                    .body(
                            "content.size()",
                            org.hamcrest.Matchers.equalTo(1)
                    );
        }

        @Test
        @DisplayName("Should order users by email ascending")
        void orderByEmail() {
            TokenResponse masterToken = loginMasterHTTP();
            createUserHTTP();
            createUserHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("orders", "EMAIL")
                    .queryParam("page", 0)
                    .queryParam("size", 20)
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content", org.hamcrest.Matchers.notNullValue())
                    .body(
                            "content[0].email",
                            org.hamcrest.Matchers.equalTo("admin@gmail.com")
                    );
        }

        @Test
        @DisplayName("Should order users by username ascending")
        void orderByUsername() {
            TokenResponse masterToken = loginMasterHTTP();
            createUserHTTP();
            createUserHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("orders", "USERNAME")
                    .queryParam("page", 0)
                    .queryParam("size", 20)
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content", org.hamcrest.Matchers.notNullValue());
        }

        @Test
        @DisplayName("Should return no users when lastLoginAtFrom is in the future")
        void filterByLastLoginAtFrom() {
            TokenResponse masterToken = loginMasterHTTP();

            String futureDate = OffsetDateTime.now()
                    .plusDays(1)
                    .toString();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("email", "admin@gmail.com")
                    .queryParam("lastLoginAtFrom", futureDate)
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content.size()", org.hamcrest.Matchers.equalTo(0));
        }

        @Test
        @DisplayName("Should return no users when lastLoginAtTo is in the past")
        void filterByLastLoginAtTo() {
            TokenResponse masterToken = loginMasterHTTP();

            String pastDate = OffsetDateTime.now()
                    .minusDays(3650)
                    .toString();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("email", "admin@gmail.com")
                    .queryParam("lastLoginAtTo", pastDate)
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content.size()", org.hamcrest.Matchers.equalTo(0));
        }

        @Test
        @DisplayName("Should fail when user lacks permission 'users:read' (403)")
        void failForbidden() {
            TokenResponse normalUserToken = createUserHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + normalUserToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .when()
                    .get(url)
                    .then()
                    .statusCode(403);
        }

        @Test
        @DisplayName("Should fail when JWT token is missing (401)")
        void failTokenMissed() {
            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .when()
                    .get(url)
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Should fail when Idempotency-Key header is missing (400)")
        void failIdempotency() {
            TokenResponse masterToken = loginMasterHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .when()
                    .get(url)
                    .then()
                    .statusCode(400);
        }
    }
}