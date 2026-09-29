package genius.project.orchestrate.swap.exception;

import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import genius.project.orchestrate.swap.SwapRequestStatus;

import java.util.UUID;

public class SwapRequestNotEditableException extends BusinessRuleViolationException {
    public SwapRequestNotEditableException(UUID requestId, SwapRequestStatus status) {
        super("SWAP_REQUEST_NOT_EDITABLE",
                "Swap request " + requestId + " is " + status + " and can no longer be changed");
    }
}
