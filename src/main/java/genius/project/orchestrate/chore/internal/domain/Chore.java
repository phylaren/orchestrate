package genius.project.orchestrate.chore.internal.domain;

import java.time.Instant;
import java.util.UUID;

public record Chore(
    UUID id,
    UUID householdId,
    String name,
    String description,
    int recurrenceDays,
    boolean requiresConfirmation,
    Instant createdAt
) {
}
