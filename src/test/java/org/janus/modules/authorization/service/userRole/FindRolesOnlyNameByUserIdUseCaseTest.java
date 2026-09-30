package org.janus.modules.authorization.service.userRole;

import org.janus.modules.authorization.application.service.userRole.FindRolesOnlyNameByUserIdUseCase;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindRolesOnlyNameByUserIdUseCaseTest {

    @Mock
    private UserRoleRepository repository;

    @InjectMocks
    private FindRolesOnlyNameByUserIdUseCase useCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 Bad Request when userId is null")
        void shouldReturn400WhenUserIdIsNull() {
            Result<List<String>> result = useCase.execute(null);

            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(400, result.getStatusCode());
            assertTrue(result.getFirstError().contains("User ID cannot be null"));
            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should successfully return list of role names for valid userId")
        void shouldReturnRoleNamesSuccessfully() {
            List<String> expectedRoles = List.of("ROLE_ADMIN", "ROLE_USER");
            when(repository.findRolesOnlyNameByUserId(userId)).thenReturn(expectedRoles);

            Result<List<String>> result = useCase.execute(userId);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(200, result.getStatusCode());
            assertEquals(2, result.getValue().size());
            assertEquals(expectedRoles, result.getValue());

            verify(repository, times(1)).findRolesOnlyNameByUserId(userId);
        }

        @Test
        @DisplayName("Should return empty list when user has no active roles")
        void shouldReturnEmptyListWhenNoRolesFound() {
            when(repository.findRolesOnlyNameByUserId(userId)).thenReturn(Collections.emptyList());

            Result<List<String>> result = useCase.execute(userId);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(200, result.getStatusCode());
            assertTrue(result.getValue().isEmpty());

            verify(repository, times(1)).findRolesOnlyNameByUserId(userId);
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when repository throws exception")
        void shouldThrowInternalServerErrorExceptionOnRepositoryFailure() {
            when(repository.findRolesOnlyNameByUserId(userId))
                    .thenThrow(new RuntimeException("Database connection failed"));

            InternalServerErrorException exception = assertThrows(
                    InternalServerErrorException.class,
                    () -> useCase.execute(userId)
            );

            assertTrue(exception.getMessage().contains("Error retrieving role names for userId: " + userId));
            verify(repository, times(1)).findRolesOnlyNameByUserId(userId);
        }
    }
}