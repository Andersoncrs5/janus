package org.janus.modules.authorization.service.userRole;

import org.janus.modules.authorization.application.service.userRole.DeleteRoleByIdUseCase;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteRoleByIdUseCaseTest {

    @Mock
    private UserRoleRepository repository;

    @InjectMocks
    private DeleteRoleByIdUseCase useCase;

    private UUID userRoleId;

    @BeforeEach
    void setUp() {
        userRoleId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should delete user role successfully when valid id is provided")
    void shouldDeleteUserRoleSuccessfully() {
        when(repository.deleteById(userRoleId)).thenReturn(1);

        Result<Void> result = useCase.execute(userRoleId);

        assertTrue(result.isSuccess());
        verify(repository, times(1)).deleteById(userRoleId);
    }

    @Test
    @DisplayName("Should return not found when user role does not exist (0 rows deleted)")
    void shouldReturnNotFoundWhenUserRoleDoesNotExist() {
        when(repository.deleteById(userRoleId)).thenReturn(0);

        Result<Void> result = useCase.execute(userRoleId);

        assertFalse(result.isSuccess());
        assertEquals(404, result.getStatusCode());
        assertEquals("User Role not found", result.getMessage().get());
        verify(repository, times(1)).deleteById(userRoleId);
    }

    @Test
    @DisplayName("Should return not found when multiple user roles are deleted (> 1 rows deleted)")
    void shouldReturnNotFoundWhenMultipleUserRolesDeleted() {
        when(repository.deleteById(userRoleId)).thenReturn(2);

        Result<Void> result = useCase.execute(userRoleId);

        assertFalse(result.isSuccess());
        assertEquals(404, result.getStatusCode());
        assertEquals("More of one user role deleted", result.getMessage().get());
        verify(repository, times(1)).deleteById(userRoleId);
    }

    @Test
    @DisplayName("Should handle data integrity violation exception correctly")
    void shouldHandleDataIntegrityViolationException() {
        when(repository.deleteById(userRoleId)).thenThrow(new DataIntegrityViolationException("Constraint violation"));

        Result<Void> result = useCase.execute(userRoleId);

        assertFalse(result.isSuccess());
        verify(repository, times(1)).deleteById(userRoleId);
    }

    @Test
    @DisplayName("Should throw InternalServerErrorException on unexpected generic exception")
    void shouldThrowInternalServerErrorExceptionOnGenericError() {
        when(repository.deleteById(userRoleId)).thenThrow(new RuntimeException("Database error"));

        assertThrows(InternalServerErrorException.class, () -> useCase.execute(userRoleId));
        verify(repository, times(1)).deleteById(userRoleId);
    }
}