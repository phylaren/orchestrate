package genius.project.orchestrate.user.exception;

import genius.project.orchestrate.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(UUID userId) {
        super("USER_NOT_FOUND", "user with id '%s' was not found".formatted(userId));
    }
}
