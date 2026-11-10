package genius.project.orchestrate.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Один факт виконання аудованої операції, який аспект передає в {@link AuditLogService}.
 *
 * @param userId       поточний користувач; {@code null} для анонімного виклику
 * @param action       предметна назва операції з {@code @AuditAction}, наприклад {@code REGISTER_CHORE}
 * @param methodName   {@code Клас.метод}
 * @param arguments    аргументи у текстовому вигляді; {@code null}, якщо логування параметрів вимкнене
 * @param result       результат у текстовому вигляді; {@code null} для {@code void} або при помилці
 * @param success      {@code true}, якщо метод завершився без винятку
 * @param errorType    повна назва класу винятку; лише при {@code success = false}
 * @param errorMessage повідомлення винятку; лише при {@code success = false}
 * @param occurredAt   момент початку виклику
 * @param durationMs   тривалість виклику в мілісекундах
 */
public record AuditRecord(
        UUID userId,
        String action,
        String methodName,
        String arguments,
        String result,
        boolean success,
        String errorType,
        String errorMessage,
        Instant occurredAt,
        long durationMs) {

    public AuditRecord {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(methodName, "methodName");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public static AuditRecord success(UUID userId, String action, String methodName, String arguments,
                                      String result, Instant occurredAt, long durationMs) {
        return new AuditRecord(userId, action, methodName, arguments, result,
                true, null, null, occurredAt, durationMs);
    }

    public static AuditRecord failure(UUID userId, String action, String methodName, String arguments,
                                      Throwable error, Instant occurredAt, long durationMs) {
        Objects.requireNonNull(error, "error");
        return new AuditRecord(userId, action, methodName, arguments, null,
                false, error.getClass().getName(), error.getMessage(), occurredAt, durationMs);
    }
}
