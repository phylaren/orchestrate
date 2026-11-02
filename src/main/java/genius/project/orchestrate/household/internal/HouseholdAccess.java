package genius.project.orchestrate.household.internal;

import genius.project.orchestrate.household.MembershipPermission;
import genius.project.orchestrate.household.client.HouseholdClient;
import genius.project.orchestrate.identity.TokenAuthentication;
import genius.project.orchestrate.user.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Перевірки доступу до домогосподарства для виразів SpEL в анотаціях Method Security, наприклад:
 * <pre>{@code @PreAuthorize("@householdAccess.hasPermission(authentication, #householdId, 'MANAGE_MEMBERS')")}</pre>
 * Право визначається членством поточного користувача в домі; власник має всі права.
 */
@Component("householdAccess")
public class HouseholdAccess {

    private final HouseholdClient householdClient;

    public HouseholdAccess(HouseholdClient householdClient) {
        this.householdClient = householdClient;
    }

    public boolean hasPermission(Authentication authentication, UUID householdId, String permission) {
        MembershipPermission requiredPermission = MembershipPermission.valueOf(permission);

        // Шлях 1: Stateless API (JWT). Беремо права з токена, без запиту в БД.
        if (authentication instanceof TokenAuthentication tokenAuth) {
            return tokenAuth.permissionsIn(householdId).contains(requiredPermission.name());
        }

        // Шлях 2: Fallback (веб-сесія, тести). Йдемо в БД через HouseholdClient.
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return householdClient.hasPermission(householdId, principal.getId(), requiredPermission);
    }
}