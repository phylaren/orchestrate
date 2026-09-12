package genius.project.orchestrate.chore.dto;

import genius.project.orchestrate.chore.internal.domain.ChoreAssignment;

import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID choreId,
        UUID currentResponsibleUserId,
        int cycleNumber,
        Instant cycleStartedAt
) {
    public static AssignmentResponse from(ChoreAssignment assignment) {
        return new AssignmentResponse(
                assignment.choreId(),
                assignment.currentResponsibleUserId(),
                assignment.cycleNumber(),
                assignment.cycleStartedAt());
    }
}
