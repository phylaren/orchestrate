package genius.project.starter.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class NotificationPublisherTest {

    private static final Instant NOW = Instant.parse("2026-10-07T10:15:30Z");

    @Test
    void buildsNotificationInUnifiedFormatAndSendsIt() {
        List<Notification> sent = new ArrayList<>();
        NotificationPublisher publisher = new NotificationPublisher(
                sent::add, "orchestrate", Clock.fixed(NOW, ZoneOffset.UTC));

        Notification result = publisher.publish(
                "chore.turn-swap-requested", "Swap requested", Map.of("cycle", 3));

        assertThat(sent).containsExactly(result);
        assertThat(result.source()).isEqualTo("orchestrate");
        assertThat(result.topic()).isEqualTo("chore.turn-swap-requested");
        assertThat(result.message()).isEqualTo("Swap requested");
        assertThat(result.details()).containsEntry("cycle", 3);
        assertThat(result.occurredAt()).isEqualTo(NOW);
    }

    @Test
    void usesEmptyDetailsWhenNoneGiven() {
        NotificationPublisher publisher = new NotificationPublisher(
                notification -> { }, "orchestrate", Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(publisher.publish("topic", "message").details()).isEmpty();
    }
}
