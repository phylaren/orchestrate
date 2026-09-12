package genius.project.orchestrate.chore.dto;

import genius.project.orchestrate.chore.validation.ValidRecurrenceDays;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChoreCreateRequest(

        @NotNull(message = "householdId is required")
        UUID householdId,

        @NotBlank(message = "name must not be blank")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,

        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description,

        @ValidRecurrenceDays
        Integer recurrenceDays,

        @NotNull(message = "requiresConfirmation is required")
        Boolean requiresConfirmation
) {
}
