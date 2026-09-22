package genius.project.orchestrate.household.dto;

import genius.project.orchestrate.household.MembershipPermission;
import genius.project.orchestrate.household.internal.domain.Membership;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MembershipResponse(
        UUID householdId,
        UUID userId,
        Set<MembershipPermission> permissions,
        Instant joinedAt
) {
    public static MembershipResponse from(Membership membership) {
        return new MembershipResponse(
                membership.householdId(),
                membership.userId(),
                membership.permissions(),
                membership.joinedAt());
    }
}
