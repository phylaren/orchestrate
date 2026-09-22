package genius.project.orchestrate.chore.internal.repository.inmemory;

import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.repository.ChoreStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
class InMemoryChoreStore implements ChoreStore {

    private final Map<UUID, Chore> store = new ConcurrentHashMap<>();

    @Override
    public Chore save(Chore chore) {
        store.put(chore.id(), chore);
        return chore;
    }

    @Override
    public Optional<Chore> findById(UUID choreId) {
        return Optional.ofNullable(store.get(choreId));
    }

    @Override
    public List<Chore> findAll() {
        return List.copyOf(store.values());
    }
}
