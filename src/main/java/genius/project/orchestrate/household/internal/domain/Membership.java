package genius.project.orchestrate.household.internal.domain;

import genius.project.orchestrate.household.MembershipPermission;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record Membership(
    UUID householdId,
    UUID userId,
    Set<MembershipPermission> permissions,
    Instant joinedAt
) {
    public Membership {
        permissions = Collections.unmodifiableSet(permissions.isEmpty()
                ? EnumSet.noneOf(MembershipPermission.class)
                : EnumSet.copyOf(permissions));
    }

    public boolean has(MembershipPermission permission) {
        return permissions.contains(permission);
    }

    public Membership withPermissions(Set<MembershipPermission> newPermissions) {
        return new Membership(householdId, userId, newPermissions, joinedAt);
    }
}
