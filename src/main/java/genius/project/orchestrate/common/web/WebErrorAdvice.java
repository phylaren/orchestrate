package genius.project.orchestrate.common.web;

import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import genius.project.orchestrate.common.exception.ForbiddenException;
import genius.project.orchestrate.common.exception.InvalidStateTransitionException;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import genius.project.orchestrate.common.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Для веб-сторінок помилки мають бути HTML, а не JSON із {@code GlobalExceptionHandler}.
 * Порада діє лише для контролерів вебінтерфейсу, API-контролери лишаються як були.
 * Помилки доступу повторно кидаються: їх обробляє ланцюг безпеки (401 або 403).
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(basePackages = {
        "genius.project.orchestrate.chore.web",
        "genius.project.orchestrate.household.web",
        "genius.project.orchestrate.identity.web"
})
public class WebErrorAdvice {

    private static final Logger log = LoggerFactory.getLogger(WebErrorAdvice.class);

    static final String ERROR_VIEW = "error/error";

    @ExceptionHandler(ResourceNotFoundException.class)
    ModelAndView handleNotFound() {
        return pageView("error/404", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ForbiddenException.class)
    ModelAndView handleForbidden() {
        return pageView("error/403", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ValidationException.class)
    ModelAndView handleValidation(ValidationException ex) {
        return messageView(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    ModelAndView handleBusinessRule(BusinessRuleViolationException ex) {
        return messageView(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    ModelAndView handleInvalidState(InvalidStateTransitionException ex) {
        return messageView(ex.getMessage(), HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @ExceptionHandler(Exception.class)
    ModelAndView handleUnexpected(Exception ex) throws Exception {
        if (isHandledElsewhere(ex)) {
            throw ex;
        }
        log.error("Unexpected error on a web page", ex);
        return pageView("error/5xx", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Помилки доступу відповідає ланцюг безпеки, а помилки запиту Spring MVC (невірний тип параметра,
     * відсутній параметр, нечитабельне тіло) — стандартні обробники з кодом 4xx.
     */
    private static boolean isHandledElsewhere(Exception ex) {
        return ex instanceof AccessDeniedException
                || ex instanceof AuthenticationException
                || ex instanceof ErrorResponse
                || ex instanceof TypeMismatchException
                || ex instanceof ServletRequestBindingException
                || ex instanceof HttpMessageNotReadableException;
    }

    private static ModelAndView pageView(String viewName, HttpStatus status) {
        ModelAndView view = new ModelAndView(viewName);
        view.setStatus(status);
        return view;
    }

    private static ModelAndView messageView(String message, HttpStatus status) {
        ModelAndView view = new ModelAndView(ERROR_VIEW);
        view.setStatus(status);
        view.addObject("status", status.value());
        view.addObject("message", message);
        return view;
    }
}
