package genius.project.orchestrate.swap.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SwapRequestRequest(
        @NotNull UUID receiverUserId
) {}