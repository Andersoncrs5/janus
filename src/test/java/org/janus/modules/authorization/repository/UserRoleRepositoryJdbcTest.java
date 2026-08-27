package org.janus.modules.authorization.repository;

import io.quarkus.test.junit.QuarkusTest;
import org.janus.help.BaseTest;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
public class UserRoleRepositoryJdbcTest extends BaseTest {

    @Nested
    class Insert {

        @Test
        void shouldInsertUserRoleWithGeneratedIdWhenIdIsNull() {
            UserEntity user = initUser();
            RoleEntity role = initRole();

            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());


            UserRoleEntity saved = userRoleRepository.insert(userRole);

            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getVersion()).isEqualTo(0L);
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();

            Optional<UserRoleEntity> fetched = userRoleRepository.findByUserIdAndRoleId(user.getId(), role.getId());
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getExpiresAt()).isNull();
        }

        @Test
        void shouldInsertUserRoleWithProvidedId() {
            UserEntity user = initUser();
            RoleEntity role = initRole();

            UserRoleEntity userRole = new UserRoleEntity();
            UUID customId = UUID.randomUUID();
            userRole.setId(customId);
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());

            UserRoleEntity saved = userRoleRepository.insert(userRole);

            assertThat(saved.getId()).isEqualTo(customId);

            Optional<UserRoleEntity> fetched = userRoleRepository.findByUserIdAndRoleId(user.getId(), role.getId());
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getId()).isEqualTo(customId);
        }
    }

    @Nested
    class Save {

        @Test
        void shouldInsertWhenUserRoleDoesNotExist() {
            UserEntity user = initUser();
            RoleEntity role = initRole();

            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());

            UserRoleEntity result = userRoleRepository.save(userRole);

            assertThat(result.getId()).isNotNull();
            assertThat(userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())).isTrue();
        }

        @Test
        void shouldUpdateWhenUserRoleAlreadyExists() {
            UserEntity user = initUser();
            RoleEntity role = initRole();


            UserRoleEntity userRole = initUserRole(user.getId(), role.getId(), null);
            Long initialVersion = userRole.getVersion();


            OffsetDateTime newExpiration = OffsetDateTime.now().plusDays(10);
            userRole.setExpiresAt(newExpiration);
            userRole.setAssignedById(user.getId());

            UserRoleEntity updated = userRoleRepository.save(userRole);


            assertThat(updated.getExpiresAt()).isEqualTo(newExpiration);


            Optional<UserRoleEntity> fetched = userRoleRepository.findByUserIdAndRoleId(user.getId(), role.getId());
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getAssignedById()).isEqualTo(user.getId());


            assertThat(fetched.get().getVersion()).isEqualTo(initialVersion + 1);
        }

        @Test
        void shouldThrowExceptionWhenOptimisticLockFails() {
            UserEntity user = initUser();
            RoleEntity role = initRole();

            UserRoleEntity userRole = initUserRole(user.getId(), role.getId(), null);

            userRole.setVersion(99L);

            assertThatThrownBy(() -> userRoleRepository.save(userRole))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to update UserRole entity");
        }
    }

    @Nested
    class ExistsByUserIdAndRoleId {
        @Test
        void shouldReturnTrueWhenLinkExists() {
            UserEntity user = initUser();
            RoleEntity role = initRole();
            initUserRole(user.getId(), role.getId(), null);

            boolean exists = userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId());

            assertThat(exists).isTrue();
        }

        @Test
        void shouldReturnFalseWhenLinkDoesNotExist() {
            UserEntity user = initUser();
            RoleEntity role = initRole();

            boolean exists = userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId());

            assertThat(exists).isFalse();
        }
    }

    @Nested
    class FindByUserIdAndRoleId {

        @Test
        void shouldReturnEntityWhenFound() {
            UserEntity user = initUser();
            RoleEntity role = initRole();
            UserRoleEntity created = initUserRole(user.getId(), role.getId(), null);

            Optional<UserRoleEntity> found = userRoleRepository.findByUserIdAndRoleId(user.getId(), role.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(created.getId());
            assertThat(found.get().getUserId()).isEqualTo(user.getId());
            assertThat(found.get().getRoleId()).isEqualTo(role.getId());
        }

        @Test
        void shouldReturnEmptyWhenNotFound() {
            Optional<UserRoleEntity> found = userRoleRepository.findByUserIdAndRoleId(UUID.randomUUID(), UUID.randomUUID());

            assertThat(found).isEmpty();
        }
    }

    @Nested
    class FindAllRoleIdsByUserId {

        @Test
        void shouldReturnListOfRoleIdsForGivenUser() {
            UserEntity user = initUser();
            RoleEntity role1 = initRole();
            RoleEntity role2 = initRole();

            initUserRole(user.getId(), role1.getId(), null);
            initUserRole(user.getId(), role2.getId(), null);

            List<UUID> roleIds = userRoleRepository.findAllRoleIdsByUserId(user.getId());

            assertThat(roleIds).hasSize(2);
            assertThat(roleIds).containsExactlyInAnyOrder(role1.getId(), role2.getId());
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoRoles() {
            UserEntity user = initUser();

            List<UUID> roleIds = userRoleRepository.findAllRoleIdsByUserId(user.getId());

            assertThat(roleIds).isEmpty();
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteSpecificUserRole() {
            UserEntity user = initUser();
            RoleEntity role = initRole();
            initUserRole(user.getId(), role.getId(), null);

            int deletedRows = userRoleRepository.deleteByUserIdAndRoleId(user.getId(), role.getId());

            assertThat(deletedRows).isGreaterThan(0);
            assertThat(userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())).isFalse();
        }

        @Test
        void shouldDeleteAllRolesForSpecificUser() {
            UserEntity user = initUser();
            RoleEntity role1 = initRole();
            RoleEntity role2 = initRole();

            initUserRole(user.getId(), role1.getId(), null);
            initUserRole(user.getId(), role2.getId(), null);

            int deletedRows = userRoleRepository.deleteAllByUserId(user.getId());

            assertThat(deletedRows).isEqualTo(2);
            assertThat(userRoleRepository.findAllRoleIdsByUserId(user.getId())).isEmpty();
        }
    }

    @Nested
    class ExistsActiveByUserIdAndRoleId {

        @Test
        void shouldReturnTrueWhenExpiresAtIsNull() {
            UserEntity user = initUser();
            RoleEntity role = initRole();
            initUserRole(user.getId(), role.getId(), null);

            boolean isActive = userRoleRepository.existsActiveByUserIdAndRoleId(user.getId(), role.getId());

            assertThat(isActive).isTrue();
        }

        @Test
        void shouldReturnTrueWhenExpiresAtIsInTheFuture() {
            UserEntity user = initUser();
            RoleEntity role = initRole();
            initUserRole(user.getId(), role.getId(), OffsetDateTime.now().plusDays(5));

            boolean isActive = userRoleRepository.existsActiveByUserIdAndRoleId(user.getId(), role.getId());

            assertThat(isActive).isTrue();
        }

        @Test
        void shouldReturnFalseWhenExpiresAtIsInThePast() {
            UserEntity user = initUser();
            RoleEntity role = initRole();
            initUserRole(user.getId(), role.getId(), OffsetDateTime.now().minusDays(1));

            boolean isActive = userRoleRepository.existsActiveByUserIdAndRoleId(user.getId(), role.getId());

            assertThat(isActive).isFalse();
        }
    }
}