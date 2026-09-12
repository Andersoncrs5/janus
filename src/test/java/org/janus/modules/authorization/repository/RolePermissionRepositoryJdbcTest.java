package org.janus.modules.authorization.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.janus.help.BaseTest;
import org.janus.modules.authorization.application.dto.rolePermission.filter.RolePermissionFilterDTO;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.shared.domain.enums.rolePermission.PermissionEffectEnum;
import org.janus.shared.domain.page.Page;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class RolePermissionRepositoryJdbcTest extends BaseTest {

    @Inject
    RolePermissionRepository repository;

    @Nested
    class Create {

        @Test
        void success() {
            UserEntity user = initUser();
            RoleEntity role = initRole();
            PermissionEntity permission = initPermission(user.getId());

            RolePermissionEntity rolePermission = createRolePermissionEntity(role.getId(), permission.getId(), user.getId());

            RolePermissionEntity saved = repository.save(rolePermission);

            assertNotNull(saved.getId());
            assertEquals(role.getId(), saved.getRoleId());
            assertEquals(permission.getId(), saved.getPermissionId());
            assertEquals(user.getId(), saved.getAssignedBy());
            assertEquals(PermissionEffectEnum.ALLOW, saved.getEffect());
            assertNotNull(saved.getCreatedAt());
            assertNotNull(saved.getUpdatedAt());
            assertEquals(0L, saved.getVersion());
        }
    }

    @Nested
    class FindByRoleIdAndPermissionId {

        @Test
        void successWhenExists() {
            UserEntity user = initUser();
            RoleEntity role = initRole();
            PermissionEntity permission = initPermission(user.getId());
            RolePermissionEntity created = repository.save(createRolePermissionEntity(role.getId(), permission.getId(), user.getId()));

            Optional<RolePermissionEntity> result = repository.findByRoleIdAndPermissionId(role.getId(), permission.getId());

            assertTrue(result.isPresent());
            assertEquals(created.getId(), result.get().getId());
        }

        @Test
        void emptyWhenNotFound() {
            Optional<RolePermissionEntity> result = repository.findByRoleIdAndPermissionId(UUID.randomUUID(), UUID.randomUUID());

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class FindAllByRoleId {

        @Test
        void success() {
            UserEntity user = initUser();
            RoleEntity role = initRole();
            PermissionEntity perm1 = initPermission(user.getId());
            PermissionEntity perm2 = initPermission(user.getId());

            repository.save(createRolePermissionEntity(role.getId(), perm1.getId(), user.getId()));
            repository.save(createRolePermissionEntity(role.getId(), perm2.getId(), user.getId()));

            List<RolePermissionEntity> results = repository.findAllByRoleId(role.getId());

            assertEquals(2, results.size());
        }
    }

    @Nested
    class FindAllByPermissionId {

        @Test
        void success() {
            UserEntity user = initUser();
            RoleEntity role1 = initRole();
            RoleEntity role2 = initRole();
            PermissionEntity perm = initPermission(user.getId());

            repository.save(createRolePermissionEntity(role1.getId(), perm.getId(), user.getId()));
            repository.save(createRolePermissionEntity(role2.getId(), perm.getId(), user.getId()));

            List<RolePermissionEntity> results = repository.findAllByPermissionId(perm.getId());

            assertEquals(2, results.size());
        }
    }

    @Nested
    class ExistsByRoleIdAndPermissionId {

        @Test
        void returnsTrueWhenExists() {
            UserEntity user = initUser();
            RoleEntity role = initRole();
            PermissionEntity permission = initPermission(user.getId());
            repository.save(createRolePermissionEntity(role.getId(), permission.getId(), user.getId()));

            boolean exists = repository.existsByRoleIdAndPermissionId(role.getId(), permission.getId());

            assertTrue(exists);
        }

        @Test
        void returnsFalseWhenDoesNotExist() {
            boolean exists = repository.existsByRoleIdAndPermissionId(UUID.randomUUID(), UUID.randomUUID());

            assertFalse(exists);
        }
    }

    @Nested
    class FindAllActiveByRoleId {

        @Test
        void returnsOnlyActiveOrNonExpired() {
            UserEntity user = initUser();
            RoleEntity role = initRole();
            PermissionEntity activePerm = initPermission(user.getId());
            PermissionEntity expiredPerm = initPermission(user.getId());

            RolePermissionEntity activeRp = createRolePermissionEntity(role.getId(), activePerm.getId(), user.getId());
            activeRp.setExpiresAt(OffsetDateTime.now().plusDays(1));
            repository.save(activeRp);

            RolePermissionEntity expiredRp = createRolePermissionEntity(role.getId(), expiredPerm.getId(), user.getId());
            expiredRp.setExpiresAt(OffsetDateTime.now().minusDays(1));
            repository.save(expiredRp);

            List<RolePermissionEntity> activeList = repository.findAllActiveByRoleId(role.getId(), OffsetDateTime.now());

            assertEquals(1, activeList.size());
            assertEquals(activePerm.getId(), activeList.get(0).getPermissionId());
        }
    }

    @Nested
    class CountByRoleId {

        @Test
        void success() {
            UserEntity user = initUser();
            RoleEntity role = initRole();
            PermissionEntity perm1 = initPermission(user.getId());
            PermissionEntity perm2 = initPermission(user.getId());

            repository.save(createRolePermissionEntity(role.getId(), perm1.getId(), user.getId()));
            repository.save(createRolePermissionEntity(role.getId(), perm2.getId(), user.getId()));

            long count = repository.countByRoleId(role.getId());

            assertEquals(2, count);
        }
    }

    @Nested
    class FindAll {

        @Test
        void withPaginationAndFilters() {
            UserEntity user = initUser();
            RoleEntity role = initRole();
            PermissionEntity perm = initPermission(user.getId());
            repository.save(createRolePermissionEntity(role.getId(), perm.getId(), user.getId()));

            RolePermissionFilterDTO filter = new RolePermissionFilterDTO();
            filter.setRoleId(role.getId());
            filter.setPage(0);
            filter.setSize(10);

            Page<RolePermissionEntity> page = repository.findAll(filter);

            assertEquals(1, page.getTotalElements());
            assertEquals(1, page.getContent().size());
            assertEquals(role.getId(), page.getContent().get(0).getRoleId());
        }
    }

    @Nested
    class Update {

        @Test
        void successWithOptimisticLock() {
            UserEntity user = initUser();
            RoleEntity role = initRole();
            PermissionEntity permission = initPermission(user.getId());

            RolePermissionEntity entity = repository.save(createRolePermissionEntity(role.getId(), permission.getId(), user.getId()));

            entity.setEffect(PermissionEffectEnum.DENY);
            RolePermissionEntity updated = repository.save(entity);

            assertEquals(PermissionEffectEnum.DENY, updated.getEffect());

        }

        @Test
        void throwsExceptionOnVersionMismatch() {
            UserEntity user = initUser();
            RoleEntity role = initRole();
            PermissionEntity permission = initPermission(user.getId());

            RolePermissionEntity entity = repository.save(createRolePermissionEntity(role.getId(), permission.getId(), user.getId()));

            entity.setVersion(99L);

            assertThrows(IllegalStateException.class, () -> repository.save(entity));
        }
    }
}