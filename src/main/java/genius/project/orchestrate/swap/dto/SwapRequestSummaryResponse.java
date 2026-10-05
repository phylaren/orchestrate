package genius.project.orchestrate.swap.dto;

import genius.project.orchestrate.swap.SwapRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.UUID;

@Schema(description = "Зведення запитів на обмін за статусами")
public record SwapRequestSummaryResponse(
        @Schema(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID choreId,

        @Schema(description = "Загальна кількість запитів", example = "3")
        long total,

        @Schema(description = "Кількість запитів за статусами", example = "{\"PENDING\": 1, \"ACCEPTED\": 2}")
        Map<SwapRequestStatus, Long> byStatus
) {
}
