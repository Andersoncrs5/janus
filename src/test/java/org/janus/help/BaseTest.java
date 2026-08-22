package org.janus.help;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
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
    protected UserCredentialRepository userCredentialRepository;

    @Inject
    protected InboxRepository inboxRepository;

    @Inject
    protected ObjectMapper mapper;

    @BeforeEach
    void setup() {
        this.inboxRepository.deleteAll();
        this.userCredentialRepository.deleteAll();
        this.userRepository.deleteAll();
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
}