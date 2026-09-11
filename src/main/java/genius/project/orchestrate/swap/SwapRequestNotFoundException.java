package genius.project.orchestrate.swap;

import java.util.UUID;

public class SwapRequestNotFoundException extends RuntimeException {
    public SwapRequestNotFoundException(UUID id) {
        super("Swap request not found: " + id);
    }
}