package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.RotationService;
import genius.project.orchestrate.chore.dto.AssignmentResponse;
import genius.project.orchestrate.chore.dto.ParticipantResponse;
import genius.project.orchestrate.chore.dto.RotationScheduleResponse;
import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.repository.ChoreParticipantStore;
import genius.project.orchestrate.chore.internal.repository.ChoreStore;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import genius.project.orchestrate.identity.CurrentUserProvider;
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

    @Mock private ChoreStore choreStore;
    @Mock private ChoreParticipantStore participantStore;
    @Mock private RotationService rotationService;
    @Mock private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ChoreParticipantServiceImpl service;

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID HOUSEHOLD_ID = UUID.randomUUID();
    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();
    private static final UUID ADMIN = UUID.randomUUID();

    @Nested
    @DisplayName("joinChore")
    class Join {

        @Test
        @DisplayName("saves and delegates to rotation")
        void savesAndDelegates() {
            when(currentUserProvider.getUserId()).thenReturn(USER_A);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(false);
            when(participantStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ParticipantResponse result = service.joinChore(CHORE_ID);

            verify(rotationService).addParticipant(CHORE_ID, USER_A);
            assertThat(result.userId()).isEqualTo(USER_A);
            assertThat(result.addedByAdmin()).isFalse();
        }

        @Test
        @DisplayName("duplicate: throws")
        void duplicate() {
            when(currentUserProvider.getUserId()).thenReturn(USER_A);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(true);

            assertThatThrownBy(() -> service.joinChore(CHORE_ID))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode").isEqualTo("ALREADY_PARTICIPANT");

            verify(rotationService, never()).addParticipant(any(), any());
        }
    }

    @Nested
    @DisplayName("addParticipant")
    class AddParticipant {

        @Test
        @DisplayName("admin adds another user; actor resolved from provider")
        void adminAdds() {
            when(currentUserProvider.getUserId()).thenReturn(ADMIN);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(false);
            when(participantStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ParticipantResponse result = service.addParticipant(CHORE_ID, USER_A);

            verify(rotationService).addParticipant(CHORE_ID, USER_A);
            assertThat(result.userId()).isEqualTo(USER_A);
            assertThat(result.addedByAdmin()).isTrue();
        }
    }

    @Nested
    @DisplayName("removeMember")
    class RemoveMember {

        @Test
        @DisplayName("self-leave: actor removes themselves")
        void selfLeave() {
            when(currentUserProvider.getUserId()).thenReturn(USER_A);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(true);

            service.removeMember(CHORE_ID, USER_A);

            verify(participantStore).deleteByChoreIdAndUserId(CHORE_ID, USER_A);
            verify(rotationService).removeParticipant(CHORE_ID, USER_A);
        }

        @Test
        @DisplayName("admin removes another member")
        void adminRemoves() {
            when(currentUserProvider.getUserId()).thenReturn(ADMIN);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(true);

            service.removeMember(CHORE_ID, USER_A);

            verify(participantStore).deleteByChoreIdAndUserId(CHORE_ID, USER_A);
            verify(rotationService).removeParticipant(CHORE_ID, USER_A);
        }

        @Test
        @DisplayName("unknown participant: throws")
        void unknown() {
            when(currentUserProvider.getUserId()).thenReturn(USER_A);
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(false);

            assertThatThrownBy(() -> service.removeMember(CHORE_ID, USER_A))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("isParticipant")
    class IsParticipant {

        @Test
        @DisplayName("true when exists")
        void exists() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(true);

            assertThat(service.isParticipant(CHORE_ID, USER_A)).isTrue();
        }

        @Test
        @DisplayName("false when absent")
        void absent() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(participantStore.existsByChoreIdAndUserId(CHORE_ID, USER_A)).thenReturn(false);

            assertThat(service.isParticipant(CHORE_ID, USER_A)).isFalse();
        }
    }

    @Nested
    @DisplayName("currentCycleNumber")
    class CurrentCycleNumber {

        @Test
        @DisplayName("returns cycle from rotation schedule")
        void returnsCycle() {
            RotationScheduleResponse response = new RotationScheduleResponse(
                    CHORE_ID, List.of(USER_A, USER_B), USER_A, 7, Instant.now());
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(rotationService.getSchedule(CHORE_ID)).thenReturn(Optional.of(response));

            assertThat(service.currentCycleNumber(CHORE_ID)).isEqualTo(7);
        }

        @Test
        @DisplayName("no schedule: throws")
        void noSchedule() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(rotationService.getSchedule(CHORE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.currentCycleNumber(CHORE_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getCurrentAssignment")
    class GetCurrentAssignment {

        @Test
        @DisplayName("returns assignment when non-empty")
        void returnsAssignment() {
            AssignmentResponse assignment = new AssignmentResponse(
                    CHORE_ID, USER_A, 3, Instant.now());
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(rotationService.getCurrentAssignment(CHORE_ID))
                    .thenReturn(Optional.of(assignment));

            Optional<AssignmentResponse> result = service.getCurrentAssignment(CHORE_ID);

            assertThat(result).isPresent();
            assertThat(result.get().currentResponsibleUserId()).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("returns empty when schedule empty")
        void empty() {
            when(choreStore.findById(CHORE_ID)).thenReturn(Optional.of(chore()));
            when(rotationService.getCurrentAssignment(CHORE_ID)).thenReturn(Optional.empty());

            assertThat(service.getCurrentAssignment(CHORE_ID)).isEmpty();
        }
    }

    private Chore chore() {
        return new Chore(CHORE_ID, HOUSEHOLD_ID, "Dishes", null, 7, false, Instant.now());
    }
}