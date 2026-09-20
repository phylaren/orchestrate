package genius.project.orchestrate.chore.internal.repository.inmemory;

import genius.project.orchestrate.chore.internal.domain.ChoreParticipant;
import genius.project.orchestrate.chore.internal.repository.ChoreParticipantStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
class InMemoryChoreParticipantStore implements ChoreParticipantStore {

    private final Map<UUID, Map<UUID, ChoreParticipant>> store = new ConcurrentHashMap<>();

    @Override
    public ChoreParticipant save(ChoreParticipant participant) {
        store.computeIfAbsent(participant.choreId(), k -> new ConcurrentHashMap<>())
                .put(participant.userId(), participant);
        return participant;
    }

    @Override
    public Optional<ChoreParticipant> findByChoreIdAndUserId(UUID choreId, UUID userId) {
        return Optional.ofNullable(store.getOrDefault(choreId, Map.of()).get(userId));
    }

    @Override
    public List<ChoreParticipant> findByChoreId(UUID choreId) {
        return List.copyOf(store.getOrDefault(choreId, Map.of()).values());
    }

    @Override
    public boolean existsByChoreIdAndUserId(UUID choreId, UUID userId) {
        return store.getOrDefault(choreId, Map.of()).containsKey(userId);
    }

    @Override
    public void deleteByChoreIdAndUserId(UUID choreId, UUID userId) {
        store.getOrDefault(choreId, Map.of()).remove(userId);
    }
}
