package org.janus.modules.authorization.service.permission;

import org.janus.modules.authorization.application.dto.permission.request.UpdatePermissionDTO;
import org.janus.modules.authorization.application.mapper.PermissionMapper;
import org.janus.modules.authorization.application.service.permission.UpdatePermissionUseCase;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdatePermissionUseCaseTest {

    @Mock
    private PermissionRepository repository;

    @Mock
    private PermissionMapper mapper;

    @InjectMocks
    private UpdatePermissionUseCase useCase;

    private UpdatePermissionDTO createSampleDto() {
        return new UpdatePermissionDTO(
                "Updated Permission",
                "users:updated",
                "Updated description",
                PermissionModule.IDENTITY,
                PermissionResource.USER,
                "update",
                PermissionRiskLevel.MEDIUM,
                true,
                false,
                "{\"updated\":true}"
        );
    }

    @Test
    @DisplayName("Should return bad request when DTO is null")
    void shouldReturnBadRequestWhenDtoIsNull() {
        UUID id = UUID.randomUUID();

        Result<PermissionEntity> result = useCase.execute(null, id);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission data must be provided");
        verifyNoInteractions(mapper, repository);
    }

    @Test
    @DisplayName("Should return bad request when ID is null")
    void shouldReturnBadRequestWhenIdIsNull() {
        UpdatePermissionDTO dto = createSampleDto();

        Result<PermissionEntity> result = useCase.execute(dto, null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Id should be defined");
        verifyNoInteractions(mapper, repository);
    }

    @Test
    @DisplayName("Should return not found when permission does not exist")
    void shouldReturnNotFoundWhenPermissionDoesNotExist() {
        UUID id = UUID.randomUUID();
        UpdatePermissionDTO dto = createSampleDto();

        when(repository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        Result<PermissionEntity> result = useCase.execute(dto, id);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission not found");
        verify(repository, times(1)).findByIdForUpdate(id);
        verifyNoInteractions(mapper);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should update permission successfully")
    void shouldUpdatePermissionSuccessfully() {
        UUID id = UUID.randomUUID();
        UpdatePermissionDTO dto = createSampleDto();

        PermissionEntity existingEntity = PermissionEntity.builder()
                .id(id)
                .name("Old Name")
                .slug("users:old")
                .build();

        PermissionEntity savedEntity = PermissionEntity.builder()
                .id(id)
                .name(dto.name())
                .slug(dto.slug())
                .build();

        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(existingEntity));
        doNothing().when(mapper).updateEntityFromDto(dto, existingEntity);
        when(repository.save(existingEntity)).thenReturn(savedEntity);

        Result<PermissionEntity> result = useCase.execute(dto, id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getId()).isEqualTo(id);
        assertThat(result.getData().getName()).isEqualTo("Updated Permission");

        verify(repository, times(1)).findByIdForUpdate(id);
        verify(mapper, times(1)).updateEntityFromDto(dto, existingEntity);
        verify(repository, times(1)).save(existingEntity);
    }

    @Test
    @DisplayName("Should return 409 conflict when updated slug already exists")
    void shouldReturnConflictWhenSlugAlreadyExists() {
        UUID id = UUID.randomUUID();
        UpdatePermissionDTO dto = createSampleDto();
        PermissionEntity existingEntity = PermissionEntity.builder().id(id).build();

        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(existingEntity));
        doNothing().when(mapper).updateEntityFromDto(dto, existingEntity);
        when(repository.save(existingEntity)).thenThrow(
                new DataIntegrityViolationException("duplicate key value violates unique constraint \"uk_permissions_slug\"")
        );

        Result<PermissionEntity> result = useCase.execute(dto, id);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getStatus()).isEqualTo(409);
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission already exists with slug: '" + dto.slug() + "'");
    }

    @Test
    @DisplayName("Should return 409 conflict when updated name already exists")
    void shouldReturnConflictWhenNameAlreadyExists() {
        UUID id = UUID.randomUUID();
        UpdatePermissionDTO dto = createSampleDto();
        PermissionEntity existingEntity = PermissionEntity.builder().id(id).build();

        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(existingEntity));
        doNothing().when(mapper).updateEntityFromDto(dto, existingEntity);
        when(repository.save(existingEntity)).thenThrow(
                new DataIntegrityViolationException("duplicate key value violates unique constraint \"uk_permissions_name\"")
        );

        Result<PermissionEntity> result = useCase.execute(dto, id);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getStatus()).isEqualTo(409);
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission already exists with name: '" + dto.name() + "'");
    }

    @Test
    @DisplayName("Should return bad request when slug check constraint fails on update")
    void shouldReturnBadRequestWhenSlugCheckConstraintFails() {
        UUID id = UUID.randomUUID();
        UpdatePermissionDTO dto = createSampleDto();
        PermissionEntity existingEntity = PermissionEntity.builder().id(id).build();

        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(existingEntity));
        doNothing().when(mapper).updateEntityFromDto(dto, existingEntity);
        when(repository.save(existingEntity)).thenThrow(
                new DataIntegrityViolationException("violates check constraint \"ck_permissions_slug_not_empty\"")
        );

        Result<PermissionEntity> result = useCase.execute(dto, id);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission slug cannot be empty or blank");
    }

    @Test
    @DisplayName("Should throw InternalServerErrorException on unexpected exception during update")
    void shouldThrowInternalServerErrorExceptionOnUnexpectedError() {
        UUID id = UUID.randomUUID();
        UpdatePermissionDTO dto = createSampleDto();
        PermissionEntity existingEntity = PermissionEntity.builder().id(id).build();

        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(existingEntity));
        doNothing().when(mapper).updateEntityFromDto(dto, existingEntity);
        when(repository.save(existingEntity)).thenThrow(new RuntimeException("Database timeout"));

        assertThatThrownBy(() -> useCase.execute(dto, id))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessage("Database timeout");
    }
}