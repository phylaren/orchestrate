package genius.project.orchestrate.chore.internal.service.handler;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import genius.project.orchestrate.chore.internal.service.strategy.PermanentSwapStrategy;
import genius.project.orchestrate.chore.internal.service.strategy.SwapCommand;
import genius.project.orchestrate.chore.internal.service.strategy.SwapOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PermanentSwapStrategyTest {

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();
    private static final UUID USER_C = UUID.randomUUID();

    private final PermanentSwapStrategy strategy = new PermanentSwapStrategy();

    @Test
    @DisplayName("type is PERMANENT")
    void type() {
        assertThat(strategy.getSwapType()).isEqualTo(SwapType.PERMANENT);
    }

    @Test
    @DisplayName("swaps two users, responsible not involved: index unchanged")
    void swapUsers_ResponsibleUnchanged() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B, USER_C), 0, 1);

        SwapOutcome outcome = strategy.execute(current,
                new SwapCommand.Swap(USER_B, USER_C, null));

        RotationSchedule result = ((SwapOutcome.ApplyNow) outcome).schedule();
        assertThat(result.baseOrder()).containsExactly(USER_A, USER_C, USER_B);
        assertThat(result.currentIndex()).isEqualTo(0);
        assertThat(result.currentResponsible()).isEqualTo(USER_A);
    }

    @Test
    @DisplayName("responsible swapped: currentIndex follows responsible")
    void responsibleSwapped_IndexFollows() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B, USER_C), 0, 1);

        SwapOutcome outcome = strategy.execute(current,
                new SwapCommand.Swap(USER_A, USER_C, null));

        RotationSchedule result = ((SwapOutcome.ApplyNow) outcome).schedule();
        assertThat(result.baseOrder()).containsExactly(USER_C, USER_B, USER_A);
        assertThat(result.currentIndex()).isEqualTo(2);
        assertThat(result.currentResponsible()).isEqualTo(USER_A);
    }

    @Test
    @DisplayName("user not in group: returns current unchanged")
    void userNotInGroup_ReturnsCurrent() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B), 0, 1);
        UUID stranger = UUID.randomUUID();

        SwapOutcome outcome = strategy.execute(current,
                new SwapCommand.Swap(USER_A, stranger, null));

        RotationSchedule result = ((SwapOutcome.ApplyNow) outcome).schedule();
        assertThat(result).isSameAs(current);
    }

    private RotationSchedule schedule(List<UUID> order, int index, int cycle) {
        return new RotationSchedule(CHORE_ID, order, index, cycle, Instant.now());
    }
}