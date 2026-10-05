package genius.project.orchestrate.notification;

import genius.project.starter.notification.LoggingNotificationSender;
import genius.project.starter.notification.NotificationProperties;
import genius.project.starter.notification.NotificationSender;
import genius.project.starter.notification.WebhookNotificationSender;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "ORCHESTRATE_NOTIFICATION_WEBHOOK_URL=https://hooks.example.test/orchestrate")
@ActiveProfiles("prod")
class ProdProfileNotificationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private NotificationProperties properties;

    @Test
    @DisplayName("Профіль prod: сповіщення надсилаються на webhook з URL зі змінної оточення")
    void prodProfile_UsesWebhookChannel() {
        assertThat(context.getBean(NotificationSender.class)).isInstanceOf(WebhookNotificationSender.class);
        assertThat(context.getBeansOfType(LoggingNotificationSender.class)).isEmpty();
        assertThat(properties.getWebhook().getUrl()).isEqualTo(URI.create("https://hooks.example.test/orchestrate"));
        assertThat(properties.getSource()).isEqualTo("orchestrate-prod");
        assertThat(properties.isIncludeDetails()).isFalse();
    }

    @Test
    @DisplayName("Профіль prod перевизначає базові налаштування з application.properties")
    void prodProfile_OverridesBaseProperties() {
        assertThat(context.getEnvironment().getProperty("spring.jpa.show-sql")).isEqualTo("false");
        assertThat(context.getEnvironment().getProperty("spring.h2.console.enabled")).isEqualTo("false");
        // Не перевизначене в prod — береться з application.properties.
        assertThat(context.getEnvironment().getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }
}
