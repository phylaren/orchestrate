package genius.project.orchestrate.household.dto;

import genius.project.orchestrate.household.internal.domain.Household;

import java.time.Instant;
import java.util.UUID;

public record HouseholdResponse(
        UUID id,
        String name,
        UUID ownerId,
        Instant createdAt
) {
    public static HouseholdResponse from(Household household) {
        return new HouseholdResponse(household.id(), household.name(), household.ownerId(), household.createdAt());
    }
}
