package genius.project.orchestrate.chore.internal.service.strategy;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import org.springframework.stereotype.Component;

@Component
public class PermanentSwapStrategy implements SwapStrategy {

    @Override
    public SwapType getSwapType() {
        return SwapType.PERMANENT;
    }

    @Override
    public SwapOutcome execute(RotationSchedule current, SwapCommand command) {
        var swap = (SwapCommand.Swap) command;
        RotationSchedule result =
                SwapTwoUsersOp.apply(current, swap.fromUserId(), swap.toUserId());
        return new SwapOutcome.ApplyNow(result);
    }
}