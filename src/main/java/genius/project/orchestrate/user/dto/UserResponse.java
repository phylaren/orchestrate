package genius.project.orchestrate.user.dto;

import genius.project.orchestrate.user.internal.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String displayName,
        String email,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.displayName(), user.email(), user.createdAt());
    }
}
