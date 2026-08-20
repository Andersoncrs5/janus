package org.janus.modules.identity.service.user;

import org.janus.modules.identity.application.user.service.user.ExistsUserByEmailUseCase;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExistsUserByEmailUseCaseTest {

    @InjectMocks
    private ExistsUserByEmailUseCase existsUserByEmailUseCase;

    @Mock
    private UserRepository repository;

    private String sampleEmail;

    @BeforeEach
    void setUp() {
        sampleEmail = "test@janus.org";
    }

    @Nested
    @DisplayName("Email Existence Scenarios")
    class ExistsCases {

        @Test
        @DisplayName("Should return true when email exists in the repository")
        void shouldReturnTrueWhenEmailExists() {
            when(repository.existsByEmail(sampleEmail)).thenReturn(true);

            Result<Boolean> result = existsUserByEmailUseCase.execute(sampleEmail);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isTrue();

            verify(repository).existsByEmail(sampleEmail);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should return false when email does not exist in the repository")
        void shouldReturnFalseWhenEmailDoesNotExist() {
            when(repository.existsByEmail(sampleEmail)).thenReturn(false);

            Result<Boolean> result = existsUserByEmailUseCase.execute(sampleEmail);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isFalse();

            verify(repository).existsByEmail(sampleEmail);
            verifyNoMoreInteractions(repository);
        }
    }
}