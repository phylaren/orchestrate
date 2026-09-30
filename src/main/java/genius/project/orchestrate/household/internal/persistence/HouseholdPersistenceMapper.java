package genius.project.orchestrate.household.internal.persistence;

import genius.project.orchestrate.household.internal.domain.Household;
import genius.project.orchestrate.household.internal.domain.InvitationCode;
import genius.project.orchestrate.household.internal.domain.Membership;


final class HouseholdPersistenceMapper {

    private HouseholdPersistenceMapper() {}

    static Household toDomain(HouseholdEntity entity) {
        return new Household(entity.getId(), entity.getName(), entity.getOwnerId(), entity.getCreatedAt());
    }

    static Membership toDomain(MembershipEntity entity) {
        return new Membership(
                entity.getId().getHouseholdId(),
                entity.getId().getUserId(),
                entity.getPermissions(),
                entity.getJoinedAt());
    }

    // household.getId() on a lazy proxy returns the key without initializing it.
    static InvitationCode toDomain(InvitationCodeEntity entity) {
        return new InvitationCode(
                entity.getCode(),
                entity.getHousehold().getId(),
                entity.getCreatedByUserId(),
                entity.getCreatedAt(),
                entity.getExpiresAt());
    }
}
