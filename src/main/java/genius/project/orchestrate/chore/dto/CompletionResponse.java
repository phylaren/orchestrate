package genius.project.orchestrate.chore.dto;

import genius.project.orchestrate.chore.ConfirmationStatus;
import genius.project.orchestrate.chore.internal.domain.ChoreCompletion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Запис про виконання обов'язку")
public record CompletionResponse(
        @Schema(description = "Ідентифікатор запису", example = "c0a8f3d2-6b1e-4f9a-9d3c-5e7b8a1f2c4d")
        UUID id,

        @Schema(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID choreId,

        @Schema(description = "Хто відмітив виконання", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID completedByUserId,

        @Schema(description = "Момент відмітки виконання", example = "2026-10-05T18:00:00Z")
        Instant completedAt,

        @Schema(description = "Статус підтвердження", example = "PENDING")
        ConfirmationStatus status,

        @Schema(description = "Хто підтвердив або відхилив; null, доки рішення немає",
                example = "5d1c0e7a-2b3f-4c8d-9e6a-7f8b9c0d1e2f", nullable = true)
        UUID confirmedByUserId,

        @Schema(description = "Момент рішення; null, доки рішення немає",
                example = "2026-10-05T18:30:00Z", nullable = true)
        Instant confirmedAt
) {
    public static CompletionResponse from(ChoreCompletion completion) {
        return new CompletionResponse(
                completion.id(),
                completion.choreId(),
                completion.completedByUserId(),
                completion.completedAt(),
                completion.status(),
                completion.confirmedByUserId(),
                completion.confirmedAt());
    }
}
