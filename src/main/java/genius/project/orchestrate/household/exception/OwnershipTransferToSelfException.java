package genius.project.orchestrate.household.exception;

import genius.project.orchestrate.common.exception.ValidationException;

import java.util.UUID;

public class OwnershipTransferToSelfException extends ValidationException {
    public OwnershipTransferToSelfException(UUID householdId) {
        super("OWNERSHIP_TRANSFER_TO_SELF",
                "The owner of household '%s' cannot transfer ownership to themselves".formatted(householdId));
    }
}
