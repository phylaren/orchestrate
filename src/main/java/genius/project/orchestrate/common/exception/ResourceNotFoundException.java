package genius.project.orchestrate.common.exception;

import org.springframework.modulith.NamedInterface;

@NamedInterface
public class ResourceNotFoundException extends RuntimeException {

    private final String errorCode;

    public ResourceNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static ResourceNotFoundException of(String resourceName, Object id) {
        return new ResourceNotFoundException(
                resourceName.toUpperCase() + "_NOT_FOUND",
                "%s with id '%s' was not found".formatted(resourceName, id));
    }

    public String getErrorCode() {
        return errorCode;
    }
}
