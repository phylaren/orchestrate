package genius.project.orchestrate.identity;

import java.util.UUID;

public interface CurrentUserProvider {
    UUID getUserId();
}