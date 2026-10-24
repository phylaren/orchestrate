package genius.project.orchestrate.identity;

import java.util.Optional;
import java.util.UUID;

public interface UserRoles {

    Optional<UserRole> roleOf(UUID userId);
}
