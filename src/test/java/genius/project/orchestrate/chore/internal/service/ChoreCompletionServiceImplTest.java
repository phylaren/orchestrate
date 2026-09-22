package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.ConfirmationStatus;
import genius.project.orchestrate.chore.RotationService;
import genius.project.orchestrate.chore.dto.CompletionResponse;
import genius.project.orchestrate.chore.exception.InvalidConfirmationStatusException;
import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreCompletion;
import genius.project.orchestrate.chore.internal.repository.ChoreCompletionStore;
import genius.project.orchestrate.chore.internal.repository.ChoreStore;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
class ChoreCompletionServiceImplTest {

    @Mock
    private ChoreStore choreStore;

    @Mock
    private ChoreCompletionStore completionStore;

    @Mock
    private RotationService rotationService;

    @InjectMocks
    private ChoreCompletionServiceImpl service;

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID HOUSEHOLD_ID = UUID.randomUUID();
    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // markCompleted
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("markCompleted")
    class MarkCompleted {

        @Test
        @DisplayName("chore without confirmation: status NOT_REQUIRED, rotation advances")
        void withoutConfirmation_AdvancesRotation() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore(false)));
            when(rotationService.currentResponsible(CHORE_ID)).thenReturn(Optional.of(USER_A));
            when(completionStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CompletionResponse result = service.markCompleted(CHORE_ID, USER_A);

            assertThat(result.status()).isEqualTo(ConfirmationStatus.NOT_REQUIRED);
            verify(rotationService).advance(CHORE_ID);
        }

        @Test
        @DisplayName("chore with confirmation: status PENDING, rotation does not advance")
        void withConfirmation_StatusPending_NoRotation() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore(true)));
            when(rotationService.currentResponsible(CHORE_ID)).thenReturn(Optional.of(USER_A));
            when(completionStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CompletionResponse result = service.markCompleted(CHORE_ID, USER_A);

            assertThat(result.status()).isEqualTo(ConfirmationStatus.PENDING);
            verify(rotationService, never()).advance(any());
        }

        @Test
        @DisplayName("non-responsible user throws BusinessRuleViolationException")
        void nonResponsible_Throws() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore(false)));
            when(rotationService.currentResponsible(CHORE_ID)).thenReturn(Optional.of(USER_A));

            assertThatThrownBy(() -> service.markCompleted(CHORE_ID, USER_B))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode").isEqualTo("NOT_CURRENT_RESPONSIBLE");

            verify(completionStore, never()).save(any());
        }

        @Test
        @DisplayName("no active responsible throws BusinessRuleViolationException")
        void noResponsible_Throws() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore(false)));
            when(rotationService.currentResponsible(CHORE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.markCompleted(CHORE_ID, USER_A))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode").isEqualTo("NO_ACTIVE_ASSIGNMENT");

            verify(completionStore, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // getCompletion / listCompletions
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getCompletion")
    class GetCompletion {

        @Test
        @DisplayName("returns completion when choreId matches")
        void returnsCompletion_WhenMatches() {
            ChoreCompletion c = completion(UUID.randomUUID(), USER_A, ConfirmationStatus.NOT_REQUIRED);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore(false)));
            when(completionStore.findById(c.id())).thenReturn(Optional.of(c));

            CompletionResponse result = service.getCompletion(CHORE_ID, c.id());

            assertThat(result.id()).isEqualTo(c.id());
        }

        @Test
        @DisplayName("throws when completion belongs to different chore")
        void throwsWhenDifferentChore() {
            ChoreCompletion other = new ChoreCompletion(
                    UUID.randomUUID(), UUID.randomUUID(), USER_A, Instant.now(),
                    ConfirmationStatus.NOT_REQUIRED, null, null);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore(false)));
            when(completionStore.findById(other.id())).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.getCompletion(CHORE_ID, other.id()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorCode").isEqualTo("COMPLETION_NOT_FOUND");
        }

        @Test
        @DisplayName("listCompletions returns sorted list")
        void listCompletions_ReturnsSorted() {
            Instant t1 = Instant.now().minusSeconds(10);
            Instant t2 = Instant.now();
            ChoreCompletion first = new ChoreCompletion(
                    UUID.randomUUID(), CHORE_ID, USER_A, t1,
                    ConfirmationStatus.NOT_REQUIRED, null, null);
            ChoreCompletion second = new ChoreCompletion(
                    UUID.randomUUID(), CHORE_ID, USER_A, t2,
                    ConfirmationStatus.NOT_REQUIRED, null, null);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore(false)));
            when(completionStore.findByChoreId(CHORE_ID)).thenReturn(List.of(second, first));

            List<CompletionResponse> result = service.listCompletions(CHORE_ID);

            assertThat(result.get(0).completedAt()).isEqualTo(t1);
            assertThat(result.get(1).completedAt()).isEqualTo(t2);
        }
    }

    // -------------------------------------------------------------------------
    // decideConfirmation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("decideConfirmation")
    class DecideConfirmation {

        @Test
        @DisplayName("approved=true: status CONFIRMED, rotation advances")
        void approved_Confirmed_AdvancesRotation() {
            ChoreCompletion pending = completion(UUID.randomUUID(), USER_A, ConfirmationStatus.PENDING);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore(true)));
            when(completionStore.findById(pending.id())).thenReturn(Optional.of(pending));
            when(completionStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CompletionResponse result = service.decideConfirmation(CHORE_ID, pending.id(), USER_B, true);

            assertThat(result.status()).isEqualTo(ConfirmationStatus.CONFIRMED);
            assertThat(result.confirmedByUserId()).isEqualTo(USER_B);
            verify(rotationService).advance(CHORE_ID);
        }

        @Test
        @DisplayName("approved=false: status REJECTED, rotation does not advance")
        void rejected_NoRotation() {
            ChoreCompletion pending = completion(UUID.randomUUID(), USER_A, ConfirmationStatus.PENDING);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore(true)));
            when(completionStore.findById(pending.id())).thenReturn(Optional.of(pending));
            when(completionStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CompletionResponse result = service.decideConfirmation(CHORE_ID, pending.id(), USER_B, false);

            assertThat(result.status()).isEqualTo(ConfirmationStatus.REJECTED);
            verify(rotationService, never()).advance(any());
        }

        @Test
        @DisplayName("self-confirmation throws BusinessRuleViolationException")
        void selfConfirmation_Throws() {
            ChoreCompletion pending = completion(UUID.randomUUID(), USER_A, ConfirmationStatus.PENDING);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore(true)));
            when(completionStore.findById(pending.id())).thenReturn(Optional.of(pending));

            assertThatThrownBy(() -> service.decideConfirmation(CHORE_ID, pending.id(), USER_A, true))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode").isEqualTo("SELF_CONFIRMATION_NOT_ALLOWED");

            verify(completionStore, never()).save(any());
        }

        @ParameterizedTest(name = "{0} + approved={1} -> InvalidConfirmationStatusException")
        @CsvSource({
                "NOT_REQUIRED, true",
                "NOT_REQUIRED, false",
                "CONFIRMED,    true",
                "CONFIRMED,    false",
                "REJECTED,     true",
                "REJECTED,     false"
        })
        @DisplayName("non-PENDING status throws InvalidConfirmationStatusException, nothing saved")
        void nonPending_Throws(ConfirmationStatus current, boolean approved) {
            ChoreCompletion resolved = completion(UUID.randomUUID(), USER_A, current);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore(true)));
            when(completionStore.findById(resolved.id())).thenReturn(Optional.of(resolved));

            assertThatThrownBy(() -> service.decideConfirmation(CHORE_ID, resolved.id(), USER_B, approved))
                    .isInstanceOf(InvalidConfirmationStatusException.class)
                    .extracting("errorCode").isEqualTo("INVALID_CONFIRMATION_STATUS");

            verify(completionStore, never()).save(any());
            verify(rotationService, never()).advance(any());
        }

        @Test
        @DisplayName("transition guard runs before self-confirmation check")
        void guardRunsBeforeSelfConfirmationCheck() {
            ChoreCompletion confirmed = completion(UUID.randomUUID(), USER_A, ConfirmationStatus.CONFIRMED);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore(true)));
            when(completionStore.findById(confirmed.id())).thenReturn(Optional.of(confirmed));

            assertThatThrownBy(() -> service.decideConfirmation(CHORE_ID, confirmed.id(), USER_A, true))
                    .isInstanceOf(InvalidConfirmationStatusException.class);

            verify(completionStore, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private Chore chore(boolean requiresConfirmation) {
        return new Chore(CHORE_ID, HOUSEHOLD_ID, "Dishes", null, 7, requiresConfirmation, Instant.now());
    }

    private ChoreCompletion completion(UUID id, UUID completedBy, ConfirmationStatus status) {
        return new ChoreCompletion(id, CHORE_ID, completedBy, Instant.now(), status, null, null);
    }
}