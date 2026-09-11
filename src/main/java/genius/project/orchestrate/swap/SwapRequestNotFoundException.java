package genius.project.orchestrate.swap;

public class SwapRequestNotFoundException extends RuntimeException {
    public SwapRequestNotFoundException(Long id) {
        super("Swap request not found: " + id);
    }
}