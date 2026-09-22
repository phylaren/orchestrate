package genius.project.orchestrate.chore.internal.domain;

import java.time.Instant;
import java.util.UUID;

public record ScheduledSwap(
        UUID id,
        UUID choreId,
        UUID fromUserId,
        UUID toUserId,
        int cycleNumber,
        Instant createdAt
) {}