package genius.project.orchestrate.chore.dto;

import jakarta.validation.constraints.NotNull;

public record ConfirmationDecisionRequest(
        @NotNull(message = "approved is required")
        Boolean approved
) {
}