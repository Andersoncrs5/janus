package org.janus.modules.reliability.repository;

import io.quarkus.test.junit.QuarkusTest;
import org.janus.help.BaseTest;
import org.janus.modules.reliability.application.dto.inbox.filter.InboxFilterDTO;
import org.janus.modules.reliability.application.dto.inbox.filter.InboxOrder;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.shared.domain.enums.InboxStatusEnum;
import org.janus.shared.domain.page.Page;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class InboxRepositoryJdbcTest extends BaseTest {


    // =========================================================
    // INSERT
    // =========================================================

    @Nested
    class InsertTests {

        @Test
        void shouldInsertInboxWithProvidedId() {

            InboxEntity inbox = createSampleInbox();

            UUID expectedId = inbox.getId();

            InboxEntity saved =
                    inboxRepository.insert(inbox);

            assertThat(saved)
                    .isNotNull();

            assertThat(saved.getId())
                    .isEqualTo(expectedId);

            assertThat(saved.getVersion())
                    .isZero();

            Optional<InboxEntity> fetched =
                    inboxRepository.findByMessageKeyAndConsumerGroup(
                            inbox.getMessageKey(),
                            inbox.getConsumerGroup()
                    );

            assertThat(fetched)
                    .isPresent();

            InboxEntity persisted = fetched.orElseThrow();

            assertThat(persisted.getId())
                    .isEqualTo(expectedId);

            assertThat(persisted.getMessageKey())
                    .isEqualTo(inbox.getMessageKey());

            assertThat(persisted.getConsumerGroup())
                    .isEqualTo(inbox.getConsumerGroup());

            assertThat(persisted.getStatus())
                    .isEqualTo(InboxStatusEnum.PROCESSING);

            assertThat(persisted.getResponsePayload())
                    .isEqualTo(inbox.getResponsePayload());

            assertThat(persisted.getResponseCode())
                    .isEqualTo(200);

            assertThat(persisted.getVersion())
                    .isZero();

            assertThat(persisted.getCreatedAt())
                    .isNotNull();

            assertThat(persisted.getUpdatedAt())
                    .isNotNull();
        }


        @Test
        void shouldGenerateIdWhenIdIsNull() {

            InboxEntity inbox =
                    createSampleInbox();

            inbox.setId(null);

            InboxEntity saved =
                    inboxRepository.insert(inbox);

            assertThat(saved.getId())
                    .isNotNull();

            assertThat(saved.getVersion())
                    .isZero();

            assertThat(
                    inboxRepository.existsByMessageKeyAndConsumerGroup(
                            inbox.getMessageKey(),
                            inbox.getConsumerGroup()
                    )
            ).isTrue();
        }


        @Test
        void shouldFallbackToProcessingWhenStatusIsNull() {

            InboxEntity inbox =
                    createSampleInbox();

            inbox.setStatus(null);

            InboxEntity saved =
                    inboxRepository.insert(inbox);

            Optional<InboxEntity> fetched =
                    inboxRepository.findByMessageKeyAndConsumerGroup(
                            saved.getMessageKey(),
                            saved.getConsumerGroup()
                    );

            assertThat(fetched)
                    .isPresent();

            assertThat(fetched.orElseThrow().getStatus())
                    .isEqualTo(InboxStatusEnum.PROCESSING);
        }


        @Test
        void shouldAllowNullResponsePayload() {

            InboxEntity inbox =
                    createSampleInbox();

            inbox.setResponsePayload(null);

            inboxRepository.insert(inbox);

            InboxEntity persisted =
                    inboxRepository.findById(
                            inbox.getId()
                    ).orElseThrow();

            assertThat(persisted.getResponsePayload())
                    .isNull();
        }


        @Test
        void shouldAllowNullResponseCode() {

            InboxEntity inbox =
                    createSampleInbox();

            inbox.setResponseCode(null);

            inboxRepository.insert(inbox);

            InboxEntity persisted =
                    inboxRepository.findById(
                            inbox.getId()
                    ).orElseThrow();

            assertThat(persisted.getResponseCode())
                    .isNull();
        }


        @Test
        void shouldPersistAllInboxFields() {

            InboxEntity inbox =
                    createSampleInbox();

            inbox.setMessageKey("message-key-test");
            inbox.setConsumerGroup("consumer-group-test");
            inbox.setStatus(InboxStatusEnum.COMPLETED);
            inbox.setResponsePayload("{\"foo\":\"bar\"}");
            inbox.setResponseCode(201);

            inboxRepository.insert(inbox);

            InboxEntity persisted =
                    inboxRepository.findById(
                            inbox.getId()
                    ).orElseThrow();

            assertThat(persisted.getMessageKey())
                    .isEqualTo("message-key-test");

            assertThat(persisted.getConsumerGroup())
                    .isEqualTo("consumer-group-test");

            assertThat(persisted.getStatus())
                    .isEqualTo(InboxStatusEnum.COMPLETED);

            assertThat(persisted.getResponsePayload())
                    .isEqualTo("{\"foo\":\"bar\"}");

            assertThat(persisted.getResponseCode())
                    .isEqualTo(201);
        }
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    @Nested
    class FindByIdTests {

        @Test
        void shouldFindExistingInboxById() {

            InboxEntity inbox =
                    initInbox();

            Optional<InboxEntity> result =
                    inboxRepository.findById(
                            inbox.getId()
                    );

            assertThat(result)
                    .isPresent();

            InboxEntity found =
                    result.orElseThrow();

            assertThat(found.getId())
                    .isEqualTo(inbox.getId());

            assertThat(found.getMessageKey())
                    .isEqualTo(inbox.getMessageKey());

            assertThat(found.getConsumerGroup())
                    .isEqualTo(inbox.getConsumerGroup());
        }


        @Test
        void shouldReturnEmptyWhenIdDoesNotExist() {

            Optional<InboxEntity> result =
                    inboxRepository.findById(
                            UUID.randomUUID()
                    );

            assertThat(result)
                    .isEmpty();
        }


        @Test
        void shouldNotFindSoftDeletedInbox() {

            InboxEntity inbox =
                    initInbox();

            int deleted =
                    inboxRepository.deleteById(
                            inbox.getId()
                    );

            assertThat(deleted)
                    .isEqualTo(1);

            Optional<InboxEntity> result =
                    inboxRepository.findById(
                            inbox.getId()
                    );

            assertThat(result)
                    .isEmpty();
        }
    }


    // =========================================================
    // FIND BY MESSAGE KEY + CONSUMER GROUP
    // =========================================================

    @Nested
    class FindByMessageKeyAndConsumerGroupTests {

        @Test
        void shouldFindInbox() {

            InboxEntity inbox =
                    initInbox();

            Optional<InboxEntity> result =
                    inboxRepository.findByMessageKeyAndConsumerGroup(
                            inbox.getMessageKey(),
                            inbox.getConsumerGroup()
                    );

            assertThat(result)
                    .isPresent();

            assertThat(result.orElseThrow().getId())
                    .isEqualTo(inbox.getId());
        }


        @Test
        void shouldReturnEmptyWhenMessageKeyDoesNotExist() {

            initInbox();

            Optional<InboxEntity> result =
                    inboxRepository.findByMessageKeyAndConsumerGroup(
                            "does-not-exist",
                            "order-processing-group"
                    );

            assertThat(result)
                    .isEmpty();
        }


        @Test
        void shouldReturnEmptyWhenConsumerGroupDoesNotExist() {

            InboxEntity inbox =
                    initInbox();

            Optional<InboxEntity> result =
                    inboxRepository.findByMessageKeyAndConsumerGroup(
                            inbox.getMessageKey(),
                            "does-not-exist"
                    );

            assertThat(result)
                    .isEmpty();
        }


        @Test
        void shouldNotFindSoftDeletedInbox() {

            InboxEntity inbox =
                    initInbox();

            inboxRepository.deleteById(
                    inbox.getId()
            );

            Optional<InboxEntity> result =
                    inboxRepository.findByMessageKeyAndConsumerGroup(
                            inbox.getMessageKey(),
                            inbox.getConsumerGroup()
                    );

            assertThat(result)
                    .isEmpty();
        }
    }


    // =========================================================
    // EXISTS BY ID
    // =========================================================

    @Nested
    class ExistsByIdTests {

        @Test
        void shouldReturnTrueWhenInboxExists() {

            InboxEntity inbox =
                    initInbox();

            assertThat(
                    inboxRepository.existsById(
                            inbox.getId()
                    )
            ).isTrue();
        }


        @Test
        void shouldReturnFalseWhenInboxDoesNotExist() {

            assertThat(
                    inboxRepository.existsById(
                            UUID.randomUUID()
                    )
            ).isFalse();
        }


        @Test
        void shouldReturnFalseForSoftDeletedInbox() {

            InboxEntity inbox =
                    initInbox();

            inboxRepository.deleteById(
                    inbox.getId()
            );

            assertThat(
                    inboxRepository.existsById(
                            inbox.getId()
                    )
            ).isFalse();
        }
    }


    // =========================================================
    // EXISTS BY MESSAGE KEY + CONSUMER GROUP
    // =========================================================

    @Nested
    class ExistsByMessageKeyAndConsumerGroupTests {

        @Test
        void shouldReturnTrueWhenExists() {

            InboxEntity inbox =
                    initInbox();

            assertThat(
                    inboxRepository.existsByMessageKeyAndConsumerGroup(
                            inbox.getMessageKey(),
                            inbox.getConsumerGroup()
                    )
            ).isTrue();
        }


        @Test
        void shouldReturnFalseWhenDoesNotExist() {

            assertThat(
                    inboxRepository.existsByMessageKeyAndConsumerGroup(
                            "unknown",
                            "unknown"
                    )
            ).isFalse();
        }


        @Test
        void shouldReturnFalseWhenSoftDeleted() {

            InboxEntity inbox =
                    initInbox();

            inboxRepository.deleteById(
                    inbox.getId()
            );

            assertThat(
                    inboxRepository.existsByMessageKeyAndConsumerGroup(
                            inbox.getMessageKey(),
                            inbox.getConsumerGroup()
                    )
            ).isFalse();
        }
    }


    // =========================================================
    // EXISTS BY MESSAGE KEY
    // =========================================================

    @Nested
    class ExistsByMessageKeyTests {

        @Test
        void shouldReturnTrueWhenMessageKeyExists() {

            InboxEntity inbox =
                    initInbox();

            assertThat(
                    inboxRepository.existsByMessageKey(
                            inbox.getMessageKey()
                    )
            ).isTrue();
        }


        @Test
        void shouldReturnFalseWhenMessageKeyDoesNotExist() {

            assertThat(
                    inboxRepository.existsByMessageKey(
                            "does-not-exist"
                    )
            ).isFalse();
        }


        @Test
        void shouldReturnFalseForSoftDeletedInbox() {

            InboxEntity inbox =
                    initInbox();

            inboxRepository.deleteById(
                    inbox.getId()
            );

            assertThat(
                    inboxRepository.existsByMessageKey(
                            inbox.getMessageKey()
                    )
            ).isFalse();
        }
    }

    // =========================================================
    // SAVE
    // =========================================================

    @Nested
    class SaveTests {

        @Test
        void shouldInsertWhenIdDoesNotExist() {

            InboxEntity inbox =
                    createSampleInbox();

            InboxEntity result =
                    inboxRepository.save(inbox);

            assertThat(result.getId())
                    .isNotNull();

            assertThat(result.getVersion())
                    .isZero();

            assertThat(
                    inboxRepository.existsById(
                            result.getId()
                    )
            ).isTrue();
        }


        @Test
        void shouldInsertWhenIdIsNull() {

            InboxEntity inbox =
                    createSampleInbox();

            inbox.setId(null);

            InboxEntity result =
                    inboxRepository.save(inbox);

            assertThat(result.getId())
                    .isNotNull();

            assertThat(result.getVersion())
                    .isZero();
        }


        @Test
        void shouldUpdateWhenEntityAlreadyExists() {

            InboxEntity inbox =
                    initInbox();

            assertThat(inbox.getVersion())
                    .isZero();

            inbox.setStatus(
                    InboxStatusEnum.COMPLETED
            );

            inbox.setResponsePayload(
                    "{\"result\":\"ok\"}"
            );

            inbox.setResponseCode(201);

            InboxEntity result =
                    inboxRepository.save(inbox);

            assertThat(result.getVersion())
                    .isEqualTo(0L);

            InboxEntity persisted =
                    inboxRepository.findById(
                            inbox.getId()
                    ).orElseThrow();

            assertThat(persisted.getStatus())
                    .isEqualTo(InboxStatusEnum.COMPLETED);

            assertThat(persisted.getResponsePayload())
                    .isEqualTo("{\"result\":\"ok\"}");

            assertThat(persisted.getResponseCode())
                    .isEqualTo(201);

            assertThat(persisted.getVersion())
                    .isEqualTo(1L);
        }


        @Test
        void shouldThrowWhenOptimisticLockFails() {

            InboxEntity inbox =
                    initInbox();

            inbox.setVersion(999L);

            assertThatThrownBy(
                    () -> inboxRepository.save(inbox)
            )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(
                            "Failed to update Inbox entity"
                    );
        }


    }


    @Nested
    class FindAllTests {

        @Test
        void shouldReturnAllActiveInboxes() {

            InboxEntity first =
                    createSampleInbox();

            InboxEntity second =
                    createSampleInbox();

            inboxRepository.insert(first);
            inboxRepository.insert(second);

            List<InboxEntity> result =
                    inboxRepository.findAll();

            assertThat(result)
                    .hasSize(2);

            assertThat(result)
                    .extracting(InboxEntity::getId)
                    .containsExactlyInAnyOrder(
                            first.getId(),
                            second.getId()
                    );
        }

        @Test
        void shouldNotReturnSoftDeletedInboxes() {

            InboxEntity active =
                    initInbox();

            InboxEntity deleted =
                    initInbox();

            inboxRepository.deleteById(
                    deleted.getId()
            );

            List<InboxEntity> result =
                    inboxRepository.findAll();

            assertThat(result)
                    .extracting(InboxEntity::getId)
                    .contains(active.getId())
                    .doesNotContain(deleted.getId());
        }


        @Test
        void shouldReturnEmptyWhenNoActiveInboxesExist() {

            List<InboxEntity> result =
                    inboxRepository.findAll();

            assertThat(result)
                    .isEmpty();
        }
    }


    // =========================================================
    // PAGINATION + FILTER
    // =========================================================

    @Nested
    class FindAllWithFiltersTests {

        @Test
        void shouldFilterByMessageKey() {

            InboxEntity matching =
                    createSampleInbox();

            matching.setMessageKey(
                    "payment-created-123"
            );

            InboxEntity other =
                    createSampleInbox();

            other.setMessageKey(
                    "user-created-456"
            );

            inboxRepository.insert(matching);
            inboxRepository.insert(other);

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setMessageKey("payment");

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .hasSize(1);

            assertThat(page.getContent().get(0).getId())
                    .isEqualTo(matching.getId());
        }


        @Test
        void shouldFilterMessageKeyCaseInsensitively() {

            InboxEntity inbox =
                    createSampleInbox();

            inbox.setMessageKey(
                    "PAYMENT-CREATED"
            );

            inboxRepository.insert(inbox);

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setMessageKey("payment");

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .hasSize(1);
        }


        @Test
        void shouldFilterByConsumerGroup() {

            InboxEntity matching =
                    createSampleInbox();

            matching.setConsumerGroup(
                    "order-processing"
            );

            InboxEntity other =
                    createSampleInbox();

            other.setConsumerGroup(
                    "email-processing"
            );

            inboxRepository.insert(matching);
            inboxRepository.insert(other);

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setConsumerGroup("order");

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .hasSize(1);

            assertThat(page.getContent().get(0).getId())
                    .isEqualTo(matching.getId());
        }


        @Test
        void shouldFilterByStatus() {

            InboxEntity processing =
                    createSampleInbox();

            processing.setStatus(
                    InboxStatusEnum.PROCESSING
            );

            InboxEntity completed =
                    createSampleInbox();

            completed.setStatus(
                    InboxStatusEnum.COMPLETED
            );

            inboxRepository.insert(processing);
            inboxRepository.insert(completed);

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setStatus(
                    List.of(InboxStatusEnum.COMPLETED)
            );

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .hasSize(1);

            assertThat(page.getContent().get(0).getStatus())
                    .isEqualTo(InboxStatusEnum.COMPLETED);
        }


        @Test
        void shouldFilterByMultipleStatuses() {

            InboxEntity processing =
                    createSampleInbox();

            processing.setStatus(
                    InboxStatusEnum.PROCESSING
            );

            InboxEntity completed =
                    createSampleInbox();

            completed.setStatus(
                    InboxStatusEnum.COMPLETED
            );

            inboxRepository.insert(processing);
            inboxRepository.insert(completed);

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setStatus(
                    List.of(
                            InboxStatusEnum.PROCESSING,
                            InboxStatusEnum.COMPLETED
                    )
            );

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .hasSize(2);

            assertThat(page.getContent())
                    .extracting(InboxEntity::getStatus)
                    .containsExactlyInAnyOrder(
                            InboxStatusEnum.PROCESSING,
                            InboxStatusEnum.COMPLETED
                    );
        }


        @Test
        void shouldFilterByResponsePayload() {

            InboxEntity matching =
                    createSampleInbox();

            matching.setResponsePayload(
                    "{\"order\":\"12345\",\"status\":\"approved\"}"
            );

            InboxEntity other =
                    createSampleInbox();

            other.setResponsePayload(
                    "{\"order\":\"99999\",\"status\":\"failed\"}"
            );

            inboxRepository.insert(matching);
            inboxRepository.insert(other);

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setResponsePayload("APPROVED");

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .hasSize(1);

            assertThat(page.getContent().get(0).getId())
                    .isEqualTo(matching.getId());
        }


        @Test
        void shouldFilterByResponseCodeMin() {

            InboxEntity low =
                    createSampleInbox();

            low.setResponseCode(200);

            InboxEntity high =
                    createSampleInbox();

            high.setResponseCode(500);

            inboxRepository.insert(low);
            inboxRepository.insert(high);

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setResponseCodeMin(400);

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .hasSize(1);

            assertThat(page.getContent().get(0).getResponseCode())
                    .isEqualTo(500);
        }


        @Test
        void shouldFilterByResponseCodeMax() {

            InboxEntity low =
                    createSampleInbox();

            low.setResponseCode(200);

            InboxEntity high =
                    createSampleInbox();

            high.setResponseCode(500);

            inboxRepository.insert(low);
            inboxRepository.insert(high);

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setResponseCodeMax(300);

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .hasSize(1);

            assertThat(page.getContent().get(0).getResponseCode())
                    .isEqualTo(200);
        }


        @Test
        void shouldFilterByResponseCodeRange() {

            InboxEntity low =
                    createSampleInbox();

            low.setResponseCode(200);

            InboxEntity middle =
                    createSampleInbox();

            middle.setResponseCode(300);

            InboxEntity high =
                    createSampleInbox();

            high.setResponseCode(500);

            inboxRepository.insert(low);
            inboxRepository.insert(middle);
            inboxRepository.insert(high);

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setResponseCodeMin(250);
            filter.setResponseCodeMax(400);

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .hasSize(1);

            assertThat(page.getContent().get(0).getResponseCode())
                    .isEqualTo(300);
        }


        @Test
        void shouldSupportPagination() {

            inboxRepository.insert(createSampleInbox());
            inboxRepository.insert(createSampleInbox());
            inboxRepository.insert(createSampleInbox());

            InboxFilterDTO firstPageFilter =
                    new InboxFilterDTO();

            firstPageFilter.setPage(0);
            firstPageFilter.setSize(2);

            Page<InboxEntity> firstPage =
                    inboxRepository.findAll(
                            firstPageFilter
                    );

            assertThat(firstPage.getContent())
                    .hasSize(2);

            assertThat(firstPage.getTotalElements())
                    .isEqualTo(3);


            InboxFilterDTO secondPageFilter =
                    new InboxFilterDTO();

            secondPageFilter.setPage(1);
            secondPageFilter.setSize(2);

            Page<InboxEntity> secondPage =
                    inboxRepository.findAll(
                            secondPageFilter
                    );

            assertThat(secondPage.getContent())
                    .hasSize(1);

            assertThat(secondPage.getTotalElements())
                    .isEqualTo(3);
        }


        @Test
        void shouldReturnEmptyPageWhenOffsetExceedsTotal() {

            inboxRepository.insert(
                    createSampleInbox()
            );

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setPage(10);
            filter.setSize(20);

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .isEmpty();

            assertThat(page.getTotalElements())
                    .isEqualTo(1);
        }


        @Test
        void shouldNotReturnDeletedInboxInPaginatedQuery() {

            InboxEntity active =
                    initInbox();

            InboxEntity deleted =
                    initInbox();

            inboxRepository.deleteById(
                    deleted.getId()
            );

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .extracting(InboxEntity::getId)
                    .contains(active.getId())
                    .doesNotContain(deleted.getId());

            assertThat(page.getTotalElements())
                    .isEqualTo(1);
        }
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Nested
    class DeleteTests {

        @Test
        void shouldSoftDeleteById() {

            InboxEntity inbox =
                    initInbox();

            int affected =
                    inboxRepository.deleteById(
                            inbox.getId()
                    );

            assertThat(affected)
                    .isEqualTo(1);

            assertThat(
                    inboxRepository.findById(
                            inbox.getId()
                    )
            ).isEmpty();

            assertThat(
                    inboxRepository.existsById(
                            inbox.getId()
                    )
            ).isFalse();
        }


        @Test
        void shouldReturnZeroWhenDeletingUnknownId() {

            int affected =
                    inboxRepository.deleteById(
                            UUID.randomUUID()
                    );

            assertThat(affected)
                    .isZero();
        }


        @Test
        void shouldNotDeleteSameEntityTwice() {

            InboxEntity inbox =
                    initInbox();

            int first =
                    inboxRepository.deleteById(
                            inbox.getId()
                    );

            int second =
                    inboxRepository.deleteById(
                            inbox.getId()
                    );

            assertThat(first)
                    .isEqualTo(1);

            assertThat(second)
                    .isZero();
        }


        @Test
        void shouldIncrementVersionOnSoftDelete() {

            InboxEntity inbox =
                    initInbox();

            assertThat(inbox.getVersion())
                    .isZero();

            inboxRepository.deleteById(
                    inbox.getId()
            );

            /*
             * findById não retorna soft deleted.
             * Precisamos verificar via banco ou restore.
             */
            int restored =
                    inboxRepository.restoreById(
                            inbox.getId()
                    );

            assertThat(restored)
                    .isEqualTo(1);

            InboxEntity restoredEntity =
                    inboxRepository.findById(
                            inbox.getId()
                    ).orElseThrow();

            assertThat(restoredEntity.getVersion())
                    .isEqualTo(2L);
        }
    }


    // =========================================================
    // DELETE + OPTIMISTIC LOCK
    // =========================================================

    @Nested
    class OptimisticDeleteTests {

        @Test
        void shouldDeleteWhenVersionMatches() {

            InboxEntity inbox =
                    initInbox();

            int affected =
                    inboxRepository.deleteById(
                            inbox.getId(),
                            0L
                    );

            assertThat(affected)
                    .isEqualTo(1);

            assertThat(
                    inboxRepository.existsById(
                            inbox.getId()
                    )
            ).isFalse();
        }


        @Test
        void shouldReturnZeroWhenVersionDoesNotMatch() {

            InboxEntity inbox =
                    initInbox();

            int affected =
                    inboxRepository.deleteById(
                            inbox.getId(),
                            999L
                    );

            assertThat(affected)
                    .isZero();

            assertThat(
                    inboxRepository.existsById(
                            inbox.getId()
                    )
            ).isTrue();
        }


        @Test
        void shouldReturnZeroWhenEntityIsAlreadyDeleted() {

            InboxEntity inbox =
                    initInbox();

            inboxRepository.deleteById(
                    inbox.getId()
            );

            int affected =
                    inboxRepository.deleteById(
                            inbox.getId(),
                            1L
                    );

            assertThat(affected)
                    .isZero();
        }
    }


    // =========================================================
    // DELETE ALL
    // =========================================================

    @Nested
    class DeleteAllTests {

        @Test
        void shouldSoftDeleteAllActiveInboxes() {

            InboxEntity first =
                    initInbox();

            InboxEntity second =
                    initInbox();

            int affected =
                    inboxRepository.deleteAll();

            assertThat(affected)
                    .isEqualTo(2);

            assertThat(
                    inboxRepository.existsById(first.getId())
            ).isFalse();

            assertThat(
                    inboxRepository.existsById(second.getId())
            ).isFalse();
        }


        @Test
        void shouldReturnZeroWhenThereAreNoActiveInboxes() {

            int affected =
                    inboxRepository.deleteAll();

            assertThat(affected)
                    .isZero();
        }


        @Test
        void shouldNotCountPreviouslyDeletedEntities() {

            InboxEntity deleted =
                    initInbox();

            InboxEntity active =
                    initInbox();

            inboxRepository.deleteById(
                    deleted.getId()
            );

            int affected =
                    inboxRepository.deleteAll();

            assertThat(affected)
                    .isEqualTo(1);

            assertThat(
                    inboxRepository.findById(
                            deleted.getId()
                    )
            ).isEmpty();

            assertThat(
                    inboxRepository.findById(
                            active.getId()
                    )
            ).isEmpty();
        }
    }


    // =========================================================
    // DELETE ALL BY IDS
    // =========================================================

    @Nested
    class DeleteAllByIdTests {

        @Test
        void shouldSoftDeleteMultipleInboxes() {

            InboxEntity first =
                    initInbox();

            InboxEntity second =
                    initInbox();

            InboxEntity third =
                    initInbox();

            int affected =
                    inboxRepository.deleteAllById(
                            List.of(
                                    first.getId(),
                                    second.getId()
                            )
                    );

            assertThat(affected)
                    .isEqualTo(2);

            assertThat(
                    inboxRepository.existsById(
                            first.getId()
                    )
            ).isFalse();

            assertThat(
                    inboxRepository.existsById(
                            second.getId()
                    )
            ).isFalse();

            assertThat(
                    inboxRepository.existsById(
                            third.getId()
                    )
            ).isTrue();
        }


        @Test
        void shouldReturnZeroForNullList() {

            assertThat(
                    inboxRepository.deleteAllById(null)
            ).isZero();
        }


        @Test
        void shouldReturnZeroForEmptyList() {

            assertThat(
                    inboxRepository.deleteAllById(
                            List.of()
                    )
            ).isZero();
        }


        @Test
        void shouldIgnoreUnknownIds() {

            InboxEntity inbox =
                    initInbox();

            int affected =
                    inboxRepository.deleteAllById(
                            List.of(
                                    inbox.getId(),
                                    UUID.randomUUID()
                            )
                    );

            assertThat(affected)
                    .isEqualTo(1);
        }
    }


    // =========================================================
    // RESTORE
    // =========================================================

    @Nested
    class RestoreTests {

        @Test
        void shouldRestoreSoftDeletedInbox() {

            InboxEntity inbox =
                    initInbox();

            inboxRepository.deleteById(
                    inbox.getId()
            );

            assertThat(
                    inboxRepository.findById(
                            inbox.getId()
                    )
            ).isEmpty();

            int restored =
                    inboxRepository.restoreById(
                            inbox.getId()
                    );

            assertThat(restored)
                    .isEqualTo(1);

            Optional<InboxEntity> result =
                    inboxRepository.findById(
                            inbox.getId()
                    );

            assertThat(result)
                    .isPresent();
        }


        @Test
        void shouldReturnZeroWhenRestoringActiveInbox() {

            InboxEntity inbox =
                    initInbox();

            int restored =
                    inboxRepository.restoreById(
                            inbox.getId()
                    );

            assertThat(restored)
                    .isZero();
        }


        @Test
        void shouldReturnZeroWhenRestoringUnknownId() {

            int restored =
                    inboxRepository.restoreById(
                            UUID.randomUUID()
                    );

            assertThat(restored)
                    .isZero();
        }


        @Test
        void shouldRestoreMultipleInboxes() {

            InboxEntity first =
                    initInbox();

            InboxEntity second =
                    initInbox();

            InboxEntity third =
                    initInbox();

            inboxRepository.deleteAllById(
                    List.of(
                            first.getId(),
                            second.getId()
                    )
            );

            int restored =
                    inboxRepository.restoreAllByIds(
                            List.of(
                                    first.getId(),
                                    second.getId(),
                                    third.getId()
                            )
                    );

            assertThat(restored)
                    .isEqualTo(2);

            assertThat(
                    inboxRepository.existsById(
                            first.getId()
                    )
            ).isTrue();

            assertThat(
                    inboxRepository.existsById(
                            second.getId()
                    )
            ).isTrue();

            assertThat(
                    inboxRepository.existsById(
                            third.getId()
                    )
            ).isTrue();
        }


        @Test
        void shouldReturnZeroWhenRestoreListIsNull() {

            assertThat(
                    inboxRepository.restoreAllByIds(null)
            ).isZero();
        }


        @Test
        void shouldReturnZeroWhenRestoreListIsEmpty() {

            assertThat(
                    inboxRepository.restoreAllByIds(
                            List.of()
                    )
            ).isZero();
        }
    }


    // =========================================================
    // FORCE DELETE
    // =========================================================

    @Nested
    class ForceDeleteTests {

        @Test
        void shouldPhysicallyDeleteInbox() {

            InboxEntity inbox =
                    initInbox();

            inboxRepository.deleteForceById(
                    inbox.getId()
            );

            assertThat(
                    inboxRepository.findById(
                            inbox.getId()
                    )
            ).isEmpty();

            /*
             * Restore também deve falhar porque a linha não existe.
             */
            assertThat(
                    inboxRepository.restoreById(
                            inbox.getId()
                    )
            ).isZero();
        }


        @Test
        void shouldReturnZeroWhenForceDeletingUnknownId() {

            int affected =
                    inboxRepository.deleteForceById(
                            UUID.randomUUID()
                    );

            assertThat(affected)
                    .isZero();
        }


        @Test
        void shouldForceDeleteMultipleInboxes() {

            InboxEntity first =
                    initInbox();

            InboxEntity second =
                    initInbox();

            InboxEntity third =
                    initInbox();

            int affected =
                    inboxRepository.deleteAllForceById(
                            List.of(
                                    first.getId(),
                                    second.getId()
                            )
                    );

            assertThat(affected)
                    .isEqualTo(2);

            assertThat(
                    inboxRepository.findById(
                            first.getId()
                    )
            ).isEmpty();

            assertThat(
                    inboxRepository.findById(
                            second.getId()
                    )
            ).isEmpty();

            assertThat(
                    inboxRepository.findById(
                            third.getId()
                    )
            ).isPresent();
        }


        @Test
        void shouldReturnZeroWhenForceDeleteListIsNull() {

            assertThat(
                    inboxRepository.deleteAllForceById(null)
            ).isZero();
        }


        @Test
        void shouldReturnZeroWhenForceDeleteListIsEmpty() {

            assertThat(
                    inboxRepository.deleteAllForceById(
                            List.of()
                    )
            ).isZero();
        }
    }


    // =========================================================
    // BASE FILTER
    // =========================================================

    @Nested
    class BaseFilterTests {

        @Test
        void shouldFilterById() {

            InboxEntity target =
                    initInbox();

            initInbox();

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setId(
                    target.getId()
            );

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .hasSize(1);

            assertThat(
                    page.getContent().get(0).getId()
            ).isEqualTo(target.getId());
        }


        @Test
        void shouldFilterByMinimumVersion() {

            InboxEntity first =
                    initInbox();

            InboxEntity second =
                    initInbox();

            /*
             * Atualiza o segundo para version 1.
             */
            inboxRepository.save(
                    second
            );

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setVersionMin(1L);

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .extracting(InboxEntity::getId)
                    .contains(second.getId())
                    .doesNotContain(first.getId());
        }


        @Test
        void shouldFilterByMaximumVersion() {

            InboxEntity first =
                    initInbox();

            InboxEntity second =
                    initInbox();

            inboxRepository.save(
                    second
            );

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setVersionMax(0L);

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .extracting(InboxEntity::getId)
                    .contains(first.getId())
                    .doesNotContain(second.getId());
        }


        @Test
        void shouldFilterByCreatedAtMin() {

            InboxEntity inbox =
                    initInbox();

            OffsetDateTime now =
                    OffsetDateTime.now();

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setCreatedAtMin(
                    inbox.getCreatedAt().minusSeconds(1)
            );

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .extracting(InboxEntity::getId)
                    .contains(inbox.getId());

            assertThat(now)
                    .isAfter(inbox.getCreatedAt());
        }


        @Test
        void shouldFilterByCreatedAtMax() {

            InboxEntity inbox =
                    initInbox();

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setCreatedAtMax(
                    inbox.getCreatedAt().plusSeconds(1)
            );

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .extracting(InboxEntity::getId)
                    .contains(inbox.getId());
        }


        @Test
        void shouldFilterByUpdatedAtMin() {

            InboxEntity inbox =
                    initInbox();

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setUpdatedAtMin(
                    inbox.getUpdatedAt().minusSeconds(1)
            );

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .extracting(InboxEntity::getId)
                    .contains(inbox.getId());
        }


        @Test
        void shouldFilterByUpdatedAtMax() {

            InboxEntity inbox =
                    initInbox();

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setUpdatedAtMax(
                    inbox.getUpdatedAt().plusSeconds(1)
            );

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .extracting(InboxEntity::getId)
                    .contains(inbox.getId());
        }
    }


    // =========================================================
    // ORDER
    // =========================================================

    @Nested
    class OrderTests {

        @Test
        void shouldUseCreatedAtAsDefaultOrder() {

            InboxEntity first =
                    createSampleInbox();

            InboxEntity second =
                    createSampleInbox();

            inboxRepository.insert(first);
            inboxRepository.insert(second);

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setOrders(
                    List.of(InboxOrder.CREATED_AT)
            );

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .hasSize(2);

            assertThat(
                    page.getContent().get(0).getCreatedAt()
            ).isBeforeOrEqualTo(
                    page.getContent().get(1).getCreatedAt()
            );

        }

        @Test
        void shouldUseCreatedAtWhenOrdersAreEmpty() {

            inboxRepository.insert(
                    createSampleInbox()
            );

            InboxFilterDTO filter =
                    new InboxFilterDTO();

            filter.setOrders(
                    List.of()
            );

            Page<InboxEntity> page =
                    inboxRepository.findAll(filter);

            assertThat(page.getContent())
                    .hasSize(1);
        }
    }
}