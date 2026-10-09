package genius.project.orchestrate.user.internal.domain;

import java.util.UUID;

public record UserCredentials(
        UUID id,
        String email,
        String displayName,
        String passwordHash,
        boolean enabled
) {
    
    @Override
    public String toString() {
        return "UserCredentials[id=" + id + ", enabled=" + enabled + ", hasPassword=" + (passwordHash != null) + "]";
    }
}