package org.janus.modules.authorization.service.role;

import org.janus.modules.authorization.application.dto.role.request.CreateRoleDTO;
import org.janus.modules.authorization.application.mapper.RoleMapper;
import org.janus.modules.authorization.application.service.role.CreateRoleUseCase;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.infrastructure.in.userRole.ICreateUserRoleUseCase;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.modules.identity.ports.in.user.IFindUserByEmailUseCase;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.BootstrapProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateRoleUseCase")
class CreateRoleUseCaseTest {

    @Mock
    private RoleRepository repository;

    @Mock
    private RoleMapper mapper;

    @Mock
    private IFindUserByEmailUseCase findUserByEmail;

    @Mock
    private ICreateUserRoleUseCase createUserRole;

    @Mock
    private BootstrapProperties properties;

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
        roleEntity.setName(createRoleDTO.name());
        roleEntity.setDescription(createRoleDTO.description());
        roleEntity.setSlug(createRoleDTO.slug());
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 when DTO is null")
        void shouldReturnBadRequestWhenDtoIsNull() {
            Result<RoleEntity> result = useCase.execute(null);

            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getFirstError())
                    .isEqualTo("Role data must be provided");

            verifyNoInteractions(
                    repository,
                    mapper,
                    findUserByEmail,
                    createUserRole,
                    properties
            );
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should create a role successfully and return 201")
        void shouldCreateRoleSuccessfully() {
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenReturn(roleEntity);

            Result<RoleEntity> result =
                    useCase.execute(createRoleDTO);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo(201);
            assertThat(result.getData()).isSameAs(roleEntity);

            verify(mapper).toEntity(createRoleDTO);
            verify(repository).insert(roleEntity);

            verifyNoInteractions(
                    findUserByEmail,
                    createUserRole,
                    properties
            );
        }

        @Test
        @DisplayName("Should map DTO before inserting role")
        void shouldMapBeforeInsert() {
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenReturn(roleEntity);

            useCase.execute(createRoleDTO);

            InOrder inOrder = inOrder(mapper, repository);

            inOrder.verify(mapper)
                    .toEntity(createRoleDTO);

            inOrder.verify(repository)
                    .insert(roleEntity);
        }
    }

    @Nested
    @DisplayName("Data Integrity Scenarios")
    class DataIntegrityScenarios {

        @Test
        @DisplayName("Should return 409 when slug already exists")
        void shouldReturnConflictWhenSlugAlreadyExists() {
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "duplicate key value violates unique constraint \"uk_roles_slug\""
                            )
                    );

            Result<RoleEntity> result =
                    useCase.execute(createRoleDTO);

            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(409);
            assertThat(result.getFirstError())
                    .isEqualTo("Role already exists with this slug");

            verify(mapper).toEntity(createRoleDTO);
            verify(repository).insert(roleEntity);
        }

        @Test
        @DisplayName("Should return 409 when name already exists")
        void shouldReturnConflictWhenNameAlreadyExists() {
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "duplicate key value violates unique constraint \"uk_roles_name\""
                            )
                    );

            Result<RoleEntity> result =
                    useCase.execute(createRoleDTO);

            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(409);
            assertThat(result.getFirstError())
                    .isEqualTo("Role already exists with this name");

            verify(mapper).toEntity(createRoleDTO);
            verify(repository).insert(roleEntity);
        }

        @Test
        @DisplayName("Should return 400 when role name violates check constraint")
        void shouldReturnBadRequestWhenNameIsEmpty() {
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "violates check constraint \"ck_roles_name_not_empty\""
                            )
                    );

            Result<RoleEntity> result =
                    useCase.execute(createRoleDTO);

            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getFirstError())
                    .isEqualTo("Role name cannot be empty");
        }

        @Test
        @DisplayName("Should return 400 when role version is invalid")
        void shouldReturnBadRequestWhenVersionIsInvalid() {
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "violates check constraint \"ck_roles_version\""
                            )
                    );

            Result<RoleEntity> result =
                    useCase.execute(createRoleDTO);

            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getFirstError())
                    .isEqualTo("Invalid record version");
        }

        @Test
        @DisplayName("Should delegate unknown constraint to DatabaseConstraintHandler")
        void shouldDelegateUnknownConstraint() {
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "some_unknown_constraint"
                            )
                    );

            Result<RoleEntity> result =
                    useCase.execute(createRoleDTO);

            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getFirstError()).isNotBlank();

            verify(mapper).toEntity(createRoleDTO);
            verify(repository).insert(roleEntity);
        }

        @Test
        @DisplayName("Should delegate to DatabaseConstraintHandler when exception message is null")
        void shouldDelegateWhenExceptionMessageIsNull() {
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenThrow(
                            new DataIntegrityViolationException(null)
                    );

            Result<RoleEntity> result =
                    useCase.execute(createRoleDTO);

            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getFirstError()).isNotBlank();

            verify(mapper).toEntity(createRoleDTO);
            verify(repository).insert(roleEntity);
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when mapper fails")
        void shouldThrowInternalServerErrorWhenMapperFails() {
            when(mapper.toEntity(createRoleDTO))
                    .thenThrow(
                            new RuntimeException(
                                    "Unexpected mapper failure"
                            )
                    );

            assertThatThrownBy(
                    () -> useCase.execute(createRoleDTO)
            )
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining(
                            "Unexpected mapper failure"
                    );

            verify(mapper).toEntity(createRoleDTO);
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException when repository fails")
        void shouldThrowInternalServerErrorWhenRepositoryFails() {
            when(mapper.toEntity(createRoleDTO))
                    .thenReturn(roleEntity);

            when(repository.insert(roleEntity))
                    .thenThrow(
                            new RuntimeException(
                                    "Database connection timeout"
                            )
                    );

            assertThatThrownBy(
                    () -> useCase.execute(createRoleDTO)
            )
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining(
                            "Database connection timeout"
                    );

            verify(mapper).toEntity(createRoleDTO);
            verify(repository).insert(roleEntity);
        }
    }
}