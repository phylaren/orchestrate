package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.dto.RotationScheduleResponse;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import genius.project.orchestrate.chore.internal.domain.ScheduledSwap;
import genius.project.orchestrate.chore.internal.repository.RotationRepository;
import genius.project.orchestrate.chore.internal.repository.ScheduledSwapRepository;
import genius.project.orchestrate.chore.internal.service.strategy.PermanentSwapStrategy;
import genius.project.orchestrate.chore.internal.service.strategy.SwapStrategy;
import genius.project.orchestrate.chore.internal.service.strategy.TemporarySwapStrategy;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class RotationServiceImplTest {

    @Mock
    private RotationRepository rotationRepository;

    @Mock
    private ScheduledSwapRepository scheduledSwapRepository;

    private RotationServiceImpl service;

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();
    private static final UUID USER_C = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        List<SwapStrategy> strategies = List.of(
                new PermanentSwapStrategy(),
                new TemporarySwapStrategy());
        service = new RotationServiceImpl(rotationRepository, scheduledSwapRepository, strategies);
    }

    @Nested
    @DisplayName("getSchedule")
    class GetSchedule {

        @Test
        @DisplayName("returns mapped response when schedule exists")
        void returnsResponse() {
            RotationSchedule schedule = schedule(List.of(USER_A, USER_B), 0, 1);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));

            Optional<RotationScheduleResponse> result = service.getSchedule(CHORE_ID);

            assertThat(result).isPresent();
            assertThat(result.get().order()).containsExactly(USER_A, USER_B);
            assertThat(result.get().currentResponsibleUserId()).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("returns empty when no schedule")
        void returnsEmpty() {
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.empty());

            assertThat(service.getSchedule(CHORE_ID)).isEmpty();
        }

        @Test
        @DisplayName("does not touch scheduled swaps")
        void doesNotTouchScheduledSwaps() {
            RotationSchedule schedule = schedule(List.of(USER_A), 0, 5);
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.of(schedule));

            service.getSchedule(CHORE_ID);

            verify(scheduledSwapRepository, never()).deleteExpired(any(), any(Integer.class));
            verify(scheduledSwapRepository, never()).findByChoreIdAndCycleNumber(any(), any(Integer.class));
        }
    }

    @Nested
    @DisplayName("currentResponsible")
    class CurrentResponsible {

        @Test
        @DisplayName("returns responsible when non-empty")
        void returnsResponsible() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(USER_A, USER_B), 1, 2)));

            assertThat(service.currentResponsible(CHORE_ID)).contains(USER_B);
        }

        @Test
        @DisplayName("returns empty when schedule empty")
        void emptySchedule() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(), 0, 1)));

            assertThat(service.currentResponsible(CHORE_ID)).isEmpty();
        }

        @Test
        @DisplayName("returns empty when no schedule")
        void noSchedule() {
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.empty());

            assertThat(service.currentResponsible(CHORE_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("isEmpty")
    class IsEmpty {

        @Test
        @DisplayName("true when no schedule")
        void noSchedule() {
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.empty());

            assertThat(service.isEmpty(CHORE_ID)).isTrue();
        }

        @Test
        @DisplayName("true when schedule has no participants")
        void emptySchedule() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(), 0, 1)));

            assertThat(service.isEmpty(CHORE_ID)).isTrue();
        }

        @Test
        @DisplayName("false when schedule has participants")
        void nonEmpty() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(USER_A), 0, 1)));

            assertThat(service.isEmpty(CHORE_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("addParticipant")
    class AddParticipant {

        @Test
        @DisplayName("first participant: creates schedule")
        void firstParticipant() {
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.empty());
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.addParticipant(CHORE_ID, USER_A);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            assertThat(captor.getValue().baseOrder()).containsExactly(USER_A);
            assertThat(captor.getValue().currentResponsible()).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("second participant appended, index unchanged")
        void secondParticipant() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(USER_A), 0, 2)));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.addParticipant(CHORE_ID, USER_B);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            assertThat(captor.getValue().baseOrder()).containsExactly(USER_A, USER_B);
            assertThat(captor.getValue().currentIndex()).isEqualTo(0);
            assertThat(captor.getValue().currentCycleNumber()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("removeParticipant")
    class RemoveParticipant {

        @Test
        @DisplayName("remove before current index: index decremented")
        void beforeCurrent() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(USER_A, USER_B, USER_C), 2, 1)));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.removeParticipant(CHORE_ID, USER_A);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            assertThat(captor.getValue().baseOrder()).containsExactly(USER_B, USER_C);
            assertThat(captor.getValue().currentIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("remove responsible: next becomes responsible, cycle increments")
        void removeResponsible() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(USER_A, USER_B, USER_C), 1, 2)));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.removeParticipant(CHORE_ID, USER_B);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            assertThat(captor.getValue().baseOrder()).containsExactly(USER_A, USER_C);
            assertThat(captor.getValue().currentResponsible()).isEqualTo(USER_C);
            assertThat(captor.getValue().currentCycleNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("remove last participant: empty schedule")
        void lastParticipant() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(USER_A), 0, 1)));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.removeParticipant(CHORE_ID, USER_A);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            assertThat(captor.getValue().baseOrder()).isEmpty();
        }

        @Test
        @DisplayName("no schedule: throws ResourceNotFoundException")
        void noSchedule() {
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeParticipant(CHORE_ID, USER_A))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorCode").isEqualTo("ROTATIONSCHEDULE_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("advance")
    class Advance {

        @Test
        @DisplayName("advances to next, cycle increments, materializes scheduled swaps, purges expired")
        void advances() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(USER_A, USER_B, USER_C), 0, 1)));
            when(scheduledSwapRepository.findByChoreIdAndCycleNumber(CHORE_ID, 2))
                    .thenReturn(List.of());
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.advance(CHORE_ID);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            assertThat(captor.getValue().currentIndex()).isEqualTo(1);
            assertThat(captor.getValue().currentCycleNumber()).isEqualTo(2);

            verify(scheduledSwapRepository).deleteExpired(CHORE_ID, 3);
        }

        @Test
        @DisplayName("wraps around")
        void wraps() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(USER_A, USER_B), 1, 3)));
            when(scheduledSwapRepository.findByChoreIdAndCycleNumber(CHORE_ID, 4))
                    .thenReturn(List.of());
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.advance(CHORE_ID);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            assertThat(captor.getValue().currentIndex()).isEqualTo(0);
        }

        @Test
        @DisplayName("applies scheduled swap for the new cycle")
        void appliesScheduledSwapForNewCycle() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(USER_A, USER_B, USER_C), 0, 1)));
            ScheduledSwap scheduled = new ScheduledSwap(
                    UUID.randomUUID(), CHORE_ID, USER_B, USER_C, 2, Instant.now());
            when(scheduledSwapRepository.findByChoreIdAndCycleNumber(CHORE_ID, 2))
                    .thenReturn(List.of(scheduled));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.advance(CHORE_ID);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            // advanced to index 1 (USER_B), then swap B<->C applied -> [A, C, B], index stays 1
            assertThat(captor.getValue().baseOrder()).containsExactly(USER_A, USER_C, USER_B);
        }

        @Test
        @DisplayName("skips scheduled swap when user not in group")
        void skipsScheduledSwapWhenUserNotInGroup() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(USER_A, USER_B), 0, 1)));
            UUID stranger = UUID.randomUUID();
            ScheduledSwap scheduled = new ScheduledSwap(
                    UUID.randomUUID(), CHORE_ID, USER_A, stranger, 2, Instant.now());
            when(scheduledSwapRepository.findByChoreIdAndCycleNumber(CHORE_ID, 2))
                    .thenReturn(List.of(scheduled));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.advance(CHORE_ID);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            assertThat(captor.getValue().baseOrder()).containsExactly(USER_A, USER_B);
        }

        @Test
        @DisplayName("empty schedule: no save, no scheduled-swap lookup")
        void empty() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(), 0, 1)));

            service.advance(CHORE_ID);

            verify(rotationRepository, never()).save(any());
            verify(scheduledSwapRepository, never()).deleteExpired(any(), any(Integer.class));
        }

        @Test
        @DisplayName("no schedule: throws")
        void noSchedule() {
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.advance(CHORE_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("swap")
    class Swap {

        @Test
        @DisplayName("PERMANENT: mutates and persists rotation")
        void permanent() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(USER_A, USER_B, USER_C), 0, 1)));
            when(rotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.swap(CHORE_ID, USER_B, USER_C, SwapType.PERMANENT, null);

            ArgumentCaptor<RotationSchedule> captor = ArgumentCaptor.forClass(RotationSchedule.class);
            verify(rotationRepository).save(captor.capture());
            assertThat(captor.getValue().baseOrder()).containsExactly(USER_A, USER_C, USER_B);
        }

        @Test
        @DisplayName("TEMPORARY: saves ScheduledSwap, does not touch rotation")
        void temporary() {
            when(rotationRepository.findByChoreId(CHORE_ID))
                    .thenReturn(Optional.of(schedule(List.of(USER_A, USER_B), 0, 3)));
            when(scheduledSwapRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.swap(CHORE_ID, USER_A, USER_B, SwapType.TEMPORARY, 5);

            ArgumentCaptor<ScheduledSwap> captor = ArgumentCaptor.forClass(ScheduledSwap.class);
            verify(scheduledSwapRepository).save(captor.capture());
            verify(rotationRepository, never()).save(any());

            ScheduledSwap saved = captor.getValue();
            assertThat(saved.choreId()).isEqualTo(CHORE_ID);
            assertThat(saved.fromUserId()).isEqualTo(USER_A);
            assertThat(saved.toUserId()).isEqualTo(USER_B);
            assertThat(saved.cycleNumber()).isEqualTo(5);
        }

        @Test
        @DisplayName("no schedule: throws")
        void noSchedule() {
            when(rotationRepository.findByChoreId(CHORE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.swap(
                    CHORE_ID, USER_A, USER_B, SwapType.PERMANENT, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    private RotationSchedule schedule(List<UUID> order, int index, int cycle) {
        return new RotationSchedule(CHORE_ID, order, index, cycle, Instant.now());
    }
}