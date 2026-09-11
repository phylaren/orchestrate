package genius.project.orchestrate.swap;

public record SwapRequestAcceptedEvent(
        Long choreId,
        Long initiatorMembershipId,
        Long receiverMembershipId
) {}