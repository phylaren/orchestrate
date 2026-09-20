package genius.project.orchestrate.chore.internal.repository;

import genius.project.orchestrate.chore.internal.domain.ChoreParticipant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChoreParticipantStore {

    ChoreParticipant save(ChoreParticipant participant);

    Optional<ChoreParticipant> findByChoreIdAndUserId(UUID choreId, UUID userId);

    List<ChoreParticipant> findByChoreId(UUID choreId);

    boolean existsByChoreIdAndUserId(UUID choreId, UUID userId);

    void deleteByChoreIdAndUserId(UUID choreId, UUID userId);
}
