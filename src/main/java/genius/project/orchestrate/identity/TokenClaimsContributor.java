package genius.project.orchestrate.identity;

import java.util.Map;
import java.util.UUID;

public interface TokenClaimsContributor {

    Map<String, Object> claimsFor(UUID userId);
}