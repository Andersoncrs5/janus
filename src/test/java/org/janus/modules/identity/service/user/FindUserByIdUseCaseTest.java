package org.janus.modules.identity.service.user;

import org.janus.modules.identity.application.user.service.user.FindUserByIdUseCase;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.out.UserRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FindUserByIdUseCaseTest {

    @InjectMocks
    private FindUserByIdUseCase findUserByIdUseCase;

    @Mock
    private UserRepository repository;

    private UUID userId;
    private UserEntity sampleUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = UserEntity.builder()
                .id(userId)
                .email("test@janus.org")
                .username("janus_user")
                .fullName("Janus User")
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessCases {

        @Test
        @DisplayName("Should return success with user data when ID is found")
        void shouldReturnUserWhenIdExists() {
            when(repository.findById(userId)).thenReturn(Optional.of(sampleUser));

            Result<UserEntity> result = findUserByIdUseCase.execute(userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getId()).isEqualTo(userId);
            assertThat(result.getData().getEmail()).isEqualTo("test@janus.org");

            verify(repository).findById(userId);
        }
    }

    @Nested
    @DisplayName("Error and Not Found Scenarios")
    class NotFoundCases {

        @Test
        @DisplayName("Should return HTTP 404 failure when user is not found")
        void shouldReturnNotFoundWhenUserDoesNotExist() {
            when(repository.findById(userId)).thenReturn(Optional.empty());

            Result<UserEntity> result = findUserByIdUseCase.execute(userId);

            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getFirstError()).isEqualTo("User not found");

            verify(repository).findById(userId);
        }
    }
}