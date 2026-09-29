package genius.project.orchestrate.swap.dto;

import genius.project.orchestrate.chore.SwapType;
import jakarta.validation.constraints.NotNull;

public record SwapRequestUpdateRequest(
        @NotNull SwapType swapType,
        Integer cycleNumber
) {}
