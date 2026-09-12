package genius.project.orchestrate.swap.internal.domain;

import genius.project.orchestrate.swap.SwapRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SwapRequest(
        UUID id,
        UUID choreId,
        UUID initiatorUserId,
        UUID receiverUserId,
        SwapRequestStatus status,
        LocalDateTime createdAt
) {}