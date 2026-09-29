package genius.project.orchestrate.chore.internal.repository.jpa;

import genius.project.orchestrate.chore.internal.persistence.ChoreCompletionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataChoreCompletionRepository
        extends JpaRepository<ChoreCompletionEntity, UUID> {

    List<ChoreCompletionEntity> findByChoreId(UUID choreId);
}