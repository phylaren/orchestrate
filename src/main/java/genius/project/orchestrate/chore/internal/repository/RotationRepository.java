package genius.project.orchestrate.chore.internal.repository;

import genius.project.orchestrate.chore.internal.domain.RotationSchedule;

import java.util.Optional;
import java.util.UUID;

public interface RotationRepository {

    RotationSchedule save(RotationSchedule schedule);

    Optional<RotationSchedule> findByChoreId(UUID choreId);
}
