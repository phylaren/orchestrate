package genius.project.orchestrate.identity.internal;

import genius.project.orchestrate.identity.CurrentUserProvider;
import genius.project.orchestrate.user.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public UUID getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("No authenticated user in SecurityContext");
        }
        return principal.getId();
    }
}