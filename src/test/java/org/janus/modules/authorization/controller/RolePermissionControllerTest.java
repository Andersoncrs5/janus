package org.janus.modules.authorization.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.janus.help.BaseTest;
import org.janus.modules.authorization.application.dto.permission.response.PermissionDTO;
import org.janus.modules.authorization.application.dto.role.response.RoleDTO;
import org.janus.modules.authorization.application.dto.rolePermission.request.CreateRolePermissionDTO;
import org.janus.modules.authorization.application.dto.rolePermission.request.UpdateRolePermissionDTO;
import org.janus.modules.authorization.application.dto.rolePermission.response.RolePermissionDTO;
import org.janus.shared.domain.api.TokenResponse;
import org.janus.shared.domain.enums.rolePermission.PermissionEffectEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class RolePermissionControllerTest extends BaseTest {
    private final String url = "/v1/role-permission";

    @Nested
    @DisplayName("POST /v1/role-permission")
    class Create {

        @Test
        @DisplayName("Should return 404 Not Found when role does not exist")
        void shouldReturn404WhenRoleNotFound() {
            TokenResponse master = loginMasterHTTP();
            PermissionDTO permission = createPermissionHTTP(master);
            UUID nonexistentRoleId = UUID.randomUUID();

            CreateRolePermissionDTO dto = new CreateRolePermissionDTO(
                    nonexistentRoleId,
                    permission.getId(),
                    PermissionEffectEnum.ALLOW,
                    "{\"scope\": \"all\"}",
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + master.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(dto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(404)
                    .body("message", equalTo("Role not found with ID: " + nonexistentRoleId));
        }

        @Test
        @DisplayName("Should return 404 Not Found when permission does not exist")
        void shouldReturn404WhenPermissionNotFound() {
            TokenResponse master = loginMasterHTTP();
            RoleDTO role = createRoleHTTTP(master);
            UUID nonexistentPermissionId = UUID.randomUUID();

            CreateRolePermissionDTO dto = new CreateRolePermissionDTO(
                    role.getId(),
                    nonexistentPermissionId,
                    PermissionEffectEnum.ALLOW,
                    "{\"scope\": \"all\"}",
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + master.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(dto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(404)
                    .body("message", equalTo("Permission not found with ID: " + nonexistentPermissionId));
        }

        @Test
        @DisplayName("Should return 409 Conflict when permission is already assigned to role")
        void shouldReturn409WhenPermissionAlreadyAssignedToRole() {
            TokenResponse master = loginMasterHTTP();
            PermissionDTO permission = createPermissionHTTP(master);
            RoleDTO role = createRoleHTTTP(master);

            createRolePermissionHTTP(master, permission, role);

            CreateRolePermissionDTO duplicateDto = new CreateRolePermissionDTO(
                    role.getId(),
                    permission.getId(),
                    PermissionEffectEnum.ALLOW,
                    "{\"scope\": \"all\"}",
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + master.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(duplicateDto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(409)
                    .body("message", equalTo("Permission is already assigned to this role"));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when JWT token is missing")
        void shouldReturn401WhenNoTokenProvided() {
            CreateRolePermissionDTO dto = new CreateRolePermissionDTO(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    PermissionEffectEnum.ALLOW,
                    null,
                    null
            );

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
        @DisplayName("Should assign permission to role successfully (201)")
        void shouldCreateRolePermissionSuccessfully() {
            TokenResponse master = loginMasterHTTP();
            PermissionDTO permission = createPermissionHTTP(master);
            RoleDTO role = createRoleHTTTP(master);

            CreateRolePermissionDTO dto = new CreateRolePermissionDTO(
                    role.getId(),
                    permission.getId(),
                    PermissionEffectEnum.ALLOW,
                    "{\"scope\": \"all\"}",
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + master.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(dto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(201)
                    .body("data.roleId", equalTo(role.getId().toString()))
                    .body("data.permissionId", equalTo(permission.getId().toString()))
                    .body("message", equalTo("Permission added to role!"));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/role-permission/{id}")
    class Update {

        @Test
        @DisplayName("Should update role permission successfully (200)")
        void shouldUpdateRolePermissionSuccessfully() {
            TokenResponse master = loginMasterHTTP();
            PermissionDTO permission = createPermissionHTTP(master);
            RoleDTO role = createRoleHTTTP(master);
            RolePermissionDTO rolePermission = createRolePermissionHTTP(master, permission, role);

            UpdateRolePermissionDTO dto = new UpdateRolePermissionDTO(
                    PermissionEffectEnum.DENY,
                    "{\"scope\": \"restricted\"}",
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + master.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(dto)
                    .when()
                    .patch(url + "/" + rolePermission.getId())
                    .then()
                    .statusCode(200)
                    .body("message", equalTo("OK"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when role permission does not exist")
        void shouldReturn400WhenRolePermissionNotFound() {
            TokenResponse master = loginMasterHTTP();
            UUID nonexistentId = UUID.randomUUID();

            UpdateRolePermissionDTO dto = new UpdateRolePermissionDTO(
                    PermissionEffectEnum.DENY,
                    "{\"scope\": \"restricted\"}",
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + master.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(dto)
                    .when()
                    .patch(url + "/" + nonexistentId)
                    .then()
                    .statusCode(400)
                    .body("message", equalTo("Role Permission not found"));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when JWT token is missing")
        void shouldReturn401WhenNoTokenProvidedOnUpdate() {
            UpdateRolePermissionDTO dto = new UpdateRolePermissionDTO(
                    PermissionEffectEnum.DENY,
                    null,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(dto)
                    .when()
                    .patch(url + "/" + UUID.randomUUID())
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("DELETE /v1/role-permission/{id}")
    class Delete {

        @Test
        @DisplayName("Should delete role permission successfully (200)")
        void shouldDeleteRolePermissionSuccessfully() {
            TokenResponse master = loginMasterHTTP();
            PermissionDTO permission = createPermissionHTTP(master);
            RoleDTO role = createRoleHTTTP(master);
            RolePermissionDTO rolePermission = createRolePermissionHTTP(master, permission, role);

            String idempotencyKey = UUID.randomUUID().toString();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + master.token())
                    .header("Idempotency-Key", idempotencyKey)
                    .when()
                    .delete(url + "/" + rolePermission.getId())
                    .then()
                    .statusCode(200)
                    .body("message", equalTo("Removed!!"));
        }

        @Test
        @DisplayName("Should return 404 Not Found when role permission does not exist")
        void shouldReturn404WhenRolePermissionNotFound() {
            TokenResponse master = loginMasterHTTP();
            UUID nonexistentId = UUID.randomUUID();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + master.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .when()
                    .delete(url + "/" + nonexistentId)
                    .then()
                    .statusCode(404)
                    .body("message", equalTo("Role Permission not found"));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when JWT token is missing")
        void shouldReturn401WhenNoTokenProvidedOnDelete() {
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
    @DisplayName("GET /v1/role-permission/exists/role/permission/{roleId}/{permissionId}")
    class Exists {

        @Test
        @DisplayName("Should return 200 OK with true when relationship exists")
        void shouldReturnTrueWhenExists() {
            TokenResponse master = loginMasterHTTP();
            PermissionDTO permission = createPermissionHTTP(master);
            RoleDTO role = createRoleHTTTP(master);
            createRolePermissionHTTP(master, permission, role);

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + master.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .when()
                    .get(url + "/exists/role/permission/" + role.getId() + "/" + permission.getId())
                    .then()
                    .statusCode(200)
                    .body("data", equalTo(true))
                    .body("message", equalTo("OK"));
        }

        @Test
        @DisplayName("Should return 200 OK with false when relationship does not exist")
        void shouldReturnFalseWhenDoesNotExist() {
            TokenResponse master = loginMasterHTTP();
            UUID randomRoleId = UUID.randomUUID();
            UUID randomPermissionId = UUID.randomUUID();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + master.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .when()
                    .get(url + "/exists/role/permission/" + randomRoleId + "/" + randomPermissionId)
                    .then()
                    .statusCode(200)
                    .body("data", equalTo(false))
                    .body("message", equalTo("OK"));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when JWT token is missing")
        void shouldReturn401WhenNoTokenProvidedOnExists() {
            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .when()
                    .get(url + "/exists/role/permission/" + UUID.randomUUID() + "/" + UUID.randomUUID())
                    .then()
                    .statusCode(401);
        }
    }
}