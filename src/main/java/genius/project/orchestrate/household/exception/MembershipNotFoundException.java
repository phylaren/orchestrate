package genius.project.orchestrate.household.exception;

import genius.project.orchestrate.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class MembershipNotFoundException extends ResourceNotFoundException {
    public MembershipNotFoundException(UUID householdId, UUID userId) {
        super("MEMBERSHIP_NOT_FOUND",
                "User '%s' is not a member of household '%s'".formatted(userId, householdId));
    }
}
