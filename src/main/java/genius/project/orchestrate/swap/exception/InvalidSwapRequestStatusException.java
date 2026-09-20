package genius.project.orchestrate.swap.exception;

import genius.project.orchestrate.common.exception.InvalidStateTransitionException;
import genius.project.orchestrate.swap.SwapRequestStatus;

import java.util.UUID;

public class InvalidSwapRequestStatusException extends InvalidStateTransitionException {
    public InvalidSwapRequestStatusException(UUID requestId, SwapRequestStatus from, SwapRequestStatus to) {
        super("INVALID_SWAP_REQUEST_STATUS", "swap request", requestId, from, to);
    }
}
