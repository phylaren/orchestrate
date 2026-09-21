package genius.project.orchestrate.chore.internal.service.handler;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import genius.project.orchestrate.chore.internal.service.strategy.InsertSwapStrategy;
import genius.project.orchestrate.chore.internal.service.strategy.SwapCommand;
import genius.project.orchestrate.chore.internal.service.strategy.SwapOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InsertSwapStrategyTest {

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();
    private static final UUID USER_C = UUID.randomUUID();
    private static final UUID USER_D = UUID.randomUUID();

    private final InsertSwapStrategy strategy = new InsertSwapStrategy();

    @Test
    @DisplayName("type is INSERT")
    void type() {
        assertThat(strategy.getSwapType()).isEqualTo(SwapType.INSERT);
    }

    @Test
    @DisplayName("insert user at position, others shift, responsible not moved")
    void insertShiftsOthers_ResponsibleNotMoved() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B, USER_C, USER_D), 0, 1);

        SwapOutcome outcome = strategy.execute(current,
                new SwapCommand.Insert(USER_D, 1));

        RotationSchedule result = ((SwapOutcome.ApplyNow) outcome).schedule();
        assertThat(result.baseOrder()).containsExactly(USER_A, USER_D, USER_B, USER_C);
        assertThat(result.currentIndex()).isEqualTo(0);
        assertThat(result.currentResponsible()).isEqualTo(USER_A);
        assertThat(result.currentCycleNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("responsible moves himself: next becomes responsible, cycle increments")
    void responsibleMoves_NextBecomesResponsible() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B, USER_C, USER_D), 0, 3);

        SwapOutcome outcome = strategy.execute(current,
                new SwapCommand.Insert(USER_A, 3));

        RotationSchedule result = ((SwapOutcome.ApplyNow) outcome).schedule();
        assertThat(result.baseOrder()).containsExactly(USER_B, USER_C, USER_D, USER_A);
        assertThat(result.currentIndex()).isEqualTo(0);
        assertThat(result.currentResponsible()).isEqualTo(USER_B);
        assertThat(result.currentCycleNumber()).isEqualTo(4);
    }

    @Test
    @DisplayName("responsible not involved: currentIndex follows responsible")
    void currentIndexFollowsResponsible() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B, USER_C), 1, 1);

        SwapOutcome outcome = strategy.execute(current,
                new SwapCommand.Insert(USER_C, 0));

        RotationSchedule result = ((SwapOutcome.ApplyNow) outcome).schedule();
        assertThat(result.baseOrder()).containsExactly(USER_C, USER_A, USER_B);
        assertThat(result.currentIndex()).isEqualTo(2);
        assertThat(result.currentResponsible()).isEqualTo(USER_B);
    }

    @Test
    @DisplayName("user not in group: no-op")
    void userNotInGroup_NoOp() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B), 0, 1);
        UUID stranger = UUID.randomUUID();

        SwapOutcome outcome = strategy.execute(current,
                new SwapCommand.Insert(stranger, 0));

        RotationSchedule result = ((SwapOutcome.ApplyNow) outcome).schedule();
        assertThat(result).isSameAs(current);
    }

    @Test
    @DisplayName("position out of bounds: no-op")
    void positionOutOfBounds_NoOp() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B), 0, 1);

        SwapOutcome outcome = strategy.execute(current,
                new SwapCommand.Insert(USER_A, 5));

        RotationSchedule result = ((SwapOutcome.ApplyNow) outcome).schedule();
        assertThat(result).isSameAs(current);
    }

    @Test
    @DisplayName("user already at position: no-op")
    void alreadyAtPosition_NoOp() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B, USER_C), 0, 1);

        SwapOutcome outcome = strategy.execute(current,
                new SwapCommand.Insert(USER_C, 2));

        RotationSchedule result = ((SwapOutcome.ApplyNow) outcome).schedule();
        assertThat(result).isSameAs(current);
    }

    private RotationSchedule schedule(List<UUID> order, int index, int cycle) {
        return new RotationSchedule(CHORE_ID, order, index, cycle, Instant.now());
    }
}