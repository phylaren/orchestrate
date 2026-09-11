package genius.project.orchestrate.identity.internal;

import genius.project.orchestrate.identity.CurrentUserProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// TODO: replace with SecurityContextCurrentUserProvider when Spring Security is wired
@Primary
@Component
public class StubCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Long getMembershipId() {
        return 1L;
    }
}