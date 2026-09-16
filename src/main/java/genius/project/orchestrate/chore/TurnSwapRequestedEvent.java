package genius.project.orchestrate.chore;

import java.util.UUID;

public record TurnSwapRequestedEvent(
        UUID choreId,
        UUID fromUserId,
        UUID toUserId
) {
}