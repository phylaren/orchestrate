package genius.project.orchestrate.chore.internal.service.strategy;

import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import genius.project.orchestrate.chore.internal.domain.ScheduledSwap;

public sealed interface SwapOutcome {

    record ApplyNow(RotationSchedule schedule) implements SwapOutcome {}

    record ScheduleForCycle(ScheduledSwap scheduled) implements SwapOutcome {}
}