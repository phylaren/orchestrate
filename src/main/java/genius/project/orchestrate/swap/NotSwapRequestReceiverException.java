package genius.project.orchestrate.swap;

public class NotSwapRequestReceiverException extends RuntimeException {
    public NotSwapRequestReceiverException(Long membershipId, Long requestId) {
        super("Member " + membershipId + " is not the receiver of swap request " + requestId);
    }
}