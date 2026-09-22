package genius.project.orchestrate.chore.internal.service.handler;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.exception.SameUserSwapException;
import genius.project.orchestrate.chore.exception.UserNotInRotationException;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import genius.project.orchestrate.chore.internal.service.strategy.SwapCommand;
import genius.project.orchestrate.chore.internal.service.strategy.SwapOutcome;
import genius.project.orchestrate.chore.internal.service.strategy.TemporarySwapStrategy;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemporarySwapStrategyTest {

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();

    private final TemporarySwapStrategy strategy = new TemporarySwapStrategy();

    @Test
    @DisplayName("type is TEMPORARY")
    void type() {
        assertThat(strategy.getSwapType()).isEqualTo(SwapType.TEMPORARY);
    }

    @Test
    @DisplayName("returns ScheduleForCycle, does not touch schedule")
    void returnsScheduleForCycle() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B), 0, 3);

        SwapOutcome outcome = strategy.execute(current,
                new SwapCommand.Swap(USER_A, USER_B, 5));

        assertThat(outcome).isInstanceOf(SwapOutcome.ScheduleForCycle.class);
        var scheduled = ((SwapOutcome.ScheduleForCycle) outcome).scheduled();
        assertThat(scheduled.choreId()).isEqualTo(CHORE_ID);
        assertThat(scheduled.fromUserId()).isEqualTo(USER_A);
        assertThat(scheduled.toUserId()).isEqualTo(USER_B);
        assertThat(scheduled.cycleNumber()).isEqualTo(5);
        assertThat(scheduled.id()).isNull();
    }

    @Test
    @DisplayName("missing cycleNumber: throws BusinessRuleViolationException")
    void missingCycleNumber_Throws() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B), 0, 3);

        assertThatThrownBy(() -> strategy.execute(current,
                new SwapCommand.Swap(USER_A, USER_B, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode").isEqualTo("MISSING_CYCLE_NUMBER");
    }

    @Test
    @DisplayName("same user: throws")
    void sameUser_Throws() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B), 0, 3);

        assertThatThrownBy(() -> strategy.execute(current,
                new SwapCommand.Swap(USER_A, USER_A, 5)))
                .isInstanceOf(SameUserSwapException.class);
    }

    @Test
    @DisplayName("user not in group: throws")
    void userNotInGroup_Throws() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B), 0, 3);
        UUID stranger = UUID.randomUUID();

        assertThatThrownBy(() -> strategy.execute(current,
                new SwapCommand.Swap(USER_A, stranger, 5)))
                .isInstanceOf(UserNotInRotationException.class);
    }

    private RotationSchedule schedule(List<UUID> order, int index, int cycle) {
        return new RotationSchedule(CHORE_ID, order, index, cycle, Instant.now());
    }
}