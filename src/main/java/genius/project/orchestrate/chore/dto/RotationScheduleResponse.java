package genius.project.orchestrate.chore.dto;

import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Розклад ротації відповідальних за обов'язок")
public record RotationScheduleResponse(
        @Schema(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID choreId,

        @Schema(description = "Базовий порядок учасників у ротації",
                example = "[\"3fa85f64-5717-4562-b3fc-2c963f66afa6\", \"5d1c0e7a-2b3f-4c8d-9e6a-7f8b9c0d1e2f\"]")
        List<UUID> order,

        @Schema(description = "Відповідальний на поточний цикл; null, якщо група порожня",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", nullable = true)
        UUID currentResponsibleUserId,

        @Schema(description = "Номер поточного циклу", example = "1")
        int cycleNumber,

        @Schema(description = "Момент початку поточного циклу", example = "2026-10-05T10:26:00Z")
        Instant cycleStartedAt
) {
    public static RotationScheduleResponse from(RotationSchedule schedule) {
        return new RotationScheduleResponse(
                schedule.choreId(),
                List.copyOf(schedule.baseOrder()),
                schedule.isEmpty() ? null : schedule.currentResponsible(),
                schedule.currentCycleNumber(),
                schedule.cycleStartedAt());
    }
}
