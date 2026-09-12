package genius.project.orchestrate.swap.dto;

import genius.project.orchestrate.swap.SwapRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SwapRequestResponse(
        UUID id,
        UUID choreId,
        UUID initiatorUserId,
        UUID receiverUserId,
        SwapRequestStatus status,
        LocalDateTime createdAt
) {}