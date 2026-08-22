package org.janus.modules.identity.infra.repository;

import io.quarkus.test.junit.QuarkusTest;
import org.janus.help.BaseTest;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
public class UserCredentialRepositoryJdbcTest extends BaseTest {

    @Nested
    class Insert {

        @Test
        void shouldInsertCredentialsWithGeneratedIdWhenIdIsNull() {
            UserEntity user = initUser();
            UserCredentialsEntity credentials = createSampleCredentials(user.getId());
            credentials.setId(null);

            UserCredentialsEntity saved = userCredentialRepository.insert(credentials);

            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getVersion()).isEqualTo(0L);

            Optional<UserCredentialsEntity> fetched = userCredentialRepository.findByUserId(user.getId());
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getPasswordHash()).isEqualTo(credentials.getPasswordHash());
            assertThat(fetched.get().getAlgorithm()).isEqualTo(credentials.getAlgorithm());
        }

        @Test
        void shouldInsertCredentialsWithProvidedId() {
            UserEntity user = initUser();
            UserCredentialsEntity credentials = createSampleCredentials(user.getId());
            UUID customId = credentials.getId();

            UserCredentialsEntity saved = userCredentialRepository.insert(credentials);

            assertThat(saved.getId()).isEqualTo(customId);

            Optional<UserCredentialsEntity> fetched = userCredentialRepository.findByUserId(user.getId());
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getId()).isEqualTo(customId);
        }

        @Test
        void shouldFallbackToDefaultAlgorithmWhenNull() {
            UserEntity user = initUser();
            UserCredentialsEntity credentials = createSampleCredentials(user.getId());
            credentials.setAlgorithm(null);

            UserCredentialsEntity saved = userCredentialRepository.insert(credentials);

            Optional<UserCredentialsEntity> fetched = userCredentialRepository.findByUserId(user.getId());
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getAlgorithm()).isEqualTo("argon2id");
        }
    }

    @Nested
    class Save {

        @Test
        void shouldInsertWhenCredentialsDoNotExist() {
            UserEntity user = initUser();
            UserCredentialsEntity credentials = createSampleCredentials(user.getId());

            UserCredentialsEntity result = userCredentialRepository.save(credentials);

            assertThat(result.getId()).isNotNull();
            assertThat(userCredentialRepository.existsByUserId(user.getId())).isTrue();
        }

        @Test
        void shouldUpdateWhenCredentialsAlreadyExist() {
            UserCredentialsEntity credentials = initCredentials();

            credentials.setPasswordHash("$argon2id$v=19$m=65536,t=3,p=4$newhashvalue");
            credentials.setAlgorithm("bcrypt");

            UserCredentialsEntity updated = userCredentialRepository.save(credentials);

            assertThat(updated.getPasswordHash()).isEqualTo("$argon2id$v=19$m=65536,t=3,p=4$newhashvalue");

            Optional<UserCredentialsEntity> fetched = userCredentialRepository.findByUserId(credentials.getUserId());
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getPasswordHash()).isEqualTo("$argon2id$v=19$m=65536,t=3,p=4$newhashvalue");
            assertThat(fetched.get().getAlgorithm()).isEqualTo("bcrypt");
        }

        @Test
        void shouldThrowExceptionWhenOptimisticLockFails() {
            UserCredentialsEntity credentials = initCredentials();

            credentials.setVersion(99L);

            assertThatThrownBy(() -> userCredentialRepository.save(credentials))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to update UserCredentials entity");
        }
    }

    @Nested
    class FindByUserId {

        @Test
        void shouldReturnCredentialsWhenUserIdExists() {
            UserCredentialsEntity credentials = initCredentials();

            Optional<UserCredentialsEntity> result = userCredentialRepository.findByUserId(credentials.getUserId());

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(credentials.getId());
            assertThat(result.get().getUserId()).isEqualTo(credentials.getUserId());
        }

        @Test
        void shouldReturnEmptyWhenUserIdDoesNotExist() {
            Optional<UserCredentialsEntity> result = userCredentialRepository.findByUserId(UUID.randomUUID());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class ExistsByUserId {

        @Test
        void shouldReturnTrueWhenUserIdExists() {
            UserCredentialsEntity credentials = initCredentials();

            boolean exists = userCredentialRepository.existsByUserId(credentials.getUserId());

            assertThat(exists).isTrue();
        }

        @Test
        void shouldReturnFalseWhenUserIdDoesNotExist() {
            boolean exists = userCredentialRepository.existsByUserId(UUID.randomUUID());

            assertThat(exists).isFalse();
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteByUserIdWhenCredentialsExist() {
            UserCredentialsEntity credentials = initCredentials();

            boolean deleted = userCredentialRepository.deleteByUserId(credentials.getUserId());

            assertThat(deleted).isTrue();
            assertThat(userCredentialRepository.existsByUserId(credentials.getUserId())).isFalse();
            assertThat(userCredentialRepository.findByUserId(credentials.getUserId())).isEmpty();
        }

        @Test
        void shouldReturnFalseWhenDeletingByNonExistentUserId() {
            boolean deleted = userCredentialRepository.deleteByUserId(UUID.randomUUID());

            assertThat(deleted).isFalse();
        }

        @Test
        void shouldDeleteByIdWhenCredentialsExist() {
            UserCredentialsEntity credentials = initCredentials();

            int deleted = userCredentialRepository.deleteById(credentials.getId());

            assertThat(deleted > 0).isTrue();
            assertThat(userCredentialRepository.existsByUserId(credentials.getUserId())).isFalse();
        }

        @Test
        void shouldReturnFalseWhenDeletingByNonExistentId() {
            int deleted = userCredentialRepository.deleteById(UUID.randomUUID());

            assertThat(deleted > 0).isFalse();
        }
    }

}