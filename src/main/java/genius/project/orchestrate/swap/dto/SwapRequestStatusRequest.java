package genius.project.orchestrate.swap.dto;

import genius.project.orchestrate.swap.SwapRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Відповідь отримувача на запит на обмін")
public record SwapRequestStatusRequest(
        @Schema(description = "Рішення: ACCEPTED або REJECTED (перехід із PENDING)", example = "ACCEPTED")
        @NotNull SwapRequestStatus status
) {}
