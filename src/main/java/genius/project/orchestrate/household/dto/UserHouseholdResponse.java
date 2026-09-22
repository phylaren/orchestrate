package genius.project.orchestrate.household.dto;

import genius.project.orchestrate.household.MembershipPermission;
import genius.project.orchestrate.household.internal.domain.UserHousehold;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserHouseholdResponse(
        UUID householdId,
        String householdName,
        boolean owner,
        Set<MembershipPermission> permissions,
        Instant joinedAt
) {
    public static UserHouseholdResponse from(UserHousehold userHousehold) {
        return new UserHouseholdResponse(
                userHousehold.household().id(),
                userHousehold.household().name(),
                userHousehold.household().isOwnedBy(userHousehold.membership().userId()),
                userHousehold.membership().permissions(),
                userHousehold.membership().joinedAt());
    }
}
