package genius.project.orchestrate.chore.dto;

import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Поточний відповідальний за обов'язок на активний цикл")
public record AssignmentResponse(
        @Schema(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID choreId,

        @Schema(description = "Відповідальний на поточний цикл", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID currentResponsibleUserId,

        @Schema(description = "Номер поточного циклу", example = "1")
        int cycleNumber,

        @Schema(description = "Момент початку поточного циклу", example = "2026-10-05T10:26:00Z")
        Instant cycleStartedAt
) {
    public static AssignmentResponse from(RotationSchedule schedule) {
        return new AssignmentResponse(
                schedule.choreId(),
                schedule.currentResponsible(),
                schedule.currentCycleNumber(),
                schedule.cycleStartedAt());
    }
}
