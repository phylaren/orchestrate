package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.dto.RotationScheduleResponse;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import genius.project.orchestrate.chore.internal.repository.RotationRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RotationServiceImplTest {

    @Mock
    private RotationRepository rotationRepository;

    @InjectMocks
    private RotationServiceImpl service;

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();
    private static final UUID USER_C = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // getSchedule
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getSchedule")
    class GetSchedule {

        @Test
        @DisplayName("returns mapped response when schedule exists")
        void returnsResponse_WhenExists() {
            RotationSchedule schedule = schedule(List.of(USER_A, USER_B), 0, 1);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));

            Optional<RotationScheduleResponse> result = service.getSchedule(CHORE_ID);

            assertThat(result).isPresent();
            assertThat(result.get().choreId()).isEqualTo(CHORE_ID);
            assertThat(result.get().order()).containsExactly(USER_A, USER_B);
            assertThat(result.get().currentResponsibleUserId()).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("returns empty when no schedule")
        void returnsEmpty_WhenAbsent() {
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.empty());

            assertThat(service.getSchedule(CHORE_ID)).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // currentResponsible
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("currentResponsible")
    class CurrentResponsible {

        @Test
        @DisplayName("returns current responsible when schedule non-empty")
        void returnsResponsible_WhenNonEmpty() {
            RotationSchedule schedule = schedule(List.of(USER_A, USER_B), 1, 2);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));

            assertThat(service.currentResponsible(CHORE_ID)).contains(USER_B);
        }

        @Test
        @DisplayName("returns empty when schedule is empty")
        void returnsEmpty_WhenScheduleEmpty() {
            RotationSchedule schedule = schedule(List.of(), 0, 1);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));

            assertThat(service.currentResponsible(CHORE_ID)).isEmpty();
        }

        @Test
        @DisplayName("returns empty when no schedule")
        void returnsEmpty_WhenNoSchedule() {
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.empty());

            assertThat(service.currentResponsible(CHORE_ID)).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // isEmpty
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("isEmpty")
    class IsEmpty {

        @Test
        @DisplayName("returns true when no schedule")
        void returnsTrue_WhenNoSchedule() {
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.empty());

            assertThat(service.isEmpty(CHORE_ID)).isTrue();
        }

        @Test
        @DisplayName("returns true when schedule has no participants")
        void returnsTrue_WhenScheduleEmpty() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(), 0, 1)));

            assertThat(service.isEmpty(CHORE_ID)).isTrue();
        }

        @Test
        @DisplayName("returns false when schedule has participants")
        void returnsFalse_WhenScheduleNonEmpty() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(USER_A), 0, 1)));

            assertThat(service.isEmpty(CHORE_ID)).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // addParticipant
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("addParticipant")
    class AddParticipant {

        @Test
        @DisplayName("first participant: creates schedule with index 0, increments cycle")
        void firstParticipant_CreatesSchedule() {
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.empty());
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.addParticipant(CHORE_ID, USER_A);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            RotationSchedule saved = captor.getValue();
            assertThat(saved.baseOrder()).containsExactly(USER_A);
            assertThat(saved.currentIndex()).isEqualTo(0);
            assertThat(saved.currentResponsible()).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("second participant: appended to end, index unchanged, cycle unchanged")
        void secondParticipant_AppendedToEnd_IndexUnchanged() {
            RotationSchedule existing = schedule(List.of(USER_A), 0, 2);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(existing));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.addParticipant(CHORE_ID, USER_B);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            RotationSchedule saved = captor.getValue();
            assertThat(saved.baseOrder()).containsExactly(USER_A, USER_B);
            assertThat(saved.currentIndex()).isEqualTo(0);
            assertThat(saved.currentCycleNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("participant added to empty schedule: becomes responsible, cycle increments")
        void participantAddedToEmptySchedule_BecomesResponsible() {
            RotationSchedule empty = schedule(List.of(), 0, 3);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(empty));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.addParticipant(CHORE_ID, USER_A);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            RotationSchedule saved = captor.getValue();
            assertThat(saved.currentIndex()).isEqualTo(0);
            assertThat(saved.currentResponsible()).isEqualTo(USER_A);
            assertThat(saved.currentCycleNumber()).isEqualTo(4);
        }
    }

    // -------------------------------------------------------------------------
    // removeParticipant
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("removeParticipant")
    class RemoveParticipant {

        @Test
        @DisplayName("remove non-responsible before current index: index decremented")
        void removeBeforeCurrentIndex_IndexDecremented() {
            RotationSchedule schedule = schedule(List.of(USER_A, USER_B, USER_C), 2, 1);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.removeParticipant(CHORE_ID, USER_A);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            RotationSchedule saved = captor.getValue();
            assertThat(saved.baseOrder()).containsExactly(USER_B, USER_C);
            assertThat(saved.currentIndex()).isEqualTo(1);
            assertThat(saved.currentCycleNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("remove non-responsible after current index: index unchanged")
        void removeAfterCurrentIndex_IndexUnchanged() {
            RotationSchedule schedule = schedule(List.of(USER_A, USER_B, USER_C), 0, 1);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.removeParticipant(CHORE_ID, USER_C);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            RotationSchedule saved = captor.getValue();
            assertThat(saved.baseOrder()).containsExactly(USER_A, USER_B);
            assertThat(saved.currentIndex()).isEqualTo(0);
        }

        @Test
        @DisplayName("remove responsible: next participant becomes responsible, cycle increments")
        void removeResponsible_NextBecomesResponsible() {
            RotationSchedule schedule = schedule(List.of(USER_A, USER_B, USER_C), 1, 2);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.removeParticipant(CHORE_ID, USER_B);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            RotationSchedule saved = captor.getValue();
            assertThat(saved.baseOrder()).containsExactly(USER_A, USER_C);
            assertThat(saved.currentIndex()).isEqualTo(1);
            assertThat(saved.currentResponsible()).isEqualTo(USER_C);
            assertThat(saved.currentCycleNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("remove responsible who is last in list: wraps to index 0, cycle increments")
        void removeResponsibleAtEnd_WrapsToZero() {
            RotationSchedule schedule = schedule(List.of(USER_A, USER_B), 1, 1);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.removeParticipant(CHORE_ID, USER_B);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            RotationSchedule saved = captor.getValue();
            assertThat(saved.baseOrder()).containsExactly(USER_A);
            assertThat(saved.currentIndex()).isEqualTo(0);
            assertThat(saved.currentResponsible()).isEqualTo(USER_A);
            assertThat(saved.currentCycleNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("remove last participant: schedule becomes empty")
        void removeLastParticipant_ScheduleBecomesEmpty() {
            RotationSchedule schedule = schedule(List.of(USER_A), 0, 1);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.removeParticipant(CHORE_ID, USER_A);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            RotationSchedule saved = captor.getValue();
            assertThat(saved.baseOrder()).isEmpty();
            assertThat(saved.currentResponsible()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // advance
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("advance")
    class Advance {

        @Test
        @DisplayName("advances to next participant, cycle increments")
        void advancesToNext() {
            RotationSchedule schedule = schedule(List.of(USER_A, USER_B, USER_C), 0, 1);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.advance(CHORE_ID);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            RotationSchedule saved = captor.getValue();
            assertThat(saved.currentIndex()).isEqualTo(1);
            assertThat(saved.currentResponsible()).isEqualTo(USER_B);
            assertThat(saved.currentCycleNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("wraps around from last to first participant")
        void wrapsAroundToFirst() {
            RotationSchedule schedule = schedule(List.of(USER_A, USER_B), 1, 3);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.advance(CHORE_ID);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            RotationSchedule saved = captor.getValue();
            assertThat(saved.currentIndex()).isEqualTo(0);
            assertThat(saved.currentResponsible()).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("empty schedule: no change to order")
        void emptySchedule_NoChange() {
            RotationSchedule schedule = schedule(List.of(), 0, 1);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));

            service.advance(CHORE_ID);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository, org.mockito.Mockito.never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // swapPositions
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("swapPositions")
    class SwapPositions {

        @Test
        @DisplayName("swaps order of two participants, responsible not involved: index unchanged")
        void swapsOrder_ResponsibleUnchanged() {
            RotationSchedule schedule = schedule(List.of(USER_A, USER_B, USER_C), 0, 1);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.swapPositions(CHORE_ID, USER_B, USER_C);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            RotationSchedule saved = captor.getValue();
            assertThat(saved.baseOrder()).containsExactly(USER_A, USER_C, USER_B);
            assertThat(saved.currentIndex()).isEqualTo(0);
            assertThat(saved.currentResponsible()).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("responsible swapped: currentIndex follows responsible")
        void responsibleSwapped_IndexFollows() {
            RotationSchedule schedule = schedule(List.of(USER_A, USER_B, USER_C), 0, 1);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.swapPositions(CHORE_ID, USER_A, USER_C);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            RotationSchedule saved = captor.getValue();
            assertThat(saved.baseOrder()).containsExactly(USER_C, USER_B, USER_A);
            assertThat(saved.currentIndex()).isEqualTo(2);
            assertThat(saved.currentResponsible()).isEqualTo(USER_A);
        }
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private RotationSchedule schedule(List<UUID> order, int currentIndex, int cycleNumber) {
        return new RotationSchedule(CHORE_ID, order, currentIndex, cycleNumber, Instant.now());
    }
}