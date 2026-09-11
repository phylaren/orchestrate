package genius.project.orchestrate.swap.dto;

import genius.project.orchestrate.swap.SwapRequestStatus;
import java.time.LocalDateTime;

public record SwapRequestResponse(
        Long id,
        Long choreId,
        Long initiatorMembershipId,
        Long receiverMembershipId,
        SwapRequestStatus status,
        LocalDateTime createdAt
) {}