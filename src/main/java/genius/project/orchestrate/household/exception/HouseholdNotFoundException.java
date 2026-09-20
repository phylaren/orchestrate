package genius.project.orchestrate.household.exception;

import genius.project.orchestrate.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class HouseholdNotFoundException extends ResourceNotFoundException {
    public HouseholdNotFoundException(UUID householdId) {
        super("HOUSEHOLD_NOT_FOUND", "household with id '%s' was not found".formatted(householdId));
    }
}
