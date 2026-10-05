package genius.project.starter.notification;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Єдиний формат сповіщення, спільний для всіх модулів і всіх каналів доставки.
 *
 * @param source     хто надсилає (значення {@code orchestrate.notification.source})
 * @param topic      машинний ідентифікатор події, напр. {@code chore.turn-swap-requested}
 * @param message    людський текст сповіщення
 * @param details    додаткові атрибути події
 * @param occurredAt момент створення сповіщення
 */
public record Notification(
        String source,
        String topic,
        String message,
        Map<String, Object> details,
        Instant occurredAt
) {

    public Notification {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
