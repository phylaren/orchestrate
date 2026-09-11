package genius.project.orchestrate.swap;

public class InvalidSwapRequestRecipientException extends RuntimeException {
    public InvalidSwapRequestRecipientException(Long membershipId, Long choreId) {
        super("Member " + membershipId + " is not a participant of chore " + choreId);
    }
}