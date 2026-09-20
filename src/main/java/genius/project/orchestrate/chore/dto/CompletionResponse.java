package genius.project.orchestrate.chore.dto;

import genius.project.orchestrate.chore.internal.domain.ChoreCompletion;
import genius.project.orchestrate.chore.ConfirmationStatus;

import java.time.Instant;
import java.util.UUID;

public record CompletionResponse(
        UUID id,
        UUID choreId,
        UUID completedByUserId,
        Instant completedAt,
        ConfirmationStatus status,
        UUID confirmedByUserId,
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
