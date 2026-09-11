package genius.project.orchestrate.swap.internal;

import genius.project.orchestrate.swap.internal.domain.SwapRequest;

import java.util.List;
import java.util.Optional;

public interface SwapRequestRepository {
    SwapRequest save(SwapRequest swapRequest);
    Optional<SwapRequest> findById(Long id);
    List<SwapRequest> findByChoreId(Long choreId);
    boolean existsPending(Long choreId, Long initiatorMembershipId, Long receiverMembershipId);
    SwapRequest update(SwapRequest swapRequest);
}