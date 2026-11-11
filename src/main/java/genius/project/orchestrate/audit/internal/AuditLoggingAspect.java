package genius.project.orchestrate.audit.internal;

import genius.project.orchestrate.audit.AuditAction;
import genius.project.orchestrate.identity.CurrentUserProvider;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class AuditLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggingAspect.class);

    private final AuditLogWriter writer;
    private final CurrentUserProvider currentUserProvider;

    AuditLoggingAspect(AuditLogWriter writer, CurrentUserProvider currentUserProvider) {
        this.writer = writer;
        this.currentUserProvider = currentUserProvider;
    }

    @Around("@annotation(auditAction)")
    public Object audit(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        Instant occurredAt = Instant.now();
        long startNanos = System.nanoTime();
        UUID userId = resolveUserId();
        Object result = null;
        Throwable failure = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            failure = ex;
            throw ex;
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            store(joinPoint, auditAction, userId, result, failure, durationMs, occurredAt);
        }
    }

    private void store(ProceedingJoinPoint joinPoint, AuditAction auditAction, UUID userId,
                       Object result, Throwable failure, long durationMs, Instant occurredAt) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
            String arguments = auditAction.logParameters()
                    ? formatArguments(signature, joinPoint.getArgs()) : null;
            String resultText = auditAction.logResult() && result != null
                    ? abbreviate(String.valueOf(result), AuditLogEntity.RESULT_LENGTH) : null;
            String errorType = failure != null ? failure.getClass().getName() : null;
            String errorMessage = failure != null && auditAction.logParameters()
                    ? abbreviate(failure.getMessage(), AuditLogEntity.ERROR_MESSAGE_LENGTH) : null;

            writer.save(new AuditLogEntity(UUID.randomUUID(), occurredAt, userId, auditAction.action(),
                    methodName, arguments, resultText, failure == null, errorType, errorMessage, durationMs));
        } catch (RuntimeException ex) {
            log.error("Could not store audit record for action={}", auditAction.action(), ex);
        }
    }

    private UUID resolveUserId() {
        try {
            return currentUserProvider.getUserId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String formatArguments(MethodSignature signature, Object[] args) {
        if (args.length == 0) {
            return null;
        }
        String[] names = signature.getParameterNames();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(names != null && i < names.length ? names[i] : "arg" + i)
                    .append('=')
                    .append(args[i]);
        }
        return abbreviate(text.toString(), AuditLogEntity.ARGUMENTS_LENGTH);
    }

    private static String abbreviate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 3) + "...";
    }
}