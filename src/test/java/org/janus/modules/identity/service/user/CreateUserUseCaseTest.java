package org.janus.modules.identity.service.user;

import org.janus.modules.identity.application.user.dto.CreateUserDTO;
import org.janus.modules.identity.application.user.mapper.UserMapper;
import org.janus.modules.identity.application.user.service.user.CreateUserUseCase;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.out.UserRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateUserUseCaseTest {

    @InjectMocks
    CreateUserUseCase createUserUseCase;

    @Mock
    UserRepository repository;

    @Mock
    UserMapper mapper;

    private CreateUserDTO sampleDto;
    private UserEntity sampleEntity;

    @BeforeEach
    void setUp() {
        sampleDto = new CreateUserDTO();
        sampleDto.setEmail("test@janus.org");
        sampleDto.setUsername("janus_user");
        sampleDto.setFullName("Janus User");

        sampleEntity = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("test@janus.org")
                .username("janus_user")
                .fullName("Janus User")
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("Cenários de Sucesso")
    class SuccessCases {

        @Test
        @DisplayName("Deve criar usuário com sucesso")
        void shouldCreateUserSuccessfully() {
            when(mapper.toEntity(sampleDto)).thenReturn(sampleEntity);
            when(repository.insert(sampleEntity)).thenReturn(sampleEntity);

            Result<UserEntity> result = createUserUseCase.execute(sampleDto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getEmail()).isEqualTo(sampleDto.getEmail());
            assertThat(result.getData().getUsername()).isEqualTo(sampleDto.getUsername());

            verify(mapper).toEntity(sampleDto);
            verify(repository).insert(sampleEntity);
        }
    }

    @Nested
    @DisplayName("Cenários de Duplicidade (Unique Key Violations)")
    class DuplicateKeyCases {

        @Test
        @DisplayName("Deve retornar falha HTTP 409 quando o e-mail já existir")
        void shouldReturnFailureWhenEmailAlreadyExists() {
            when(mapper.toEntity(sampleDto)).thenReturn(sampleEntity);

            DataIntegrityViolationException exception =
                    new DataIntegrityViolationException("ERROR: duplicate key value violates unique constraint \"uk_email_user\"");

            when(repository.insert(sampleEntity)).thenThrow(exception);

            Result<UserEntity> result = createUserUseCase.execute(sampleDto);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(409);
            assertThat(result.getFirstError()).isEqualTo("Email: 'test@janus.org' already exists");
        }

        @Test
        @DisplayName("Deve retornar falha HTTP 409 quando o username já existir")
        void shouldReturnFailureWhenUsernameAlreadyExists() {
            when(mapper.toEntity(sampleDto)).thenReturn(sampleEntity);

            DataIntegrityViolationException exception =
                    new DataIntegrityViolationException("ERROR: duplicate key value violates unique constraint \"uk_username_user\"");

            when(repository.insert(sampleEntity)).thenThrow(exception);

            Result<UserEntity> result = createUserUseCase.execute(sampleDto);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("Cenários de Restrição de Banco de Dados (DatabaseConstraintHandler)")
    class ConstraintHandlerCases {

        @Test
        @DisplayName("Deve tratar exceção quando a mensagem for nula")
        void shouldHandleNullMessageInDataIntegrityException() {
            when(mapper.toEntity(sampleDto)).thenReturn(sampleEntity);

            DataIntegrityViolationException exception = new DataIntegrityViolationException(null);
            when(repository.insert(sampleEntity)).thenThrow(exception);

            Result<UserEntity> result = createUserUseCase.execute(sampleDto);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).isEqualTo("Database integrity error");
        }

        @Test
        @DisplayName("Deve identificar campo obrigatório ausente (null value)")
        void shouldHandleNullValueConstraint() {
            when(mapper.toEntity(sampleDto)).thenReturn(sampleEntity);

            DataIntegrityViolationException exception = new DataIntegrityViolationException(
                    "null value in column \"full_name\" violates not-null constraint"
            );
            when(repository.insert(sampleEntity)).thenThrow(exception);

            Result<UserEntity> result = createUserUseCase.execute(sampleDto);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).isEqualTo("Required field 'full_name' is missing");
        }

        @Test
        @DisplayName("Deve identificar valor maior do que o permitido (value too long)")
        void shouldHandleValueTooLongConstraint() {
            when(mapper.toEntity(sampleDto)).thenReturn(sampleEntity);

            DataIntegrityViolationException exception = new DataIntegrityViolationException(
                    "value too long for type character varying(50) in column \"email\""
            );
            when(repository.insert(sampleEntity)).thenThrow(exception);

            Result<UserEntity> result = createUserUseCase.execute(sampleDto);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).isEqualTo("Field 'email' exceeded the allowed size");
        }

        @Test
        @DisplayName("Deve tratar violação de integridade genérica com causa")
        void shouldHandleGenericIntegrityViolationWithCause() {
            when(mapper.toEntity(sampleDto)).thenReturn(sampleEntity);

            RuntimeException cause = new RuntimeException("foreign key constraint failure");
            DataIntegrityViolationException exception = new DataIntegrityViolationException("Outer error", cause);

            when(repository.insert(sampleEntity)).thenThrow(exception);

            Result<UserEntity> result = createUserUseCase.execute(sampleDto);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("Database integrity error: foreign key constraint failure");
        }
    }

    @Nested
    @DisplayName("Cenários de Erro Inesperado")
    class InternalServerErrorCases {

        @Test
        @DisplayName("Deve lançar InternalServerErrorException ao ocorrer exceção inesperada")
        void shouldThrowInternalServerErrorExceptionOnUnexpectedError() {
            when(mapper.toEntity(sampleDto)).thenReturn(sampleEntity);
            when(repository.insert(any())).thenThrow(new RuntimeException("Unexpected DB Connection Failure"));

            assertThatThrownBy(() -> createUserUseCase.execute(sampleDto))
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining("Unexpected DB Connection Failure");
        }
    }
}