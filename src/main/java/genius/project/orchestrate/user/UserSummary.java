package genius.project.orchestrate.user;

import java.util.UUID;

public record UserSummary(UUID id, String displayName, String email) {
}
