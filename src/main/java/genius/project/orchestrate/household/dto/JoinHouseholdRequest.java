package genius.project.orchestrate.household.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinHouseholdRequest(
        @NotBlank(message = "code must not be blank")
        @Size(max = 32, message = "code must be at most 32 characters")
        String code
) {
}
