package org.janus.modules.authorization.service;

import org.janus.modules.authorization.application.service.role.DeleteRoleByIdUseCase;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteRoleByIdUseCaseTest {

    @Mock
    private RoleRepository repository;

    @InjectMocks
    private DeleteRoleByIdUseCase useCase;

    private UUID roleId;
    private RoleEntity roleEntity;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();
        roleEntity = new RoleEntity();
        roleEntity.setIsSystem(false);
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should successfully delete a valid non-system role")
        void shouldDeleteRoleSuccessfully() {
            // Arrange
            when(repository.findById(roleId)).thenReturn(Optional.of(roleEntity));
            when(repository.deleteById(roleId)).thenReturn(1);

            // Act
            Result<Void> result = useCase.execute(roleId);

            // Assert
            assertNotNull(result);
            assertTrue(result.isSuccess());

            verify(repository, times(1)).findById(roleId);
            verify(repository, times(1)).deleteById(roleId);
        }
    }

    @Nested
    @DisplayName("Validation and Error Scenarios")
    class ErrorScenarios {

        @Test
        @DisplayName("Should return notFound when role does not exist on initial lookup")
        void shouldReturnNotFoundWhenRoleDoesNotExist() {
            // Arrange
            when(repository.findById(roleId)).thenReturn(Optional.empty());

            // Act
            Result<Void> result = useCase.execute(roleId);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertTrue(result.getFirstError().contains("Role not found"));

            verify(repository, times(1)).findById(roleId);
            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should return failure with 403 status when attempting to delete a system role")
        void shouldReturnForbiddenWhenRoleIsSystemRole() {
            // Arrange
            roleEntity.setIsSystem(true);
            when(repository.findById(roleId)).thenReturn(Optional.of(roleEntity));

            // Act
            Result<Void> result = useCase.execute(roleId);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(403, result.getStatusCode());
            assertTrue(result.getFirstError().contains("This role is of system"));

            verify(repository, times(1)).findById(roleId);
            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should return notFound when deleteById affects 0 rows")
        void shouldReturnNotFoundWhenZeroRowsDeleted() {
            // Arrange
            when(repository.findById(roleId)).thenReturn(Optional.of(roleEntity));
            when(repository.deleteById(roleId)).thenReturn(0);

            // Act
            Result<Void> result = useCase.execute(roleId);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertTrue(result.getFirstError().contains("Role not found"));

            verify(repository, times(1)).findById(roleId);
            verify(repository, times(1)).deleteById(roleId);
        }

        @Test
        @DisplayName("Should return error when deleteById affects more than 1 row")
        void shouldReturnErrorWhenMultipleRowsDeleted() {
            // Arrange
            when(repository.findById(roleId)).thenReturn(Optional.of(roleEntity));
            when(repository.deleteById(roleId)).thenReturn(2);

            // Act
            Result<Void> result = useCase.execute(roleId);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertTrue(result.getFirstError().contains("More of one role deleted"));

            verify(repository, times(1)).findById(roleId);
            verify(repository, times(1)).deleteById(roleId);
        }
    }
}