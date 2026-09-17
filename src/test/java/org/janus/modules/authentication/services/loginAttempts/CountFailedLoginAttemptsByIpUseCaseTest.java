package org.janus.modules.authentication.services.loginAttempts;

import org.janus.modules.authentication.application.service.loginAttempts.CountFailedLoginAttemptsByIpUseCase;
import org.janus.modules.authentication.port.out.LoginAttemptRepository;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountFailedLoginAttemptsByIpUseCaseTest {

    @Mock
    private LoginAttemptRepository repository;

    @InjectMocks
    private CountFailedLoginAttemptsByIpUseCase useCase;

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 Bad Request when IP address is null")
        void shouldReturn400WhenIpAddressIsNull() {
            OffsetDateTime since = OffsetDateTime.now();

            Result<Long> result = useCase.execute(null, since);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("IP address cannot be null or blank");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when IP address is blank")
        void shouldReturn400WhenIpAddressIsBlank() {
            OffsetDateTime since = OffsetDateTime.now();

            Result<Long> result = useCase.execute("   ", since);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("IP address cannot be null or blank");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when since is null")
        void shouldReturn400WhenSinceIsNull() {
            String ipAddress = "192.168.1.100";

            Result<Long> result = useCase.execute(ipAddress, null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("Since timestamp cannot be null");

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should successfully return count of failed login attempts and trim the IP address")
        void shouldReturnCountOfFailedLoginAttemptsSuccessfully() {
            String rawIpAddress = "  192.168.1.100  ";
            String trimmedIpAddress = "192.168.1.100";
            OffsetDateTime since = OffsetDateTime.now().minusHours(1);
            long expectedCount = 5L;

            when(repository.countFailedAttemptsByIpAddress(trimmedIpAddress, since)).thenReturn(expectedCount);

            Result<Long> result = useCase.execute(rawIpAddress, since);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEqualTo(expectedCount);

            verify(repository).countFailedAttemptsByIpAddress(trimmedIpAddress, since);
        }

        @Test
        @DisplayName("Should return zero when no failed attempts exist")
        void shouldReturnZeroWhenNoFailedAttemptsExist() {
            String ipAddress = "10.0.0.1";
            OffsetDateTime since = OffsetDateTime.now().minusHours(1);

            when(repository.countFailedAttemptsByIpAddress(ipAddress, since)).thenReturn(0L);

            Result<Long> result = useCase.execute(ipAddress, since);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEqualTo(0L);

            verify(repository).countFailedAttemptsByIpAddress(ipAddress, since);
        }
    }
}