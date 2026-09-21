package genius.project.orchestrate.chore.internal.service.strategy;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import genius.project.orchestrate.chore.internal.domain.ScheduledSwap;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TemporarySwapStrategy implements SwapStrategy {

    @Override
    public SwapType getSwapType() {
        return SwapType.TEMPORARY;
    }

    @Override
    public SwapOutcome execute(RotationSchedule current, SwapCommand command) {
        var swap = (SwapCommand.Swap) command;

        if (swap.cycleNumber() == null) {
            throw new BusinessRuleViolationException(
                    "MISSING_CYCLE_NUMBER",
                    "TEMPORARY swap requires a cycle number.");
        }

        ScheduledSwap scheduled = new ScheduledSwap(
                null,
                current.choreId(),
                swap.fromUserId(),
                swap.toUserId(),
                swap.cycleNumber(),
                Instant.now());

        return new SwapOutcome.ScheduleForCycle(scheduled);
    }
}