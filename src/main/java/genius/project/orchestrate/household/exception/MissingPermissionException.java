package genius.project.orchestrate.household.exception;

import genius.project.orchestrate.common.exception.ForbiddenException;
import genius.project.orchestrate.household.MembershipPermission;

import java.util.UUID;

public class MissingPermissionException extends ForbiddenException {
    public MissingPermissionException(UUID householdId, UUID userId, MembershipPermission permission) {
        super("MISSING_PERMISSION",
                "User '%s' lacks permission %s in household '%s'".formatted(userId, permission, householdId));
    }
}
