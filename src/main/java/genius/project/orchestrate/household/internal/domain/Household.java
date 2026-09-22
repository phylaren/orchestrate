package genius.project.orchestrate.household.internal.domain;

import java.time.Instant;
import java.util.UUID;

public record Household(
    UUID id,
    String name,
    UUID ownerId,
    Instant createdAt
) {
    public boolean isOwnedBy(UUID userId) {
        return ownerId.equals(userId);
    }

    public Household withOwner(UUID newOwnerId) {
        return new Household(id, name, newOwnerId, createdAt);
    }
}
