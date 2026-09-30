package genius.project.orchestrate.household.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataInvitationCodeRepository extends JpaRepository<InvitationCodeEntity, String> {

    Optional<InvitationCodeEntity> findByHouseholdId(UUID householdId);

    void deleteByHouseholdId(UUID householdId);
}
