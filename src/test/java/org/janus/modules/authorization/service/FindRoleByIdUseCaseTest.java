package org.janus.modules.authorization.service;

import org.janus.modules.authorization.application.service.role.FindRoleByIdUseCase;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindRoleByIdUseCaseTest {

    @Mock
    private RoleRepository repository;

    @InjectMocks
    private FindRoleByIdUseCase useCase;

    private UUID roleId;
    private RoleEntity sampleRole;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();
        sampleRole = new RoleEntity();
        sampleRole.setId(roleId);
        sampleRole.setName("ROLE_ADMIN");
        sampleRole.setSlug("admin");
        sampleRole.setDescription("Administrador do sistema");
        sampleRole.setIsActive(true);
    }

    @Nested
    @DisplayName("Cenários de Sucesso")
    class SuccessScenarios {

        @Test
        @DisplayName("Deve retornar Result.success com a Role quando o ID existir")
        void shouldReturnSuccessWhenRoleExists() {
            when(repository.findById(roleId)).thenReturn(Optional.of(sampleRole));

            Result<RoleEntity> result = useCase.execute(roleId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(200);
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getId()).isEqualTo(roleId);
            assertThat(result.getData().getName()).isEqualTo("ROLE_ADMIN");

            verify(repository).findById(roleId);
        }
    }

    @Nested
    @DisplayName("Cenários de Falha")
    class FailureScenarios {

        @Test
        @DisplayName("Deve retornar Result.notFound quando o ID não existir")
        void shouldReturnNotFoundWhenRoleDoesNotExist() {
            when(repository.findById(roleId)).thenReturn(Optional.empty());

            Result<RoleEntity> result = useCase.execute(roleId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().get()).isEqualTo("Role not found");
            assertThat(result.getData()).isNull();

            verify(repository).findById(roleId);
        }
    }
}