package org.janus.modules.authorization.repository;

import io.quarkus.test.junit.QuarkusTest;
import org.janus.help.BaseTest;
import org.janus.modules.authorization.application.dto.role.filter.RoleFilterDTO;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.shared.domain.page.Page;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
public class RoleRepositoryJdbcTest extends BaseTest {

    @Nested
    class Insert {

        @Test
        void shouldInsertRoleWithGeneratedIdWhenIdIsNull() {
            RoleEntity role = createSampleRole("ROLE_ADMIN", "admin");
            role.setId(null);

            RoleEntity saved = roleRepository.insert(role);

            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getVersion()).isEqualTo(0L);

            Optional<RoleEntity> fetched = roleRepository.findByName("ROLE_ADMIN");
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getSlug()).isEqualTo("admin");
        }

        @Test
        void shouldInsertRoleWithProvidedId() {
            RoleEntity role = createSampleRole("ROLE_USER", "user");
            UUID customId = role.getId();

            RoleEntity saved = roleRepository.insert(role);

            assertThat(saved.getId()).isEqualTo(customId);

            Optional<RoleEntity> fetched = roleRepository.findByName("ROLE_USER");
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getId()).isEqualTo(customId);
        }
    }

    @Nested
    class Save {

        @Test
        void shouldInsertWhenRoleDoesNotExist() {
            RoleEntity role = createSampleRole("ROLE_MANAGER", "manager");

            RoleEntity result = roleRepository.save(role);

            assertThat(result.getId()).isNotNull();
            assertThat(roleRepository.existsByName("ROLE_MANAGER")).isTrue();
        }

        @Test
        void shouldUpdateWhenRoleAlreadyExists() {
            RoleEntity role = initRole("ROLE_OPERATOR", "operator");

            role.setDescription("Nova descrição atualizada");
            role.setIsActive(false);

            RoleEntity updated = roleRepository.save(role);

            assertThat(updated.getDescription()).isEqualTo("Nova descrição atualizada");

            Optional<RoleEntity> fetched = roleRepository.findByName("ROLE_OPERATOR");
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getDescription()).isEqualTo("Nova descrição atualizada");
            assertThat(fetched.get().getIsActive()).isFalse();
        }

        @Test
        void shouldThrowExceptionWhenOptimisticLockFails() {
            RoleEntity role = initRole("ROLE_GUEST", "guest");

            role.setVersion(99L);

            assertThatThrownBy(() -> roleRepository.save(role))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to update Role entity");
        }
    }

    @Nested
    class FindByName {

        @Test
        void shouldReturnRoleWhenNameExists() {
            RoleEntity role = initRole("ROLE_AUDITOR", "auditor");

            Optional<RoleEntity> result = roleRepository.findByName("ROLE_AUDITOR");

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(role.getId());
            assertThat(result.get().getSlug()).isEqualTo("auditor");
        }

        @Test
        void shouldReturnEmptyWhenNameDoesNotExist() {
            Optional<RoleEntity> result = roleRepository.findByName("NON_EXISTENT_ROLE");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class ExistsByName {

        @Test
        void shouldReturnTrueWhenNameExists() {
            RoleEntity role = initRole("ROLE_SUPPORT", "support");

            boolean exists = roleRepository.existsByName(role.getName());

            assertThat(exists).isTrue();
        }

        @Test
        void shouldReturnFalseWhenNameDoesNotExist() {
            boolean exists = roleRepository.existsByName("NON_EXISTENT_ROLE");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    class FindAll {

        @Test
        void shouldFilterRolesByNameAndStatus() {
            RoleEntity r1 = createSampleRole("DEV_ADMIN", "dev-admin");
            r1.setIsActive(true);
            roleRepository.insert(r1);

            RoleEntity r2 = createSampleRole("DEV_GUEST", "dev-guest");
            r2.setIsActive(false);
            roleRepository.insert(r2);

            RoleFilterDTO filter = new RoleFilterDTO();
            filter.setName("DEV");
            filter.setIsActive(true);
            filter.setPage(0);
            filter.setSize(10);

            Page<RoleEntity> page = roleRepository.findAll(filter);

            assertThat(page.getTotalElements())
                    .isEqualTo(1);

            assertThat(page.getContent())
                    .extracting(RoleEntity::getName)
                    .contains("DEV_ADMIN")
                    .doesNotContain("DEV_GUEST");
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteByIdWhenRoleExists() {
            RoleEntity role = initRole("ROLE_TO_DELETE", "to-delete");

            int deleted = roleRepository.deleteById(role.getId());

            assertThat(deleted > 0).isTrue();
            assertThat(roleRepository.existsByName("ROLE_TO_DELETE")).isFalse();
        }

        @Test
        void shouldReturnFalseWhenDeletingByNonExistentId() {
            int deleted = roleRepository.deleteById(UUID.randomUUID());

            assertThat(deleted > 0).isFalse();
        }
    }
}