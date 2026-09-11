package genius.project.orchestrate.swap;

import java.util.UUID;

public class InvalidSwapRequestRecipientException extends RuntimeException {
    public InvalidSwapRequestRecipientException(UUID userId, UUID choreId) {
        super("User " + userId + " cannot be both initiator and receiver of a swap request for chore " + choreId);
    }
}