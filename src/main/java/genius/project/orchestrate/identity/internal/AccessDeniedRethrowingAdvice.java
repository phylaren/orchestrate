package genius.project.orchestrate.identity.internal;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Відмова {@code @PreAuthorize} кидає {@link AccessDeniedException} усередині контролера, і без цієї
 * поради її перехопив би загальний обробник {@code @ExceptionHandler(Exception.class)} із відповіддю 500.
 * Порада має найвищий пріоритет і повторно кидає виняток, тож його обробляє
 * {@code ExceptionTranslationFilter}: 401 для анонімного користувача, 403 для автентифікованого.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class AccessDeniedRethrowingAdvice {

    @ExceptionHandler(AccessDeniedException.class)
    void rethrow(AccessDeniedException ex) {
        throw ex;
    }
}
