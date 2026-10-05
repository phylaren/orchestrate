package genius.project.starter.notification;

/**
 * Використовується, коли {@code orchestrate.notification.enabled=false}: модулі й далі
 * можуть викликати {@link NotificationPublisher}, але сповіщення нікуди не йдуть.
 */
public class NoOpNotificationSender implements NotificationSender {

    @Override
    public void send(Notification notification) {
        // навмисно нічого не робимо
    }
}
