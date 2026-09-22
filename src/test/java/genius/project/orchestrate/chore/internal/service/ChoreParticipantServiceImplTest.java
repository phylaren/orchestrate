package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.RotationService;
import genius.project.orchestrate.chore.dto.AssignmentResponse;
import genius.project.orchestrate.chore.dto.ParticipantResponse;
import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreParticipant;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import genius.project.orchestrate.chore.internal.repository.ChoreParticipantStore;
import genius.project.orchestrate.chore.internal.repository.ChoreStore;
import genius.project.orchestrate.chore.internal.repository.RotationRepository;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ChoreParticipantServiceImplTest {

    @Mock
    private ChoreStore choreStore;

    @Mock
    private ChoreParticipantStore participantStore;

    @Mock
    private RotationService rotationService;

    @Mock
    private RotationRepository rotationRepository;

    @InjectMocks
    private ChoreParticipantServiceImpl service;

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID HOUSEHOLD_ID = UUID.randomUUID();
    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // joinChore
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("joinChore")
    class JoinChore {

        @Test
        @DisplayName("saves participant and delegates to rotationService")
        void savesParticipantAndDelegatesToRotation() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(false);
            when(participantStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ParticipantResponse result = service.joinChore(CHORE_ID, USER_A, false);

            verify(participantStore).save(any());
            verify(rotationService).addParticipant(CHORE_ID, USER_A);
            assertThat(result.userId()).isEqualTo(USER_A);
            assertThat(result.addedByAdmin()).isFalse();
        }

        @Test
        @DisplayName("addedByAdmin=true is reflected in response")
        void addedByAdmin_ReflectedInResponse() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(false);
            when(participantStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ParticipantResponse result = service.joinChore(CHORE_ID, USER_A, true);

            assertThat(result.addedByAdmin()).isTrue();
        }

        @Test
        @DisplayName("duplicate participant throws BusinessRuleViolationException")
        void duplicateParticipant_Throws() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(true);

            assertThatThrownBy(() -> service.joinChore(CHORE_ID, USER_A, false))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode").isEqualTo("ALREADY_PARTICIPANT");

            verify(participantStore, never()).save(any());
            verify(rotationService, never()).addParticipant(any(), any());
        }

        @Test
        @DisplayName("unknown chore throws ResourceNotFoundException")
        void unknownChore_Throws() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.empty());

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
        @DisplayName("deletes participant and delegates to rotationService")
        void deletesParticipantAndDelegatesToRotation() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(true);

            service.leaveChore(CHORE_ID, USER_A);

            verify(participantStore).deleteByChoreIdAndUserId(CHORE_ID, USER_A);
            verify(rotationService).removeParticipant(CHORE_ID, USER_A);
        }

        @Test
        @DisplayName("unknown participant throws ResourceNotFoundException")
        void unknownParticipant_Throws() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(false);

            assertThatThrownBy(() -> service.leaveChore(CHORE_ID, USER_A))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(participantStore, never()).deleteByChoreIdAndUserId(any(), any());
            verify(rotationService, never()).removeParticipant(any(), any());
        }
    }

    // -------------------------------------------------------------------------
    // listParticipants
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("listParticipants")
    class ListParticipants {

        @Test
        @DisplayName("returns mapped participants")
        void returnsMappedParticipants() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.findByChoreId(CHORE_ID)).thenReturn(List.of(
                    participant(USER_A, false),
                    participant(USER_B, true)));

            List<ParticipantResponse> result = service.listParticipants(CHORE_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).userId()).isEqualTo(USER_A);
            assertThat(result.get(1).addedByAdmin()).isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // isParticipant
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("isParticipant")
    class IsParticipant {

        @Test
        @DisplayName("returns true when participant exists")
        void returnsTrue_WhenExists() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(true);

            assertThat(service.isParticipant(CHORE_ID, USER_A)).isTrue();
        }

        @Test
        @DisplayName("returns false when participant absent")
        void returnsFalse_WhenAbsent() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(false);

            assertThat(service.isParticipant(CHORE_ID, USER_A)).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // getCurrentAssignment
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getCurrentAssignment")
    class GetCurrentAssignment {

        @Test
        @DisplayName("returns assignment when schedule non-empty")
        void returnsAssignment_WhenNonEmpty() {
            RotationSchedule schedule = new RotationSchedule(
                    CHORE_ID, List.of(USER_A, USER_B), 0, 3, Instant.now());
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));

            Optional<AssignmentResponse> result = service.getCurrentAssignment(CHORE_ID);

            assertThat(result).isPresent();
            assertThat(result.get().currentResponsibleUserId()).isEqualTo(USER_A);
            assertThat(result.get().cycleNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("returns empty when schedule is empty")
        void returnsEmpty_WhenScheduleEmpty() {
            RotationSchedule empty = new RotationSchedule(CHORE_ID, List.of(), 0, 1, Instant.now());
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(empty));

            assertThat(service.getCurrentAssignment(CHORE_ID)).isEmpty();
        }

        @Test
        @DisplayName("returns empty when no schedule")
        void returnsEmpty_WhenNoSchedule() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.empty());

            assertThat(service.getCurrentAssignment(CHORE_ID)).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // swapTurns
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("swapTurns")
    class SwapTurns {

        @Test
        @DisplayName("delegates to rotationService when both users are participants")
        void delegatesToRotationService() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(true);
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_B)).thenReturn(true);

            service.swapTurns(CHORE_ID, USER_A, USER_B);

            verify(rotationService).swapPositions(CHORE_ID, USER_A, USER_B);
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException when a user is not a participant")
        void throwsWhenUserNotParticipant() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(true);
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_B)).thenReturn(false);

            assertThatThrownBy(() -> service.swapTurns(CHORE_ID, USER_A, USER_B))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode").isEqualTo("NOT_IN_SAME_GROUP");

            verify(rotationService, never()).swapPositions(any(), any(), any());
        }
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private Chore chore() {
        return new Chore(CHORE_ID, HOUSEHOLD_ID, "Dishes", null, 7, false, Instant.now());
    }

    private ChoreParticipant participant(UUID userId, boolean addedByAdmin) {
        return new ChoreParticipant(CHORE_ID, userId, Instant.now(), addedByAdmin);
    }
}