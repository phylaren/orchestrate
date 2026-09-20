package genius.project.orchestrate.chore.internal.repository;

import genius.project.orchestrate.chore.internal.domain.ChoreCompletion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChoreCompletionStore {

    ChoreCompletion save(ChoreCompletion completion);

    Optional<ChoreCompletion> findById(UUID completionId);

    List<ChoreCompletion> findByChoreId(UUID choreId);
}
