package org.janus.modules.authorization.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.janus.help.BaseTest;
import org.janus.modules.authorization.application.dto.role.response.RoleDTO;
import org.janus.modules.authorization.application.dto.userRole.request.CreateUserRoleDTO;
import org.janus.modules.authorization.application.dto.userRole.response.UserRoleDTO;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.shared.domain.api.TokenResponse;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class UserRoleControllerTest extends BaseTest {
    private final String url = "/v1/user-role";

    @Nested
    class Create {
        @Test
        void shouldCreateSuccess() {
            TokenResponse master = loginMasterHTTP();
            TokenResponse usr = createUserHTTP();
            RoleDTO role = createRoleHTTTP(master);

            UserRoleDTO userRoleHTTP = createUserRoleHTTP(master, usr, role);
        }

        @Test
        @DisplayName("Deve retornar 401 quando não informar o JWT")
        void shouldReturn401WhenNoTokenProvided() {

            CreateUserRoleDTO dto = new CreateUserRoleDTO(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .body(dto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Deve retornar 404 quando o usuário não existir")
        void shouldReturn404WhenUserNotFound() {
            TokenResponse master = loginMasterHTTP();

            RoleDTO role = createRoleHTTTP(master);

            UUID nonexistentUserId = UUID.randomUUID();

            CreateUserRoleDTO dto = new CreateUserRoleDTO(
                    nonexistentUserId,
                    role.getId(),
                    null,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .body(dto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Deve retornar 404 quando a Role não existir")
        void shouldReturn404WhenRoleNotFound() {
            TokenResponse master = loginMasterHTTP();

            TokenResponse user = createUserHTTP();

            UUID nonexistentRoleId = UUID.randomUUID();

            CreateUserRoleDTO dto = new CreateUserRoleDTO(
                    user.user().getId(),
                    nonexistentRoleId,
                    null,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .body(dto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Deve retornar 400 quando a Role estiver inativa")
        void shouldReturn400WhenRoleIsInactive() {
            TokenResponse master = loginMasterHTTP();

            TokenResponse user = createUserHTTP();

            RoleEntity role = initRole(false);

            CreateUserRoleDTO dto = new CreateUserRoleDTO(
                    user.user().getId(),
                    role.getId(),
                    null,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .body(dto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Deve retornar 403 ao tentar atribuir Role ao próprio usuário")
        void shouldReturn403WhenAssigningRoleToSelf() {
            TokenResponse master = loginMasterHTTP();

            RoleDTO role = createRoleHTTTP(master);

            UUID masterId = master.user().getId();

            CreateUserRoleDTO dto = new CreateUserRoleDTO(
                    masterId,
                    role.getId(),
                    null,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .body(dto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(403)
                    .body(
                            "message",
                            equalTo("Cannot assign roles to yourself")
                    );
        }

        @Test
        @DisplayName("Deve retornar 403 ao tentar atribuir a Role MASTER")
        void shouldReturn403WhenAssigningMasterRole() {
            TokenResponse master = loginMasterHTTP();

            TokenResponse user = createUserHTTP();

            RoleEntity masterRole = roleRepository.findByName("MASTER").orElseThrow(() -> new InternalServerErrorException(""));

            CreateUserRoleDTO dto = new CreateUserRoleDTO(
                    user.user().getId(),
                    masterRole.getId(),
                    null,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .body(dto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(403)
                    .body(
                            "message",
                            equalTo("Assigning the MASTER role is not allowed")
                    );
        }

        @Test
        @DisplayName("Deve retornar 409 quando o usuário já possuir a Role")
        void shouldReturn409WhenRoleAlreadyAssigned() {
            TokenResponse master = loginMasterHTTP();

            TokenResponse user = createUserHTTP();

            RoleDTO role = createRoleHTTTP(master);

            createUserRoleHTTP(
                    master,
                    user,
                    role
            );

            CreateUserRoleDTO duplicateDto = new CreateUserRoleDTO(
                    user.user().getId(),
                    role.getId(),
                    null,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .body(duplicateDto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(409)
                    .body(
                            "message",
                            equalTo("The user already has this role assigned")
                    );
        }

        @Test
        @DisplayName("Deve retornar 400 quando User ID não for informado")
        void shouldReturn400WhenUserIdIsNull() {
            TokenResponse master = loginMasterHTTP();

            RoleDTO role = createRoleHTTTP(master);

            CreateUserRoleDTO dto = new CreateUserRoleDTO(
                    null,
                    role.getId(),
                    null,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .body(dto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(400)
                    .body(
                            "message",
                            equalTo("User, role, and operator IDs are required")
                    );
        }

        @Test
        @DisplayName("Deve retornar 400 quando Role ID não for informado")
        void shouldReturn400WhenRoleIdIsNull() {
            TokenResponse master = loginMasterHTTP();

            TokenResponse user = createUserHTTP();

            CreateUserRoleDTO dto = new CreateUserRoleDTO(
                    user.user().getId(),
                    null,
                    null,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .body(dto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(400)
                    .body(
                            "message",
                            equalTo("User, role, and operator IDs are required")
                    );
        }
    }

    @Nested
    @DisplayName("DELETE /v1/user-role")
    class Delete {

        @Test
        @DisplayName("Deve retornar 401 quando não informar o JWT")
        void shouldReturn401WhenNoTokenProvided() {

            UUID id = UUID.randomUUID();

            given()
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .when()
                    .delete(url + "/" + id)
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Deve retornar 404 quando User Role não existir")
        void shouldReturn404WhenUserRoleNotFound() {

            TokenResponse master = loginMasterHTTP();

            UUID id = UUID.randomUUID();

            given()
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .when()
                    .delete(url + "/" + id)
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Deve remover User Role normal com sucesso")
        void shouldDeleteUserRoleSuccessfully() {

            TokenResponse master = loginMasterHTTP();
            TokenResponse user = createUserHTTP();
            RoleDTO role = createRoleHTTTP(master);

            UserRoleDTO userRole =
                    createUserRoleHTTP(
                            master,
                            user,
                            role
                    );

            given()
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .when()
                    .delete(
                            url + "/" + userRole.getId()
                    )
                    .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Deve retornar 403 ao tentar remover a Role MASTER")
        void shouldReturn403WhenDeletingMasterRole() {

            TokenResponse master = loginMasterHTTP();
            TokenResponse user = createUserHTTP();

            RoleEntity masterRole =
                    roleRepository.findByName("MASTER")
                            .orElseThrow(() ->
                                    new InternalServerErrorException("")
                            );

            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setUserId(user.user().getId());
            userRole.setRoleId(masterRole.getId());
            userRole.setAssignedById(master.user().getId());

            UserRoleEntity inserted =
                    userRoleRepository.insert(userRole);

            given()
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .when()
                    .delete(
                            url + "/" + inserted.getId()
                    )
                    .then()
                    .statusCode(403)
                    .body(
                            "message",
                            equalTo("MASTER role cannot be deleted")
                    );
        }

        @Test
        @DisplayName("Deve retornar 403 quando não-MASTER tentar remover ADMINISTRADOR")
        void shouldReturn403WhenNonMasterDeletesAdminRole() {

            TokenResponse master = loginMasterHTTP();

            TokenResponse operator = createUserHTTP();
            TokenResponse targetUser = createUserHTTP();

            RoleEntity adminRoleEntity = roleRepository.findByName("ADMINISTRADOR")
                    .orElseThrow(() -> new InternalServerErrorException(""));

            RoleDTO adminRole = roleMapper.toDTO(adminRoleEntity);

            UserRoleDTO userRole = createUserRoleHTTP(
                    master,
                    targetUser,
                    adminRole
            );

            given()
                    .header(
                            "Authorization",
                            "Bearer " + operator.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .when()
                    .delete(
                            url + "/" + userRole.getId()
                    )
                    .then()
                    .statusCode(403)
                    .body(
                            "message",
                            equalTo(
                                    "Only MASTER users can revoke the ADMINISTRADOR role"
                            )
                    );
        }

        @Test
        @DisplayName("Deve permitir MASTER remover ADMINISTRADOR")
        void shouldAllowMasterToDeleteAdminRole() {

            TokenResponse master = loginMasterHTTP();
            TokenResponse targetUser = createUserHTTP();

            RoleEntity adminRoleEntity = roleRepository.findByName("ADMINISTRADOR")
                    .orElseThrow(() -> new InternalServerErrorException(""));

            RoleDTO adminRole = roleMapper.toDTO(adminRoleEntity);

            UserRoleDTO userRole =
                    createUserRoleHTTP(
                            master,
                            targetUser,
                            adminRole
                    );

            given()
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .when()
                    .delete(
                            url + "/" + userRole.getId()
                    )
                    .then()
                    .statusCode(200);
        }
    }

    @Nested
    @DisplayName("GET /v1/user-role/exists")
    class Exists {

        @Test
        @DisplayName("Deve retornar 401 quando não informar o JWT")
        void shouldReturn401WhenNoTokenProvided() {

            UUID userId = UUID.randomUUID();
            UUID roleId = UUID.randomUUID();

            given()
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .when()
                    .get(url + "/exists/" + userId + "/" + roleId)
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Deve retornar true quando a User Role existir")
        void shouldReturnTrueWhenUserRoleExists() {

            TokenResponse master = loginMasterHTTP();
            TokenResponse user = createUserHTTP();
            RoleDTO role = createRoleHTTTP(master);

            createUserRoleHTTP(
                    master,
                    user,
                    role
            );

            given()
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .when()
                    .get(
                            url
                                    + "/exists/"
                                    + user.user().getId()
                                    + "/"
                                    + role.getId()
                    )
                    .then()
                    .statusCode(200)
                    .body("data", equalTo(true));
        }

        @Test
        @DisplayName("Deve retornar false quando a User Role não existir")
        void shouldReturnFalseWhenUserRoleDoesNotExist() {

            TokenResponse master = loginMasterHTTP();
            TokenResponse user = createUserHTTP();
            RoleDTO role = createRoleHTTTP(master);

            given()
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .when()
                    .get(
                            url
                                    + "/exists/"
                                    + user.user().getId()
                                    + "/"
                                    + role.getId()
                    )
                    .then()
                    .statusCode(200)
                    .body("data", equalTo(false));
        }
    }

    @Nested
    @DisplayName("GET /v1/user-role/exists/active")
    class ExistsActive {

        @Test
        @DisplayName("Deve retornar 401 quando não informar o JWT")
        void shouldReturn401WhenNoTokenProvided() {

            UUID userId = UUID.randomUUID();
            UUID roleId = UUID.randomUUID();

            given()
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .when()
                    .get(
                            url
                                    + "/exists/active/"
                                    + userId
                                    + "/"
                                    + roleId
                    )
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Deve retornar true quando a User Role estiver ativa")
        void shouldReturnTrueWhenUserRoleIsActive() {

            TokenResponse master = loginMasterHTTP();
            TokenResponse user = createUserHTTP();
            RoleDTO role = createRoleHTTTP(master);

            createUserRoleHTTP(
                    master,
                    user,
                    role
            );

            given()
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .when()
                    .get(
                            url
                                    + "/exists/active/"
                                    + user.user().getId()
                                    + "/"
                                    + role.getId()
                    )
                    .then()
                    .statusCode(200)
                    .body("data", equalTo(true));
        }

        @Test
        @DisplayName("Deve retornar false quando a User Role não existir")
        void shouldReturnFalseWhenUserRoleDoesNotExist() {

            TokenResponse master = loginMasterHTTP();
            TokenResponse user = createUserHTTP();
            RoleDTO role = createRoleHTTTP(master);

            given()
                    .header(
                            "Authorization",
                            "Bearer " + master.token()
                    )
                    .header(
                            "Idempotency-Key",
                            UUID.randomUUID().toString()
                    )
                    .when()
                    .get(
                            url
                                    + "/exists/active/"
                                    + user.user().getId()
                                    + "/"
                                    + role.getId()
                    )
                    .then()
                    .statusCode(200)
                    .body("data", equalTo(false));
        }
    }
    
}