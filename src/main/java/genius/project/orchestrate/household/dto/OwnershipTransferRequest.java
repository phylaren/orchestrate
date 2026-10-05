package genius.project.orchestrate.household.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Передача статусу власника іншому учаснику домогосподарства")
public record OwnershipTransferRequest(
        @Schema(description = "Ідентифікатор учасника, який стане новим власником",
                example = "5d1c0e7a-2b3f-4c8d-9e6a-7f8b9c0d1e2f")
        @NotNull(message = "newOwnerUserId is required")
        UUID newOwnerUserId
) {
}
