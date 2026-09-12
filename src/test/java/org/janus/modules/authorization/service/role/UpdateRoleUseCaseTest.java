package org.janus.modules.authorization.service.role;


import org.janus.modules.authorization.application.dto.role.request.UpdateRoleDTO;
import org.janus.modules.authorization.application.mapper.RoleMapper;
import org.janus.modules.authorization.application.service.role.UpdateRoleUseCase;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateRoleUseCaseTest {

    @Mock
    private RoleRepository repository;

    @Mock
    private RoleMapper mapper;

    @InjectMocks
    private UpdateRoleUseCase useCase;

    private UUID roleId;
    private RoleEntity existingRole;
    private UpdateRoleDTO updateDTO;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();

        existingRole = new RoleEntity();
        existingRole.setId(roleId);
        existingRole.setName("OLD_NAME");
        existingRole.setSlug("old-slug");
        existingRole.setDescription("Old Description");

        updateDTO = new UpdateRoleDTO(
                "NEW_NAME",
                "New Description",
                true,
                "new-slug"
        );
    }

    @Nested
    @DisplayName("Cenários de Sucesso")
    class SuccessScenarios {

        @Test
        @DisplayName("Deve atualizar e retornar Result.created com sucesso quando a role existe")
        void shouldUpdateAndReturnCreatedResultWhenRoleExists() {
            when(repository.findById(roleId)).thenReturn(Optional.of(existingRole));

            // Simula a alteração que o MapStruct/Mapper faria na entidade
            doAnswer(invocation -> {
                UpdateRoleDTO dto = invocation.getArgument(0);
                RoleEntity entity = invocation.getArgument(1);
                entity.setName(dto.name());
                entity.setSlug(dto.slug());
                entity.setDescription(dto.description());
                return null;
            }).when(mapper).updateEntityFromDto(updateDTO, existingRole);

            when(repository.save(existingRole)).thenReturn(existingRole);

            Result<RoleEntity> result = useCase.execute(roleId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(201);
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getName()).isEqualTo("NEW_NAME");
            assertThat(result.getData().getSlug()).isEqualTo("new-slug");

            verify(repository).findById(roleId);
            verify(mapper).updateEntityFromDto(updateDTO, existingRole);
            verify(repository).save(existingRole);
        }
    }

    @Nested
    @DisplayName("Cenários de Falha e Validações")
    class FailureScenarios {

        @Test
        @DisplayName("Deve retornar Result.notFound quando a role não for encontrada")
        void shouldReturnNotFoundWhenRoleDoesNotExist() {
            when(repository.findById(roleId)).thenReturn(Optional.empty());

            Result<RoleEntity> result = useCase.execute(roleId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().get()).isEqualTo("Role not found");

            verify(repository).findById(roleId);
            verify(mapper, never()).updateEntityFromDto(any(), any());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve retornar 409 conflito quando violar constraint de slug único (uk_roles_slug)")
        void shouldReturn409ConflictWhenSlugAlreadyExists() {
            when(repository.findById(roleId)).thenReturn(Optional.of(existingRole));
            when(repository.save(existingRole)).thenThrow(
                    new DataIntegrityViolationException("duplicate key value violates unique constraint \"uk_roles_slug\"")
            );

            Result<RoleEntity> result = useCase.execute(roleId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(409);
            assertThat(result.getMessage()).contains("Role already exists with slug: '" + updateDTO.slug() + "'");
        }

        @Test
        @DisplayName("Deve retornar 409 conflito quando violar constraint de name único (uk_roles_name)")
        void shouldReturn409ConflictWhenNameAlreadyExists() {
            when(repository.findById(roleId)).thenReturn(Optional.of(existingRole));
            when(repository.save(existingRole)).thenThrow(
                    new DataIntegrityViolationException("duplicate key value violates unique constraint \"uk_roles_name\"")
            );

            Result<RoleEntity> result = useCase.execute(roleId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(409);
            assertThat(result.getMessage()).contains("Role already exists with name: '" + updateDTO.name() + "'");
        }

        @Test
        @DisplayName("Deve lançar InternalServerErrorException em caso de erro inesperado no repositório")
        void shouldThrowInternalServerErrorExceptionOnUnexpectedException() {
            when(repository.findById(roleId)).thenReturn(Optional.of(existingRole));
            when(repository.save(existingRole)).thenThrow(new RuntimeException("Database connection failure"));

            assertThatThrownBy(() -> useCase.execute(roleId, updateDTO))
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining("Database connection failure");
        }
    }
}