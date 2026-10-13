package genius.project.orchestrate.identity.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Записує помилки безпеки у відповідь у форматі RFC 9457 (application/problem+json).
 * Формат збігається з тілом помилок {@code GlobalExceptionHandler}: type, title, status, detail,
 * instance, timestamp і стабільне поле code. Потрібен тому, що помилки фільтрів безпеки виникають
 * до DispatcherServlet і не доходять до @RestControllerAdvice.
 */
@Component
class SecurityProblemWriter {

    private static final String PROBLEM_TYPE_BASE = "https://orchestrate.example.com/problems/";

    private final JsonMapper jsonMapper;

    SecurityProblemWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    void write(HttpServletRequest request, HttpServletResponse response,
               HttpStatus status, String title, String detail, String code) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(PROBLEM_TYPE_BASE + slug(title)));
        problem.setInstance(URI.create(request.getRequestURI()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", problem.getType().toString());
        body.put("title", problem.getTitle());
        body.put("status", problem.getStatus());
        body.put("detail", problem.getDetail());
        body.put("instance", problem.getInstance().toString());
        body.put("timestamp", Instant.now().toString());
        body.put("code", code);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getWriter(), body);
    }

    private String slug(String title) {
        return title.toLowerCase().replace(" ", "-");
    }
}
