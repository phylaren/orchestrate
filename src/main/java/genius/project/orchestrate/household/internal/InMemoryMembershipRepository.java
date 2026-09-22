package genius.project.orchestrate.household.internal;

import genius.project.orchestrate.household.internal.domain.Membership;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryMembershipRepository implements MembershipRepository {

    private final Map<UUID, Map<UUID, Membership>> byHousehold = new ConcurrentHashMap<>();

    @Override
    public Membership save(Membership membership) {
        byHousehold.computeIfAbsent(membership.householdId(), k -> new ConcurrentHashMap<>())
                .put(membership.userId(), membership);
        return membership;
    }

    @Override
    public Optional<Membership> find(UUID householdId, UUID userId) {
        return Optional.ofNullable(byHousehold.getOrDefault(householdId, Map.of()).get(userId));
    }

    @Override
    public List<Membership> findByHouseholdId(UUID householdId) {
        return List.copyOf(byHousehold.getOrDefault(householdId, Map.of()).values());
    }

    @Override
    public List<Membership> findByUserId(UUID userId) {
        return byHousehold.values().stream()
                .map(members -> members.get(userId))
                .filter(m -> m != null)
                .toList();
    }

    @Override
    public void delete(UUID householdId, UUID userId) {
        Map<UUID, Membership> members = byHousehold.get(householdId);
        if (members != null) {
            members.remove(userId);
        }
    }

    @Override
    public void deleteByHouseholdId(UUID householdId) {
        byHousehold.remove(householdId);
    }
}
