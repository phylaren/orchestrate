package genius.project.orchestrate.user.internal.domain;

import genius.project.orchestrate.user.UserRole;

import java.util.UUID;

public record UserCredentials(
        UUID id,
        String email,
        String displayName,
        String passwordHash,
        boolean enabled,
        UserRole role
) {

    public UserCredentials(UUID id, String email, String displayName, String passwordHash, boolean enabled) {
        this(id, email, displayName, passwordHash, enabled, UserRole.USER);
    }

    public UserCredentials {
        if (role == null) {
            role = UserRole.USER;
        }
    }

    @Override
    public String toString() {
        return "UserCredentials[id=" + id + ", enabled=" + enabled + ", hasPassword=" + (passwordHash != null) +  ", role=" + role + "]\"";
    }
}