package genius.project.orchestrate.swap;

import java.util.UUID;

public class NotSwapRequestReceiverException extends RuntimeException {
    public NotSwapRequestReceiverException(UUID userId, UUID requestId) {
        super("User " + userId + " is not the receiver of swap request " + requestId);
    }
}