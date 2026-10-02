package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.dto.ChoreResponse;
import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreParticipant;
import genius.project.orchestrate.chore.internal.domain.ChoreWithParticipants;
import genius.project.orchestrate.chore.internal.repository.ChoreStore;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import genius.project.orchestrate.identity.CurrentUserProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChoreLifecycleServiceImplTest {

    @Mock
    private ChoreStore choreStore;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ChoreLifecycleServiceImpl service;

    private static final UUID HOUSEHOLD_ID = UUID.randomUUID();
    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("saves chore with correct fields and returns response with needsAttention=true")
    void savesChoreAndReturns() {
        when(currentUserProvider.getUserId()).thenReturn(USER_ID);
        Chore saved = chore(CHORE_ID, HOUSEHOLD_ID);
        when(choreStore.save(any())).thenReturn(saved);

        ChoreResponse result = service.createChore(HOUSEHOLD_ID, "Dishes", "Wash dishes", 3, true);

        ArgumentCaptor<Chore> captor = ArgumentCaptor.forClass(Chore.class);
        verify(choreStore).save(captor.capture());
        assertThat(captor.getValue().householdId()).isEqualTo(HOUSEHOLD_ID);
        assertThat(captor.getValue().name()).isEqualTo("Dishes");
        assertThat(captor.getValue().recurrenceDays()).isEqualTo(3);
        assertThat(captor.getValue().requiresConfirmation()).isTrue();

        assertThat(result.id()).isEqualTo(CHORE_ID);
        assertThat(result.needsAttention()).isTrue();
    }

    @Nested
    @DisplayName("listChores")
    class ListChores {

        @Test
        @DisplayName("delegates the household filter to the store (single fetch query)")
        void delegatesFilterToStore() {
            when(choreStore.findAllWithParticipants(HOUSEHOLD_ID))
                    .thenReturn(List.of(withParticipants(CHORE_ID, HOUSEHOLD_ID, true)));

            List<ChoreResponse> result = service.listChores(HOUSEHOLD_ID);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo(CHORE_ID);
            verify(choreStore).findAllWithParticipants(HOUSEHOLD_ID);
        }

        @Test
        @DisplayName("null householdId returns all chores")
        void nullHouseholdId_ReturnsAll() {
            when(choreStore.findAllWithParticipants(null)).thenReturn(List.of(
                    withParticipants(UUID.randomUUID(), HOUSEHOLD_ID, true),
                    withParticipants(UUID.randomUUID(), UUID.randomUUID(), false)));

            assertThat(service.listChores(null)).hasSize(2);
        }

        @Test
        @DisplayName("needsAttention is true only for chores with an empty rotation group")
        void needsAttention_ReflectsParticipants() {
            UUID withGroup = UUID.randomUUID();
            UUID withoutGroup = UUID.randomUUID();
            when(choreStore.findAllWithParticipants(HOUSEHOLD_ID)).thenReturn(List.of(
                    withParticipants(withGroup, HOUSEHOLD_ID, true),
                    withParticipants(withoutGroup, HOUSEHOLD_ID, false)));

            List<ChoreResponse> result = service.listChores(HOUSEHOLD_ID);

            assertThat(result).filteredOn(r -> r.id().equals(withGroup))
                    .singleElement().extracting(ChoreResponse::needsAttention).isEqualTo(false);
            assertThat(result).filteredOn(r -> r.id().equals(withoutGroup))
                    .singleElement().extracting(ChoreResponse::needsAttention).isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("getChore")
    class GetChore {

        @Test
        @DisplayName("returns response when chore exists")
        void returnsChore_WhenExists() {
            when(choreStore.findByIdWithParticipants(CHORE_ID))
                    .thenReturn(Optional.of(withParticipants(CHORE_ID, HOUSEHOLD_ID, false)));

            ChoreResponse result = service.getChore(CHORE_ID);

            assertThat(result.id()).isEqualTo(CHORE_ID);
            assertThat(result.needsAttention()).isTrue();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsNotFound_WhenAbsent() {
            when(choreStore.findByIdWithParticipants(CHORE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getChore(CHORE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorCode").isEqualTo("CHORE_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("updateChore")
    class UpdateChore {

        @Test
        @DisplayName("replaces editable fields, keeps id, household and createdAt")
        void replacesEditableFields() {
            when(currentUserProvider.getUserId()).thenReturn(USER_ID);
            Chore existing = chore(CHORE_ID, HOUSEHOLD_ID);
            when(choreStore.findByIdWithParticipants(CHORE_ID))
                    .thenReturn(Optional.of(new ChoreWithParticipants(existing, List.of(participant()))));
            when(choreStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ChoreResponse result = service.updateChore(CHORE_ID, "Vacuum", "Living room", 14, false);

            ArgumentCaptor<Chore> captor = ArgumentCaptor.forClass(Chore.class);
            verify(choreStore).save(captor.capture());
            Chore saved = captor.getValue();
            assertThat(saved.id()).isEqualTo(CHORE_ID);
            assertThat(saved.householdId()).isEqualTo(HOUSEHOLD_ID);
            assertThat(saved.createdAt()).isEqualTo(existing.createdAt());
            assertThat(saved.name()).isEqualTo("Vacuum");
            assertThat(saved.description()).isEqualTo("Living room");
            assertThat(saved.recurrenceDays()).isEqualTo(14);
            assertThat(saved.requiresConfirmation()).isFalse();

            assertThat(result.needsAttention()).isFalse();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException and saves nothing when chore is absent")
        void throwsNotFound_WhenAbsent() {
            when(choreStore.findByIdWithParticipants(CHORE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateChore(CHORE_ID, "Vacuum", null, 7, false))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorCode").isEqualTo("CHORE_NOT_FOUND");

            verify(choreStore, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteChore")
    class DeleteChore {

        @Test
        @DisplayName("deletes an existing chore")
        void deletesExisting() {
            when(currentUserProvider.getUserId()).thenReturn(USER_ID);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore(CHORE_ID, HOUSEHOLD_ID)));

            service.deleteChore(CHORE_ID);

            verify(choreStore).deleteById(CHORE_ID);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException and deletes nothing when chore is absent")
        void throwsNotFound_WhenAbsent() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteChore(CHORE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorCode").isEqualTo("CHORE_NOT_FOUND");

            verify(choreStore, never()).deleteById(any());
        }
    }

    private Chore chore(UUID id, UUID householdId) {
        return new Chore(id, householdId, "Dishes", "Wash dishes", 3, true, Instant.now());
    }

    private ChoreParticipant participant() {
        return new ChoreParticipant(CHORE_ID, USER_ID, Instant.now(), false);
    }

    private ChoreWithParticipants withParticipants(UUID choreId, UUID householdId, boolean hasParticipants) {
        List<ChoreParticipant> participants = hasParticipants
                ? List.of(new ChoreParticipant(choreId, USER_ID, Instant.now(), false))
                : List.of();
        return new ChoreWithParticipants(chore(choreId, householdId), participants);
    }
}