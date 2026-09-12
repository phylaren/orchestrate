package genius.project.orchestrate.chore.internal.domain;

import java.time.Instant;
import java.util.UUID;

public record ChoreParticipant(
    UUID choreId,
    UUID userId,
    Instant joinedAt,
    boolean addedByAdmin
) {
}
