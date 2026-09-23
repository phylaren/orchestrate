package genius.project.orchestrate.chore.internal.service.strategy;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.exception.SameUserSwapException;
import genius.project.orchestrate.chore.exception.UserNotInRotationException;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import genius.project.orchestrate.chore.internal.domain.ScheduledSwap;
import genius.project.orchestrate.common.exception.InvalidCycleNumberException;
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
    public SwapOutcome execute(RotationSchedule current,
                               UUID fromUserId,
                               UUID toUserId,
                               Integer cycleNumber) {
        validate(current, fromUserId, toUserId, cycleNumber);

        ScheduledSwap scheduled = new ScheduledSwap(
                null,
                current.choreId(),
                fromUserId,
                toUserId,
                cycleNumber,
                Instant.now());

        return new SwapOutcome.ScheduleForCycle(scheduled);
    }

    private void validate(RotationSchedule current, UUID from, UUID to, Integer cycleNumber) {
        if (cycleNumber == null) {
            throw InvalidCycleNumberException.missing();
        }
        if (cycleNumber <= current.currentCycleNumber()) {
            throw InvalidCycleNumberException.inPast(cycleNumber, current.currentCycleNumber());
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