package genius.project.orchestrate.chore.dto;

import genius.project.orchestrate.chore.internal.domain.ChoreParticipant;

import java.time.Instant;
import java.util.UUID;

public record ParticipantResponse(
        UUID choreId,
        UUID userId,
        boolean addedByAdmin,
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
