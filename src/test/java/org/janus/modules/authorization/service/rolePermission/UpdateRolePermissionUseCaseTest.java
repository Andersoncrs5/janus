package org.janus.modules.authorization.service.rolePermission;

import org.janus.modules.authorization.application.dto.rolePermission.request.UpdateRolePermissionDTO;
import org.janus.modules.authorization.application.mapper.RolePermissionMapper;
import org.janus.modules.authorization.application.service.rolePermission.UpdateRolePermissionUseCase;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.shared.domain.enums.rolePermission.PermissionEffectEnum;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateRolePermissionUseCaseTest {

    @Mock
    private RolePermissionRepository repository;

    @Mock
    private RolePermissionMapper mapper;

    @InjectMocks
    private UpdateRolePermissionUseCase useCase;

    private UpdateRolePermissionDTO updateRolePermissionDTO;
    private RolePermissionEntity rolePermissionEntity;
    private UUID id;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();

        updateRolePermissionDTO = new UpdateRolePermissionDTO(
                PermissionEffectEnum.ALLOW,
                null,
                null
        );

        rolePermissionEntity = new RolePermissionEntity();
        rolePermissionEntity.setId(id);
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 bad request when DTO is null")
        void shouldReturnBadRequestWhenDtoIsNull() {
            Result<RolePermissionEntity> result = useCase.execute(null, id);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getMessage()).contains("CreateRolePermissionDTO must not be null");

            verifyNoInteractions(mapper, repository);
        }

        @Test
        @DisplayName("Should return 400 bad request when ID is null")
        void shouldReturnBadRequestWhenIdIsNull() {
            Result<RolePermissionEntity> result = useCase.execute(updateRolePermissionDTO, null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getMessage()).contains("Role Permission Id should be defined");

            verifyNoInteractions(mapper, repository);
        }

        @Test
        @DisplayName("Should return 400 bad request when entity is not found")
        void shouldReturnBadRequestWhenEntityDoesNotExist() {
            when(repository.findById(id)).thenReturn(Optional.empty());

            Result<RolePermissionEntity> result = useCase.execute(updateRolePermissionDTO, id);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getMessage()).contains("Role Permission not found");

            verify(repository, times(1)).findById(id);
            verifyNoInteractions(mapper);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should successfully update role permission")
        void shouldUpdateRolePermissionSuccessfully() {
            when(repository.findById(id)).thenReturn(Optional.of(rolePermissionEntity));
            doNothing().when(mapper).updateEntityFromDto(updateRolePermissionDTO, rolePermissionEntity);
            when(repository.save(rolePermissionEntity)).thenReturn(rolePermissionEntity);

            Result<RolePermissionEntity> result = useCase.execute(updateRolePermissionDTO, id);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(201);
            assertThat(result.getValue()).isEqualTo(rolePermissionEntity);

            verify(repository, times(1)).findById(id);
            verify(mapper, times(1)).updateEntityFromDto(updateRolePermissionDTO, rolePermissionEntity);
            verify(repository, times(1)).save(rolePermissionEntity);
        }
    }

    @Nested
    @DisplayName("Data Integrity Exception Scenarios")
    class DataIntegrityScenarios {

        @Test
        @DisplayName("Should delegate to DatabaseConstraintHandler on DataIntegrityViolationException")
        void shouldDelegateToHandlerOnDataIntegrityViolation() {
            when(repository.findById(id)).thenReturn(Optional.of(rolePermissionEntity));
            doNothing().when(mapper).updateEntityFromDto(updateRolePermissionDTO, rolePermissionEntity);
            when(repository.save(rolePermissionEntity))
                    .thenThrow(new DataIntegrityViolationException("Database constraint error"));

            Result<RolePermissionEntity> result = useCase.execute(updateRolePermissionDTO, id);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();

            verify(repository, times(1)).findById(id);
            verify(mapper, times(1)).updateEntityFromDto(updateRolePermissionDTO, rolePermissionEntity);
            verify(repository, times(1)).save(rolePermissionEntity);
        }

        @Test
        @DisplayName("Should delegate to DatabaseConstraintHandler when exception message is null")
        void shouldDelegateToHandlerWhenMessageIsNull() {
            when(repository.findById(id)).thenReturn(Optional.of(rolePermissionEntity));
            doNothing().when(mapper).updateEntityFromDto(updateRolePermissionDTO, rolePermissionEntity);
            when(repository.save(rolePermissionEntity))
                    .thenThrow(new DataIntegrityViolationException(null));

            Result<RolePermissionEntity> result = useCase.execute(updateRolePermissionDTO, id);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();

            verify(repository, times(1)).findById(id);
            verify(mapper, times(1)).updateEntityFromDto(updateRolePermissionDTO, rolePermissionEntity);
            verify(repository, times(1)).save(rolePermissionEntity);
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when generic exception occurs")
        void shouldThrowInternalServerErrorExceptionOnGenericFailure() {
            when(repository.findById(id)).thenReturn(Optional.of(rolePermissionEntity));
            doNothing().when(mapper).updateEntityFromDto(updateRolePermissionDTO, rolePermissionEntity);
            when(repository.save(rolePermissionEntity))
                    .thenThrow(new RuntimeException("Database connection timeout"));

            InternalServerErrorException exception = assertThrows(
                    InternalServerErrorException.class,
                    () -> useCase.execute(updateRolePermissionDTO, id)
            );

            assertThat(exception.getMessage()).isEqualTo("Database connection timeout");
        }
    }
}