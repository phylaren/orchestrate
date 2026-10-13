package genius.project.orchestrate.identity.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Відповідь 403 Forbidden ({@code ACCESS_DENIED}) для автентифікованого користувача,
 * якому бракує прав: правила {@code authorizeHttpRequests} або {@code @PreAuthorize}.
 */
@Component
class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailAccessDeniedHandler.class);

    static final String ACCESS_DENIED = "ACCESS_DENIED";

    private final SecurityProblemWriter problemWriter;

    ProblemDetailAccessDeniedHandler(SecurityProblemWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Access denied on {} {}: {}",
                request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage());
        problemWriter.write(request, response, HttpStatus.FORBIDDEN, "Access denied",
                "You do not have permission to perform this operation.", ACCESS_DENIED);
    }
}
