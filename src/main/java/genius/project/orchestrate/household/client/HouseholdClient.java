package genius.project.orchestrate.household.client;

import genius.project.orchestrate.household.MembershipPermission;

import java.util.UUID;

public interface HouseholdClient {

    boolean isMember(UUID householdId, UUID userId);

    boolean hasPermission(UUID householdId, UUID userId, MembershipPermission permission);
}
