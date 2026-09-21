package genius.project.orchestrate.chore.internal.repository;

import genius.project.orchestrate.chore.internal.domain.ScheduledSwap;

import java.util.List;
import java.util.UUID;

public interface ScheduledSwapRepository {

    ScheduledSwap save(ScheduledSwap swap);

    List<ScheduledSwap> findByChoreIdAndCycleNumber(UUID choreId, int cycleNumber);

    void deleteExpired(UUID choreId, int currentCycleNumber);
}