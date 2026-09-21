package genius.project.orchestrate.swap.dto;

import genius.project.orchestrate.chore.SwapType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SwapRequestRequest(
        @NotNull UUID receiverUserId,
        @NotNull SwapType swapType,
        Integer cycleNumber
) {}