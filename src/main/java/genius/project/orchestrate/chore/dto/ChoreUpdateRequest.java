package genius.project.orchestrate.chore.dto;

import genius.project.orchestrate.chore.validation.ValidRecurrenceDays;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Нові значення полів обов'язку (повна заміна)")
public record ChoreUpdateRequest(

        @Schema(description = "Назва обов'язку", example = "Помити посуд", maxLength = 120)
        @NotBlank(message = "name must not be blank")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,

        @Schema(description = "Опис обов'язку", example = "Помити посуд після вечері та протерти стіл",
                maxLength = 1000, nullable = true)
        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description,

        @Schema(description = "Інтервал повторення в днях", example = "1", minimum = "1", maximum = "365")
        @ValidRecurrenceDays
        Integer recurrenceDays,

        @Schema(description = "Чи потребує виконання підтвердження іншим учасником", example = "false")
        @NotNull(message = "requiresConfirmation is required")
        Boolean requiresConfirmation
) {
}
