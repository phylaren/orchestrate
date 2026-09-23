package genius.project.orchestrate.swap.exception;

import genius.project.orchestrate.common.exception.BusinessRuleViolationException;

public class NotInSameGroupException extends BusinessRuleViolationException {
    public NotInSameGroupException() {
        super("NOT_IN_SAME_GROUP",
                "Both users must still be members of the rotation group.");
    }
}