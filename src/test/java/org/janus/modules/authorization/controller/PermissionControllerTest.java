package org.janus.modules.authorization.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.janus.help.BaseTest;
import org.janus.modules.authorization.application.dto.permission.request.CreatePermissionDTO;
import org.janus.modules.authorization.application.dto.permission.request.UpdatePermissionDTO;
import org.janus.modules.authorization.application.dto.permission.response.PermissionDTO;
import org.janus.shared.domain.api.TokenResponse;
import org.janus.shared.domain.enums.permission.PermissionModule;
import org.janus.shared.domain.enums.permission.PermissionResource;
import org.janus.shared.domain.enums.permission.PermissionRiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class PermissionControllerTest extends BaseTest {

    private final String url = "/v1/permission";

    @Nested
    @DisplayName("POST /v1/permission")
    public class Create {

        @Test
        @DisplayName("Deve criar permissão com sucesso")
        void shouldCreatePermissionSuccessfully() {
            TokenResponse masterToken = loginMasterHTTP();
            PermissionDTO permission = createPermissionHTTP(masterToken);
        }

        @Test
        @DisplayName("Deve retornar 409 Conflict ao tentar criar permissão com slug já existente")
        void shouldReturn409WhenSlugAlreadyExists() {
            TokenResponse masterToken = loginMasterHTTP();
            String key = generateRandomString().toLowerCase();
            String slug = "dup-slug-" + key;

            CreatePermissionDTO firstDto = new CreatePermissionDTO(
                    "Name-1-" + key,
                    slug,
                    "Descrição 1",
                    PermissionModule.IDENTITY,
                    PermissionResource.USER,
                    "action-1-" + key,
                    PermissionRiskLevel.LOW,
                    true,
                    false,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(firstDto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(201);

            CreatePermissionDTO secondDto = new CreatePermissionDTO(
                    "Name-2-" + key,
                    slug,
                    "Descrição 2",
                    PermissionModule.IDENTITY,
                    PermissionResource.USER,
                    "action-2-" + key,
                    PermissionRiskLevel.HIGH,
                    true,
                    false,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(secondDto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(409)
                    .body("success", equalTo(false))
                    .body("message", containsString("slug"));
        }

        @Test
        @DisplayName("Deve retornar 409 Conflict ao tentar criar permissão com nome já existente")
        void shouldReturn409WhenNameAlreadyExists() {
            TokenResponse masterToken = loginMasterHTTP();
            String key = generateRandomString().toLowerCase();
            String name = "Dup-Name-" + key;

            CreatePermissionDTO firstDto = new CreatePermissionDTO(
                    name,
                    "slug-1-" + key,
                    "Descrição 1",
                    PermissionModule.IDENTITY,
                    PermissionResource.USER,
                    "action-1-" + key,
                    PermissionRiskLevel.LOW,
                    true,
                    false,
                    null
            );

            // Cria a primeira permissão
            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(firstDto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(201);

            // Tenta criar outra permissão reutilizando o mesmo nome
            CreatePermissionDTO secondDto = new CreatePermissionDTO(
                    name,
                    "slug-2-" + key,
                    "Descrição 2",
                    PermissionModule.IDENTITY,
                    PermissionResource.USER,
                    "action-2-" + key,
                    PermissionRiskLevel.HIGH,
                    true,
                    false,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(secondDto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(409)
                    .body("success", equalTo(false))
                    .body("message", containsString("name"));
        }

        @Test
        @DisplayName("Deve retornar 409 Conflict ao tentar criar permissão com mesmo módulo, recurso e ação")
        void shouldReturn409WhenModuleResourceActionAlreadyExists() {
            TokenResponse masterToken = loginMasterHTTP();
            String key = generateRandomString().toLowerCase();
            String action = "action-dup-" + key;

            CreatePermissionDTO firstDto = new CreatePermissionDTO(
                    "Name-1-" + key,
                    "slug-1-" + key,
                    "Descrição 1",
                    PermissionModule.IDENTITY,
                    PermissionResource.USER,
                    action,
                    PermissionRiskLevel.LOW,
                    true,
                    false,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(firstDto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(201);

            CreatePermissionDTO secondDto = new CreatePermissionDTO(
                    "Name-2-" + key,
                    "slug-2-" + key,
                    "Descrição 2",
                    PermissionModule.IDENTITY,
                    PermissionResource.USER,
                    action,
                    PermissionRiskLevel.HIGH,
                    true,
                    false,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(secondDto)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(409)
                    .body("success", equalTo(false))
                    .body("message", containsString("module, resource, and action"));
        }

        @Test
        @DisplayName("Deve retornar 401 Unauthorized quando não for enviado o token JWT")
        void shouldReturn401WhenNoTokenProvided() {
            String key = generateRandomString().toLowerCase();
            CreatePermissionDTO dto = new CreatePermissionDTO(
                    "Unauth-Name-" + key,
                    "unauth-slug-" + key,
                    "Descrição",
                    PermissionModule.IDENTITY,
                    PermissionResource.USER,
                    "read",
                    PermissionRiskLevel.LOW,
                    true,
                    false,
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
    }

    @Nested
    @DisplayName("GET /v1/permission/{id}")
    public class GetById {

        @Test
        @DisplayName("Deve buscar permissão por ID com sucesso")
        void shouldGetPermissionByIdSuccessfully() {
            TokenResponse masterToken = loginMasterHTTP();
            PermissionDTO createdPermission = createPermissionHTTP(masterToken);

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .pathParam("id", createdPermission.getId())
                    .when()
                    .get(url + "/{id}")
                    .then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.id", equalTo(createdPermission.getId().toString()))
                    .body("data.slug", equalTo(createdPermission.getSlug()))
                    .body("data.name", equalTo(createdPermission.getName()));
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando o ID não existir no banco")
        void shouldReturn404WhenPermissionNotFound() {
            TokenResponse masterToken = loginMasterHTTP();
            UUID nonExistentId = UUID.randomUUID();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .pathParam("id", nonExistentId)
                    .when()
                    .get(url + "/{id}")
                    .then()
                    .statusCode(404)
                    .body("success", equalTo(false))
                    .body("message", containsString("Permission not found"));
        }


        @Test
        @DisplayName("Deve retornar 401 Unauthorized quando não for enviado o token JWT")
        void shouldReturn401WhenNoTokenProvided() {
            UUID permissionId = UUID.randomUUID();

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .pathParam("id", permissionId)
                    .when()
                    .get(url + "/{id}")
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("DELETE /v1/permission/{id}")
    public class Delete {

        @Test
        @DisplayName("Deve deletar permissão por ID com sucesso")
        void shouldDeletePermissionSuccessfully() {
            TokenResponse masterToken = loginMasterHTTP();
            PermissionDTO createdPermission = createPermissionHTTP(masterToken);

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .pathParam("id", createdPermission.getId())
                    .when()
                    .delete(url + "/{id}")
                    .then()
                    .statusCode(200)
                    .body("success", equalTo(true));

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .pathParam("id", createdPermission.getId())
                    .when()
                    .get(url + "/{id}")
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando a permissão a ser deletada não existir")
        void shouldReturn404WhenPermissionNotFound() {
            TokenResponse masterToken = loginMasterHTTP();
            UUID nonExistentId = UUID.randomUUID();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .pathParam("id", nonExistentId)
                    .when()
                    .delete(url + "/{id}")
                    .then()
                    .statusCode(404)
                    .body("success", equalTo(false))
                    .body("message", containsString("Permission not found"));
        }

        @Test
        @DisplayName("Deve retornar 401 Unauthorized quando não for enviado o token JWT")
        void shouldReturn401WhenNoTokenProvided() {
            UUID permissionId = UUID.randomUUID();

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .pathParam("id", permissionId)
                    .when()
                    .delete(url + "/{id}")
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("GET /v1/permission/exists/name")
    public class ExistsByName {

        @Test
        @DisplayName("Deve retornar true quando a permissão existir por nome")
        void shouldReturnTrueWhenPermissionExistsByName() {
            TokenResponse masterToken = loginMasterHTTP();
            PermissionDTO permission = createPermissionHTTP(masterToken);

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("name", permission.getName())
                    .when()
                    .get(url + "/exists/name")
                    .then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data", equalTo(true));
        }

        @Test
        @DisplayName("Deve retornar false quando a permissão não existir por nome")
        void shouldReturnFalseWhenPermissionDoesNotExistByName() {
            TokenResponse masterToken = loginMasterHTTP();
            String nonExistentName = "non-existent-name-" + generateRandomString();

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("name", nonExistentName)
                    .when()
                    .get(url + "/exists/name")
                    .then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data", equalTo(false));
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando o parâmetro name for omitido")
        void shouldReturn400WhenNameParamIsMissing() {
            TokenResponse masterToken = loginMasterHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .when()
                    .get(url + "/exists/name")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Deve retornar 401 Unauthorized quando não for enviado o token JWT")
        void shouldReturn401WhenNoTokenProvided() {
            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("name", "any-name")
                    .when()
                    .get(url + "/exists/name")
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("GET /v1/permission/exists/slug")
    public class ExistsBySlug {

        @Test
        @DisplayName("Deve retornar true quando a permissão existir por slug")
        void shouldReturnTrueWhenPermissionExistsBySlug() {
            TokenResponse masterToken = loginMasterHTTP();
            PermissionDTO permission = createPermissionHTTP(masterToken);

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .queryParam("slug", permission.getSlug())
                    .when()
                    .get(url + "/exists/slug")
                    .then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data", equalTo(true));
        }

        @Test
        @DisplayName("Deve retornar false quando a permissão não existir por slug")
        void shouldReturnFalseWhenPermissionDoesNotExistBySlug() {
            TokenResponse masterToken = loginMasterHTTP();
            String nonExistentSlug = "non-existent-slug-" + generateRandomString();

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .queryParam("slug", nonExistentSlug)
                    .when()
                    .get(url + "/exists/slug")
                    .then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data", equalTo(false));
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando o parâmetro slug for omitido")
        void shouldReturn400WhenSlugParamIsMissing() {
            TokenResponse masterToken = loginMasterHTTP();

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .when()
                    .get(url + "/exists/slug")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Deve retornar 401 Unauthorized quando não for enviado o token JWT")
        void shouldReturn401WhenNoTokenProvided() {
            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .queryParam("slug", "any-slug")
                    .when()
                    .get(url + "/exists/slug")
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("PATCH /v1/permission/{id}")
    public class Update {

        @Test
        @DisplayName("Deve atualizar permissão por ID com sucesso")
        void shouldUpdatePermissionSuccessfully() {
            TokenResponse masterToken = loginMasterHTTP();
            PermissionDTO createdPermission = createPermissionHTTP(masterToken);

            String key = generateRandomString().toLowerCase();
            UpdatePermissionDTO updateDto = new UpdatePermissionDTO(
                    "Updated-Name-" + key,
                    "updated-slug-" + key,
                    "Nova descrição atualizada",
                    PermissionModule.IDENTITY,
                    PermissionResource.USER,
                    "updated-action-" + key,
                    PermissionRiskLevel.HIGH,
                    true,
                    false,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .pathParam("id", createdPermission.getId())
                    .body(updateDto)
                    .when()
                    .patch(url + "/{id}")
                    .then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.id", equalTo(createdPermission.getId().toString()))
                    .body("data.name", equalTo("Updated-Name-" + key))
                    .body("data.slug", equalTo("updated-slug-" + key));
        }

        @Test
        @DisplayName("Deve retornar 409 Conflict ao tentar atualizar permissão com slug já pertencente a outra permissão")
        void shouldReturn409WhenSlugAlreadyExists() {
            TokenResponse masterToken = loginMasterHTTP();
            String key = generateRandomString().toLowerCase();

            // Permissão 1
            PermissionDTO permission1 = createPermissionHTTP(masterToken);

            // Permissão 2 (da qual pegaremos o slug)
            PermissionDTO permission2 = createPermissionHTTP(masterToken);

            // Tenta atualizar a permissão 1 usando o slug da permissão 2
            UpdatePermissionDTO updateDto = new UpdatePermissionDTO(
                    "Name-Updated-" + key,
                    permission2.getSlug(),
                    "Descrição",
                    PermissionModule.IDENTITY,
                    PermissionResource.USER,
                    "action-updated-" + key,
                    PermissionRiskLevel.LOW,
                    true,
                    false,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .pathParam("id", permission1.getId())
                    .body(updateDto)
                    .when()
                    .patch(url + "/{id}")
                    .then()
                    .statusCode(409)
                    .body("success", equalTo(false))
                    .body("message", containsString("slug"));
        }

        @Test
        @DisplayName("Deve retornar 409 Conflict ao tentar atualizar permissão com nome já pertencente a outra permissão")
        void shouldReturn409WhenNameAlreadyExists() {
            TokenResponse masterToken = loginMasterHTTP();
            String key = generateRandomString().toLowerCase();

            // Permissão 1
            PermissionDTO permission1 = createPermissionHTTP(masterToken);

            // Permissão 2 (da qual pegaremos o nome)
            PermissionDTO permission2 = createPermissionHTTP(masterToken);

            // Tenta atualizar a permissão 1 usando o nome da permissão 2
            UpdatePermissionDTO updateDto = new UpdatePermissionDTO(
                    permission2.getName(),
                    "slug-updated-" + key,
                    "Descrição",
                    PermissionModule.IDENTITY,
                    PermissionResource.USER,
                    "action-updated-" + key,
                    PermissionRiskLevel.LOW,
                    true,
                    false,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .pathParam("id", permission1.getId())
                    .body(updateDto)
                    .when()
                    .patch(url + "/{id}")
                    .then()
                    .statusCode(409)
                    .body("success", equalTo(false))
                    .body("message", containsString("name"));
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando a permissão a ser atualizada não existir")
        void shouldReturn404WhenPermissionNotFound() {
            TokenResponse masterToken = loginMasterHTTP();
            UUID nonExistentId = UUID.randomUUID();
            String key = generateRandomString().toLowerCase();

            UpdatePermissionDTO updateDto = new UpdatePermissionDTO(
                    "Name-" + key,
                    "slug-" + key,
                    "Descrição",
                    PermissionModule.IDENTITY,
                    PermissionResource.USER,
                    "action-" + key,
                    PermissionRiskLevel.LOW,
                    true,
                    false,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + masterToken.token())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .pathParam("id", nonExistentId)
                    .body(updateDto)
                    .when()
                    .patch(url + "/{id}")
                    .then()
                    .statusCode(404)
                    .body("success", equalTo(false))
                    .body("message", containsString("Permission not found"));
        }

        @Test
        @DisplayName("Deve retornar 401 Unauthorized quando não for enviado o token JWT")
        void shouldReturn401WhenNoTokenProvided() {
            UUID permissionId = UUID.randomUUID();
            String key = generateRandomString().toLowerCase();

            UpdatePermissionDTO updateDto = new UpdatePermissionDTO(
                    "Name-" + key,
                    "slug-" + key,
                    "Descrição",
                    PermissionModule.IDENTITY,
                    PermissionResource.USER,
                    "action-" + key,
                    PermissionRiskLevel.LOW,
                    true,
                    false,
                    null
            );

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .pathParam("id", permissionId)
                    .body(updateDto)
                    .when()
                    .patch(url + "/{id}")
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("GET /v1/permission")
    public class FindAll {

        @Test
        @DisplayName("Deve listar permissões com sucesso sem filtros")
        void shouldFindAllPermissionsSuccessfully() {
            TokenResponse masterToken = loginMasterHTTP();
            createPermissionHTTP(masterToken);

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content", hasSize(greaterThanOrEqualTo(1)))
                    .body("totalElements", greaterThanOrEqualTo(1));
        }

        @Test
        @DisplayName("Deve filtrar permissões por name, slug e action")
        void shouldFilterPermissionsByNameSlugAndAction() {
            TokenResponse masterToken = loginMasterHTTP();
            PermissionDTO permission = createPermissionHTTP(masterToken);

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .queryParam("name", permission.getName())
                    .queryParam("slug", permission.getSlug())
                    .queryParam("action", permission.getAction())
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content", hasSize(greaterThanOrEqualTo(1)))
                    .body("content[0].id", equalTo(permission.getId().toString()));
        }

        @Test
        @DisplayName("Deve filtrar permissões por enums (module, resource e riskLevel)")
        void shouldFilterPermissionsByEnums() {
            TokenResponse masterToken = loginMasterHTTP();
            PermissionDTO permission = createPermissionHTTP(masterToken);

            given()
                    .contentType(ContentType.JSON)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .header("Authorization", "Bearer " + masterToken.token())
                    .queryParam("module", permission.getModule().name())
                    .queryParam("resource", permission.getResource().name())
                    .queryParam("riskLevel", permission.getRiskLevel().name())
                    .when()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("content", hasSize(greaterThanOrEqualTo(1)));
        }
        
        @Test
        @DisplayName("Deve retornar 401 Unauthorized quando não for enviado o token JWT")
        void shouldReturn401WhenNoTokenProvided() {
            given()
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(ContentType.JSON)
                    .when()
                    .get(url)
                    .then()
                    .statusCode(401);
        }
    }

}