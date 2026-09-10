package org.janus.modules.authorization.service.permission;

import org.janus.modules.authorization.application.dto.permission.request.CreatePermissionDTO;
import org.janus.modules.authorization.application.mapper.PermissionMapper;
import org.janus.modules.authorization.application.service.permission.CreatePermissionUseCase;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.enums.permission.PermissionModule;
import org.janus.shared.domain.enums.permission.PermissionResource;
import org.janus.shared.domain.enums.permission.PermissionRiskLevel;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePermissionUseCaseTest {

    @Mock
    private PermissionRepository repository;

    @Mock
    private PermissionMapper mapper;

    @InjectMocks
    private CreatePermissionUseCase useCase;

    private CreatePermissionDTO createSampleDto() {
        return new CreatePermissionDTO(
                "Read Users",
                "users:read",
                "Description test",
                PermissionModule.IDENTITY,
                PermissionResource.USER,
                "read",
                PermissionRiskLevel.LOW,
                true,
                false,
                "{\"category\":\"test\"}"
        );
    }

    @Test
    @DisplayName("Should return bad request when DTO is null")
    void shouldReturnBadRequestWhenDtoIsNull() {
        UUID createdBy = UUID.randomUUID();

        Result<PermissionEntity> result = useCase.execute(null, createdBy);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission data must be provided");
        verifyNoInteractions(mapper, repository);
    }

    @Test
    @DisplayName("Should create permission successfully")
    void shouldCreatePermissionSuccessfully() {
        UUID createdBy = UUID.randomUUID();
        CreatePermissionDTO dto = createSampleDto();

        PermissionEntity mappedEntity = PermissionEntity.builder()
                .name(dto.name())
                .slug(dto.slug())
                .build();

        PermissionEntity insertedEntity = PermissionEntity.builder()
                .id(UUID.randomUUID())
                .name(dto.name())
                .slug(dto.slug())
                .createdBy(createdBy)
                .build();

        when(mapper.toEntity(dto)).thenReturn(mappedEntity);
        when(repository.insert(mappedEntity)).thenReturn(insertedEntity);

        Result<PermissionEntity> result = useCase.execute(dto, createdBy);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo(201);
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getId()).isEqualTo(insertedEntity.getId());
        assertThat(result.getData().getCreatedBy()).isEqualTo(createdBy);

        verify(mapper, times(1)).toEntity(dto);
        verify(repository, times(1)).insert(mappedEntity);
    }

    @Test
    @DisplayName("Should return 409 conflict when slug constraint is violated")
    void shouldReturnConflictWhenSlugAlreadyExists() {
        UUID createdBy = UUID.randomUUID();
        CreatePermissionDTO dto = createSampleDto();
        PermissionEntity mappedEntity = new PermissionEntity();

        when(mapper.toEntity(dto)).thenReturn(mappedEntity);
        when(repository.insert(mappedEntity)).thenThrow(
                new DataIntegrityViolationException("duplicate key value violates unique constraint \"uk_permissions_slug\"")
        );

        Result<PermissionEntity> result = useCase.execute(dto, createdBy);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getStatus()).isEqualTo(409);
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission already exists with slug: '" + dto.slug() + "'");

        verify(repository, times(1)).insert(mappedEntity);
    }

    @Test
    @DisplayName("Should return 409 conflict when name constraint is violated")
    void shouldReturnConflictWhenNameAlreadyExists() {
        UUID createdBy = UUID.randomUUID();
        CreatePermissionDTO dto = createSampleDto();
        PermissionEntity mappedEntity = new PermissionEntity();

        when(mapper.toEntity(dto)).thenReturn(mappedEntity);
        when(repository.insert(mappedEntity)).thenThrow(
                new DataIntegrityViolationException("duplicate key value violates unique constraint \"uk_permissions_name\"")
        );

        Result<PermissionEntity> result = useCase.execute(dto, createdBy);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getStatus()).isEqualTo(409);
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission already exists with name: '" + dto.name() + "'");

        verify(repository, times(1)).insert(mappedEntity);
    }

    @Test
    @DisplayName("Should return bad request when slug check constraint fails")
    void shouldReturnBadRequestWhenSlugCheckConstraintFails() {
        UUID createdBy = UUID.randomUUID();
        CreatePermissionDTO dto = createSampleDto();
        PermissionEntity mappedEntity = new PermissionEntity();

        when(mapper.toEntity(dto)).thenReturn(mappedEntity);
        when(repository.insert(mappedEntity)).thenThrow(
                new DataIntegrityViolationException("violates check constraint \"ck_permissions_slug_not_empty\"")
        );

        Result<PermissionEntity> result = useCase.execute(dto, createdBy);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission slug cannot be empty or blank");

        verify(repository, times(1)).insert(mappedEntity);
    }

    @Test
    @DisplayName("Should return bad request when createdBy foreign key constraint fails")
    void shouldReturnBadRequestWhenCreatedByForeignKeyFails() {
        UUID createdBy = UUID.randomUUID();
        CreatePermissionDTO dto = createSampleDto();
        PermissionEntity mappedEntity = new PermissionEntity();

        when(mapper.toEntity(dto)).thenReturn(mappedEntity);
        when(repository.insert(mappedEntity)).thenThrow(
                new DataIntegrityViolationException("violates foreign key constraint \"fk_permissions_created_by\"")
        );

        Result<PermissionEntity> result = useCase.execute(dto, createdBy);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("User specified in 'createdBy' does not exist");

        verify(repository, times(1)).insert(mappedEntity);
    }

    @Test
    @DisplayName("Should throw InternalServerErrorException on unexpected exception")
    void shouldThrowInternalServerErrorExceptionOnUnexpectedError() {
        UUID createdBy = UUID.randomUUID();
        CreatePermissionDTO dto = createSampleDto();
        PermissionEntity mappedEntity = new PermissionEntity();

        when(mapper.toEntity(dto)).thenReturn(mappedEntity);
        when(repository.insert(mappedEntity)).thenThrow(new RuntimeException("Unexpected DB failure"));

        assertThatThrownBy(() -> useCase.execute(dto, createdBy))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessage("Unexpected DB failure");

        verify(repository, times(1)).insert(mappedEntity);
    }
}