package org.janus.modules.bootstrap.service.user;

import org.janus.modules.bootstrap.gateway.BootstrapInBoundGateway;
import org.janus.modules.identity.application.user.dto.request.CreateUserDTO;
import org.janus.modules.identity.application.user.service.CreateUserUseCase;
import org.janus.modules.identity.application.user.service.ExistsUserByEmailUseCase;
import org.janus.modules.identity.application.userCredentials.dto.CreateUserCredentialsDTO;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.gateway.user.UserOutboundGateway;
import org.janus.modules.identity.ports.in.userCredentials.ICreateUserCredentialUseCase;
import org.janus.shared.domain.enums.PasswordAlgorithmEnum;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.BootstrapProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserMasterUseCaseTest {

    @Mock
    private BootstrapInBoundGateway gateway;

    @Mock
    private UserOutboundGateway userOutboundGateway;

    @Mock
    private BootstrapProperties properties;

    @Mock
    private BootstrapProperties.MasterConfig masterConfig;

    @Mock
    private ExistsUserByEmailUseCase existsUserByEmailUseCase;

    @Mock
    private CreateUserUseCase createUserUseCase;

    @Mock
    private ICreateUserCredentialUseCase createUserCredential;

    @InjectMocks
    private CreateUserMasterUseCase useCase;

    private static final String EMAIL = "admin@gmail.com";
    private static final String USERNAME = "admin";
    private static final String FULL_NAME = "System Administrator";
    private static final String PASSWORD = "MasterPassword123!";

    private UUID userId;
    private UserEntity masterUser;

    @BeforeEach
    void setUp() {

        userId = UUID.randomUUID();

        masterUser = new UserEntity();
        masterUser.setId(userId);
        masterUser.setEmail(EMAIL);
        masterUser.setUsername(USERNAME);
        masterUser.setFullName(FULL_NAME);

        lenient()
                .when(properties.master())
                .thenReturn(masterConfig);

        lenient()
                .when(masterConfig.email())
                .thenReturn(EMAIL);

        lenient()
                .when(masterConfig.username())
                .thenReturn(USERNAME);

        lenient()
                .when(masterConfig.fullName())
                .thenReturn(FULL_NAME);

        lenient()
                .when(masterConfig.password())
                .thenReturn(PASSWORD);

        lenient()
                .when(gateway.userOutboundGateway())
                .thenReturn(userOutboundGateway);

        lenient()
                .when(userOutboundGateway.existsUserByEmailUseCase())
                .thenReturn(existsUserByEmailUseCase);

        lenient()
                .when(userOutboundGateway.createUserUseCase())
                .thenReturn(createUserUseCase);
    }

    @Nested
    @DisplayName("Successful Scenarios")
    class SuccessfulScenarios {

        @Test
        @DisplayName("Should create master user and credentials successfully")
        void shouldCreateMasterUserAndCredentialsSuccessfully() {

            when(existsUserByEmailUseCase.execute(EMAIL))
                    .thenReturn(Result.success(false));

            when(createUserUseCase.execute(any(CreateUserDTO.class)))
                    .thenReturn(Result.success(masterUser));

            UserCredentialsEntity credentials =
                    new UserCredentialsEntity();

            when(createUserCredential.execute(
                    any(CreateUserCredentialsDTO.class),
                    any(UUID.class)
            )).thenReturn(Result.success(credentials));

            useCase.execute();

            verify(existsUserByEmailUseCase)
                    .execute(EMAIL);

            verify(createUserUseCase)
                    .execute(any(CreateUserDTO.class));

            verify(createUserCredential)
                    .execute(any(CreateUserCredentialsDTO.class), eq(userId));
        }

        @Test
        @DisplayName("Should create user with values from master configuration")
        void shouldCreateUserWithConfiguredValues() {

            when(existsUserByEmailUseCase.execute(EMAIL))
                    .thenReturn(Result.success(false));

            when(createUserUseCase.execute(any(CreateUserDTO.class)))
                    .thenReturn(Result.success(masterUser));

            when(createUserCredential.execute(
                    any(CreateUserCredentialsDTO.class),
                    eq(userId)
            )).thenReturn(Result.success(new UserCredentialsEntity()));

            useCase.execute();

            ArgumentCaptor<CreateUserDTO> captor =
                    ArgumentCaptor.forClass(CreateUserDTO.class);

            verify(createUserUseCase)
                    .execute(captor.capture());

            CreateUserDTO dto = captor.getValue();

            assertThat(dto.getEmail())
                    .isEqualTo(EMAIL);

            assertThat(dto.getUsername())
                    .isEqualTo(USERNAME);

            assertThat(dto.getFullName())
                    .isEqualTo(FULL_NAME);

            assertThat(dto.getPassword())
                    .isEqualTo(PASSWORD);
        }

        @Test
        @DisplayName("Should create credentials using configured password and ARGON2ID")
        void shouldCreateCredentialsWithArgon2Id() {

            when(existsUserByEmailUseCase.execute(EMAIL))
                    .thenReturn(Result.success(false));

            when(createUserUseCase.execute(any(CreateUserDTO.class)))
                    .thenReturn(Result.success(masterUser));

            when(createUserCredential.execute(
                    any(CreateUserCredentialsDTO.class),
                    eq(userId)
            )).thenReturn(Result.success(new UserCredentialsEntity()));

            useCase.execute();

            ArgumentCaptor<CreateUserCredentialsDTO> captor =
                    ArgumentCaptor.forClass(
                            CreateUserCredentialsDTO.class
                    );

            verify(createUserCredential)
                    .execute(captor.capture(), eq(userId));

            CreateUserCredentialsDTO credentials =
                    captor.getValue();

            assertThat(credentials.algorithm())
                    .isEqualTo(PasswordAlgorithmEnum.ARGON2ID);
        }

        @Test
        @DisplayName("Should execute operations in the correct order")
        void shouldExecuteOperationsInCorrectOrder() {

            when(existsUserByEmailUseCase.execute(EMAIL))
                    .thenReturn(Result.success(false));

            when(createUserUseCase.execute(any(CreateUserDTO.class)))
                    .thenReturn(Result.success(masterUser));

            when(createUserCredential.execute(
                    any(CreateUserCredentialsDTO.class),
                    eq(userId)
            )).thenReturn(Result.success(new UserCredentialsEntity()));

            useCase.execute();

            InOrder order =
                    inOrder(
                            existsUserByEmailUseCase,
                            createUserUseCase,
                            createUserCredential
                    );

            order.verify(existsUserByEmailUseCase)
                    .execute(EMAIL);

            order.verify(createUserUseCase)
                    .execute(any(CreateUserDTO.class));

            order.verify(createUserCredential)
                    .execute(
                            any(CreateUserCredentialsDTO.class),
                            eq(userId)
                    );
        }
    }

    @Nested
    @DisplayName("Existing User Scenarios")
    class ExistingUserScenarios {

        @Test
        @DisplayName("Should skip creation when master user already exists")
        void shouldSkipCreationWhenMasterUserAlreadyExists() {

            when(existsUserByEmailUseCase.execute(EMAIL))
                    .thenReturn(Result.success(true));

            useCase.execute();

            verify(existsUserByEmailUseCase)
                    .execute(EMAIL);

            verify(createUserUseCase, never())
                    .execute(any(CreateUserDTO.class));

            verifyNoInteractions(createUserCredential);
        }
    }

    @Nested
    @DisplayName("Existence Check Failure Scenarios")
    class ExistenceCheckFailureScenarios {

        @Test
        @DisplayName("Should abort when master user existence check fails")
        void shouldAbortWhenExistenceCheckFails() {

            when(existsUserByEmailUseCase.execute(EMAIL))
                    .thenReturn(
                            Result.failure(
                                    "Database connection error",
                                    500
                            )
                    );

            useCase.execute();

            verify(existsUserByEmailUseCase)
                    .execute(EMAIL);

            verify(createUserUseCase, never())
                    .execute(any(CreateUserDTO.class));

            verifyNoInteractions(createUserCredential);
        }
    }

    @Nested
    @DisplayName("User Creation Failure Scenarios")
    class UserCreationFailureScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when master user creation fails")
        void shouldThrowWhenMasterUserCreationFails() {

            when(existsUserByEmailUseCase.execute(EMAIL))
                    .thenReturn(Result.success(false));

            when(createUserUseCase.execute(any(CreateUserDTO.class)))
                    .thenReturn(
                            Result.failure(
                                    "Error creating user",
                                    500
                            )
                    );

            assertThatThrownBy(
                    () -> useCase.execute()
            )
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining(
                            "Failed to create user master"
                    )
                    .hasMessageContaining(
                            "Error creating user"
                    );

            verify(createUserUseCase)
                    .execute(any(CreateUserDTO.class));

            verifyNoInteractions(createUserCredential);
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should propagate unexpected exception from existence check")
        void shouldPropagateUnexpectedExceptionFromExistenceCheck() {

            when(existsUserByEmailUseCase.execute(EMAIL))
                    .thenThrow(
                            new RuntimeException(
                                    "Database connection failure"
                            )
                    );

            assertThatThrownBy(
                    () -> useCase.execute()
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(
                            "Database connection failure"
                    );

            verify(createUserUseCase, never())
                    .execute(any(CreateUserDTO.class));

            verifyNoInteractions(createUserCredential);
        }

        @Test
        @DisplayName("Should propagate unexpected exception from user creation")
        void shouldPropagateUnexpectedExceptionFromUserCreation() {

            when(existsUserByEmailUseCase.execute(EMAIL))
                    .thenReturn(Result.success(false));

            when(createUserUseCase.execute(any(CreateUserDTO.class)))
                    .thenThrow(
                            new RuntimeException(
                                    "User creation failure"
                            )
                    );

            assertThatThrownBy(
                    () -> useCase.execute()
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(
                            "User creation failure"
                    );

            verifyNoInteractions(createUserCredential);
        }

        @Test
        @DisplayName("Should propagate unexpected exception from credential creation")
        void shouldPropagateUnexpectedExceptionFromCredentialCreation() {

            when(existsUserByEmailUseCase.execute(EMAIL))
                    .thenReturn(Result.success(false));

            when(createUserUseCase.execute(any(CreateUserDTO.class)))
                    .thenReturn(Result.success(masterUser));

            when(createUserCredential.execute(
                    any(CreateUserCredentialsDTO.class),
                    eq(userId)
            )).thenThrow(
                    new RuntimeException(
                            "Credential creation failure"
                    )
            );

            assertThatThrownBy(
                    () -> useCase.execute()
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(
                            "Credential creation failure"
                    );
        }
    }
}

