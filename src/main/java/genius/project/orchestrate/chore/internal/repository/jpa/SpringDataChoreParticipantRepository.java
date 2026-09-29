package genius.project.orchestrate.chore.internal.repository.jpa;

import genius.project.orchestrate.chore.internal.persistence.ChoreParticipantEntity;
import genius.project.orchestrate.chore.internal.persistence.ChoreParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataChoreParticipantRepository
        extends JpaRepository<ChoreParticipantEntity, ChoreParticipantId> {

    List<ChoreParticipantEntity> findByIdChoreId(UUID choreId);

    boolean existsByIdChoreIdAndIdUserId(UUID choreId, UUID userId);

    void deleteByIdChoreIdAndIdUserId(UUID choreId, UUID userId);
}