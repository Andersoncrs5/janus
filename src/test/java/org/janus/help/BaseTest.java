package org.janus.help;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.janus.modules.authorization.adapter.out.persistence.repository.RoleRepositoryJdbc;
import org.janus.modules.authorization.adapter.out.persistence.repository.UserRoleRepositoryJdbc;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.out.UserCredentialRepository;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.enums.InboxStatusEnum;
import org.junit.jupiter.api.BeforeEach;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BaseTest {

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
    protected ObjectMapper mapper;

    @BeforeEach
    void setup() {
        this.userRoleRepository.deleteAll();
        this.inboxRepository.deleteAll();
        this.userCredentialRepository.deleteAll();
        this.userRepository.deleteAll();
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