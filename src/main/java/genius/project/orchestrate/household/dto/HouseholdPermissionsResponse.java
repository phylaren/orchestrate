package genius.project.orchestrate.household.dto;

import genius.project.orchestrate.household.internal.domain.Household;
import genius.project.orchestrate.household.internal.domain.Membership;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Зведення прав усіх учасників домогосподарства")
public record HouseholdPermissionsResponse(
        @Schema(description = "Ідентифікатор домогосподарства", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
        UUID householdId,

        @Schema(description = "Ідентифікатор власника", example = "5d1c0e7a-2b3f-4c8d-9e6a-7f8b9c0d1e2f")
        UUID ownerId,

        @Schema(description = "Учасники та їхні права, відсортовані за часом приєднання")
        List<MembershipResponse> members
) {
    public static HouseholdPermissionsResponse from(Household household, List<Membership> members) {
        return new HouseholdPermissionsResponse(
                household.id(),
                household.ownerId(),
                members.stream().map(MembershipResponse::from).toList());
    }
}
