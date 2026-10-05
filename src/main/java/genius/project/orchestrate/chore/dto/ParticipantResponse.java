package genius.project.orchestrate.chore.dto;

import genius.project.orchestrate.chore.internal.domain.ChoreParticipant;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Учасник групи виконання (пулу ротації) обов'язку")
public record ParticipantResponse(
        @Schema(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID choreId,

        @Schema(description = "Ідентифікатор учасника", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID userId,

        @Schema(description = "true, якщо учасника додав адміністратор, а не він сам", example = "false")
        boolean addedByAdmin,

        @Schema(description = "Момент приєднання до групи виконання", example = "2026-10-05T10:26:00Z")
        Instant joinedAt
) {
    public static ParticipantResponse from(ChoreParticipant participant) {
        return new ParticipantResponse(
                participant.choreId(),
                participant.userId(),
                participant.addedByAdmin(),
                participant.joinedAt());
    }
}
