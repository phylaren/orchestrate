package genius.project.orchestrate.swap.dto;

import genius.project.orchestrate.swap.SwapRequestStatus;
import jakarta.validation.constraints.NotNull;

public record SwapRequestStatusRequest(
        @NotNull SwapRequestStatus status
) {}