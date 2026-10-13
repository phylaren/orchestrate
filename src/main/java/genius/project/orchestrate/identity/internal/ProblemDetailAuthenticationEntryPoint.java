package genius.project.orchestrate.identity.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Відповідь 401 Unauthorized для запитів без валідної автентифікації.
 * <ul>
 *     <li>{@code AUTHENTICATION_REQUIRED} — токен не передано;</li>
 *     <li>{@code INVALID_TOKEN} — токен підроблено, пошкоджено або строк його дії минув.</li>
 * </ul>
 * Заголовок {@code WWW-Authenticate} формується за RFC 6750.
 */
@Component
class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailAuthenticationEntryPoint.class);

    static final String AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED";
    static final String INVALID_TOKEN = "INVALID_TOKEN";

    private final SecurityProblemWriter problemWriter;

    ProblemDetailAuthenticationEntryPoint(SecurityProblemWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (authException instanceof InsufficientAuthenticationException) {
            log.warn("Unauthenticated request to {} {}", request.getMethod(), request.getRequestURI());
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            problemWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Authentication required",
                    "A valid Bearer access token is required to access this resource.",
                    AUTHENTICATION_REQUIRED);
            return;
        }

        log.warn("Rejected access token on {} {}: {}",
                request.getMethod(), request.getRequestURI(), authException.getMessage());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE,
                "Bearer error=\"invalid_token\", error_description=\"The access token is invalid or expired\"");
        problemWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Invalid token",
                "The access token is invalid or expired.", INVALID_TOKEN);
    }
}
