package genius.project.orchestrate.household.exception;

import genius.project.orchestrate.common.exception.ForbiddenException;

import java.util.UUID;

public class NotHouseholdOwnerException extends ForbiddenException {
    public NotHouseholdOwnerException(UUID householdId, UUID userId) {
        super("NOT_HOUSEHOLD_OWNER",
                "Only the owner of household '%s' can do this; user '%s' is not the owner".formatted(householdId, userId));
    }
}
