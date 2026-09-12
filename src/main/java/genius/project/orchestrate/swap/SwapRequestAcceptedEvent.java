package genius.project.orchestrate.swap;

import java.util.UUID;

// TODO: an event listener must be implemented in the chore module to consume this
//  event and apply the swap to the rotation (via ChoreClient.swapTurns). Currently
//  the event is published after a swap request is accepted, but nothing handles it,
//  so the exchange is never applied. This is outside the swap module's responsibility.
public record SwapRequestAcceptedEvent(
        UUID choreId,
        UUID initiatorUserId,
        UUID receiverUserId
) {}