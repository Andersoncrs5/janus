package org.janus.modules.identity.service.user;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.janus.modules.identity.application.user.service.user.DeleteUserByIdUseCase;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class DeleteUserByIdUseCaseTest {

    @Inject
    DeleteUserByIdUseCase deleteUserByIdUseCase;

    @InjectMock
    UserRepository repository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessCases {

        @Test
        @DisplayName("Should successfully delete user when ID exists")
        void shouldDeleteUserSuccessfullyWhenIdExists() {
            when(repository.deleteById(userId)).thenReturn(1);

            Result<Void> result = deleteUserByIdUseCase.execute(userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();

            verify(repository).deleteById(userId);
        }
    }

    @Nested
    @DisplayName("Failure and Business Logic Scenarios")
    class FailureCases {

        @Test
        @DisplayName("Should return Result.notFound when no records are deleted")
        void shouldReturnNotFoundWhenUserDoesNotExist() {
            when(repository.deleteById(userId)).thenReturn(0);

            Result<Void> result = deleteUserByIdUseCase.execute(userId);

            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getFirstError()).isEqualTo("User not found");

            verify(repository).deleteById(userId);
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class ExceptionalCases {

        @Test
        @DisplayName("Should propagate exception when repository fails")
        void shouldThrowExceptionWhenRepositoryFails() {
            when(repository.deleteById(any()))
                    .thenThrow(new RuntimeException("Database connection error"));

            assertThatThrownBy(() -> deleteUserByIdUseCase.execute(userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database connection error");

            verify(repository).deleteById(userId);
        }
    }
}