package genius.project.orchestrate.swap;

public class InvalidSwapRequestStatusException extends RuntimeException {
    public InvalidSwapRequestStatusException(String message) {
        super(message);
    }
}