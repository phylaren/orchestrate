package genius.project.orchestrate.swap.exception;

import genius.project.orchestrate.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class SwapRequestNotFoundException extends ResourceNotFoundException {
    public SwapRequestNotFoundException(UUID id) {
        super("SWAP_REQUEST_NOT_FOUND", "Swap request not found: " + id);
    }
}