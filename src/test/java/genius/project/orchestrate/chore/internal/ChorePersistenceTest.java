package genius.project.orchestrate.chore.internal;

import genius.project.orchestrate.chore.ChoreLifecycleService;
import genius.project.orchestrate.chore.ConfirmationStatus;
import genius.project.orchestrate.chore.dto.ChoreResponse;
import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.persistence.ChoreCompletionEntity;
import genius.project.orchestrate.chore.internal.persistence.ChoreEntity;
import genius.project.orchestrate.chore.internal.persistence.ChoreParticipantEntity;
import genius.project.orchestrate.chore.internal.persistence.ChoreParticipantId;
import genius.project.orchestrate.chore.internal.repository.ChoreStore;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
@Sql(scripts = "/db/test-seed/referenced-aggregates.sql")
class ChorePersistenceTest {

    private static final UUID USER_1 = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID USER_2 = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID HOUSEHOLD = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID SEED_CHORE_1 = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID SEED_CHORE_2 = UUID.fromString("00000000-0000-0000-0000-0000000000c2");

    @Autowired
    private ChoreLifecycleService lifecycleService;

    @Autowired
    private ChoreStore choreStore;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    // -------------------------------------------------------------------------
    // N+1
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("JOIN FETCH / N+1")
    class FetchStrategy {

        @Test
        @DisplayName("listing 5 chores with participants runs exactly one SQL statement")
        void listChoresIsSingleStatement() {
            addParticipant(SEED_CHORE_1, USER_1);
            addParticipant(SEED_CHORE_1, USER_2);
            for (int i = 0; i < 3; i++) {
                UUID id = persistChore("Extra " + i);
                addParticipant(id, USER_1);
            }
            // SEED_CHORE_2 keeps an empty rotation group
            entityManager.flush();
            entityManager.clear();
            statistics.clear();

            List<ChoreResponse> chores = lifecycleService.listChores(HOUSEHOLD);

            assertThat(chores).hasSize(5);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
            assertThat(chores).filteredOn(ChoreResponse::needsAttention)
                    .extracting(ChoreResponse::id).containsExactly(SEED_CHORE_2);
        }

        @Test
        @DisplayName("reading one chore with participants runs exactly one SQL statement")
        void getChoreIsSingleStatement() {
            addParticipant(SEED_CHORE_1, USER_1);
            addParticipant(SEED_CHORE_1, USER_2);
            entityManager.flush();
            entityManager.clear();
            statistics.clear();

            ChoreResponse chore = lifecycleService.getChore(SEED_CHORE_1);

            assertThat(chore.needsAttention()).isFalse();
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        }
    }

    // -------------------------------------------------------------------------
    // cascade / orphanRemoval
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("cascade and orphanRemoval")
    class Cascade {

        @Test
        @DisplayName("deleting a chore removes its participants and completions")
        void deleteCascadesToChildren() {
            addParticipant(SEED_CHORE_1, USER_1);
            addCompletion(SEED_CHORE_1, USER_1);
            entityManager.flush();
            entityManager.clear();

            lifecycleService.deleteChore(SEED_CHORE_1);
            entityManager.flush();
            entityManager.clear();

            assertThat(count("chores", SEED_CHORE_1)).isZero();
            assertThat(count("chore_participants", SEED_CHORE_1)).isZero();
            assertThat(count("chore_completions", SEED_CHORE_1)).isZero();
        }

        @Test
        @DisplayName("removing a participant from the collection deletes its row (orphanRemoval)")
        void orphanRemovalDeletesRow() {
            addParticipant(SEED_CHORE_1, USER_1);
            addParticipant(SEED_CHORE_1, USER_2);
            entityManager.flush();
            entityManager.clear();

            ChoreEntity loaded = entityManager.find(ChoreEntity.class, SEED_CHORE_1);
            loaded.getParticipants().removeIf(p -> p.getId().getUserId().equals(USER_2));
            entityManager.flush();

            assertThat(count("chore_participants", SEED_CHORE_1)).isEqualTo(1);
        }
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("updating a chore changes its fields and keeps participants and completions")
        void updateKeepsChildren() {
            addParticipant(SEED_CHORE_1, USER_1);
            addCompletion(SEED_CHORE_1, USER_1);
            entityManager.flush();
            entityManager.clear();

            ChoreResponse updated = lifecycleService.updateChore(SEED_CHORE_1, "Renamed", "New text", 30, false);
            entityManager.flush();
            entityManager.clear();

            assertThat(updated.name()).isEqualTo("Renamed");
            Chore reloaded = choreStore.findById(SEED_CHORE_1).orElseThrow();
            assertThat(reloaded.name()).isEqualTo("Renamed");
            assertThat(reloaded.description()).isEqualTo("New text");
            assertThat(reloaded.recurrenceDays()).isEqualTo(30);
            assertThat(reloaded.requiresConfirmation()).isFalse();
            assertThat(count("chore_participants", SEED_CHORE_1)).isEqualTo(1);
            assertThat(count("chore_completions", SEED_CHORE_1)).isEqualTo(1);
        }
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private UUID persistChore(String name) {
        UUID id = UUID.randomUUID();
        entityManager.persist(new ChoreEntity(id, HOUSEHOLD, name, null, 7, false, Instant.now()));
        return id;
    }

    private void addParticipant(UUID choreId, UUID userId) {
        ChoreEntity chore = entityManager.getReference(ChoreEntity.class, choreId);
        entityManager.persist(new ChoreParticipantEntity(
                chore, new ChoreParticipantId(choreId, userId), Instant.now(), false));
    }

    private void addCompletion(UUID choreId, UUID userId) {
        ChoreEntity chore = entityManager.getReference(ChoreEntity.class, choreId);
        entityManager.persist(new ChoreCompletionEntity(
                UUID.randomUUID(), chore, userId, Instant.now(), ConfirmationStatus.PENDING, null, null));
    }

    private long count(String table, UUID choreId) {
        String column = table.equals("chores") ? "id" : "chore_id";
        Object result = entityManager
                .createNativeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = :id")
                .setParameter("id", choreId)
                .getSingleResult();
        return ((Number) result).longValue();
    }
}
