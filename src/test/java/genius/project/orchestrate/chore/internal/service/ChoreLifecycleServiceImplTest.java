package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.RotationService;
import genius.project.orchestrate.chore.dto.ChoreResponse;
import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.repository.ChoreStore;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChoreLifecycleServiceImplTest {

    @Mock
    private ChoreStore choreStore;

    @Mock
    private RotationService rotationService;

    @InjectMocks
    private ChoreLifecycleServiceImpl service;

    private static final UUID HOUSEHOLD_ID = UUID.randomUUID();
    private static final UUID CHORE_ID = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // createChore
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("saves chore with correct fields and returns response with needsAttention=true")
    void savesChoreAndReturns() {
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

    // -------------------------------------------------------------------------
    // listChores
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("listChores")
    class ListChores {

        @Test
        @DisplayName("filters by householdId")
        void filtersByHouseholdId() {
            Chore match = chore(CHORE_ID, HOUSEHOLD_ID);
            Chore other = chore(UUID.randomUUID(), UUID.randomUUID());
            when(choreStore.findAll()).thenReturn(List.of(match, other));
            when(rotationService.isEmpty(CHORE_ID)).thenReturn(false);

            List<ChoreResponse> result = service.listChores(HOUSEHOLD_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(CHORE_ID);
        }

        @Test
        @DisplayName("null householdId returns all chores")
        void nullHouseholdId_ReturnsAll() {
            Chore a = chore(UUID.randomUUID(), HOUSEHOLD_ID);
            Chore b = chore(UUID.randomUUID(), UUID.randomUUID());
            when(choreStore.findAll()).thenReturn(List.of(a, b));
            when(rotationService.isEmpty(any())).thenReturn(true);

            assertThat(service.listChores(null)).hasSize(2);
        }

        @Test
        @DisplayName("needsAttention reflects rotationService.isEmpty")
        void needsAttention_ReflectsRotationService() {
            Chore c = chore(CHORE_ID, HOUSEHOLD_ID);
            when(choreStore.findAll()).thenReturn(List.of(c));
            when(rotationService.isEmpty(CHORE_ID)).thenReturn(false);

            ChoreResponse result = service.listChores(HOUSEHOLD_ID).get(0);

            assertThat(result.needsAttention()).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // getChore
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getChore")
    class GetChore {

        @Test
        @DisplayName("returns response when chore exists")
        void returnsChore_WhenExists() {
            Chore c = chore(CHORE_ID, HOUSEHOLD_ID);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(c));
            when(rotationService.isEmpty(CHORE_ID)).thenReturn(true);

            ChoreResponse result = service.getChore(CHORE_ID);

            assertThat(result.id()).isEqualTo(CHORE_ID);
            assertThat(result.needsAttention()).isTrue();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsNotFound_WhenAbsent() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getChore(CHORE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorCode").isEqualTo("CHORE_NOT_FOUND");
        }
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private Chore chore(UUID id, UUID householdId) {
        return new Chore(id, householdId, "Dishes", "Wash dishes", 3, true, Instant.now());
    }
}