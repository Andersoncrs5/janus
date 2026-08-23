package org.janus.modules.reliability.service.inbox;

import org.janus.modules.reliability.application.dto.inbox.request.UpdateInboxDTO;
import org.janus.modules.reliability.application.mapper.InboxMapper;
import org.janus.modules.reliability.application.service.inbox.UpdateInboxUseCase;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateInboxUseCaseTest {

    @Mock
    private InboxRepository repository;

    @Mock
    private InboxMapper mapper;

    @InjectMocks
    private UpdateInboxUseCase useCase;

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessCases {

        @Test
        @DisplayName("Should update Inbox entity successfully")
        void shouldUpdateInboxSuccessfully() {
            // Arrange
            UUID id = UUID.randomUUID();
            UpdateInboxDTO dto = new UpdateInboxDTO(null, null);
            InboxEntity existingEntity = new InboxEntity();
            existingEntity.setId(id);

            InboxEntity updatedEntity = new InboxEntity();
            updatedEntity.setId(id);

            when(repository.findById(id)).thenReturn(Optional.of(existingEntity));
            doNothing().when(mapper).updateEntityFromDto(dto, existingEntity);
            when(repository.save(existingEntity)).thenReturn(updatedEntity);

            // Act
            Result<InboxEntity> result = useCase.execute(id, dto);

            // Assert
            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(updatedEntity, result.getValue());

            verify(repository, times(1)).findById(id);
            verify(mapper, times(1)).updateEntityFromDto(dto, existingEntity);
            verify(repository, times(1)).save(existingEntity);
        }
    }

    @Nested
    @DisplayName("Failure Scenarios and Exceptions")
    class FailureCases {

        @Test
        @DisplayName("Should return Result.notFound when entity does not exist")
        void shouldReturnNotFoundWhenEntityDoesNotExist() {
            // Arrange
            UUID id = UUID.randomUUID();
            UpdateInboxDTO dto = new UpdateInboxDTO(null, null);

            when(repository.findById(id)).thenReturn(Optional.empty());

            // Act
            Result<InboxEntity> result = useCase.execute(id, dto);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals("Inbox not found", result.getFirstError());

            verify(repository, times(1)).findById(id);
            verifyNoInteractions(mapper);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle DataIntegrityViolationException and return constraint handling result")
        void shouldHandleDataIntegrityViolationException() {
            // Arrange
            UUID id = UUID.randomUUID();
            UpdateInboxDTO dto = new UpdateInboxDTO(null, null);
            InboxEntity existingEntity = new InboxEntity();

            when(repository.findById(id)).thenReturn(Optional.of(existingEntity));
            doNothing().when(mapper).updateEntityFromDto(dto, existingEntity);
            when(repository.save(existingEntity))
                    .thenThrow(new DataIntegrityViolationException("uk_inbox_message_key"));

            // Act
            Result<InboxEntity> result = useCase.execute(id, dto);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());

            verify(repository, times(1)).findById(id);
            verify(mapper, times(1)).updateEntityFromDto(dto, existingEntity);
            verify(repository, times(1)).save(existingEntity);
        }

        @Test
        @DisplayName("Should handle DataIntegrityViolationException with null message")
        void shouldHandleDataIntegrityViolationExceptionWithNullMessage() {
            // Arrange
            UUID id = UUID.randomUUID();
            UpdateInboxDTO dto = new UpdateInboxDTO(null, null);
            InboxEntity existingEntity = new InboxEntity();

            when(repository.findById(id)).thenReturn(Optional.of(existingEntity));
            doNothing().when(mapper).updateEntityFromDto(dto, existingEntity);
            when(repository.save(existingEntity))
                    .thenThrow(new DataIntegrityViolationException(null));

            // Act
            Result<InboxEntity> result = useCase.execute(id, dto);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());

            verify(repository, times(1)).findById(id);
            verify(repository, times(1)).save(existingEntity);
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException on unexpected error")
        void shouldThrowInternalServerErrorExceptionOnUnexpectedError() {
            // Arrange
            UUID id = UUID.randomUUID();
            UpdateInboxDTO dto = new UpdateInboxDTO(null, null);
            InboxEntity existingEntity = new InboxEntity();

            when(repository.findById(id)).thenReturn(Optional.of(existingEntity));
            doNothing().when(mapper).updateEntityFromDto(dto, existingEntity);
            when(repository.save(existingEntity))
                    .thenThrow(new RuntimeException("Database connection timeout"));

            // Act & Assert
            InternalServerErrorException exception = assertThrows(
                    InternalServerErrorException.class,
                    () -> useCase.execute(id, dto)
            );

            assertEquals("Database connection timeout", exception.getMessage());

            verify(repository, times(1)).findById(id);
            verify(mapper, times(1)).updateEntityFromDto(dto, existingEntity);
            verify(repository, times(1)).save(existingEntity);
        }
    }
}