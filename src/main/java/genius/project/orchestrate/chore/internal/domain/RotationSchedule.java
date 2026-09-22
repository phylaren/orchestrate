package genius.project.orchestrate.chore.internal.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RotationSchedule(
        UUID choreId,
        List<UUID> baseOrder,
        int currentIndex,
        int currentCycleNumber,
        Instant cycleStartedAt
) {
    public boolean isEmpty() {
        return baseOrder.isEmpty();
    }

    public UUID currentResponsible() {
        if (isEmpty()) return null;
        return baseOrder.get(currentIndex);
    }
}
