package genius.project.orchestrate.household.internal;

import genius.project.orchestrate.household.internal.domain.InvitationCode;

import java.util.Optional;
import java.util.UUID;

public interface InvitationCodeRepository {
    /** Stores the code as the household's only active one, replacing any previous code. */
    InvitationCode save(InvitationCode invitationCode);
    Optional<InvitationCode> findByCode(String code);
    Optional<InvitationCode> findByHouseholdId(UUID householdId);
    boolean existsByCode(String code);
    void deleteByHouseholdId(UUID householdId);
}
