package genius.project.orchestrate.chore.dto;

import genius.project.orchestrate.chore.validation.ValidRecurrenceDays;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Дані для створення побутового обов'язку")
public record ChoreCreateRequest(

        @Schema(description = "Домогосподарство, якому належить обов'язок",
                example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
        @NotNull(message = "householdId is required")
        UUID householdId,

        @Schema(description = "Назва обов'язку", example = "Винести сміття", maxLength = 120)
        @NotBlank(message = "name must not be blank")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,

        @Schema(description = "Опис обов'язку", example = "Винести пакети з кухні до баків у дворі до 20:00",
                maxLength = 1000, nullable = true)
        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description,

        @Schema(description = "Інтервал повторення в днях", example = "3", minimum = "1", maximum = "365")
        @ValidRecurrenceDays
        Integer recurrenceDays,

        @Schema(description = "Чи потребує виконання підтвердження іншим учасником", example = "true")
        @NotNull(message = "requiresConfirmation is required")
        Boolean requiresConfirmation
) {
}
