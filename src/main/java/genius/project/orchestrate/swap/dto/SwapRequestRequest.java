package genius.project.orchestrate.swap.dto;

import genius.project.orchestrate.chore.SwapType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Запит на обмін чергою виконання з іншим учасником тієї самої групи")
public record SwapRequestRequest(
        @Schema(description = "Учасник групи виконання, якому адресовано запит",
                example = "5d1c0e7a-2b3f-4c8d-9e6a-7f8b9c0d1e2f")
        @NotNull UUID receiverUserId,

        @Schema(description = "Тип обміну", example = "TEMPORARY")
        @NotNull SwapType swapType,

        @Schema(description = "Номер циклу, на який діє обмін; обов'язковий для TEMPORARY "
                + "(майбутній цикл, не далі ніж на 10 вперед), ігнорується для PERMANENT",
                example = "3", nullable = true)
        Integer cycleNumber
) {}
