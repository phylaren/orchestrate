package genius.project.starter.notification;

/**
 * Канал доставки сповіщень. Застосунок може оголосити власний бін цього типу —
 * тоді стандартні канали стартера не створюються.
 */
@FunctionalInterface
public interface NotificationSender {

    void send(Notification notification);
}
