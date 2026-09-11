package genius.project.orchestrate.swap.internal.domain;

import genius.project.orchestrate.swap.SwapRequestStatus;

import java.time.LocalDateTime;

public record SwapRequest(
        Long id,
        Long choreId,
        Long initiatorMembershipId,
        Long receiverMembershipId,
        SwapRequestStatus status,
        LocalDateTime createdAt
) {}