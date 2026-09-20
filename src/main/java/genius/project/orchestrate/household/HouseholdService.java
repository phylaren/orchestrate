package genius.project.orchestrate.household;

import genius.project.orchestrate.household.client.HouseholdClient;
import genius.project.orchestrate.household.internal.domain.Household;
import genius.project.orchestrate.household.internal.domain.InvitationCode;
import genius.project.orchestrate.household.internal.domain.Membership;
import genius.project.orchestrate.household.internal.domain.UserHousehold;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface HouseholdService extends HouseholdClient {

    Household createHousehold(String name);

    Household getHousehold(UUID householdId);

    void deleteHousehold(UUID householdId);

    Household transferOwnership(UUID householdId, UUID newOwnerUserId);

    List<Membership> listMembers(UUID householdId);

    Membership getMember(UUID householdId, UUID userId);

    void removeMember(UUID householdId, UUID userId);

    Membership updatePermissions(UUID householdId, UUID userId, Set<MembershipPermission> permissions);

    InvitationCode createInvitationCode(UUID householdId);

    InvitationCode getInvitationCode(UUID householdId);

    void revokeInvitationCode(UUID householdId);

    Membership joinByInvitationCode(String code);

    List<UserHousehold> listHouseholdsOfUser(UUID userId);
}
