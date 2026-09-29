package genius.project.orchestrate.swap.internal.persistence;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.swap.SwapRequestStatus;
import genius.project.orchestrate.swap.internal.domain.SwapRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
@Sql(scripts = "/db/test-seed/referenced-aggregates.sql")
class SwapRequestPersistenceTest {

    private static final UUID CHORE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID OTHER_CHORE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final UUID INITIATOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID RECEIVER = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Autowired
    private SwapRequestJpaRepository repository;

    @Autowired
    private JpaSwapRequestRepository adapter;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Nested
    @DisplayName("mapping")
    class Mapping {

        @Test
        @DisplayName("saves a new row and reads every column back")
        void savesAndReadsBack() {
            SwapRequestEntity saved = repository.save(entity(SwapRequestStatus.PENDING, SwapType.TEMPORARY, 5));

            assertThat(saved.getId()).isNotNull();

            entityManager.flush();
            entityManager.clear();

            SwapRequest domain = adapter.findById(saved.getId()).orElseThrow();
            assertThat(domain.choreId()).isEqualTo(CHORE_ID);
            assertThat(domain.initiatorUserId()).isEqualTo(INITIATOR);
            assertThat(domain.receiverUserId()).isEqualTo(RECEIVER);
            assertThat(domain.status()).isEqualTo(SwapRequestStatus.PENDING);
            assertThat(domain.swapType()).isEqualTo(SwapType.TEMPORARY);
            assertThat(domain.cycleNumber()).isEqualTo(5);
            assertThat(domain.createdAt()).isEqualTo(BASE_TIME);
        }

        @Test
        @DisplayName("stores enums as strings, not ordinals")
        void storesEnumsAsStrings() {
            SwapRequestEntity saved = repository.save(entity(SwapRequestStatus.PENDING, SwapType.PERMANENT, null));
            entityManager.flush();

            Object[] row = (Object[]) entityManager
                    .createNativeQuery("select status, swap_type from swap_requests where id = :id")
                    .setParameter("id", saved.getId())
                    .getSingleResult();

            assertThat(row[0]).isEqualTo("PENDING");
            assertThat(row[1]).isEqualTo("PERMANENT");
        }
    }

    @Nested
    @DisplayName("query methods")
    class Queries {

        @Test
        @DisplayName("derived exists... matches the pending pair only")
        void derivedExists() {
            repository.save(entity(SwapRequestStatus.PENDING, SwapType.PERMANENT, null));

            assertThat(repository.existsByChoreIdAndInitiatorUserIdAndReceiverUserIdAndStatus(
                    CHORE_ID, INITIATOR, RECEIVER, SwapRequestStatus.PENDING)).isTrue();
            assertThat(repository.existsByChoreIdAndInitiatorUserIdAndReceiverUserIdAndStatus(
                    CHORE_ID, INITIATOR, RECEIVER, SwapRequestStatus.ACCEPTED)).isFalse();
        }

        @Test
        @DisplayName("derived count... counts per status")
        void derivedCount() {
            repository.save(entity(SwapRequestStatus.PENDING, SwapType.PERMANENT, null));
            repository.save(entity(SwapRequestStatus.PENDING, SwapType.TEMPORARY, 3));
            repository.save(entity(SwapRequestStatus.ACCEPTED, SwapType.PERMANENT, null));
            repository.save(entity(OTHER_CHORE_ID, SwapRequestStatus.PENDING, SwapType.PERMANENT, null));

            assertThat(repository.countByChoreIdAndStatus(CHORE_ID, SwapRequestStatus.PENDING)).isEqualTo(2);
            assertThat(repository.countByChoreIdAndStatus(CHORE_ID, SwapRequestStatus.ACCEPTED)).isEqualTo(1);
            assertThat(repository.countByChoreIdAndStatus(CHORE_ID, SwapRequestStatus.REJECTED)).isZero();
        }

        @Test
        @DisplayName("explicit @Query filters by status and orders by creation time desc")
        void explicitQueryFiltersAndOrders() {
            repository.save(entity(SwapRequestStatus.PENDING, SwapType.PERMANENT, null));
            repository.save(entityAt(SwapRequestStatus.PENDING, SwapType.PERMANENT, null, BASE_TIME.plusHours(1)));
            repository.save(entity(SwapRequestStatus.ACCEPTED, SwapType.PERMANENT, null));

            List<SwapRequestEntity> pending = repository.findByChoreIdAndStatusOrdered(
                    CHORE_ID, SwapRequestStatus.PENDING);

            assertThat(pending).hasSize(2);
            assertThat(pending).extracting(SwapRequestEntity::getCreatedAt)
                    .containsExactly(BASE_TIME.plusHours(1), BASE_TIME);
        }

        @Test
        @DisplayName("explicit @Query finds requests involving the user on either side")
        void explicitQueryInvolvingUser() {
            repository.save(entity(SwapRequestStatus.PENDING, SwapType.PERMANENT, null));
            repository.save(entityAt(SwapRequestStatus.PENDING, SwapType.PERMANENT, null, BASE_TIME.plusHours(2)));
            repository.save(entity(OTHER_CHORE_ID, SwapRequestStatus.PENDING, SwapType.PERMANENT, null));

            assertThat(repository.findInvolvingUser(CHORE_ID, RECEIVER)).hasSize(2);
            assertThat(repository.findInvolvingUser(CHORE_ID, UUID.randomUUID())).isEmpty();
        }

        @Test
        @DisplayName("native aggregation returns the per-status counters through its projection")
        void nativeAggregation() {
            repository.save(entity(SwapRequestStatus.PENDING, SwapType.PERMANENT, null));
            repository.save(entity(SwapRequestStatus.PENDING, SwapType.TEMPORARY, 3));
            repository.save(entity(SwapRequestStatus.REJECTED, SwapType.PERMANENT, null));

            Map<SwapRequestStatus, Long> counts = adapter.countGroupedByStatus(CHORE_ID);

            assertThat(counts)
                    .containsEntry(SwapRequestStatus.PENDING, 2L)
                    .containsEntry(SwapRequestStatus.ACCEPTED, 0L)
                    .containsEntry(SwapRequestStatus.REJECTED, 1L);
        }

        @Test
        @DisplayName("a list read is a single statement: the aggregate has no collections to fetch")
        void singleStatementPerRead() {
            repository.save(entity(SwapRequestStatus.PENDING, SwapType.PERMANENT, null));
            repository.save(entity(SwapRequestStatus.PENDING, SwapType.TEMPORARY, 3));
            entityManager.flush();

            Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
            statistics.clear();

            List<SwapRequestEntity> rows = repository.findByChoreIdOrderByCreatedAtDesc(CHORE_ID);

            assertThat(rows).hasSize(2);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("constraints")
    class Constraints {

        @Test
        @DisplayName("TEMPORARY without cycle_number is rejected by the CHECK constraint")
        void temporaryWithoutCycle() {
            assertThatThrownBy(() -> {
                repository.save(entity(SwapRequestStatus.PENDING, SwapType.TEMPORARY, null));
                entityManager.flush();
            })
                    .isInstanceOf(ConstraintViolationException.class)
                    .hasMessageContaining("chk_swap_requests_cycle_number");
        }

        @Test
        @DisplayName("PERMANENT with cycle_number is rejected by the CHECK constraint")
        void permanentWithCycle() {
            assertThatThrownBy(() -> {
                repository.save(entity(SwapRequestStatus.PENDING, SwapType.PERMANENT, 4));
                entityManager.flush();
            })
                    .isInstanceOf(ConstraintViolationException.class)
                    .hasMessageContaining("chk_swap_requests_cycle_number");
        }

        @Test
        @DisplayName("initiator = receiver is rejected by the CHECK constraint")
        void selfSwap() {
            SwapRequestEntity self = entity(SwapRequestStatus.PENDING, SwapType.PERMANENT, null);
            self.setReceiverUserId(INITIATOR);

            assertThatThrownBy(() -> {
                repository.save(self);
                entityManager.flush();
            })
                    .isInstanceOf(ConstraintViolationException.class)
                    .hasMessageContaining("chk_swap_requests_initiator_receiver");
        }

        @Test
        @DisplayName("an unknown chore id is rejected by the foreign key")
        void unknownChore() {
            assertThatThrownBy(() -> {
                repository.save(entity(UUID.randomUUID(), SwapRequestStatus.PENDING, SwapType.PERMANENT, null));
                entityManager.flush();
            })
                    .isInstanceOf(ConstraintViolationException.class)
                    .hasMessageContaining("fk_swap_requests_chore");
        }
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("update through the adapter changes status and terms only")
        void updateChangesMutableState() {
            SwapRequestEntity saved = repository.save(entity(SwapRequestStatus.PENDING, SwapType.PERMANENT, null));

            SwapRequest updated = adapter.update(new SwapRequest(
                    saved.getId(), CHORE_ID, INITIATOR, RECEIVER,
                    SwapRequestStatus.PENDING, SwapType.TEMPORARY, 7, BASE_TIME.plusDays(30)));

            assertThat(updated.status()).isEqualTo(SwapRequestStatus.PENDING);
            assertThat(updated.swapType()).isEqualTo(SwapType.TEMPORARY);
            assertThat(updated.cycleNumber()).isEqualTo(7);
            assertThat(updated.createdAt()).isEqualTo(BASE_TIME);
            assertThat(updated.initiatorUserId()).isEqualTo(INITIATOR);
        }

        @Test
        @DisplayName("delete removes the row")
        void deleteRemovesRow() {
            SwapRequestEntity saved = repository.save(entity(SwapRequestStatus.PENDING, SwapType.PERMANENT, null));
            entityManager.flush();

            adapter.deleteById(saved.getId());
            entityManager.flush();

            assertThat(repository.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("findByChoreId returns the newest request first")
        void findByChoreIdOrdersDesc() {
            repository.save(entity(SwapRequestStatus.PENDING, SwapType.PERMANENT, null));
            repository.save(entityAt(SwapRequestStatus.REJECTED, SwapType.PERMANENT, null, BASE_TIME.plusDays(1)));

            List<SwapRequest> requests = adapter.findByChoreId(CHORE_ID);

            assertThat(requests).extracting(SwapRequest::createdAt)
                    .containsExactly(BASE_TIME.plusDays(1), BASE_TIME);
        }
    }

    private SwapRequestEntity entity(SwapRequestStatus status, SwapType type, Integer cycle) {
        return entity(CHORE_ID, status, type, cycle);
    }

    private SwapRequestEntity entity(UUID choreId, SwapRequestStatus status, SwapType type, Integer cycle) {
        return entityAt(choreId, status, type, cycle, BASE_TIME);
    }

    private SwapRequestEntity entityAt(SwapRequestStatus status, SwapType type, Integer cycle, LocalDateTime createdAt) {
        return entityAt(CHORE_ID, status, type, cycle, createdAt);
    }

    private SwapRequestEntity entityAt(UUID choreId, SwapRequestStatus status, SwapType type,
                                       Integer cycle, LocalDateTime createdAt) {
        SwapRequestEntity entity = new SwapRequestEntity();
        entity.setChoreId(choreId);
        entity.setInitiatorUserId(INITIATOR);
        entity.setReceiverUserId(RECEIVER);
        entity.setStatus(status);
        entity.setSwapType(type);
        entity.setCycleNumber(cycle);
        entity.setCreatedAt(createdAt);
        return entity;
    }
}
