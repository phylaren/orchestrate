package genius.project.orchestrate.notification.internal;

import genius.project.orchestrate.chore.TurnSwapRequestedEvent;
import genius.project.starter.notification.NotificationPublisher;
import java.util.Map;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class ChoreNotificationListener {

    static final String TURN_SWAP_REQUESTED = "chore.turn-swap-requested";

    private final NotificationPublisher notificationPublisher;

    ChoreNotificationListener(NotificationPublisher notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    @ApplicationModuleListener
    void on(TurnSwapRequestedEvent event) {
        notificationPublisher.publish(
                TURN_SWAP_REQUESTED,
                "Turn swap accepted for chore " + event.choreId(),
                Map.of(
                        "choreId", event.choreId(),
                        "fromUserId", event.fromUserId(),
                        "toUserId", event.toUserId(),
                        "swapType", event.swapType(),
                        "cycleNumber", String.valueOf(event.cycleNumber())));
    }
}
