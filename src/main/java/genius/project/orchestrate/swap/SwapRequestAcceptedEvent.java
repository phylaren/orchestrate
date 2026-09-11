package genius.project.orchestrate.swap;

import java.util.UUID;

public record SwapRequestAcceptedEvent(
        UUID choreId,
        UUID initiatorUserId,
        UUID receiverUserId
) {}