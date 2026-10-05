package genius.project.orchestrate.household.dto;

import genius.project.orchestrate.household.internal.domain.Household;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Домогосподарство")
public record HouseholdResponse(
        @Schema(description = "Ідентифікатор домогосподарства", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
        UUID id,

        @Schema(description = "Назва домогосподарства", example = "Квартира на Хрещатику")
        String name,

        @Schema(description = "Ідентифікатор власника — завжди рівно один користувач",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID ownerId,

        @Schema(description = "Момент створення", example = "2026-10-05T10:15:30Z")
        Instant createdAt
) {
    public static HouseholdResponse from(Household household) {
        return new HouseholdResponse(household.id(), household.name(), household.ownerId(), household.createdAt());
    }
}
