package genius.project.orchestrate.user.client;

import java.util.UUID;

public interface UserClient {

    boolean existsById(UUID userId);
}
