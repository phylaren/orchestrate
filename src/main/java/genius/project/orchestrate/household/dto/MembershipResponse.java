package genius.project.orchestrate.household.dto;

import genius.project.orchestrate.household.MembershipPermission;
import genius.project.orchestrate.household.internal.domain.Membership;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Участь користувача в домогосподарстві та його адміністративні права")
public record MembershipResponse(
        @Schema(description = "Ідентифікатор домогосподарства", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
        UUID householdId,

        @Schema(description = "Ідентифікатор користувача", example = "5d1c0e7a-2b3f-4c8d-9e6a-7f8b9c0d1e2f")
        UUID userId,

        @Schema(description = "Надані адміністративні права", example = "[\"CONFIRM_COMPLETIONS\"]")
        Set<MembershipPermission> permissions,

        @Schema(description = "Момент приєднання", example = "2026-10-05T10:20:00Z")
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
