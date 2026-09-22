package genius.project.orchestrate.chore.internal.repository.inmemory;

import genius.project.orchestrate.chore.internal.domain.ScheduledSwap;
import genius.project.orchestrate.chore.internal.repository.ScheduledSwapRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
class InMemoryScheduledSwapRepository implements ScheduledSwapRepository {

    private final Map<UUID, ScheduledSwap> store = new ConcurrentHashMap<>();

    @Override
    public ScheduledSwap save(ScheduledSwap swap) {
        UUID id = swap.id() != null ? swap.id() : UUID.randomUUID();
        ScheduledSwap saved = new ScheduledSwap(
                id,
                swap.choreId(),
                swap.fromUserId(),
                swap.toUserId(),
                swap.cycleNumber(),
                swap.createdAt());
        store.put(id, saved);
        return saved;
    }

    @Override
    public List<ScheduledSwap> findByChoreIdAndCycleNumber(UUID choreId, int cycleNumber) {
        return store.values().stream()
                .filter(s -> s.choreId().equals(choreId) && s.cycleNumber() == cycleNumber)
                .sorted(Comparator.comparing(ScheduledSwap::createdAt))
                .toList();
    }

    @Override
    public void deleteExpired(UUID choreId, int currentCycleNumber) {
        store.values().removeIf(s ->
                s.choreId().equals(choreId) && s.cycleNumber() < currentCycleNumber);
    }
}