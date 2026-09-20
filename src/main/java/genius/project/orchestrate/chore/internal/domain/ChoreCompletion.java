package genius.project.orchestrate.chore.internal.domain;

import genius.project.orchestrate.chore.ConfirmationStatus;

import java.time.Instant;
import java.util.UUID;

public record ChoreCompletion(
    UUID id,
    UUID choreId,
    UUID completedByUserId,
    Instant completedAt,
    ConfirmationStatus status,
    UUID confirmedByUserId,
    Instant confirmedAt
) {
}
