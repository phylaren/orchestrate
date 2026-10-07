package genius.project.starter.notification;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Налаштування стартера. Значення за замовчуванням задані тут; застосунок перевизначає їх
 * у {@code application.properties}, а потім — у {@code application-dev} / {@code application-prod}.
 */
@ConfigurationProperties(prefix = "orchestrate.notification")
public class NotificationProperties {

    /** Чи вмикати сповіщення. Якщо false — створюється {@link NoOpNotificationSender}. */
    private boolean enabled = true;

    /** Канал доставки сповіщень. */
    private Channel channel = Channel.LOG;

    /** Значення поля {@code source} у кожному сповіщенні. */
    private String source = "orchestrate";

    /** Чи писати в лог атрибути {@code details} (канал {@code log}). */
    private boolean includeDetails = true;

    private final Webhook webhook = new Webhook();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isIncludeDetails() {
        return includeDetails;
    }

    public void setIncludeDetails(boolean includeDetails) {
        this.includeDetails = includeDetails;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public enum Channel {
        LOG,
        WEBHOOK
    }

    public static class Webhook {

        /** URL, на який надсилаються сповіщення. Обов'язковий для каналу {@code webhook}. */
        private URI url;

        /** Таймаут з'єднання та читання для webhook-запиту. */
        private Duration timeout = Duration.ofSeconds(5);

        public URI getUrl() {
            return url;
        }

        public void setUrl(URI url) {
            this.url = url;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }
}
