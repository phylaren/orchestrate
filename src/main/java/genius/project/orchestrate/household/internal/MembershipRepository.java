package genius.project.orchestrate.household.internal;

import genius.project.orchestrate.household.internal.domain.Membership;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository {
    Membership save(Membership membership);
    Optional<Membership> find(UUID householdId, UUID userId);
    List<Membership> findByHouseholdId(UUID householdId);
    List<Membership> findByUserId(UUID userId);
    void delete(UUID householdId, UUID userId);
    void deleteByHouseholdId(UUID householdId);
}
