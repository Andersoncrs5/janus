package org.janus.modules.authorization.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.janus.help.BaseTest;
import org.janus.modules.authorization.application.dto.role.request.CreateRoleDTO;
import org.janus.modules.authorization.application.dto.role.request.UpdateRoleDTO;
import org.janus.modules.authorization.application.dto.role.response.RoleDTO;
import org.janus.shared.domain.api.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class RoleControllerTest extends BaseTest {

    private final String url = "/v1/role";

    @Nested
    @DisplayName("POST /v1/role")
    public class Post {

        @Test
        @DisplayName("Should create role successfully and return 201")
        void shouldCreateRoleSuccessfully() {
            TokenResponse masterToken = loginMasterHTTP();
            String key = generateRandomString();

            CreateRoleDTO dto = new CreateRoleDTO(
                    "Role " + key,
                    "Description " + key,
                    "role-" + key.toLowerCase()
            );

            RoleDTO result = given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .body(dto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(201)
                    .extract()
                    .jsonPath()
                    .getObject("data", RoleDTO.class);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getName()).isEqualTo(dto.name());
            assertThat(result.getSlug()).isEqualTo(dto.slug());
            assertThat(result.getDescription()).isEqualTo(dto.description());
            assertThat(result.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Should return 409 Conflict when slug already exists")
        void shouldReturn409WhenSlugAlreadyExists() {
            TokenResponse masterToken = loginMasterHTTP();
            String key = generateRandomString();

            CreateRoleDTO dto1 = new CreateRoleDTO(
                    "Role A " + key,
                    "Description A",
                    "duplicate-slug-" + key.toLowerCase()
            );

            CreateRoleDTO dto2 = new CreateRoleDTO(
                    "Role B " + key,
                    "Description B",
                    "duplicate-slug-" + key.toLowerCase()
            );

            // Primeira criação com sucesso
            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .body(dto1)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(201);

            // Segunda criação deve falhar por slug duplicado
            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .body(dto2)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(409);
        }

        @Test
        @DisplayName("Should return 409 Conflict when name already exists")
        void shouldReturn409WhenNameAlreadyExists() {
            TokenResponse masterToken = loginMasterHTTP();
            String key = generateRandomString();

            CreateRoleDTO dto1 = new CreateRoleDTO(
                    "Duplicate Name " + key,
                    "Description A",
                    "slug-a-" + key.toLowerCase()
            );

            CreateRoleDTO dto2 = new CreateRoleDTO(
                    "Duplicate Name " + key,
                    "Description B",
                    "slug-b-" + key.toLowerCase()
            );

            // Primeira criação
            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .body(dto1)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(201);

            // Segunda criação deve falhar por nome duplicado
            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .body(dto2)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(409);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when no auth header is provided")
        void shouldReturn401WhenUnauthenticated() {
            CreateRoleDTO dto = new CreateRoleDTO("Name", "Desc", "slug");

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(dto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when name is empty")
        void shouldReturn400WhenNameIsEmpty() {
            TokenResponse masterToken = loginMasterHTTP();

            CreateRoleDTO dto = new CreateRoleDTO(
                    "",
                    "Description",
                    "invalid-role-slug"
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .body(dto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(400);
        }
    }

    @Nested
    @DisplayName("GET /v1/role/{id}")
    public class GetById {

        @Test
        @DisplayName("Should return 200 OK with role data when found")
        void shouldReturn200WhenRoleExists() {
            TokenResponse masterToken = loginMasterHTTP();
            RoleDTO createdRole = createRoleHTTTP(masterToken);

            RoleDTO foundRole = given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .when()
                    .get(url + "/" + createdRole.getId())
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getObject("data", RoleDTO.class);

            assertThat(foundRole).isNotNull();
            assertThat(foundRole.getId()).isEqualTo(createdRole.getId());
        }

        @Test
        @DisplayName("Should return 404 Not Found when role does not exist")
        void shouldReturn404WhenRoleNotFound() {
            TokenResponse masterToken = loginMasterHTTP();
            UUID nonExistentId = UUID.randomUUID();

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .when()
                    .get(url + "/" + nonExistentId)
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when no auth header is provided")
        void shouldReturn401WhenUnauthenticated() {
            given()
                    .contentType(ContentType.JSON)
                    .when()
                    .get(url + "/" + UUID.randomUUID())
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("DELETE /v1/role/{id}")
    public class Delete {

        @Test
        @DisplayName("Should delete role successfully and return 200 OK")
        void shouldDeleteRoleSuccessfully() {
            TokenResponse masterToken = loginMasterHTTP();
            RoleDTO createdRole = createRoleHTTTP(masterToken);

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .when()
                    .delete(url + "/" + createdRole.getId())
                    .then()
                    .statusCode(200);

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .when()
                    .get(url + "/" + createdRole.getId())
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Should return 404 Not Found when attempting to delete non-existent role")
        void shouldReturn404WhenRoleNotFound() {
            TokenResponse masterToken = loginMasterHTTP();
            UUID nonExistentId = UUID.randomUUID();

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .when()
                    .delete(url + "/" + nonExistentId)
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when no auth header is provided")
        void shouldReturn401WhenUnauthenticated() {
            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .when()
                    .delete(url + "/" + UUID.randomUUID())
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("PATCH /v1/role/{id}")
    public class Patch {

        @Test
        @DisplayName("Should update role successfully and return 200 OK")
        void shouldUpdateRoleSuccessfully() {
            TokenResponse masterToken = loginMasterHTTP();
            RoleDTO createdRole = createRoleHTTTP(masterToken);
            String key = generateRandomString();

            UpdateRoleDTO updateDto = new UpdateRoleDTO(
                    "Updated Role " + key,
                    "Updated Description " + key,
                    false,
                    "updated-slug-" + key.toLowerCase()
            );

            RoleDTO updatedRole = given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .body(updateDto)
                    .when()
                    .patch(url + "/" + createdRole.getId())
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getObject("data", RoleDTO.class);

            assertThat(updatedRole).isNotNull();
            assertThat(updatedRole.getId()).isEqualTo(createdRole.getId());
            assertThat(updatedRole.getName()).isEqualTo(updateDto.name());
            assertThat(updatedRole.getDescription()).isEqualTo(updateDto.description());
            assertThat(updatedRole.getSlug()).isEqualTo(updateDto.slug());
            assertThat(updatedRole.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Should return 404 Not Found when role does not exist")
        void shouldReturn404WhenRoleNotFound() {
            TokenResponse masterToken = loginMasterHTTP();
            UUID nonExistentId = UUID.randomUUID();

            UpdateRoleDTO updateDto = new UpdateRoleDTO(
                    "Updated Role",
                    "Updated Description",
                    true,
                    "updated-slug"
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .body(updateDto)
                    .when()
                    .patch(url + "/" + nonExistentId)
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Should return 409 Conflict when updated slug already exists")
        void shouldReturn409WhenSlugAlreadyExists() {
            TokenResponse masterToken = loginMasterHTTP();
            RoleDTO role1 = createRoleHTTTP(masterToken);
            RoleDTO role2 = createRoleHTTTP(masterToken);

            UpdateRoleDTO updateDto = new UpdateRoleDTO(
                    "New Name",
                    "New Description",
                    true,
                    role1.getSlug()
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .body(updateDto)
                    .when()
                    .patch(url + "/" + role2.getId())
                    .then()
                    .statusCode(409);
        }

        @Test
        @DisplayName("Should return 409 Conflict when updated name already exists")
        void shouldReturn409WhenNameAlreadyExists() {
            TokenResponse masterToken = loginMasterHTTP();
            RoleDTO role1 = createRoleHTTTP(masterToken);
            RoleDTO role2 = createRoleHTTTP(masterToken);

            UpdateRoleDTO updateDto = new UpdateRoleDTO(
                    role1.getName(),
                    "New Description",
                    true,
                    "unique-slug-" + generateRandomString().toLowerCase()
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .body(updateDto)
                    .when()
                    .patch(url + "/" + role2.getId())
                    .then()
                    .statusCode(409);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when name is empty")
        void shouldReturn400WhenNameIsEmpty() {
            TokenResponse masterToken = loginMasterHTTP();
            RoleDTO role = createRoleHTTTP(masterToken);

            UpdateRoleDTO updateDto = new UpdateRoleDTO(
                    "",
                    "Description",
                    true,
                    "valid-slug"
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .body(updateDto)
                    .when()
                    .patch(url + "/" + role.getId())
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when no auth header is provided")
        void shouldReturn401WhenUnauthenticated() {
            UpdateRoleDTO updateDto = new UpdateRoleDTO(
                    "Name",
                    "Desc",
                    true,
                    "slug"
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(updateDto)
                    .when()
                    .patch(url + "/" + UUID.randomUUID())
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("GET /v1/role")
    public class FindAll {
        @Test
        @DisplayName("Should return 401 Unauthorized when no auth header is provided")
        void shouldReturn401WhenUnauthenticated() {
            given()
                    .contentType(ContentType.JSON)
                    .when()
                    .get(url)
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("GET /v1/role/exists/name")
    public class ExistsByName {

        @Test
        @DisplayName("Should return 200 OK with true when role name exists")
        void shouldReturnTrueWhenRoleNameExists() {
            TokenResponse masterToken = loginMasterHTTP();
            RoleDTO role = createRoleHTTTP(masterToken);

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("name", role.getName())
                    .when()
                    .get(url + "/exists/name")
                    .then()
                    .statusCode(200)
                    .body("data", equalTo(true));
        }

        @Test
        @DisplayName("Should return 200 OK with false when role name does not exist")
        void shouldReturnFalseWhenRoleNameDoesNotExist() {
            TokenResponse masterToken = loginMasterHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("name", "NonExistentRole_" + UUID.randomUUID())
                    .when()
                    .get(url + "/exists/name")
                    .then()
                    .statusCode(200)
                    .body("data", equalTo(false));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when name query parameter is blank")
        void shouldReturn400WhenNameIsBlank() {
            TokenResponse masterToken = loginMasterHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("name", "")
                    .when()
                    .get(url + "/exists/name")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when no auth header is provided")
        void shouldReturn401WhenUnauthenticated() {
            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("name", "SomeRoleName")
                    .when()
                    .get(url + "/exists/name")
                    .then()
                    .statusCode(401);
        }
    }

}