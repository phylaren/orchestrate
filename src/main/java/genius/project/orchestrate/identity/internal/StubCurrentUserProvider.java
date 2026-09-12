package genius.project.orchestrate.identity.internal;

import genius.project.orchestrate.identity.CurrentUserProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

// TODO: replace with SecurityContextCurrentUserProvider when Spring Security is wired
@Primary
@Component
public class StubCurrentUserProvider implements CurrentUserProvider {

    private static final UUID STUB_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    public UUID getUserId() {
        return STUB_USER_ID;
    }
}