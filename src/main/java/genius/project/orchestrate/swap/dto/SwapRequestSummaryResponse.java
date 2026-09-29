package genius.project.orchestrate.swap.dto;

import genius.project.orchestrate.swap.SwapRequestStatus;

import java.util.Map;
import java.util.UUID;

public record SwapRequestSummaryResponse(
        UUID choreId,
        long total,
        Map<SwapRequestStatus, Long> byStatus
) {}
