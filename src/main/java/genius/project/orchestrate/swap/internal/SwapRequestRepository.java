package genius.project.orchestrate.swap.internal;

import genius.project.orchestrate.swap.SwapRequestStatus;
import genius.project.orchestrate.swap.internal.domain.SwapRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SwapRequestRepository {

    SwapRequest save(SwapRequest swapRequest);

    Optional<SwapRequest> findById(UUID id);

    List<SwapRequest> findByChoreId(UUID choreId);

    List<SwapRequest> findByChoreIdAndStatus(UUID choreId, SwapRequestStatus status);

    boolean existsPending(UUID choreId, UUID initiatorUserId, UUID receiverUserId);

    SwapRequest update(SwapRequest swapRequest);

    void deleteById(UUID id);

    long countByStatus(UUID choreId, SwapRequestStatus status);

    Map<SwapRequestStatus, Long> countGroupedByStatus(UUID choreId);
}
