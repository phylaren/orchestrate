package genius.project.orchestrate.chore.internal.service.strategy;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;

import java.util.UUID;

public interface SwapStrategy {

    SwapType getSwapType();

    SwapOutcome execute(RotationSchedule current,
                        UUID fromUserId,
                        UUID toUserId,
                        Integer cycleNumber);
}