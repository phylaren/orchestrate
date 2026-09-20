package genius.project.orchestrate.household.exception;

import genius.project.orchestrate.common.exception.BusinessRuleViolationException;

import java.util.UUID;

public class OwnerMustTransferOwnershipException extends BusinessRuleViolationException {
    public OwnerMustTransferOwnershipException(UUID householdId) {
        super("OWNER_MUST_TRANSFER_OWNERSHIP",
                "The owner cannot leave household '%s' while it has other members; transfer ownership first"
                        .formatted(householdId));
    }
}
