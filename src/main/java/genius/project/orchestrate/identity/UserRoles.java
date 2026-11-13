package genius.project.orchestrate.identity;

import genius.project.orchestrate.user.UserRole;

import java.util.Optional;
import java.util.UUID;

public interface UserRoles {

    Optional<UserRole> roleOf(UUID userId);
}
