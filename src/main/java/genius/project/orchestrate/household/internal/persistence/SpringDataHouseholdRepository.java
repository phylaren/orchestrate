package genius.project.orchestrate.household.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataHouseholdRepository extends JpaRepository<HouseholdEntity, UUID> {
}
