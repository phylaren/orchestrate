package genius.project.starter.notification;

import java.time.Clock;
import java.util.Map;

/**
 * Точка входу для модулів застосунку: збирає {@link Notification} у єдиному форматі
 * і передає його активному {@link NotificationSender}.
 */
public class NotificationPublisher {

    private final NotificationSender sender;
    private final String source;
    private final Clock clock;

    public NotificationPublisher(NotificationSender sender, String source, Clock clock) {
        this.sender = sender;
        this.source = source;
        this.clock = clock;
    }

    public Notification publish(String topic, String message) {
        return publish(topic, message, Map.of());
    }

    public Notification publish(String topic, String message, Map<String, ?> details) {
        Notification notification = new Notification(
                source, topic, message, Map.copyOf(details), clock.instant());
        sender.send(notification);
        return notification;
    }
}
