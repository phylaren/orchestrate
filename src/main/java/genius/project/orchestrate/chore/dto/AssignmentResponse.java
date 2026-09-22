package genius.project.orchestrate.chore.dto;

import genius.project.orchestrate.chore.internal.domain.RotationSchedule;

import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID choreId,
        UUID currentResponsibleUserId,
        int cycleNumber,
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