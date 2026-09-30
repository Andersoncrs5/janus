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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 when role ID is null")
        void shouldReturnBadRequestWhenRoleIdIsNull() {

            Result<RoleEntity> result =
                    useCase.execute(null, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getFirstError())
                    .isEqualTo("Role ID is required");

            verifyNoInteractions(repository, mapper);
        }

        @Test
        @DisplayName("Should return 400 when update data is null")
        void shouldReturnBadRequestWhenUpdateDataIsNull() {

            Result<RoleEntity> result =
                    useCase.execute(roleId, null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getFirstError())
                    .isEqualTo("Role data is required");

            verifyNoInteractions(repository, mapper);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should update role successfully when role exists")
        void shouldUpdateRoleSuccessfully() {

            when(repository.findById(roleId))
                    .thenReturn(Optional.of(existingRole));

            doAnswer(invocation -> {
                UpdateRoleDTO dto = invocation.getArgument(0);
                RoleEntity entity = invocation.getArgument(1);

                entity.setName(dto.name());
                entity.setSlug(dto.slug());
                entity.setDescription(dto.description());

                return null;
            }).when(mapper)
                    .updateEntityFromDto(updateDTO, existingRole);

            when(repository.save(existingRole))
                    .thenReturn(existingRole);

            Result<RoleEntity> result =
                    useCase.execute(roleId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo(200);
            assertThat(result.getData()).isSameAs(existingRole);

            assertThat(result.getData().getName())
                    .isEqualTo("NEW_NAME");

            assertThat(result.getData().getSlug())
                    .isEqualTo("new-slug");

            assertThat(result.getData().getDescription())
                    .isEqualTo("New Description");

            verify(repository)
                    .findById(roleId);

            verify(mapper)
                    .updateEntityFromDto(updateDTO, existingRole);

            verify(repository)
                    .save(existingRole);
        }

        @Test
        @DisplayName("Should execute operations in the correct order")
        void shouldExecuteOperationsInCorrectOrder() {

            when(repository.findById(roleId))
                    .thenReturn(Optional.of(existingRole));

            doAnswer(invocation -> null)
                    .when(mapper)
                    .updateEntityFromDto(updateDTO, existingRole);

            when(repository.save(existingRole))
                    .thenReturn(existingRole);

            useCase.execute(roleId, updateDTO);

            InOrder inOrder =
                    inOrder(repository, mapper);

            inOrder.verify(repository)
                    .findById(roleId);

            inOrder.verify(mapper)
                    .updateEntityFromDto(updateDTO, existingRole);

            inOrder.verify(repository)
                    .save(existingRole);
        }
    }

    @Nested
    @DisplayName("Not Found Scenarios")
    class NotFoundScenarios {

        @Test
        @DisplayName("Should return 404 when role does not exist")
        void shouldReturnNotFoundWhenRoleDoesNotExist() {

            when(repository.findById(roleId))
                    .thenReturn(Optional.empty());

            Result<RoleEntity> result =
                    useCase.execute(roleId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(404);
            assertThat(result.getFirstError())
                    .isEqualTo("Role not found");

            verify(repository)
                    .findById(roleId);

            verify(mapper, never())
                    .updateEntityFromDto(any(), any());

            verify(repository, never())
                    .save(any());
        }
    }

    @Nested
    @DisplayName("Data Integrity Scenarios")
    class DataIntegrityScenarios {

        @Test
        @DisplayName("Should return 409 when role slug already exists")
        void shouldReturnConflictWhenSlugAlreadyExists() {

            when(repository.findById(roleId))
                    .thenReturn(Optional.of(existingRole));

            when(repository.save(existingRole))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "duplicate key value violates unique constraint \"uk_roles_slug\""
                            )
                    );

            Result<RoleEntity> result =
                    useCase.execute(roleId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(409);
            assertThat(result.getFirstError())
                    .isEqualTo("Role already exists with this slug");

            verify(repository)
                    .findById(roleId);

            verify(mapper)
                    .updateEntityFromDto(updateDTO, existingRole);

            verify(repository)
                    .save(existingRole);
        }

        @Test
        @DisplayName("Should return 409 when role name already exists")
        void shouldReturnConflictWhenNameAlreadyExists() {

            when(repository.findById(roleId))
                    .thenReturn(Optional.of(existingRole));

            when(repository.save(existingRole))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "duplicate key value violates unique constraint \"uk_roles_name\""
                            )
                    );

            Result<RoleEntity> result =
                    useCase.execute(roleId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(409);
            assertThat(result.getFirstError())
                    .isEqualTo("Role already exists with this name");

            verify(repository)
                    .findById(roleId);

            verify(mapper)
                    .updateEntityFromDto(updateDTO, existingRole);

            verify(repository)
                    .save(existingRole);
        }

        @Test
        @DisplayName("Should return 400 when role name is empty")
        void shouldReturnBadRequestWhenRoleNameIsEmpty() {

            when(repository.findById(roleId))
                    .thenReturn(Optional.of(existingRole));

            when(repository.save(existingRole))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "violates check constraint \"ck_roles_name_not_empty\""
                            )
                    );

            Result<RoleEntity> result =
                    useCase.execute(roleId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getFirstError())
                    .isEqualTo("Role name cannot be empty");
        }

        @Test
        @DisplayName("Should return 409 when role version is invalid")
        void shouldReturnConflictWhenRoleVersionIsInvalid() {

            when(repository.findById(roleId))
                    .thenReturn(Optional.of(existingRole));

            when(repository.save(existingRole))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "violates check constraint \"ck_roles_version\""
                            )
                    );

            Result<RoleEntity> result =
                    useCase.execute(roleId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(409);
            assertThat(result.getFirstError())
                    .isEqualTo("Invalid role version");
        }

        @Test
        @DisplayName("Should delegate unknown constraint to DatabaseConstraintHandler")
        void shouldDelegateUnknownConstraint() {

            when(repository.findById(roleId))
                    .thenReturn(Optional.of(existingRole));

            when(repository.save(existingRole))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "some unknown database constraint"
                            )
                    );

            Result<RoleEntity> result =
                    useCase.execute(roleId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();

            verify(repository)
                    .save(existingRole);
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when repository lookup fails")
        void shouldThrowInternalServerErrorWhenFindFails() {

            when(repository.findById(roleId))
                    .thenThrow(
                            new RuntimeException(
                                    "Database connection failure"
                            )
                    );

            assertThatThrownBy(
                    () -> useCase.execute(roleId, updateDTO)
            )
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining(
                            "Database connection failure"
                    );

            verify(repository)
                    .findById(roleId);

            verify(mapper, never())
                    .updateEntityFromDto(any(), any());

            verify(repository, never())
                    .save(any());
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException when mapper fails")
        void shouldThrowInternalServerErrorWhenMapperFails() {

            when(repository.findById(roleId))
                    .thenReturn(Optional.of(existingRole));

            doThrow(
                    new RuntimeException("Mapping failure")
            ).when(mapper)
                    .updateEntityFromDto(updateDTO, existingRole);

            assertThatThrownBy(
                    () -> useCase.execute(roleId, updateDTO)
            )
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining("Mapping failure");

            verify(repository)
                    .findById(roleId);

            verify(mapper)
                    .updateEntityFromDto(updateDTO, existingRole);

            verify(repository, never())
                    .save(any());
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException when repository save fails")
        void shouldThrowInternalServerErrorWhenSaveFails() {

            when(repository.findById(roleId))
                    .thenReturn(Optional.of(existingRole));

            doAnswer(invocation -> null)
                    .when(mapper)
                    .updateEntityFromDto(updateDTO, existingRole);

            when(repository.save(existingRole))
                    .thenThrow(
                            new RuntimeException(
                                    "Database connection failure"
                            )
                    );

            assertThatThrownBy(
                    () -> useCase.execute(roleId, updateDTO)
            )
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining(
                            "Database connection failure"
                    );

            verify(repository)
                    .findById(roleId);

            verify(mapper)
                    .updateEntityFromDto(updateDTO, existingRole);

            verify(repository)
                    .save(existingRole);
        }
    }
}
