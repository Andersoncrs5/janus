package org.janus.modules.authorization.service.role;

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
@DisplayName("CreateRoleUseCase Tests")
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
        createRoleDTO = new CreateRoleDTO(
                "admin",
                "Administrator",
                "admin"
        );

        roleEntity = new RoleEntity();
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should successfully create a role")
        void shouldCreateRoleSuccessfully() {
            // Arrange
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenReturn(roleEntity);

            // Act
            Result<RoleEntity> result =
                    useCase.execute(createRoleDTO);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(roleEntity, result.getValue());

            verify(mapper, times(1))
                    .toEntity(createRoleDTO);

            verify(repository, times(1))
                    .insert(roleEntity);

            verifyNoMoreInteractions(
                    mapper,
                    repository
            );
        }
    }

    @Nested
    @DisplayName("Data Integrity Exception Scenarios")
    class DataIntegrityScenarios {

        @Test
        @DisplayName("Should return 409 when slug already exists")
        void shouldReturn409WhenSlugAlreadyExists() {
            // Arrange
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "Data integrity violation: "
                                            + "uk_roles_slug constraint failed"
                            )
                    );

            // Act
            Result<RoleEntity> result =
                    useCase.execute(createRoleDTO);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(409, result.getStatusCode());
            assertEquals(
                    "Role already exists with this slug",
                    result.getFirstError()
            );

            verify(mapper)
                    .toEntity(createRoleDTO);

            verify(repository)
                    .insert(roleEntity);
        }

        @Test
        @DisplayName("Should return 409 when name already exists")
        void shouldReturn409WhenNameAlreadyExists() {
            // Arrange
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "Data integrity violation: "
                                            + "uk_roles_name constraint failed"
                            )
                    );

            // Act
            Result<RoleEntity> result =
                    useCase.execute(createRoleDTO);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(409, result.getStatusCode());
            assertEquals(
                    "Role already exists with this name",
                    result.getFirstError()
            );

            verify(mapper)
                    .toEntity(createRoleDTO);

            verify(repository)
                    .insert(roleEntity);
        }

        @Test
        @DisplayName("Should return 400 when role name is empty")
        void shouldReturn400WhenNameIsEmpty() {
            // Arrange
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "Data integrity violation: "
                                            + "ck_roles_name_not_empty "
                                            + "constraint failed"
                            )
                    );

            // Act
            Result<RoleEntity> result =
                    useCase.execute(createRoleDTO);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(400, result.getStatusCode());
            assertEquals(
                    "Role name cannot be empty",
                    result.getFirstError()
            );
        }

        @Test
        @DisplayName("Should return 400 when version is invalid")
        void shouldReturn400WhenVersionIsInvalid() {
            // Arrange
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "Data integrity violation: "
                                            + "ck_roles_version "
                                            + "constraint failed"
                            )
                    );

            // Act
            Result<RoleEntity> result =
                    useCase.execute(createRoleDTO);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(400, result.getStatusCode());
            assertEquals(
                    "Invalid record version",
                    result.getFirstError()
            );
        }

        @Test
        @DisplayName("Should delegate unknown constraint to DatabaseConstraintHandler")
        void shouldDelegateToHandlerOnUnknownConstraint() {
            // Arrange
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "some_other_constraint"
                            )
                    );

            // Act
            Result<RoleEntity> result =
                    useCase.execute(createRoleDTO);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());

            verify(mapper)
                    .toEntity(createRoleDTO);

            verify(repository)
                    .insert(roleEntity);
        }

        @Test
        @DisplayName("Should delegate to DatabaseConstraintHandler when exception message is null")
        void shouldDelegateToHandlerWhenMessageIsNull() {
            // Arrange
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenThrow(
                            new DataIntegrityViolationException(null)
                    );

            // Act
            Result<RoleEntity> result =
                    useCase.execute(createRoleDTO);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());

            verify(mapper)
                    .toEntity(createRoleDTO);

            verify(repository)
                    .insert(roleEntity);
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when mapper fails")
        void shouldThrowInternalServerErrorExceptionWhenMapperFails() {
            // Arrange
            when(mapper.toEntity(createRoleDTO))
                    .thenThrow(
                            new RuntimeException(
                                    "Unexpected mapper failure"
                            )
                    );

            // Act & Assert
            InternalServerErrorException exception =
                    assertThrows(
                            InternalServerErrorException.class,
                            () -> useCase.execute(createRoleDTO)
                    );

            assertEquals(
                    "Unexpected mapper failure",
                    exception.getMessage()
            );

            verify(mapper)
                    .toEntity(createRoleDTO);

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException when repository fails")
        void shouldThrowInternalServerErrorExceptionOnRepositoryFailure() {
            // Arrange
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenThrow(
                            new RuntimeException(
                                    "Database connection timeout"
                            )
                    );

            // Act & Assert
            InternalServerErrorException exception =
                    assertThrows(
                            InternalServerErrorException.class,
                            () -> useCase.execute(createRoleDTO)
                    );

            assertEquals(
                    "Database connection timeout",
                    exception.getMessage()
            );

            verify(mapper)
                    .toEntity(createRoleDTO);

            verify(repository)
                    .insert(roleEntity);
        }
    }

    @Nested
    @DisplayName("Dependency Interaction Scenarios")
    class DependencyInteractionScenarios {

        @Test
        @DisplayName("Should map DTO before inserting role")
        void shouldMapBeforeInsert() {
            // Arrange
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenReturn(roleEntity);

            // Act
            useCase.execute(createRoleDTO);

            // Assert
            var inOrder = inOrder(mapper, repository);

            inOrder.verify(mapper)
                    .toEntity(createRoleDTO);

            inOrder.verify(repository)
                    .insert(roleEntity);
        }

        @Test
        @DisplayName("Should not insert role when mapping fails")
        void shouldNotInsertWhenMappingFails() {
            // Arrange
            when(mapper.toEntity(createRoleDTO))
                    .thenThrow(
                            new RuntimeException("Mapping failed")
                    );

            // Act
            assertThrows(
                    InternalServerErrorException.class,
                    () -> useCase.execute(createRoleDTO)
            );

            // Assert
            verify(mapper)
                    .toEntity(createRoleDTO);

            verifyNoInteractions(repository);
        }
    }
}