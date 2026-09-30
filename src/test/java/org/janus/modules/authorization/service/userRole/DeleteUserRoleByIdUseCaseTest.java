package org.janus.modules.authorization.service.userRole;

import org.janus.modules.authorization.application.service.userRole.DeleteUserRoleByIdUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.IFindRoleNameByIdUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.IFindRolesOnlyNameByUserIdUseCase;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteUserRoleByIdUseCaseTest {
    @Mock
    private UserRoleRepository repository;

    @Mock
    private IFindRoleNameByIdUseCase findRoleNameById;

    @Mock
    private IFindRolesOnlyNameByUserIdUseCase findRolesOnlyNameByUserId;

    @InjectMocks
    private DeleteUserRoleByIdUseCase useCase;

    private UUID userRoleId;
    private UUID operatorUserId;

    @BeforeEach
    void setUp() {
        userRoleId = UUID.randomUUID();
        operatorUserId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should successfully delete a common role by a non-master user")
        void shouldDeleteCommonRoleSuccessfully() {
            // Arrange
            when(findRolesOnlyNameByUserId.execute(operatorUserId))
                    .thenReturn(Result.success(List.of("OPERATOR")));
            when(findRoleNameById.execute(userRoleId))
                    .thenReturn(Result.success("COMMON_USER"));
            when(repository.deleteById(userRoleId)).thenReturn(1);

            // Act
            Result<Void> result = useCase.execute(userRoleId, operatorUserId);

            // Assert
            assertNotNull(result);
            assertTrue(result.isSuccess());

            verify(findRolesOnlyNameByUserId, times(1)).execute(operatorUserId);
            verify(findRoleNameById, times(1)).execute(userRoleId);
            verify(repository, times(1)).deleteById(userRoleId);
        }

        @Test
        @DisplayName("Should successfully delete ADMINISTRADOR role when operator is MASTER")
        void shouldDeleteAdminRoleWhenOperatorIsMaster() {
            // Arrange
            when(findRolesOnlyNameByUserId.execute(operatorUserId))
                    .thenReturn(Result.success(List.of("MASTER", "ADMINISTRADOR")));
            when(findRoleNameById.execute(userRoleId))
                    .thenReturn(Result.success("ADMINISTRADOR"));
            when(repository.deleteById(userRoleId)).thenReturn(1);

            // Act
            Result<Void> result = useCase.execute(userRoleId, operatorUserId);

            // Assert
            assertNotNull(result);
            assertTrue(result.isSuccess());

            verify(repository, times(1)).deleteById(userRoleId);
        }
    }

    @Nested
    @DisplayName("Validation and Rule Violation Scenarios")
    class ValidationAndRuleScenarios {

        @Test
        @DisplayName("Should return badRequest when userRoleId or operatorUserId is null")
        void shouldReturnBadRequestWhenParametersAreNull() {
            // Act
            Result<Void> resultNullId = useCase.execute(null, operatorUserId);
            Result<Void> resultNullUser = useCase.execute(userRoleId, null);

            // Assert
            assertFalse(resultNullId.isSuccess());
            assertTrue(resultNullId.getFirstError().contains("UserID and Id are required"));

            assertFalse(resultNullUser.isSuccess());
            assertTrue(resultNullUser.getFirstError().contains("UserID and Id are required"));

            verifyNoInteractions(findRolesOnlyNameByUserId, findRoleNameById, repository);
        }

        @Test
        @DisplayName("Should return failure when findRolesOnlyNameByUserId fails")
        void shouldReturnFailureWhenOperatorRolesLookupFails() {
            // Arrange
            when(findRolesOnlyNameByUserId.execute(operatorUserId))
                    .thenReturn(Result.failure("User roles error", 500));

            // Act
            Result<Void> result = useCase.execute(userRoleId, operatorUserId);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals("User roles error", result.getFirstError());

            verify(findRoleNameById, never()).execute(any());
            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should return failure when findRoleNameById fails")
        void shouldReturnFailureWhenTargetRoleNameLookupFails() {
            // Arrange
            when(findRolesOnlyNameByUserId.execute(operatorUserId))
                    .thenReturn(Result.success(List.of("OPERATOR")));
            when(findRoleNameById.execute(userRoleId))
                    .thenReturn(Result.notFound("Role not found"));

            // Act
            Result<Void> result = useCase.execute(userRoleId, operatorUserId);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals("Role not found", result.getFirstError());

            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should return 403 when attempting to delete MASTER role")
        void shouldReturnForbiddenWhenDeletingMasterRole() {
            // Arrange
            when(findRolesOnlyNameByUserId.execute(operatorUserId))
                    .thenReturn(Result.success(List.of("MASTER")));
            when(findRoleNameById.execute(userRoleId))
                    .thenReturn(Result.success("MASTER"));

            // Act
            Result<Void> result = useCase.execute(userRoleId, operatorUserId);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(403, result.getStatus());
            assertTrue(result.getFirstError().contains("MASTER role cannot be deleted"));

            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should return 403 when non-MASTER operator attempts to delete ADMINISTRADOR role")
        void shouldReturnForbiddenWhenNonMasterDeletesAdminRole() {
            // Arrange
            when(findRolesOnlyNameByUserId.execute(operatorUserId))
                    .thenReturn(Result.success(List.of("ADMINISTRADOR")));
            when(findRoleNameById.execute(userRoleId))
                    .thenReturn(Result.success("ADMINISTRADOR"));

            // Act
            Result<Void> result = useCase.execute(userRoleId, operatorUserId);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(403, result.getStatus());
            assertTrue(result.getFirstError().contains("Only MASTER users can revoke the ADMINISTRADOR role"));

            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should return notFound when deleteById affects 0 rows")
        void shouldReturnNotFoundWhenZeroRowsDeleted() {
            // Arrange
            when(findRolesOnlyNameByUserId.execute(operatorUserId))
                    .thenReturn(Result.success(List.of("OPERATOR")));
            when(findRoleNameById.execute(userRoleId))
                    .thenReturn(Result.success("COMMON_USER"));
            when(repository.deleteById(userRoleId)).thenReturn(0);

            // Act
            Result<Void> result = useCase.execute(userRoleId, operatorUserId);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertTrue(result.getFirstError().contains("User Role not found"));

            verify(repository, times(1)).deleteById(userRoleId);
        }

        @Test
        @DisplayName("Should return failure when deleteById affects more than 1 row")
        void shouldReturnErrorWhenMultipleRowsDeleted() {
            // Arrange
            when(findRolesOnlyNameByUserId.execute(operatorUserId))
                    .thenReturn(Result.success(List.of("OPERATOR")));
            when(findRoleNameById.execute(userRoleId))
                    .thenReturn(Result.success("COMMON_USER"));
            when(repository.deleteById(userRoleId)).thenReturn(2);

            // Act
            Result<Void> result = useCase.execute(userRoleId, operatorUserId);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(500, result.getStatus());

            verify(repository, times(1)).deleteById(userRoleId);
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException on unexpected runtime exception")
        void shouldThrowInternalServerErrorOnException() {
            // Arrange
            when(findRolesOnlyNameByUserId.execute(operatorUserId))
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            assertThrows(InternalServerErrorException.class, () -> useCase.execute(userRoleId, operatorUserId));
        }
    }
}