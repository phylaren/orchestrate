package genius.project.orchestrate.swap;

import genius.project.orchestrate.common.exception.ValidationException;

import java.util.UUID;

public class InvalidSwapRequestRecipientException extends ValidationException {
    public InvalidSwapRequestRecipientException(UUID userId, UUID choreId) {
        super("INVALID_SWAP_REQUEST_RECIPIENT",
                "User " + userId + " cannot be both initiator and receiver of a swap request for chore " + choreId);
    }
}