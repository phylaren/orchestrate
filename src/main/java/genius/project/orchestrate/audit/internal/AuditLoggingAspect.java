package genius.project.orchestrate.audit.internal;

import genius.project.orchestrate.audit.AuditAction;
import genius.project.orchestrate.audit.AuditLogService;
import genius.project.orchestrate.audit.AuditRecord;
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

    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;

    AuditLoggingAspect(AuditLogService auditLogService, CurrentUserProvider currentUserProvider) {
        this.auditLogService = auditLogService;
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
                    ? String.valueOf(result) : null;

            AuditRecord record = new AuditRecord(
                    userId,
                    auditAction.action(),
                    methodName,
                    arguments,
                    resultText,
                    failure == null,
                    failure != null ? failure.getClass().getName() : null,
                    failure != null && auditAction.logParameters() ? failure.getMessage() : null,
                    occurredAt,
                    durationMs);

            auditLogService.record(record);
        } catch (RuntimeException ex) {
            log.error("Could not store audit record for action={}", auditAction.action(), ex);
        }
    }

    private UUID resolveUserId() {
        try {
            return currentUserProvider.getUserId();
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    private static String formatArguments(MethodSignature signature, Object[] args) {
        if (args.length == 0) return null;
        String[] names = signature.getParameterNames();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) text.append(", ");
            text.append(names != null && i < names.length ? names[i] : "arg" + i)
                    .append('=')
                    .append(args[i]);
        }
        return text.toString();
    }
}
