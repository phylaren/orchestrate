package genius.project.orchestrate.chore.internal.service.strategy;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;

public interface SwapStrategy {

    SwapType getSwapType();

    SwapOutcome execute(RotationSchedule current, SwapCommand command);
}