package org.janus.modules.identity.infra.repository;

import io.quarkus.test.junit.QuarkusTest;
import org.janus.help.BaseTest;
import org.janus.modules.identity.application.user.dto.filter.UserFilterDTO;
import org.janus.modules.identity.application.user.dto.filter.UserOrder;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.shared.domain.page.Page;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
public class UserRepositoryJdbcTest extends BaseTest {

    @Nested
    class FindAll {

        @Test
        void shouldReturnPagedUsersWithFilters() {
            UserEntity user1 = userRepository.insert(UserEntity.builder()
                    .email("filter.test1@janus.org")
                    .username("filteruser1")
                    .fullName("Alice Filter")
                    .isActive(true)
                    .isEmailVerified(true)
                    .build());

            UserEntity user2 = userRepository.insert(UserEntity.builder()
                    .email("filter.test2@janus.org")
                    .username("filteruser2")
                    .fullName("Bob Filter")
                    .isActive(false)
                    .isEmailVerified(false)
                    .build());

            UserFilterDTO filter = UserFilterDTO.builder()
                    .email("filter.test1@janus.org")
                    .isActive(true)
                    .orders(List.of(UserOrder.CREATED_AT))
                    .page(0)
                    .size(10)
                    .build();

            Page<UserEntity> result = userRepository.findAll(filter);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).extracting(UserEntity::getId).contains(user1.getId());
            assertThat(result.getContent()).extracting(UserEntity::getId).doesNotContain(user2.getId());
        }

        @Test
        void shouldReturnEmptyPageWhenNoUsersMatchFilter() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .username("nonexistent_filter_user")
                    .page(0)
                    .size(10)
                    .build();

            Page<UserEntity> result = userRepository.findAll(filter);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    class Insert {

        @Test
        void shouldInsertUserWithGeneratedIdWhenIdIsNull() {
            UserEntity user = createSampleUser();
            user.setId(null);

            UserEntity savedUser = userRepository.insert(user);

            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getVersion()).isEqualTo(0L);

            Optional<UserEntity> fetched = userRepository.findById(savedUser.getId());
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getEmail()).isEqualTo(user.getEmail());
            assertThat(fetched.get().getUsername()).isEqualTo(user.getUsername());
        }

        @Test
        void shouldInsertUserWithProvidedId() {
            UserEntity user = createSampleUser();
            UUID customId = user.getId();

            UserEntity savedUser = userRepository.insert(user);

            assertThat(savedUser.getId()).isEqualTo(customId);

            Optional<UserEntity> fetched = userRepository.findById(customId);
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getId()).isEqualTo(customId);
        }

        @Test
        void shouldPersistAllFieldsCorrectly() {
            OffsetDateTime lockedUntil = OffsetDateTime.now().plusHours(1);
            OffsetDateTime lastLoginAt = OffsetDateTime.now().minusDays(1);

            UserEntity user = UserEntity.builder()
                    .id(UUID.randomUUID())
                    .email("full.fields@janus.org")
                    .username("fullfields")
                    .fullName("Full Fields User")
                    .isActive(true)
                    .isEmailVerified(true)
                    .failedLoginAttempts(3)
                    .lockedUntil(lockedUntil)
                    .lastLoginAt(lastLoginAt)
                    .build();

            UserEntity saved = userRepository.insert(user);

            Optional<UserEntity> fetched = userRepository.findById(saved.getId());
            assertThat(fetched).isPresent();
            UserEntity found = fetched.get();

            assertThat(found.getEmail()).isEqualTo("full.fields@janus.org");
            assertThat(found.getUsername()).isEqualTo("fullfields");
            assertThat(found.getFullName()).isEqualTo("Full Fields User");
            assertThat(found.getIsActive()).isTrue();
            assertThat(found.getIsEmailVerified()).isTrue();
            assertThat(found.getFailedLoginAttempts()).isEqualTo(3);
            assertThat(found.getLockedUntil()).isNotNull();
            assertThat(found.getLastLoginAt()).isNotNull();
        }
    }

    @Nested
    class Save {

        @Test
        void shouldInsertWhenUserDoesNotExist() {
            UserEntity user = createSampleUser();

            UserEntity result = userRepository.save(user);

            assertThat(result.getId()).isNotNull();
            assertThat(userRepository.existsById(result.getId())).isTrue();
        }

        @Test
        void shouldUpdateWhenUserAlreadyExists() {
            UserEntity user = initUser();

            user.setFullName("Updated Full Name");
            user.setFailedLoginAttempts(2);

            UserEntity updated = userRepository.save(user);

            assertThat(updated.getFullName()).isEqualTo("Updated Full Name");

            Optional<UserEntity> fetched = userRepository.findById(user.getId());
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getFullName()).isEqualTo("Updated Full Name");
            assertThat(fetched.get().getFailedLoginAttempts()).isEqualTo(2);
        }

        @Test
        void shouldThrowExceptionWhenOptimisticLockFails() {
            UserEntity user = initUser();

            user.setVersion(99L);

            assertThatThrownBy(() -> userRepository.save(user))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to update User entity");
        }
    }

    @Nested
    class FindById {

        @Test
        void shouldReturnUserWhenExists() {
            UserEntity user = initUser();

            Optional<UserEntity> result = userRepository.findById(user.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(user.getId());
        }

        @Test
        void shouldReturnEmptyWhenDoesNotExist() {
            Optional<UserEntity> result = userRepository.findById(UUID.randomUUID());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByEmail {

        @Test
        void shouldFindUserByEmailCaseInsensitive() {
            UserEntity user = initUser();

            Optional<UserEntity> resultUpper = userRepository.findByEmail(user.getEmail().toUpperCase());
            Optional<UserEntity> resultLower = userRepository.findByEmail(user.getEmail().toLowerCase());

            assertThat(resultUpper).isPresent();
            assertThat(resultLower).isPresent();
            assertThat(resultUpper.get().getId()).isEqualTo(user.getId());
        }

        @Test
        void shouldReturnEmptyWhenEmailNotFound() {
            Optional<UserEntity> result = userRepository.findByEmail("nonexistent@janus.org");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByUsername {

        @Test
        void shouldFindUserByUsernameCaseInsensitive() {
            UserEntity user = initUser();

            Optional<UserEntity> resultUpper = userRepository.findByUsername(user.getUsername().toUpperCase());
            Optional<UserEntity> resultLower = userRepository.findByUsername(user.getUsername().toLowerCase());

            assertThat(resultUpper).isPresent();
            assertThat(resultLower).isPresent();
            assertThat(resultUpper.get().getId()).isEqualTo(user.getId());
        }

        @Test
        void shouldReturnEmptyWhenUsernameNotFound() {
            Optional<UserEntity> result = userRepository.findByUsername("nonexistent_user");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class ExistsByEmail {

        @Test
        void shouldReturnTrueWhenEmailExists() {
            UserEntity user = initUser();

            boolean exists = userRepository.existsByEmail(user.getEmail());

            assertThat(exists).isTrue();
        }

        @Test
        void shouldReturnFalseWhenEmailDoesNotExist() {
            boolean exists = userRepository.existsByEmail("nonexistent@janus.org");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    class ExistsByUsername {

        @Test
        void shouldReturnTrueWhenUsernameExists() {
            UserEntity user = initUser();

            boolean exists = userRepository.existsByUsername(user.getUsername());

            assertThat(exists).isTrue();
        }

        @Test
        void shouldReturnFalseWhenUsernameDoesNotExist() {
            boolean exists = userRepository.existsByUsername("nonexistent_user");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    class ExistsById {

        @Test
        void shouldReturnTrueWhenIdExists() {
            UserEntity user = initUser();

            boolean exists = userRepository.existsById(user.getId());

            assertThat(exists).isTrue();
        }

        @Test
        void shouldReturnFalseWhenIdDoesNotExist() {
            boolean exists = userRepository.existsById(UUID.randomUUID());

            assertThat(exists).isFalse();
        }
    }

    @Nested
    class UpdateOptimistic {

        @Test
        void shouldUpdateAndIncrementVersion() {
            UserEntity user = initUser();
            Long initialVersion = user.getVersion();

            user.setFullName("Name Updated Optimistically");
            boolean updated = userRepository.updateOptimistic(user);

            assertThat(updated).isTrue();

            Optional<UserEntity> fetched = userRepository.findById(user.getId());
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getFullName()).isEqualTo("Name Updated Optimistically");
            assertThat(fetched.get().getVersion()).isEqualTo(initialVersion + 1);
        }

        @Test
        void shouldReturnFalseWhenVersionMismatch() {
            UserEntity user = initUser();

            user.setVersion(user.getVersion() + 10L);
            boolean updated = userRepository.updateOptimistic(user);

            assertThat(updated).isFalse();
        }
    }

    @Nested
    class DeleteOperations {

        @Test
        void shouldDeleteById() {
            UserEntity user = initUser();

            int rowsDeleted = userRepository.deleteById(user.getId());

            assertThat(rowsDeleted).isEqualTo(1);
            assertThat(userRepository.existsById(user.getId())).isFalse();
        }

        @Test
        void shouldReturnZeroWhenDeleteNonExistentId() {
            int rowsDeleted = userRepository.deleteById(UUID.randomUUID());

            assertThat(rowsDeleted).isEqualTo(0);
        }

        @Test
        void shouldDeleteAllById() {
            UserEntity u1 = initUser();
            UserEntity u2 = initUser();
            UserEntity u3 = initUser();

            int rowsDeleted = userRepository.deleteAllById(List.of(u1.getId(), u2.getId()));

            assertThat(rowsDeleted).isEqualTo(2);
            assertThat(userRepository.existsById(u1.getId())).isFalse();
            assertThat(userRepository.existsById(u2.getId())).isFalse();
            assertThat(userRepository.existsById(u3.getId())).isTrue();
        }

        @Test
        void shouldDeleteAll() {
            initUser();
            initUser();

            int rowsDeleted = userRepository.deleteAll();

            assertThat(rowsDeleted).isGreaterThanOrEqualTo(2);
            assertThat(userRepository.existsById(UUID.randomUUID())).isFalse();
        }
    }
}