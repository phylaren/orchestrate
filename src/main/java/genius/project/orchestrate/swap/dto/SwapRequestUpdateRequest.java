package genius.project.orchestrate.swap.dto;

import genius.project.orchestrate.chore.SwapType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Зміна параметрів запиту на обмін (лише ініціатор і лише поки статус PENDING)")
public record SwapRequestUpdateRequest(
        @Schema(description = "Тип обміну", example = "PERMANENT")
        @NotNull SwapType swapType,

        @Schema(description = "Номер циклу; обов'язковий для TEMPORARY, ігнорується для PERMANENT",
                example = "4", nullable = true)
        Integer cycleNumber
) {
}
