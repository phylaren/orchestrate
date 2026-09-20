package genius.project.orchestrate.user.exception;

import genius.project.orchestrate.common.exception.BusinessRuleViolationException;

public class EmailAlreadyTakenException extends BusinessRuleViolationException {
    public EmailAlreadyTakenException(String email) {
        super("EMAIL_ALREADY_TAKEN", "A user with email '%s' is already registered.".formatted(email));
    }
}
