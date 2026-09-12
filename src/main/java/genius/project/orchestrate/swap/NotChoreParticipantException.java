package genius.project.orchestrate.swap;

import genius.project.orchestrate.common.exception.ValidationException;

import java.util.UUID;

public class NotChoreParticipantException extends ValidationException {
    public NotChoreParticipantException(UUID userId, UUID choreId) {
        super("NOT_CHORE_PARTICIPANT",
                "User " + userId + " is not a participant of chore " + choreId);
    }
}