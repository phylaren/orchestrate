package genius.project.orchestrate.chore.internal.repository.inmemory;

import genius.project.orchestrate.chore.internal.domain.ChoreCompletion;
import genius.project.orchestrate.chore.internal.repository.ChoreCompletionStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
class InMemoryChoreCompletionStore implements ChoreCompletionStore {

    private final Map<UUID, ChoreCompletion> store = new ConcurrentHashMap<>();

    @Override
    public ChoreCompletion save(ChoreCompletion completion) {
        store.put(completion.id(), completion);
        return completion;
    }

    @Override
    public Optional<ChoreCompletion> findById(UUID completionId) {
        return Optional.ofNullable(store.get(completionId));
    }

    @Override
    public List<ChoreCompletion> findByChoreId(UUID choreId) {
        return store.values().stream()
                .filter(c -> c.choreId().equals(choreId))
                .toList();
    }
}
