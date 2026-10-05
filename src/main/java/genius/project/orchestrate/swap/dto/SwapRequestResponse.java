package genius.project.orchestrate.swap.dto;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.swap.SwapRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Запит на обмін чергою")
public record SwapRequestResponse(
        @Schema(description = "Ідентифікатор запиту", example = "e4b1d6a0-3c52-4f7e-8a19-6d2c9b0f5a73")
        UUID id,

        @Schema(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID choreId,

        @Schema(description = "Хто ініціював обмін", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID initiatorUserId,

        @Schema(description = "Кому адресовано запит", example = "5d1c0e7a-2b3f-4c8d-9e6a-7f8b9c0d1e2f")
        UUID receiverUserId,

        @Schema(description = "Статус запиту", example = "PENDING")
        SwapRequestStatus status,

        @Schema(description = "Тип обміну", example = "TEMPORARY")
        SwapType swapType,

        @Schema(description = "Цикл, на який діє тимчасовий обмін; null для PERMANENT",
                example = "3", nullable = true)
        Integer cycleNumber,

        @Schema(description = "Момент створення запиту", example = "2026-10-05T18:45:00")
        LocalDateTime createdAt
) {}
