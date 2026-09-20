package genius.project.orchestrate.household.exception;

import genius.project.orchestrate.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class InvitationCodeNotFoundException extends ResourceNotFoundException {
    public InvitationCodeNotFoundException(String code) {
        super("INVITATION_CODE_NOT_FOUND", "Invitation code '%s' does not exist".formatted(code));
    }

    public InvitationCodeNotFoundException(UUID householdId) {
        super("INVITATION_CODE_NOT_FOUND",
                "Household '%s' has no active invitation code".formatted(householdId));
    }
}
