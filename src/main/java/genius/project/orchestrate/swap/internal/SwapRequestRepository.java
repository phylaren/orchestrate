package genius.project.orchestrate.swap.internal;

import genius.project.orchestrate.swap.internal.domain.SwapRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SwapRequestRepository {
    SwapRequest save(SwapRequest swapRequest);
    Optional<SwapRequest> findById(UUID id);
    List<SwapRequest> findByChoreId(UUID choreId);
    boolean existsPending(UUID choreId, UUID initiatorUserId, UUID receiverUserId);
    SwapRequest update(SwapRequest swapRequest);
}