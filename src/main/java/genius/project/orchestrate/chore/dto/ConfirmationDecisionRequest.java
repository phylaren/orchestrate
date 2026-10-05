package genius.project.orchestrate.chore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Рішення підтверджувача щодо виконання обов'язку")
public record ConfirmationDecisionRequest(
        @Schema(description = "true — підтвердити виконання, false — відхилити", example = "true")
        @NotNull(message = "approved is required")
        Boolean approved
) {
}
