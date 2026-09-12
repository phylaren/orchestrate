package genius.project.orchestrate.swap.internal;

import genius.project.orchestrate.swap.SwapRequestStatus;
import genius.project.orchestrate.swap.internal.domain.SwapRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemorySwapRequestRepository implements SwapRequestRepository {

    private final Map<UUID, SwapRequest> store = new ConcurrentHashMap<>();

    @Override
    public SwapRequest save(SwapRequest swapRequest) {
        UUID id = UUID.randomUUID();
        SwapRequest saved = new SwapRequest(
                id,
                swapRequest.choreId(),
                swapRequest.initiatorUserId(),
                swapRequest.receiverUserId(),
                swapRequest.status(),
                swapRequest.createdAt()
        );
        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<SwapRequest> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<SwapRequest> findByChoreId(UUID choreId) {
        return store.values().stream()
                .filter(s -> s.choreId().equals(choreId))
                .toList();
    }

    @Override
    public boolean existsPending(UUID choreId, UUID initiatorUserId, UUID receiverUserId) {
        return store.values().stream()
                .anyMatch(s -> s.choreId().equals(choreId)
                        && s.initiatorUserId().equals(initiatorUserId)
                        && s.receiverUserId().equals(receiverUserId)
                        && s.status() == SwapRequestStatus.PENDING);
    }

    @Override
    public SwapRequest update(SwapRequest swapRequest) {
        store.put(swapRequest.id(), swapRequest);
        return swapRequest;
    }
}