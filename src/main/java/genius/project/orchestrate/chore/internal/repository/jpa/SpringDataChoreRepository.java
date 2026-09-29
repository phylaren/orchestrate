package genius.project.orchestrate.chore.internal.repository.jpa;

import genius.project.orchestrate.chore.internal.persistence.ChoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataChoreRepository extends JpaRepository<ChoreEntity, UUID> {

    List<ChoreEntity> findByHouseholdId(UUID householdId);

    @Query("SELECT DISTINCT c FROM ChoreEntity c LEFT JOIN FETCH c.participants")
    List<ChoreEntity> findAllWithParticipants();

    @Query("SELECT DISTINCT c FROM ChoreEntity c LEFT JOIN FETCH c.participants WHERE c.householdId = :householdId")
    List<ChoreEntity> findAllByHouseholdIdWithParticipants(@Param("householdId") UUID householdId);

    @Query("SELECT c FROM ChoreEntity c LEFT JOIN FETCH c.participants WHERE c.id = :id")
    Optional<ChoreEntity> findByIdWithParticipants(@Param("id") UUID id);
}
