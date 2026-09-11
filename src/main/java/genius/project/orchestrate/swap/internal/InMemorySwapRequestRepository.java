package genius.project.orchestrate.swap.internal;

import genius.project.orchestrate.swap.internal.domain.SwapRequest;
import genius.project.orchestrate.swap.SwapRequestStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemorySwapRequestRepository implements SwapRequestRepository {

    private final Map<Long, SwapRequest> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public SwapRequest save(SwapRequest swapRequest) {
        long id = idSequence.getAndIncrement();
        SwapRequest saved = new SwapRequest(
                id,
                swapRequest.choreId(),
                swapRequest.initiatorMembershipId(),
                swapRequest.receiverMembershipId(),
                swapRequest.status(),
                swapRequest.createdAt()
        );
        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<SwapRequest> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<SwapRequest> findByChoreId(Long choreId) {
        return store.values().stream()
                .filter(s -> s.choreId().equals(choreId))
                .toList();
    }

    @Override
    public boolean existsPending(Long choreId, Long initiatorMembershipId, Long receiverMembershipId) {
        return store.values().stream()
                .anyMatch(s -> s.choreId().equals(choreId)
                        && s.initiatorMembershipId().equals(initiatorMembershipId)
                        && s.receiverMembershipId().equals(receiverMembershipId)
                        && s.status() == SwapRequestStatus.PENDING);
    }

    @Override
    public SwapRequest update(SwapRequest swapRequest) {
        store.put(swapRequest.id(), swapRequest);
        return swapRequest;
    }
}