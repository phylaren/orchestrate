package genius.project.orchestrate.chore.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmationDecisionRequest(
        @NotNull(message = "confirmedByUserId is required")
        UUID confirmedByUserId,

        @NotNull(message = "approved is required")
        Boolean approved
) {
}
