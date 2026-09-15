package org.janus.help;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.janus.modules.authentication.adapter.out.persistence.repository.RefreshTokenRepositoryJdbc;
import org.janus.modules.authentication.adapter.out.persistence.repository.SessionRepositoryJdbc;
import org.janus.modules.authentication.domain.entity.LoginAttemptEntity;
import org.janus.modules.authentication.domain.entity.RefreshTokenEntity;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.modules.authentication.port.out.LoginAttemptRepository;
import org.janus.modules.authorization.adapter.out.persistence.repository.RolePermissionRepositoryJdbc;
import org.janus.modules.authorization.adapter.out.persistence.repository.RoleRepositoryJdbc;
import org.janus.modules.authorization.adapter.out.persistence.repository.UserRoleRepositoryJdbc;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.modules.identity.application.user.dto.CreateUserDTO;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.out.UserCredentialRepository;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.api.TokenResponse;
import org.janus.shared.domain.enums.InboxStatusEnum;
import org.janus.shared.domain.enums.permission.PermissionModule;
import org.janus.shared.domain.enums.permission.PermissionResource;
import org.janus.shared.domain.enums.permission.PermissionRiskLevel;
import org.janus.shared.domain.enums.rolePermission.PermissionEffectEnum;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class BaseTest {

    @Inject
    protected RefreshTokenRepositoryJdbc refreshTokenRepository;

    @Inject
    protected UserRepository userRepository;

    @Inject
    protected UserRoleRepositoryJdbc userRoleRepository;

    @Inject
    protected RoleRepositoryJdbc roleRepository;

    @Inject
    protected UserCredentialRepository userCredentialRepository;

    @Inject
    protected InboxRepository inboxRepository;

    @Inject
    protected SessionRepositoryJdbc sessionRepository;

    @Inject
    protected RolePermissionRepositoryJdbc rolePermissionRepository;

    @Inject
    protected LoginAttemptRepository loginAttemptRepository;

    @Inject
    protected PermissionRepository permissionRepository;

    @Inject
    protected ObjectMapper mapper;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    protected LoginAttemptEntity createSampleLoginAttempt(UUID userId, String email) {
        LoginAttemptEntity loginAttempt = new LoginAttemptEntity();

        loginAttempt.setUserId(userId);
        loginAttempt.setEmailAttempted(email);
        loginAttempt.setIpAddress("192.168.1.1");
        loginAttempt.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        loginAttempt.setSuccess(false);
        loginAttempt.setFailureReason("any");

        return loginAttempt;
    }

    protected LoginAttemptEntity createSampleLoginAttempt(UUID userId, String email, Boolean success) {
        LoginAttemptEntity loginAttempt = new LoginAttemptEntity();

        loginAttempt.setUserId(userId);
        loginAttempt.setEmailAttempted(email);
        loginAttempt.setIpAddress("192.168.1.1");
        loginAttempt.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        loginAttempt.setSuccess(success);
        loginAttempt.setFailureReason("any");

        return loginAttempt;
    }

    protected LoginAttemptEntity initLoginAttempt(UUID userId, String email) {
        return createSampleLoginAttempt(userId, email);
    }

    protected LoginAttemptEntity initLoginAttempt(UUID userId, String email, Boolean success) {
        return createSampleLoginAttempt(userId, email, success);
    }

    protected TokenResponse createUserHTTP() {
        String chars = generateRandomString().toLowerCase();
        final String url = "/v1/auth";

        CreateUserDTO dto = new CreateUserDTO(
                "user" + chars + "@gmail.com",
                "username" + chars,
                "fullname" + chars,
                "12345678"
        );

        TokenResponse result = given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID())
                .body(dto)
                .when()
                .post(url + "/register")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getObject("data", TokenResponse.class);

        assertThat(result.user().getId()).isNotNull();
        assertThat(result.user().getUsername()).isEqualTo(dto.getUsername());
        assertThat(result.user().getEmail()).isEqualTo(dto.getEmail());

        assertThat(result.token()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();

        assertThat(result.expToken()).isInTheFuture();
        assertThat(result.expRefreshToken()).isInTheFuture();

        return result;
    }

    protected String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    protected String generateRandomString() {
        return generateRandomString(15);
    }

    protected RolePermissionEntity createSampleRolePermissionEntity(UUID roleId, UUID permissionId, UUID assignedBy) {
        return RolePermissionEntity.builder()
                .roleId(roleId)
                .permissionId(permissionId)
                .effect(PermissionEffectEnum.ALLOW)
                .conditions("{\"scope\": \"all\"}")
                .expiresAt(OffsetDateTime.now().plusDays(30))
                .assignedBy(assignedBy)
                .assignedAt(OffsetDateTime.now())
                .build();
    }

    protected RolePermissionEntity createRolePermissionEntity(UUID roleId, UUID permissionId, UUID assignedBy) {
        return createSampleRolePermissionEntity(roleId, permissionId, assignedBy);
    }

    protected PermissionEntity createSamplePermissionEntity(UUID createdBy) {
        var chars = generateRandomString();
        return PermissionEntity.builder()
                .name("Read Users" + chars)
                .slug("users:read:" + chars.toLowerCase())
                .description("Permission to read user data")
                .module(PermissionModule.IDENTITY)
                .resource(PermissionResource.USER)
                .action("read")
                .riskLevel(PermissionRiskLevel.LOW)
                .isActive(true)
                .isSystem(false)
                .metadata("{\"category\": \"user_management\"}")
                .createdBy(createdBy)
                .build();
    }

    protected PermissionEntity createPermissionEntity(UUID createdBy) {
        return createSamplePermissionEntity(createdBy);
    }

    protected PermissionEntity initPermission(UUID createdBy) {
        return permissionRepository.insert(createSamplePermissionEntity(createdBy));
    }

    protected SessionEntity createSampleSession(UUID userId) {
        SessionEntity entity = new SessionEntity();
        entity.setUserId(userId);
        entity.setIpAddress("192.168.1.1");
        entity.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        entity.setIsRevoked(false);
        entity.setExpiresAt(OffsetDateTime.now().plusHours(24));
        return entity;
    }

    protected SessionEntity initSession(UUID userId) {
        return sessionRepository.insert(createSampleSession(userId));
    }

    protected RefreshTokenEntity createSampleRefreshToken(UUID sessionId, UUID userId, String tokenHash) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setSessionId(sessionId);
        entity.setUserId(userId);
        entity.setTokenHash(tokenHash);
        entity.setIsUsed(false);
        entity.setIsRevoked(false);
        entity.setExpiresAt(OffsetDateTime.now().plusDays(1));
        return entity;
    }

    protected RefreshTokenEntity initRefreshToken(UUID sessionId, UUID userId, String tokenHash) {
        return refreshTokenRepository.insert(createSampleRefreshToken(sessionId, userId, tokenHash));
    }

    protected UserRoleEntity initUserRole(UUID userId, UUID roleId, OffsetDateTime expiresAt) {
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setId(UUID.randomUUID());
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRole.setExpiresAt(expiresAt);
        userRole.setAssignedById(userId);

        return userRoleRepository.insert(userRole);
    }

    protected InboxEntity createSampleInbox() {
        InboxEntity inbox = new InboxEntity();
        inbox.setId(UUID.randomUUID());
        inbox.setMessageKey("msg-" + UUID.randomUUID());
        inbox.setConsumerGroup("order-processing-group");
        inbox.setStatus(InboxStatusEnum.PROCESSING);
        inbox.setResponsePayload("{\"status\":\"success\"}");
        inbox.setResponseCode(200);
        return inbox;
    }

    protected InboxEntity initInbox() {
        InboxEntity inbox = createSampleInbox();
        return inboxRepository.insert(inbox);
    }

    protected UserCredentialsEntity createSampleCredentials(UUID userId) {
        UserCredentialsEntity credentials = new UserCredentialsEntity();
        credentials.setId(UUID.randomUUID());
        credentials.setUserId(userId);
        credentials.setPasswordHash("$argon2id$v=19$m=65536,t=3,p=4$somehashvalue");
        credentials.setAlgorithm("argon2id");
        return credentials;
    }

    protected UserCredentialsEntity initCredentials() {
        UserEntity user = initUser();
        UserCredentialsEntity credentials = createSampleCredentials(user.getId());
        return userCredentialRepository.insert(credentials);
    }


    protected UserEntity initUser() {
        UserEntity user = createSampleUser();
        return userRepository.insert(user);
    }

    protected UserEntity createSampleUser() {
        return UserEntity.builder()
                .id(UUID.randomUUID())
                .email("test.user-" + UUID.randomUUID() + "@janus.org")
                .username("testuser_" + System.currentTimeMillis() + UUID.randomUUID())
                .fullName("Test User")
                .isActive(true)
                .isEmailVerified(true)
                .failedLoginAttempts(0)
                .version(0L)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    protected RoleEntity createSampleRole(String name, String slug) {
        RoleEntity role = new RoleEntity();
        role.setId(UUID.randomUUID());
        role.setName(name);
        role.setSlug(slug + UUID.randomUUID());
        role.setDescription("Descrição da role " + name);
        role.setIsActive(true);
        role.setIsSystem(false);
        return role;
    }

    protected RoleEntity initRole() {
        var key = UUID.randomUUID().toString();
        RoleEntity role = createSampleRole("role name" + key, "role-name-" + key);
        return roleRepository.insert(role);
    }

    protected RoleEntity initRole(String name, String slug) {
        RoleEntity role = createSampleRole(name, slug);
        return roleRepository.insert(role);
    }

}