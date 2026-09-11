package genius.project.orchestrate.swap;

import java.util.UUID;

public class DuplicateSwapRequestException extends RuntimeException {
    public DuplicateSwapRequestException(UUID choreId, UUID initiatorId, UUID receiverId) {
        super("Pending swap request already exists for chore " + choreId +
                " between users " + initiatorId + " and " + receiverId);
    }
}