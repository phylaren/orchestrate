package genius.project.orchestrate.chore.exception;

import genius.project.orchestrate.common.exception.BusinessRuleViolationException;

import java.util.UUID;

public class UserNotInRotationException extends BusinessRuleViolationException {

    public UserNotInRotationException(UUID userId) {
        super("USER_NOT_IN_ROTATION",
                "User '%s' is not a member of this rotation group.".formatted(userId));
    }
}