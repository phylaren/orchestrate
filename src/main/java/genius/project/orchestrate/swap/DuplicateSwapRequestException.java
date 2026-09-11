package genius.project.orchestrate.swap;

public class DuplicateSwapRequestException extends RuntimeException {
    public DuplicateSwapRequestException(Long choreId, Long initiatorId, Long receiverId) {
        super("Pending swap request already exists for chore " + choreId +
                " between members " + initiatorId + " and " + receiverId);
    }
}