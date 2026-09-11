package genius.project.orchestrate.swap.dto;

import jakarta.validation.constraints.NotNull;

public record SwapRequestRequest(
        @NotNull Long receiverMembershipId
) {}