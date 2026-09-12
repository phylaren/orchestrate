package genius.project.orchestrate.chore.dto;

import genius.project.orchestrate.chore.internal.domain.Chore;

import java.time.Instant;
import java.util.UUID;

public record ChoreResponse(
        UUID id,
        UUID householdId,
        String name,
        String description,
        int recurrenceDays,
        boolean requiresConfirmation,
        boolean needsAttention,
        Instant createdAt
) {
    public static ChoreResponse from(Chore chore, boolean needsAttention) {
        return new ChoreResponse(
                chore.id(),
                chore.householdId(),
                chore.name(),
                chore.description(),
                chore.recurrenceDays(),
                chore.requiresConfirmation(),
                needsAttention,
                chore.createdAt());
    }
}
