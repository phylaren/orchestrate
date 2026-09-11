package genius.project.orchestrate.swap;

import genius.project.orchestrate.common.exception.BusinessRuleViolationException;

import java.util.UUID;

public class DuplicateSwapRequestException extends BusinessRuleViolationException {
    public DuplicateSwapRequestException(UUID choreId, UUID initiatorId, UUID receiverId) {
        super("SWAP_REQUEST_ALREADY_EXISTS",
                "Pending swap request already exists for chore " + choreId +
                        " between users " + initiatorId + " and " + receiverId);
    }
}