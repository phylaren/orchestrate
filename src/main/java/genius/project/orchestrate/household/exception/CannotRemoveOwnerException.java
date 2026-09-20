package genius.project.orchestrate.household.exception;

import genius.project.orchestrate.common.exception.BusinessRuleViolationException;

import java.util.UUID;

public class CannotRemoveOwnerException extends BusinessRuleViolationException {
    public CannotRemoveOwnerException(UUID householdId) {
        super("CANNOT_REMOVE_OWNER",
                "The owner of household '%s' cannot be removed by another member".formatted(householdId));
    }
}
