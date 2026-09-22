package genius.project.orchestrate.household.internal;

import genius.project.orchestrate.household.internal.domain.Household;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryHouseholdRepository implements HouseholdRepository {

    private final Map<UUID, Household> store = new ConcurrentHashMap<>();

    @Override
    public Household save(Household household) {
        store.put(household.id(), household);
        return household;
    }

    @Override
    public Optional<Household> findById(UUID householdId) {
        return Optional.ofNullable(store.get(householdId));
    }

    @Override
    public void deleteById(UUID householdId) {
        store.remove(householdId);
    }
}
