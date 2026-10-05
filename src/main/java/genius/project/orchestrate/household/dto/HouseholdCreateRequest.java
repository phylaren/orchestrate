package genius.project.orchestrate.household.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Дані для створення домогосподарства; автор стає його власником")
public record HouseholdCreateRequest(
        @Schema(description = "Назва домогосподарства", example = "Квартира на Хрещатику", maxLength = 120)
        @NotBlank(message = "name must not be blank")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name
) {
}
