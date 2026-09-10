package org.janus.modules.authorization.repository;

import io.quarkus.test.junit.QuarkusTest;
import org.janus.help.BaseTest;
import org.janus.modules.authorization.application.dto.permission.filter.PermissionFilterDTO;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.shared.domain.enums.permission.PermissionModule;
import org.janus.shared.domain.enums.permission.PermissionResource;
import org.janus.shared.domain.enums.permission.PermissionRiskLevel;
import org.janus.shared.domain.page.Page;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
public class PermissionRepositoryJdbcTest extends BaseTest {

    protected PermissionEntity createSamplePermission(String name, String slug) {
        return PermissionEntity.builder()
                .id(UUID.randomUUID())
                .name(name)
                .slug(slug)
                .description("Test Permission Description")
                .module(PermissionModule.IDENTITY)
                .resource(PermissionResource.USER)
                .action("read")
                .riskLevel(PermissionRiskLevel.LOW)
                .isActive(true)
                .isSystem(false)
                .metadata("{\"category\":\"test\"}")
                .createdBy(null)
                .build();
    }

    protected PermissionEntity initPermission(String name, String slug) {
        return permissionRepository.insert(createSamplePermission(name, slug));
    }

    @Nested
    class Insert {

        @Test
        void shouldInsertPermissionWithGeneratedIdWhenIdIsNull() {
            PermissionEntity permission = createSamplePermission("READ_USERS", "users:read");
            permission.setId(null);

            PermissionEntity saved = permissionRepository.insert(permission);

            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getVersion()).isEqualTo(0L);

            Optional<PermissionEntity> fetched = permissionRepository.findByName("READ_USERS");
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getSlug()).containsIgnoringCase("users:read");
            assertThat(fetched.get().getModule()).isEqualTo(PermissionModule.IDENTITY);
            assertThat(fetched.get().getResource()).isEqualTo(PermissionResource.USER);
        }

        @Test
        void shouldInsertPermissionWithProvidedId() {
            PermissionEntity permission = createSamplePermission("WRITE_USERS", "users:write");
            UUID customId = permission.getId();

            PermissionEntity saved = permissionRepository.insert(permission);

            assertThat(saved.getId()).isEqualTo(customId);

            Optional<PermissionEntity> fetched = permissionRepository.findByName("WRITE_USERS");
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getId()).isEqualTo(customId);
        }
    }

    @Nested
    class Save {

        @Test
        void shouldInsertWhenPermissionDoesNotExist() {
            PermissionEntity permission = createSamplePermission("DELETE_USERS", "users:delete");

            PermissionEntity result = permissionRepository.save(permission);

            assertThat(result.getId()).isNotNull();
            assertThat(permissionRepository.existsByName("DELETE_USERS")).isTrue();
        }

        @Test
        void shouldUpdateWhenPermissionAlreadyExists() {
            PermissionEntity permission = initPermission("UPDATE_USERS", "users:update");

            permission.setDescription("Nova descrição atualizada");
            permission.setIsActive(false);
            permission.setRiskLevel(PermissionRiskLevel.HIGH);

            PermissionEntity updated = permissionRepository.save(permission);

            assertThat(updated.getDescription()).isEqualTo("Nova descrição atualizada");

            Optional<PermissionEntity> fetched = permissionRepository.findByName("UPDATE_USERS");
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getDescription()).isEqualTo("Nova descrição atualizada");
            assertThat(fetched.get().getIsActive()).isFalse();
            assertThat(fetched.get().getRiskLevel()).isEqualTo(PermissionRiskLevel.HIGH);
        }

        @Test
        void shouldThrowExceptionWhenOptimisticLockFails() {
            PermissionEntity permission = initPermission("AUDIT_READ", "audit:read");

            permission.setVersion(99L);

            assertThatThrownBy(() -> permissionRepository.save(permission))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to update Permission entity");
        }
    }

    @Nested
    class FindByName {

        @Test
        void shouldReturnPermissionWhenNameExists() {
            PermissionEntity permission = initPermission("SESSION_READ", "session:read");

            Optional<PermissionEntity> result = permissionRepository.findByName("SESSION_READ");

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(permission.getId());
            assertThat(result.get().getSlug()).containsIgnoringCase("session:read");
        }

        @Test
        void shouldReturnEmptyWhenNameDoesNotExist() {
            Optional<PermissionEntity> result = permissionRepository.findByName("NON_EXISTENT_PERMISSION");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class ExistsByName {

        @Test
        void shouldReturnTrueWhenNameExists() {
            PermissionEntity permission = initPermission("ROLE_READ", "role:read");

            boolean exists = permissionRepository.existsByName(permission.getName());

            assertThat(exists).isTrue();
        }

        @Test
        void shouldReturnFalseWhenNameDoesNotExist() {
            boolean exists = permissionRepository.existsByName("NON_EXISTENT_PERMISSION");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    class ExistsBySlug {

        @Test
        void shouldReturnTrueWhenSlugExists() {
            PermissionEntity permission = initPermission("PERMISSION_READ", "permission:read");

            boolean exists = permissionRepository.existsBySlug(permission.getSlug());

            assertThat(exists).isTrue();
        }

        @Test
        void shouldReturnFalseWhenSlugDoesNotExist() {
            boolean exists = permissionRepository.existsBySlug("non:existent:slug");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    class FindAll {

        @Test
        void shouldFilterPermissionsByNameModuleAndStatus() {
            PermissionEntity p1 = createSamplePermission("AUTH_LOGIN_READ", "auth:login:read");
            p1.setModule(PermissionModule.AUTHENTICATION);
            p1.setIsActive(true);
            permissionRepository.insert(p1);

            PermissionEntity p2 = createSamplePermission("AUTH_LOGIN_WRITE", "auth:login:write");
            p2.setModule(PermissionModule.AUTHENTICATION);
            p2.setIsActive(false);
            permissionRepository.insert(p2);

            PermissionFilterDTO filter = new PermissionFilterDTO();
            filter.setName("AUTH");
            filter.setModule(Set.of(PermissionModule.AUTHENTICATION));
            filter.setIsActive(true);
            filter.setPage(0);
            filter.setSize(10);

            Page<PermissionEntity> page = permissionRepository.findAll(filter);

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent())
                    .extracting(PermissionEntity::getName)
                    .contains("AUTH_LOGIN_READ")
                    .doesNotContain("AUTH_LOGIN_WRITE");
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteByIdWhenPermissionExists() {
            PermissionEntity permission = initPermission("PERMISSION_TO_DELETE", "permission:delete");

            int deleted = permissionRepository.deleteById(permission.getId());

            assertThat(deleted).isGreaterThan(0);
            assertThat(permissionRepository.existsByName("PERMISSION_TO_DELETE")).isFalse();
        }

        @Test
        void shouldReturnZeroWhenDeletingByNonExistentId() {
            int deleted = permissionRepository.deleteById(UUID.randomUUID());

            assertThat(deleted).isEqualTo(0);
        }
    }
}