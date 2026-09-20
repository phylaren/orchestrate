package genius.project.orchestrate.household.internal;

import genius.project.orchestrate.household.internal.domain.InvitationCode;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryInvitationCodeRepository implements InvitationCodeRepository {

    private final Map<UUID, InvitationCode> byHousehold = new ConcurrentHashMap<>();

    @Override
    public InvitationCode save(InvitationCode invitationCode) {
        byHousehold.put(invitationCode.householdId(), invitationCode);
        return invitationCode;
    }

    @Override
    public Optional<InvitationCode> findByCode(String code) {
        return byHousehold.values().stream()
                .filter(c -> c.code().equals(code))
                .findFirst();
    }

    @Override
    public Optional<InvitationCode> findByHouseholdId(UUID householdId) {
        return Optional.ofNullable(byHousehold.get(householdId));
    }

    @Override
    public boolean existsByCode(String code) {
        return findByCode(code).isPresent();
    }

    @Override
    public void deleteByHouseholdId(UUID householdId) {
        byHousehold.remove(householdId);
    }
}
