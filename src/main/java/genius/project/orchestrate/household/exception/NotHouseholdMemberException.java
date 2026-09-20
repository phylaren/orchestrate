package genius.project.orchestrate.household.exception;

import genius.project.orchestrate.common.exception.ForbiddenException;

import java.util.UUID;

public class NotHouseholdMemberException extends ForbiddenException {
    public NotHouseholdMemberException(UUID householdId, UUID userId) {
        super("NOT_HOUSEHOLD_MEMBER",
                "User '%s' is not a member of household '%s'".formatted(userId, householdId));
    }
}
