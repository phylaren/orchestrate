package genius.project.orchestrate.chore.internal.service.strategy;

import genius.project.orchestrate.chore.SwapType;
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
    public SwapOutcome execute(RotationSchedule current,
                               UUID fromUserId,
                               UUID toUserId,
                               Integer cycleNumber) {
        RotationSchedule result = current.swapParticipants(fromUserId, toUserId);
        return new SwapOutcome.ApplyNow(result);
    }
}