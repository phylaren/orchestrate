package genius.project.orchestrate.household.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OwnershipTransferRequest(
        @NotNull(message = "newOwnerUserId is required")
        UUID newOwnerUserId
) {
}
