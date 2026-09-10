package org.janus.modules.authorization.application.service.userRole;

import org.janus.modules.authorization.application.dto.userRole.request.CreateUserRoleDTO;
import org.janus.modules.authorization.application.mapper.UserRoleMapper;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
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
class CreateUserRoleUseCaseTest {

    @Mock
    private UserRoleRepository repository;

    @Mock
    private UserRoleMapper mapper;

    @InjectMocks
    private CreateUserRoleUseCase useCase;

    private CreateUserRoleDTO dto;
    private UserRoleEntity entity;

    @BeforeEach
    void setUp() {
        dto = new CreateUserRoleDTO();
        dto.setUserId(UUID.randomUUID());
        dto.setRoleId(UUID.randomUUID());
        dto.setAssignedById(UUID.randomUUID());

        entity = new UserRoleEntity();
        entity.setUserId(dto.getUserId());
        entity.setRoleId(dto.getRoleId());
        entity.setAssignedById(dto.getAssignedById());
    }

    @Test
    @DisplayName("Should create user role successfully when valid data is provided")
    void shouldCreateUserRoleSuccessfully() {
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.insert(any(UserRoleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Result<UserRoleEntity> result = useCase.execute(dto);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(dto.getUserId(), result.getData().getUserId());
        assertEquals(dto.getRoleId(), result.getData().getRoleId());
        assertEquals(dto.getAssignedById(), result.getData().getAssignedById());

        verify(mapper, times(1)).toEntity(dto);
        verify(repository, times(1)).insert(entity);
    }

    @Test
    @DisplayName("Should return 409 conflict when user does not exist (fk_user_roles_user)")
    void shouldReturnConflictWhenUserDoesNotExist() {
        when(mapper.toEntity(dto)).thenReturn(entity);

        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Key (user_id)=(...) is not present in table users. Constraint: fk_user_roles_user"
        );
        when(repository.insert(any())).thenThrow(exception);

        Result<UserRoleEntity> result = useCase.execute(dto);

        assertFalse(result.isSuccess());
        assertEquals(409, result.getStatusCode());
        assertEquals("User not found with id: '" + dto.getUserId() + "'", result.getMessage().get());
        verify(repository, times(1)).insert(any());
    }

    @Test
    @DisplayName("Should return 409 conflict when role does not exist (fk_user_roles_role)")
    void shouldReturnConflictWhenRoleDoesNotExist() {
        when(mapper.toEntity(dto)).thenReturn(entity);

        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Key (role_id)=(...) is not present in table roles. Constraint: fk_user_roles_role"
        );
        when(repository.insert(any())).thenThrow(exception);

        Result<UserRoleEntity> result = useCase.execute(dto);

        assertFalse(result.isSuccess());
        assertEquals(409, result.getStatusCode());
        assertEquals("Role not found with id: '" + dto.getRoleId() + "'", result.getMessage().get());
        verify(repository, times(1)).insert(any());
    }

    @Test
    @DisplayName("Should return 409 conflict when assigned_by user does not exist (fk_user_roles_assigned_by)")
    void shouldReturnConflictWhenAssignedByUserDoesNotExist() {
        when(mapper.toEntity(dto)).thenReturn(entity);

        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Key (assigned_by)=(...) is not present in table users. Constraint: fk_user_roles_assigned_by"
        );
        when(repository.insert(any())).thenThrow(exception);

        Result<UserRoleEntity> result = useCase.execute(dto);

        assertFalse(result.isSuccess());
        assertEquals(409, result.getStatusCode());
        assertEquals("User not found with id: '" + dto.getAssignedById() + "'", result.getMessage().get());
        verify(repository, times(1)).insert(any());
    }

    @Test
    @DisplayName("Should throw InternalServerErrorException on unexpected generic exception")
    void shouldThrowInternalServerErrorExceptionOnGenericError() {
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.insert(any())).thenThrow(new RuntimeException("Database connection timeout"));

        assertThrows(InternalServerErrorException.class, () -> useCase.execute(dto));
        verify(repository, times(1)).insert(any());
    }
}