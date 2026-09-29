package genius.project.orchestrate.chore.internal.repository.inmemory;

import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreWithParticipants;
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

    @Override
    public List<ChoreWithParticipants> findAllWithParticipants(UUID householdId) {
        return store.values().stream()
                .filter(c -> householdId == null || householdId.equals(c.householdId()))
                .map(c -> new ChoreWithParticipants(c, List.of()))
                .toList();
    }

    @Override
    public Optional<ChoreWithParticipants> findByIdWithParticipants(UUID choreId) {
        return findById(choreId).map(c -> new ChoreWithParticipants(c, List.of()));
    }

    @Override
    public void deleteById(UUID choreId) {
        store.remove(choreId);
    }
}
