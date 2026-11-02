package genius.project.orchestrate.identity;

import genius.project.orchestrate.user.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class TokenAuthentication extends UsernamePasswordAuthenticationToken {

    private final Map<UUID, Set<String>> householdPermissions;

    public TokenAuthentication(UserPrincipal principal, Map<String, Object> claims, Object details) {
        super(principal, null, principal.getAuthorities());
        setDetails(details);
        this.householdPermissions = extractPermissions(claims);
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, Set<String>> extractPermissions(Map<String, Object> claims) {
        Object rawHouseholds = claims.get("households");
        if (!(rawHouseholds instanceof Map<?, ?> householdsMap)) {
            return Collections.emptyMap();
        }

        return householdsMap.entrySet().stream()
                .filter(e -> e.getKey() instanceof String && e.getValue() instanceof Iterable)
                .collect(Collectors.toMap(
                        e -> UUID.fromString((String) e.getKey()),
                        e -> {
                            Iterable<String> perms = (Iterable<String>) e.getValue();
                            return StreamSupport.stream(perms.spliterator(), false)
                                    .collect(Collectors.toSet());
                        }
                ));
    }

    public Set<String> permissionsIn(UUID householdId) {
        return householdPermissions.getOrDefault(householdId, Collections.emptySet());
    }
}