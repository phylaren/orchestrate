package genius.project.orchestrate.swap.exception;

import genius.project.orchestrate.common.exception.ValidationException;

public class InvalidSwapRequestStatusException extends ValidationException {
    public InvalidSwapRequestStatusException(String message) {
        super("INVALID_SWAP_REQUEST_STATUS", message);
    }
}