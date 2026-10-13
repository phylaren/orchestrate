package genius.project.orchestrate.identity.internal;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailSecurityHandlersTest {

    private final SecurityProblemWriter problemWriter = new SecurityProblemWriter(JsonMapper.builder().build());
    private final ProblemDetailAuthenticationEntryPoint entryPoint =
            new ProblemDetailAuthenticationEntryPoint(problemWriter);
    private final ProblemDetailAccessDeniedHandler accessDeniedHandler =
            new ProblemDetailAccessDeniedHandler(problemWriter);

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    @DisplayName("Відсутня автентифікація: 401, AUTHENTICATION_REQUIRED і WWW-Authenticate: Bearer")
    void entryPoint_MissingAuthentication() throws Exception {
        entryPoint.commence(request, response, new InsufficientAuthenticationException("Full authentication required"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        String body = response.getContentAsString();
        assertThat(JsonPath.<String>read(body, "$.code")).isEqualTo("AUTHENTICATION_REQUIRED");
        assertThat(JsonPath.<String>read(body, "$.type"))
                .isEqualTo("https://orchestrate.example.com/problems/authentication-required");
        assertThat(JsonPath.<String>read(body, "$.instance")).isEqualTo("/api/v1/users");
        assertThat(JsonPath.<Integer>read(body, "$.status")).isEqualTo(401);
    }

    @Test
    @DisplayName("Недійсний токен: 401, INVALID_TOKEN і error=\"invalid_token\" за RFC 6750")
    void entryPoint_InvalidToken() throws Exception {
        entryPoint.commence(request, response, new BadCredentialsException("Invalid or expired JWT"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).contains("error=\"invalid_token\"");
        assertThat(JsonPath.<String>read(response.getContentAsString(), "$.code")).isEqualTo("INVALID_TOKEN");
    }

    @Test
    @DisplayName("Відповідь не розкриває текст внутрішнього винятку")
    void entryPoint_DoesNotLeakExceptionMessage() throws Exception {
        entryPoint.commence(request, response, new BadCredentialsException("signature mismatch for key 42"));

        assertThat(response.getContentAsString()).doesNotContain("signature mismatch");
    }

    @Test
    @DisplayName("Бракує прав: 403, ACCESS_DENIED у форматі ProblemDetail")
    void accessDeniedHandler_Returns403() throws Exception {
        accessDeniedHandler.handle(request, response, new AccessDeniedException("Access Denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String body = response.getContentAsString();
        assertThat(JsonPath.<String>read(body, "$.code")).isEqualTo("ACCESS_DENIED");
        assertThat(JsonPath.<String>read(body, "$.title")).isEqualTo("Access denied");
        assertThat(JsonPath.<String>read(body, "$.timestamp")).isNotBlank();
    }
}
