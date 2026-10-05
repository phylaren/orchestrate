package genius.project.orchestrate.notification.internal;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.TurnSwapRequestedEvent;
import genius.project.starter.notification.Notification;
import genius.project.starter.notification.NotificationPublisher;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChoreNotificationListenerTest {

    @Test
    @DisplayName("Подія обміну чергою перетворюється на сповіщення у спільному форматі")
    void on_TurnSwapRequested_PublishesNotification() {
        List<Notification> sent = new ArrayList<>();
        ChoreNotificationListener listener = new ChoreNotificationListener(
                new NotificationPublisher(sent::add, "orchestrate", Clock.systemUTC()));
        UUID choreId = UUID.randomUUID();
        UUID fromUserId = UUID.randomUUID();
        UUID toUserId = UUID.randomUUID();

        listener.on(new TurnSwapRequestedEvent(choreId, fromUserId, toUserId, SwapType.values()[0], 2));

        assertThat(sent).singleElement().satisfies(notification -> {
            assertThat(notification.topic()).isEqualTo(ChoreNotificationListener.TURN_SWAP_REQUESTED);
            assertThat(notification.source()).isEqualTo("orchestrate");
            assertThat(notification.details())
                    .containsEntry("choreId", choreId)
                    .containsEntry("fromUserId", fromUserId)
                    .containsEntry("toUserId", toUserId)
                    .containsEntry("cycleNumber", "2");
        });
    }
}
