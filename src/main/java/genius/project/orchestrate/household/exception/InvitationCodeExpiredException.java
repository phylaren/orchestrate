package genius.project.orchestrate.household.exception;

import genius.project.orchestrate.common.exception.BusinessRuleViolationException;

public class InvitationCodeExpiredException extends BusinessRuleViolationException {
    public InvitationCodeExpiredException(String code) {
        super("INVITATION_CODE_EXPIRED", "Invitation code '%s' has expired".formatted(code));
    }
}
