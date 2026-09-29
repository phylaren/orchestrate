package genius.project.orchestrate.swap.exception;

import genius.project.orchestrate.common.exception.ForbiddenException;

import java.util.UUID;

public class NotSwapRequestInitiatorException extends ForbiddenException {
    public NotSwapRequestInitiatorException(UUID userId, UUID requestId) {
        super("NOT_SWAP_REQUEST_INITIATOR",
                "User " + userId + " is not the initiator of swap request " + requestId);
    }
}
