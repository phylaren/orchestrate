package genius.project.orchestrate.user.internal.domain;

import java.time.Instant;
import java.util.UUID;

public record User(
    UUID id,
    String displayName,
    String email,
    Instant createdAt
) {
}
