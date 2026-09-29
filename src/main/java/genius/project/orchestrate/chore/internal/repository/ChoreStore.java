package genius.project.orchestrate.chore.internal.repository;

import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreWithParticipants;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChoreStore {
    Chore save(Chore chore);

    Optional<Chore> findById(UUID choreId);

    List<Chore> findAll();
    List<ChoreWithParticipants> findAllWithParticipants(UUID householdId);
    Optional<ChoreWithParticipants> findByIdWithParticipants(UUID choreId);
    void deleteById(UUID choreId);
}
