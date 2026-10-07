package genius.project.orchestrate.notification;

import genius.project.starter.notification.LoggingNotificationSender;
import genius.project.starter.notification.NotificationProperties;
import genius.project.starter.notification.NotificationSender;
import genius.project.starter.notification.WebhookNotificationSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class DevProfileNotificationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private NotificationProperties properties;

    @Test
    @DisplayName("Профіль dev: сповіщення йдуть у лог разом з атрибутами")
    void devProfile_UsesLogChannel() {
        assertThat(context.getBean(NotificationSender.class)).isInstanceOf(LoggingNotificationSender.class);
        assertThat(context.getBeansOfType(WebhookNotificationSender.class)).isEmpty();
        assertThat(properties.getSource()).isEqualTo("orchestrate-dev");
        assertThat(properties.isIncludeDetails()).isTrue();
    }

    @Test
    @DisplayName("Профіль dev успадковує базові налаштування з application.properties")
    void devProfile_InheritsBaseProperties() {
        assertThat(context.getEnvironment().getProperty("spring.jpa.show-sql")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty("spring.h2.console.enabled")).isEqualTo("true");
    }
}
