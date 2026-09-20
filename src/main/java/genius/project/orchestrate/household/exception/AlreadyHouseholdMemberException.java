package genius.project.orchestrate.household.exception;

import genius.project.orchestrate.common.exception.BusinessRuleViolationException;

import java.util.UUID;

public class AlreadyHouseholdMemberException extends BusinessRuleViolationException {
    public AlreadyHouseholdMemberException(UUID householdId, UUID userId) {
        super("ALREADY_HOUSEHOLD_MEMBER",
                "User '%s' is already a member of household '%s'".formatted(userId, householdId));
    }
}
