package genius.project.orchestrate.household.internal;

import genius.project.orchestrate.household.internal.domain.Household;

import java.util.Optional;
import java.util.UUID;

public interface HouseholdRepository {
    Household save(Household household);
    Optional<Household> findById(UUID householdId);
    void deleteById(UUID householdId);
}
