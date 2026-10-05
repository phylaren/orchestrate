package genius.project.orchestrate.household.dto;

import genius.project.orchestrate.household.MembershipPermission;
import genius.project.orchestrate.household.internal.domain.UserHousehold;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Домогосподарство з точки зору конкретного користувача")
public record UserHouseholdResponse(
        @Schema(description = "Ідентифікатор домогосподарства", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
        UUID householdId,

        @Schema(description = "Назва домогосподарства", example = "Квартира на Хрещатику")
        String householdName,

        @Schema(description = "Чи є користувач власником цього домогосподарства", example = "false")
        boolean owner,

        @Schema(description = "Права користувача в цьому домогосподарстві", example = "[\"CONFIRM_COMPLETIONS\"]")
        Set<MembershipPermission> permissions,

        @Schema(description = "Момент приєднання користувача", example = "2026-10-05T10:20:00Z")
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
