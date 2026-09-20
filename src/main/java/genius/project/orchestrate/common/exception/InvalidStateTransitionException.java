package genius.project.orchestrate.common.exception;

import org.springframework.modulith.NamedInterface;

@NamedInterface
public abstract class InvalidStateTransitionException extends RuntimeException {

    private final String errorCode;

    protected InvalidStateTransitionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected InvalidStateTransitionException(String errorCode, String resource, Object id,
                                              Enum<?> from, Enum<?> to) {
        this(errorCode, "Illegal state transition for %s '%s' from %s to %s".formatted(resource, id, from, to));
    }

    public String getErrorCode() {
        return errorCode;
    }
}
