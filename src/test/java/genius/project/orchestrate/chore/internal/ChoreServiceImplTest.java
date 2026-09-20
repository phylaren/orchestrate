package genius.project.orchestrate.chore.internal;

import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreAssignment;
import genius.project.orchestrate.chore.internal.domain.ChoreCompletion;
import genius.project.orchestrate.chore.internal.domain.ChoreParticipant;
import genius.project.orchestrate.chore.ConfirmationStatus;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.LinkedHashMap;
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
class ChoreServiceImplTest {

    @Mock
    private ChoreRepository repository;

    @InjectMocks
    private ChoreServiceImpl service;

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID HOUSEHOLD_ID = UUID.randomUUID();
    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();
    private static final UUID USER_C = UUID.randomUUID();

    private Chore chore;

    @BeforeEach
    void setUp() {
        chore = new Chore(CHORE_ID, HOUSEHOLD_ID, "Test chore", null, 7, false, Instant.now());
    }

    // -------------------------------------------------------------------------
    // createChore
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createChore")
    class CreateChore {

        @Test
        @DisplayName("saves chore and returns it with correct fields")
        void savesChoreAndReturns() {
            Chore saved = new Chore(UUID.randomUUID(), HOUSEHOLD_ID, "Dishes", "Wash dishes", 3, true, Instant.now());
            when(repository.saveChore(any())).thenReturn(saved);

            Chore result = service.createChore(HOUSEHOLD_ID, "Dishes", "Wash dishes", 3, true);

            ArgumentCaptor<Chore> captor = ArgumentCaptor.forClass(Chore.class);
            verify(repository).saveChore(captor.capture());
            assertThat(captor.getValue().householdId()).isEqualTo(HOUSEHOLD_ID);
            assertThat(captor.getValue().name()).isEqualTo("Dishes");
            assertThat(captor.getValue().description()).isEqualTo("Wash dishes");
            assertThat(captor.getValue().recurrenceDays()).isEqualTo(3);
            assertThat(captor.getValue().requiresConfirmation()).isTrue();
            assertThat(captor.getValue().id()).isNotNull();

            assertThat(result).isEqualTo(saved);
        }
    }

    // -------------------------------------------------------------------------
    // listChores / getChore
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("listChores and getChore")
    class ListChoresAndGetChore {

        @Test
        @DisplayName("listChores filters by householdId and returns sorted by createdAt")
        void listChores_FiltersByHouseholdId() {
            Chore other = new Chore(UUID.randomUUID(), UUID.randomUUID(), "Other", null, 1, false, Instant.now());
            when(repository.findAllChores()).thenReturn(List.of(chore, other));

            List<Chore> result = service.listChores(HOUSEHOLD_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(CHORE_ID);
        }

        @Test
        @DisplayName("listChores with null householdId returns all chores")
        void listChores_NullHouseholdId_ReturnsAll() {
            Chore other = new Chore(UUID.randomUUID(), UUID.randomUUID(), "Other", null, 1, false, Instant.now());
            when(repository.findAllChores()).thenReturn(List.of(chore, other));

            List<Chore> result = service.listChores(null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("getChore returns chore when found")
        void getChore_ReturnsChore() {
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));

            Chore result = service.getChore(CHORE_ID);

            assertThat(result.id()).isEqualTo(CHORE_ID);
        }

        @Test
        @DisplayName("getChore throws ResourceNotFoundException when not found")
        void getChore_NotFound_Throws() {
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getChore(CHORE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorCode").isEqualTo("CHORE_NOT_FOUND");
        }
    }

    // -------------------------------------------------------------------------
    // joinChore
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("joinChore")
    class JoinChore {

        @Test
        @DisplayName("first participant becomes responsible immediately")
        void firstParticipant_BecomesResponsible() {
            LinkedHashMap<UUID, ChoreParticipant> group = new LinkedHashMap<>();
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.participantGroup(CHORE_ID)).thenReturn(group);
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.empty());

            service.joinChore(CHORE_ID, USER_A, false);

            ArgumentCaptor<ChoreAssignment> captor = ArgumentCaptor.forClass(ChoreAssignment.class);
            verify(repository).saveAssignment(captor.capture());
            assertThat(captor.getValue().currentResponsibleUserId()).isEqualTo(USER_A);
            assertThat(captor.getValue().cycleNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("second participant does not change current responsible")
        void secondParticipant_DoesNotChangeResponsible() {
            LinkedHashMap<UUID, ChoreParticipant> group = new LinkedHashMap<>();
            group.put(USER_A, participant(USER_A));
            ChoreAssignment existing = new ChoreAssignment(CHORE_ID, USER_A, 1, Instant.now());
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.participantGroup(CHORE_ID)).thenReturn(group);
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.of(existing));

            service.joinChore(CHORE_ID, USER_B, false);

            verify(repository, never()).saveAssignment(any());
        }

        @Test
        @DisplayName("when assignment has null responsible, new participant becomes responsible")
        void whenAssignmentHasNullResponsible_NewParticipantBecomesResponsible() {
            LinkedHashMap<UUID, ChoreParticipant> group = new LinkedHashMap<>();
            ChoreAssignment nullResponsible = new ChoreAssignment(CHORE_ID, null, 2, Instant.now());
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.participantGroup(CHORE_ID)).thenReturn(group);
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.of(nullResponsible));

            service.joinChore(CHORE_ID, USER_A, false);

            ArgumentCaptor<ChoreAssignment> captor = ArgumentCaptor.forClass(ChoreAssignment.class);
            verify(repository).saveAssignment(captor.capture());
            assertThat(captor.getValue().currentResponsibleUserId()).isEqualTo(USER_A);
            assertThat(captor.getValue().cycleNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("duplicate participant throws BusinessRuleViolationException")
        void duplicateParticipant_ThrowsBusinessRuleViolation() {
            LinkedHashMap<UUID, ChoreParticipant> group = new LinkedHashMap<>();
            group.put(USER_A, participant(USER_A));
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.participantGroup(CHORE_ID)).thenReturn(group);

            assertThatThrownBy(() -> service.joinChore(CHORE_ID, USER_A, false))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining(USER_A.toString());
        }

        @Test
        @DisplayName("unknown chore throws ResourceNotFoundException")
        void unknownChore_ThrowsResourceNotFound() {
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.joinChore(CHORE_ID, USER_A, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // leaveChore
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("leaveChore")
    class LeaveChore {

        @Test
        @DisplayName("responsible leaves, next participant becomes responsible")
        void responsibleLeaves_NextParticipantBecomesResponsible() {
            LinkedHashMap<UUID, ChoreParticipant> group = new LinkedHashMap<>();
            group.put(USER_A, participant(USER_A));
            group.put(USER_B, participant(USER_B));
            ChoreAssignment assignment = new ChoreAssignment(CHORE_ID, USER_A, 1, Instant.now());
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.participantGroup(CHORE_ID)).thenReturn(group);
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.of(assignment));

            service.leaveChore(CHORE_ID, USER_A);

            ArgumentCaptor<ChoreAssignment> captor = ArgumentCaptor.forClass(ChoreAssignment.class);
            verify(repository).saveAssignment(captor.capture());
            assertThat(captor.getValue().currentResponsibleUserId()).isEqualTo(USER_B);
            assertThat(captor.getValue().cycleNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("responsible leaves, group empty, assignment becomes null")
        void responsibleLeaves_GroupEmpty_AssignmentBecomesNull() {
            LinkedHashMap<UUID, ChoreParticipant> group = new LinkedHashMap<>();
            group.put(USER_A, participant(USER_A));
            ChoreAssignment assignment = new ChoreAssignment(CHORE_ID, USER_A, 1, Instant.now());
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.participantGroup(CHORE_ID)).thenReturn(group);
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.of(assignment));

            service.leaveChore(CHORE_ID, USER_A);

            ArgumentCaptor<ChoreAssignment> captor = ArgumentCaptor.forClass(ChoreAssignment.class);
            verify(repository).saveAssignment(captor.capture());
            assertThat(captor.getValue().currentResponsibleUserId()).isNull();
        }

        @Test
        @DisplayName("non-responsible leaves, assignment unchanged")
        void nonResponsibleLeaves_AssignmentUnchanged() {
            LinkedHashMap<UUID, ChoreParticipant> group = new LinkedHashMap<>();
            group.put(USER_A, participant(USER_A));
            group.put(USER_B, participant(USER_B));
            ChoreAssignment assignment = new ChoreAssignment(CHORE_ID, USER_A, 1, Instant.now());
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.participantGroup(CHORE_ID)).thenReturn(group);
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.of(assignment));

            service.leaveChore(CHORE_ID, USER_B);

            verify(repository, never()).saveAssignment(any());
        }

        @Test
        @DisplayName("unknown participant throws ResourceNotFoundException")
        void unknownParticipant_ThrowsResourceNotFound() {
            LinkedHashMap<UUID, ChoreParticipant> group = new LinkedHashMap<>();
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.participantGroup(CHORE_ID)).thenReturn(group);

            assertThatThrownBy(() -> service.leaveChore(CHORE_ID, USER_A))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // markCompleted
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("markCompleted")
    class MarkCompleted {

        @Test
        @DisplayName("chore without confirmation: status NOT_REQUIRED, rotation advances immediately")
        void withoutConfirmation_AdvancesRotationImmediately() {
            Chore noConfirmChore = new Chore(CHORE_ID, HOUSEHOLD_ID, "chore", null, 7, false, Instant.now());
            LinkedHashMap<UUID, ChoreParticipant> group = new LinkedHashMap<>();
            group.put(USER_A, participant(USER_A));
            group.put(USER_B, participant(USER_B));
            ChoreAssignment assignment = new ChoreAssignment(CHORE_ID, USER_A, 1, Instant.now());

            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(noConfirmChore));
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.of(assignment));
            when(repository.saveCompletion(any())).thenAnswer(inv -> inv.getArgument(0));
            when(repository.participantGroup(CHORE_ID)).thenReturn(group);

            ChoreCompletion result = service.markCompleted(CHORE_ID, USER_A);

            assertThat(result.status()).isEqualTo(ConfirmationStatus.NOT_REQUIRED);

            ArgumentCaptor<ChoreAssignment> captor = ArgumentCaptor.forClass(ChoreAssignment.class);
            verify(repository).saveAssignment(captor.capture());
            assertThat(captor.getValue().currentResponsibleUserId()).isEqualTo(USER_B);
            assertThat(captor.getValue().cycleNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("chore with confirmation: status PENDING, no rotation")
        void withConfirmation_StatusPending_NoRotation() {
            Chore confirmChore = new Chore(CHORE_ID, HOUSEHOLD_ID, "chore", null, 7, true, Instant.now());
            ChoreAssignment assignment = new ChoreAssignment(CHORE_ID, USER_A, 1, Instant.now());

            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(confirmChore));
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.of(assignment));
            when(repository.saveCompletion(any())).thenAnswer(inv -> inv.getArgument(0));

            ChoreCompletion result = service.markCompleted(CHORE_ID, USER_A);

            assertThat(result.status()).isEqualTo(ConfirmationStatus.PENDING);
            verify(repository, never()).saveAssignment(any());
        }

        @Test
        @DisplayName("non-responsible user throws BusinessRuleViolationException")
        void nonResponsible_ThrowsBusinessRuleViolation() {
            ChoreAssignment assignment = new ChoreAssignment(CHORE_ID, USER_A, 1, Instant.now());
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.of(assignment));

            assertThatThrownBy(() -> service.markCompleted(CHORE_ID, USER_B))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode").isEqualTo("NOT_CURRENT_RESPONSIBLE");
        }

        @Test
        @DisplayName("no active assignment (empty) throws BusinessRuleViolationException")
        void noAssignment_ThrowsBusinessRuleViolation() {
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.markCompleted(CHORE_ID, USER_A))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode").isEqualTo("NO_ACTIVE_ASSIGNMENT");
        }

        @Test
        @DisplayName("assignment exists but responsible is null throws BusinessRuleViolationException")
        void assignmentWithNullResponsible_ThrowsBusinessRuleViolation() {
            ChoreAssignment nullResponsible = new ChoreAssignment(CHORE_ID, null, 2, Instant.now());
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.of(nullResponsible));

            assertThatThrownBy(() -> service.markCompleted(CHORE_ID, USER_A))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode").isEqualTo("NO_ACTIVE_ASSIGNMENT");
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
        void approved_StatusConfirmed_AdvancesRotation() {
            ChoreCompletion pending = completion(USER_A, ConfirmationStatus.PENDING);
            LinkedHashMap<UUID, ChoreParticipant> group = new LinkedHashMap<>();
            group.put(USER_A, participant(USER_A));
            group.put(USER_B, participant(USER_B));
            ChoreAssignment assignment = new ChoreAssignment(CHORE_ID, USER_A, 1, Instant.now());

            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.findCompletionById(pending.id())).thenReturn(Optional.of(pending));
            when(repository.saveCompletion(any())).thenAnswer(inv -> inv.getArgument(0));
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.of(assignment));
            when(repository.participantGroup(CHORE_ID)).thenReturn(group);

            ChoreCompletion result = service.decideConfirmation(CHORE_ID, pending.id(), USER_B, true);

            assertThat(result.status()).isEqualTo(ConfirmationStatus.CONFIRMED);
            assertThat(result.confirmedByUserId()).isEqualTo(USER_B);

            ArgumentCaptor<ChoreAssignment> captor = ArgumentCaptor.forClass(ChoreAssignment.class);
            verify(repository).saveAssignment(captor.capture());
            assertThat(captor.getValue().currentResponsibleUserId()).isEqualTo(USER_B);
        }

        @Test
        @DisplayName("approved=false: status REJECTED, no rotation")
        void rejected_StatusRejected_NoRotation() {
            ChoreCompletion pending = completion(USER_A, ConfirmationStatus.PENDING);

            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.findCompletionById(pending.id())).thenReturn(Optional.of(pending));
            when(repository.saveCompletion(any())).thenAnswer(inv -> inv.getArgument(0));

            ChoreCompletion result = service.decideConfirmation(CHORE_ID, pending.id(), USER_B, false);

            assertThat(result.status()).isEqualTo(ConfirmationStatus.REJECTED);
            verify(repository, never()).saveAssignment(any());
        }

        @Test
        @DisplayName("self-confirmation throws BusinessRuleViolationException")
        void selfConfirmation_ThrowsBusinessRuleViolation() {
            ChoreCompletion pending = completion(USER_A, ConfirmationStatus.PENDING);

            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.findCompletionById(pending.id())).thenReturn(Optional.of(pending));

            assertThatThrownBy(() -> service.decideConfirmation(CHORE_ID, pending.id(), USER_A, true))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode").isEqualTo("SELF_CONFIRMATION_NOT_ALLOWED");
        }

        @Test
        @DisplayName("already resolved completion throws BusinessRuleViolationException")
        void alreadyResolved_ThrowsBusinessRuleViolation() {
            ChoreCompletion confirmed = completion(USER_A, ConfirmationStatus.CONFIRMED);

            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.findCompletionById(confirmed.id())).thenReturn(Optional.of(confirmed));

            assertThatThrownBy(() -> service.decideConfirmation(CHORE_ID, confirmed.id(), USER_B, true))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode").isEqualTo("COMPLETION_ALREADY_RESOLVED");
        }

        @Test
        @DisplayName("completion belongs to different chore throws ResourceNotFoundException")
        void completionBelongsToDifferentChore_ThrowsNotFound() {
            ChoreCompletion otherChoreCompletion = new ChoreCompletion(
                    UUID.randomUUID(), UUID.randomUUID(), USER_A, Instant.now(),
                    ConfirmationStatus.PENDING, null, null);

            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.findCompletionById(otherChoreCompletion.id()))
                    .thenReturn(Optional.of(otherChoreCompletion));

            assertThatThrownBy(() -> service.decideConfirmation(
                    CHORE_ID, otherChoreCompletion.id(), USER_B, true))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorCode").isEqualTo("COMPLETION_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("getCompletion")
    class GetCompletion {

        @Test
        @DisplayName("returns completion when choreId matches")
        void returnsCompletion_WhenChoreIdMatches() {
            ChoreCompletion completion = completion(USER_A, ConfirmationStatus.NOT_REQUIRED);

            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.findCompletionById(completion.id()))
                    .thenReturn(Optional.of(completion));

            ChoreCompletion result = service.getCompletion(CHORE_ID, completion.id());

            assertThat(result.id()).isEqualTo(completion.id());
            assertThat(result.choreId()).isEqualTo(CHORE_ID);
        }

        @Test
        @DisplayName("completion belongs to different chore throws ResourceNotFoundException")
        void completionBelongsToDifferentChore_ThrowsNotFound() {
            ChoreCompletion otherChoreCompletion = new ChoreCompletion(
                    UUID.randomUUID(), UUID.randomUUID(), USER_A, Instant.now(),
                    ConfirmationStatus.NOT_REQUIRED, null, null);

            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.findCompletionById(otherChoreCompletion.id()))
                    .thenReturn(Optional.of(otherChoreCompletion));

            assertThatThrownBy(() -> service.getCompletion(CHORE_ID, otherChoreCompletion.id()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorCode").isEqualTo("COMPLETION_NOT_FOUND");
        }

        @Test
        @DisplayName("completion not found throws ResourceNotFoundException")
        void completionNotFound_ThrowsNotFound() {
            UUID randomId = UUID.randomUUID();

            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.findCompletionById(randomId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCompletion(CHORE_ID, randomId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorCode").isEqualTo("COMPLETION_NOT_FOUND");
        }
    }

    // -------------------------------------------------------------------------
    // swapTurns
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("swapTurns")
    class SwapTurns {

        @Test
        @DisplayName("fromUser is responsible: assignment updated to toUser")
        void fromUserIsResponsible_AssignmentUpdatedToToUser() {
            LinkedHashMap<UUID, ChoreParticipant> group = new LinkedHashMap<>();
            group.put(USER_A, participant(USER_A));
            group.put(USER_B, participant(USER_B));
            ChoreAssignment assignment = new ChoreAssignment(CHORE_ID, USER_A, 1, Instant.now());

            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.participantGroup(CHORE_ID)).thenReturn(group);
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.of(assignment));

            service.swapTurns(CHORE_ID, USER_A, USER_B);

            verify(repository).swapParticipantOrder(CHORE_ID, USER_A, USER_B);

            ArgumentCaptor<ChoreAssignment> captor = ArgumentCaptor.forClass(ChoreAssignment.class);
            verify(repository).saveAssignment(captor.capture());
            assertThat(captor.getValue().currentResponsibleUserId()).isEqualTo(USER_B);
            assertThat(captor.getValue().cycleNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("fromUser is not responsible: order swapped, assignment unchanged")
        void fromUserIsNotResponsible_OrderSwapped_AssignmentUnchanged() {
            LinkedHashMap<UUID, ChoreParticipant> group = new LinkedHashMap<>();
            group.put(USER_A, participant(USER_A));
            group.put(USER_B, participant(USER_B));
            group.put(USER_C, participant(USER_C));
            ChoreAssignment assignment = new ChoreAssignment(CHORE_ID, USER_C, 1, Instant.now());

            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.participantGroup(CHORE_ID)).thenReturn(group);
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.of(assignment));

            service.swapTurns(CHORE_ID, USER_A, USER_B);

            verify(repository).swapParticipantOrder(CHORE_ID, USER_A, USER_B);
            verify(repository, never()).saveAssignment(any());
        }

        @Test
        @DisplayName("user not in group throws BusinessRuleViolationException")
        void userNotInGroup_ThrowsBusinessRuleViolation() {
            LinkedHashMap<UUID, ChoreParticipant> group = new LinkedHashMap<>();
            group.put(USER_A, participant(USER_A));

            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.participantGroup(CHORE_ID)).thenReturn(group);

            assertThatThrownBy(() -> service.swapTurns(CHORE_ID, USER_A, USER_B))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode").isEqualTo("NOT_IN_SAME_GROUP");
        }
    }

    // -------------------------------------------------------------------------
    // needsAttention
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("needsAttention")
    class NeedsAttention {

        @Test
        @DisplayName("no assignment returns true")
        void noAssignment_ReturnsTrue() {
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.empty());

            assertThat(service.needsAttention(CHORE_ID)).isTrue();
        }

        @Test
        @DisplayName("assignment with null responsible returns true")
        void assignmentWithNullResponsible_ReturnsTrue() {
            ChoreAssignment nullResponsible = new ChoreAssignment(CHORE_ID, null, 2, Instant.now());
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.of(nullResponsible));

            assertThat(service.needsAttention(CHORE_ID)).isTrue();
        }

        @Test
        @DisplayName("active responsible returns false")
        void activeResponsible_ReturnsFalse() {
            ChoreAssignment assignment = new ChoreAssignment(CHORE_ID, USER_A, 1, Instant.now());
            when(repository.findChoreById(CHORE_ID)).thenReturn(Optional.of(chore));
            when(repository.findAssignment(CHORE_ID)).thenReturn(Optional.of(assignment));

            assertThat(service.needsAttention(CHORE_ID)).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private ChoreParticipant participant(UUID userId) {
        return new ChoreParticipant(CHORE_ID, userId, Instant.now(), false);
    }

    private ChoreCompletion completion(UUID completedBy, ConfirmationStatus status) {
        return new ChoreCompletion(UUID.randomUUID(), CHORE_ID, completedBy, Instant.now(), status, null, null);
    }
}