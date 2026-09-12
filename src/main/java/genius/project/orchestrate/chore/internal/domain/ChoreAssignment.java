package genius.project.orchestrate.chore.internal.domain;

import java.time.Instant;
import java.util.UUID;

public record ChoreAssignment(
    UUID choreId,
    UUID currentResponsibleUserId,
    int cycleNumber,
    Instant cycleStartedAt
) {
}
