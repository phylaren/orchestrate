package genius.project.orchestrate.identity.internal;

import genius.project.orchestrate.identity.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

// TODO: replace with SecurityContextCurrentUserProvider when Spring Security is wired
@Primary
@Component
public class StubCurrentUserProvider implements CurrentUserProvider {

    private static final UUID DEFAULT_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String HEADER = "X-User-Id";

    @Override
    public UUID getUserId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String header = request.getHeader(HEADER);
            if (header != null && !header.isBlank()) {
                return UUID.fromString(header);
            }
        }
        return DEFAULT_USER_ID;
    }
}