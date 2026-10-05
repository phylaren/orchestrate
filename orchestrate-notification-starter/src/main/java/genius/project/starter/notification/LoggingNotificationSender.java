package genius.project.starter.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Канал {@code log}: пише сповіщення в лог застосунку. Типовий вибір для dev-середовища.
 */
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    private final boolean includeDetails;

    public LoggingNotificationSender(boolean includeDetails) {
        this.includeDetails = includeDetails;
    }

    @Override
    public void send(Notification notification) {
        if (includeDetails) {
            log.info("[{}] {}: {} {}", notification.source(), notification.topic(),
                    notification.message(), notification.details());
        } else {
            log.info("[{}] {}: {}", notification.source(), notification.topic(), notification.message());
        }
    }
}
