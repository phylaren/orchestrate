package genius.project.orchestrate.identity;

import genius.project.orchestrate.user.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Перевірка ролі для виразів SpEL: {@code @PreAuthorize}, {@code sec:authorize} у шаблонах.
 * Роль читається з таблиці users при кожному виклику, тож зміна ролі діє без нового входу.
 */
@Component("roleAccess")
public class RoleAccess {

    private final UserRoles userRoles;

    public RoleAccess(UserRoles userRoles) {
        this.userRoles = userRoles;
    }

    public boolean hasRole(Authentication authentication, String role) {
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return userRoles.roleOf(principal.getId())
                .map(userRole -> userRole.name().equals(role))
                .orElse(false);
    }
}
