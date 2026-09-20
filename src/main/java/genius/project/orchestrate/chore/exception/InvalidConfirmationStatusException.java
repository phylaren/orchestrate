package genius.project.orchestrate.chore.exception;

import genius.project.orchestrate.chore.ConfirmationStatus;
import genius.project.orchestrate.common.exception.InvalidStateTransitionException;

import java.util.UUID;

public class InvalidConfirmationStatusException extends InvalidStateTransitionException {
    public InvalidConfirmationStatusException(UUID completionId, ConfirmationStatus from, ConfirmationStatus to) {
        super("INVALID_CONFIRMATION_STATUS", "completion", completionId, from, to);
    }
}
