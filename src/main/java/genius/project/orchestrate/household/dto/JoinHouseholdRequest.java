package genius.project.orchestrate.household.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запит на приєднання до домогосподарства за кодом запрошення")
public record JoinHouseholdRequest(
        @Schema(description = "Код запрошення", example = "K7M2QX9P", maxLength = 32)
        @NotBlank(message = "code must not be blank")
        @Size(max = 32, message = "code must be at most 32 characters")
        String code
) {
}
