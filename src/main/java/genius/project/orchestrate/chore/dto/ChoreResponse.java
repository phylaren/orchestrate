package genius.project.orchestrate.chore.dto;

import genius.project.orchestrate.chore.internal.domain.Chore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Побутовий обов'язок")
public record ChoreResponse(
        @Schema(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID id,

        @Schema(description = "Домогосподарство", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
        UUID householdId,

        @Schema(description = "Назва", example = "Винести сміття")
        String name,

        @Schema(description = "Опис", example = "Винести пакети з кухні до баків у дворі до 20:00")
        String description,

        @Schema(description = "Інтервал повторення в днях", example = "3")
        int recurrenceDays,

        @Schema(description = "Чи потребує виконання підтвердження", example = "true")
        boolean requiresConfirmation,

        @Schema(description = "true, якщо група виконання порожня — обов'язок потребує уваги", example = "true")
        boolean needsAttention,

        @Schema(description = "Момент створення", example = "2026-10-05T10:25:00Z")
        Instant createdAt
) {
    public static ChoreResponse from(Chore chore, boolean needsAttention) {
        return new ChoreResponse(
                chore.id(),
                chore.householdId(),
                chore.name(),
                chore.description(),
                chore.recurrenceDays(),
                chore.requiresConfirmation(),
                needsAttention,
                chore.createdAt());
    }
}
