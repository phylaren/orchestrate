package genius.project.orchestrate.swap.exception;

import genius.project.orchestrate.common.exception.ForbiddenException;

import java.util.UUID;

public class NotSwapRequestReceiverException extends ForbiddenException {
    public NotSwapRequestReceiverException(UUID userId, UUID requestId) {
        super("NOT_SWAP_REQUEST_RECEIVER",
                "User " + userId + " is not the receiver of swap request " + requestId);
    }
}