package genius.project.orchestrate.chore.internal.service.strategy;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.exception.SameUserSwapException;
import genius.project.orchestrate.chore.exception.UserNotInRotationException;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PermanentSwapStrategy implements SwapStrategy {

    @Override
    public SwapType getSwapType() {
        return SwapType.PERMANENT;
    }

    @Override
    public SwapOutcome execute(RotationSchedule current, SwapCommand command) {
        var swap = (SwapCommand.Swap) command;
        validate(current, swap.fromUserId(), swap.toUserId());

        RotationSchedule result =
                SwapTwoUsersOp.apply(current, swap.fromUserId(), swap.toUserId());
        return new SwapOutcome.ApplyNow(result);
    }

    private void validate(RotationSchedule current, UUID from, UUID to) {
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