package genius.project.orchestrate.chore.dto;

import genius.project.orchestrate.chore.internal.domain.RotationSchedule;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RotationScheduleResponse(
        UUID choreId,
        List<UUID> order,
        UUID currentResponsibleUserId,
        int cycleNumber,
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