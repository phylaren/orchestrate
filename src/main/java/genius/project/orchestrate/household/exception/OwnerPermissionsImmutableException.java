package genius.project.orchestrate.household.exception;

import genius.project.orchestrate.common.exception.BusinessRuleViolationException;

import java.util.UUID;

public class OwnerPermissionsImmutableException extends BusinessRuleViolationException {
    public OwnerPermissionsImmutableException(UUID householdId) {
        super("OWNER_PERMISSIONS_IMMUTABLE",
                "The owner of household '%s' always has every permission".formatted(householdId));
    }
}
