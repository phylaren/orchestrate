package genius.project.orchestrate.chore.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CompletionCreateRequest(
        @NotNull(message = "userId is required")
        UUID userId
) {
}
