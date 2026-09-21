package genius.project.orchestrate.chore.exception;

import genius.project.orchestrate.common.exception.ValidationException;

import java.util.UUID;

public class SameUserSwapException extends ValidationException {

    public SameUserSwapException(UUID userId) {
        super("SAME_USER_SWAP",
                "Cannot swap user '%s' with themselves.".formatted(userId));
    }
}