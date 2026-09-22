package genius.project.orchestrate.chore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InsertRequest(
        @NotNull UUID userId,
        @Min(0) int position
) {}