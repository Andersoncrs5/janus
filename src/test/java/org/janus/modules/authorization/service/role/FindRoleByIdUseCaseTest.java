package org.janus.modules.authorization.service.role;

import org.janus.modules.authorization.application.service.role.FindRoleByIdUseCase;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindRoleByIdUseCase")
class FindRoleByIdUseCaseTest {

    @Mock
    private RoleRepository repository;

    @InjectMocks
    private FindRoleByIdUseCase useCase;

    private UUID roleId;
    private RoleEntity sampleRole;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();

        sampleRole = new RoleEntity();
        sampleRole.setId(roleId);
        sampleRole.setName("ROLE_ADMIN");
        sampleRole.setSlug("admin");
        sampleRole.setDescription("System administrator role");
        sampleRole.setActive(true);
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 when role ID is null")
        void shouldReturnBadRequestWhenIdIsNull() {
            Result<RoleEntity> result = useCase.execute(null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError())
                    .isEqualTo("ID is required");

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should return the role when ID exists")
        void shouldReturnSuccessWhenRoleExists() {
            when(repository.findById(roleId))
                    .thenReturn(Optional.of(sampleRole));

            Result<RoleEntity> result = useCase.execute(roleId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(200);
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData()).isSameAs(sampleRole);
            assertThat(result.getData().getId()).isEqualTo(roleId);
            assertThat(result.getData().getName()).isEqualTo("ROLE_ADMIN");
            assertThat(result.getData().getSlug()).isEqualTo("admin");

            verify(repository).findById(roleId);
            verifyNoMoreInteractions(repository);
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

            Result<RoleEntity> result = useCase.execute(roleId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getFirstError())
                    .isEqualTo("Role not found");
            assertThat(result.getData()).isNull();

            verify(repository).findById(roleId);
            verifyNoMoreInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should propagate repository exceptions")
        void shouldPropagateRepositoryException() {
            RuntimeException exception =
                    new RuntimeException("Database connection failure");

            when(repository.findById(roleId))
                    .thenThrow(exception);

            assertThatThrownBy(() -> useCase.execute(roleId))
                    .isSameAs(exception)
                    .hasMessage("Database connection failure");

            verify(repository).findById(roleId);
            verifyNoMoreInteractions(repository);
        }
    }
}