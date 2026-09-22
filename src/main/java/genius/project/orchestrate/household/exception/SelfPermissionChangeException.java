package genius.project.orchestrate.household.exception;

import genius.project.orchestrate.common.exception.BusinessRuleViolationException;

import java.util.UUID;

public class SelfPermissionChangeException extends BusinessRuleViolationException {
    public SelfPermissionChangeException(UUID userId) {
        super("SELF_PERMISSION_CHANGE_NOT_ALLOWED",
                "User '%s' cannot change their own permissions".formatted(userId));
    }
}
