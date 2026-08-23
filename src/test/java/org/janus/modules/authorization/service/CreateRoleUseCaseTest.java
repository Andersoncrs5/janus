package org.janus.modules.authorization.service;

import org.janus.modules.authorization.application.dto.role.request.CreateRoleDTO;
import org.janus.modules.authorization.application.mapper.RoleMapper;
import org.janus.modules.authorization.application.service.role.CreateRoleUseCase;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateRoleUseCaseTest {

    @Mock
    private RoleRepository repository;

    @Mock
    private RoleMapper mapper;

    @InjectMocks
    private CreateRoleUseCase useCase;

    private CreateRoleDTO createRoleDTO;
    private RoleEntity roleEntity;

    @BeforeEach
    void setUp() {
        createRoleDTO = new CreateRoleDTO("admin", "Administrator", "admin");
        roleEntity = new RoleEntity();
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should successfully create a role")
        void shouldCreateRoleSuccessfully() {
            // Arrange
            when(mapper.toEntity(createRoleDTO)).thenReturn(roleEntity);
            when(repository.insert(roleEntity)).thenReturn(roleEntity);

            // Act
            Result<RoleEntity> result = useCase.execute(createRoleDTO);

            // Assert
            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(roleEntity, result.getValue());

            verify(mapper, times(1)).toEntity(createRoleDTO);
            verify(repository, times(1)).insert(roleEntity);
        }
    }

    @Nested
    @DisplayName("Data Integrity Exception Scenarios")
    class DataIntegrityScenarios {

        @Test
        @DisplayName("Should return 409 failure when slug constraint fails")
        void shouldReturn409WhenSlugAlreadyExists() {
            // Arrange
            String errorMessage = "Data integrity violation: uk_roles_slug constraint failed";
            when(mapper.toEntity(createRoleDTO)).thenReturn(roleEntity);
            when(repository.insert(roleEntity))
                    .thenThrow(new DataIntegrityViolationException(errorMessage));

            // Act
            Result<RoleEntity> result = useCase.execute(createRoleDTO);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(409, result.getStatusCode());
            assertTrue(result.getFirstError().contains("Role already exists with slug: '" + createRoleDTO.slug() + "'"));
        }

        @Test
        @DisplayName("Should return 409 failure when name constraint fails")
        void shouldReturn409WhenNameAlreadyExists() {
            // Arrange
            String errorMessage = "Data integrity violation: uk_roles_name constraint failed";
            when(mapper.toEntity(createRoleDTO)).thenReturn(roleEntity);
            when(repository.insert(roleEntity))
                    .thenThrow(new DataIntegrityViolationException(errorMessage));

            // Act
            Result<RoleEntity> result = useCase.execute(createRoleDTO);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(409, result.getStatusCode());
            assertTrue(result.getFirstError().contains("Role already exists with name: '" + createRoleDTO.name() + "'"));
        }

        @Test
        @DisplayName("Should delegate to DatabaseConstraintHandler when exception message is null")
        void shouldDelegateToHandlerWhenMessageIsNull() {
            // Arrange
            when(mapper.toEntity(createRoleDTO)).thenReturn(roleEntity);
            when(repository.insert(roleEntity))
                    .thenThrow(new DataIntegrityViolationException(null));

            // Act
            Result<RoleEntity> result = useCase.execute(createRoleDTO);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("Should delegate to DatabaseConstraintHandler on unknown constraint")
        void shouldDelegateToHandlerOnUnknownConstraint() {
            // Arrange
            when(mapper.toEntity(createRoleDTO)).thenReturn(roleEntity);
            when(repository.insert(roleEntity))
                    .thenThrow(new DataIntegrityViolationException("some_other_constraint"));

            // Act
            Result<RoleEntity> result = useCase.execute(createRoleDTO);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when generic exception occurs")
        void shouldThrowInternalServerErrorExceptionOnGenericFailure() {
            // Arrange
            when(mapper.toEntity(createRoleDTO)).thenReturn(roleEntity);
            when(repository.insert(roleEntity))
                    .thenThrow(new RuntimeException("Database connection timeout"));

            // Act & Assert
            InternalServerErrorException exception = assertThrows(
                    InternalServerErrorException.class,
                    () -> useCase.execute(createRoleDTO)
            );

            assertEquals("Database connection timeout", exception.getMessage());
        }
    }
}