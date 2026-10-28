package genius.project.orchestrate.chore.web;

import java.util.UUID;

public record ChoreBoardRow(
        UUID id,
        String name,
        boolean needsAttention,
        boolean joined,
        boolean responsible
) {
}
