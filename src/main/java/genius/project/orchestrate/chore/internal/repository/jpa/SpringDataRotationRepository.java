package genius.project.orchestrate.chore.internal.repository.jpa;

import genius.project.orchestrate.chore.internal.persistence.RotationScheduleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataRotationRepository extends JpaRepository<RotationScheduleEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = "baseOrder")
    Optional<RotationScheduleEntity> findById(UUID id);
}
