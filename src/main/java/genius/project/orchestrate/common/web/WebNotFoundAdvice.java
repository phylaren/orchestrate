package genius.project.orchestrate.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;

/**
 * Неіснуючу сторінку браузер має побачити як HTML-шаблон {@code error/404}, а API лишається
 * на ProblemDetail. Загальний {@code GlobalExceptionHandler} інакше віддавав би JSON і для сторінок.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class WebNotFoundAdvice {

    static final String API_PREFIX = "/api/";

    @ExceptionHandler(NoResourceFoundException.class)
    Object handleMissingResource(NoResourceFoundException ex, HttpServletRequest request) {
        if (request.getRequestURI().startsWith(API_PREFIX)) {
            return apiNotFound(ex, request);
        }
        ModelAndView view = new ModelAndView("error/404");
        view.setStatus(HttpStatus.NOT_FOUND);
        return view;
    }

    private static ResponseEntity<ProblemDetail> apiNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                "No endpoint or resource matches the request.");
        body.setTitle("Resource not found");
        body.setType(URI.create("https://orchestrate.example.com/problems/resource-not-found"));
        body.setInstance(URI.create(request.getRequestURI()));
        body.setProperty("timestamp", Instant.now());
        body.setProperty("code", "RESOURCE_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
