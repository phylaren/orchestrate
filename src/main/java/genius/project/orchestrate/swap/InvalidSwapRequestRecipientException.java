package genius.project.orchestrate.swap;

public class InvalidSwapRequestRecipientException extends RuntimeException {
    public InvalidSwapRequestRecipientException(Long membershipId, Long choreId) {
        super("Member " + membershipId + " cannot be both initiator and receiver of a swap request for chore " + choreId);
    }
}