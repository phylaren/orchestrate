package genius.project.orchestrate.chore.internal.service.strategy;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.exception.SameUserSwapException;
import genius.project.orchestrate.chore.exception.UserNotInRotationException;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import genius.project.orchestrate.chore.internal.domain.ScheduledSwap;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class TemporarySwapStrategy implements SwapStrategy {

    @Override
    public SwapType getSwapType() {
        return SwapType.TEMPORARY;
    }

    @Override
    public SwapOutcome execute(RotationSchedule current, SwapCommand command) {
        var swap = (SwapCommand.Swap) command;
        validate(current, swap.fromUserId(), swap.toUserId(), swap.cycleNumber());

        ScheduledSwap scheduled = new ScheduledSwap(
                null,
                current.choreId(),
                swap.fromUserId(),
                swap.toUserId(),
                swap.cycleNumber(),
                Instant.now());

        return new SwapOutcome.ScheduleForCycle(scheduled);
    }

    private void validate(RotationSchedule current, UUID from, UUID to, Integer cycleNumber) {
        if (cycleNumber == null) {
            throw new BusinessRuleViolationException(
                    "MISSING_CYCLE_NUMBER",
                    "TEMPORARY swap requires a cycle number.");
        }
        if (from.equals(to)) {
            throw new SameUserSwapException(from);
        }
        if (!current.baseOrder().contains(from)) {
            throw new UserNotInRotationException(from);
        }
        if (!current.baseOrder().contains(to)) {
            throw new UserNotInRotationException(to);
        }
    }
}