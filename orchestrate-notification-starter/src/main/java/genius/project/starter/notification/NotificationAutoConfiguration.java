package genius.project.starter.notification;

import java.time.Clock;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Автоконфігурація спільного модуля сповіщень.
 *
 * <ul>
 *   <li>{@code orchestrate.notification.enabled=false} → {@link NoOpNotificationSender};</li>
 *   <li>{@code orchestrate.notification.channel=log} (за замовчуванням) → {@link LoggingNotificationSender};</li>
 *   <li>{@code orchestrate.notification.channel=webhook} + spring-web у classpath → {@link WebhookNotificationSender};</li>
 *   <li>власний бін {@link NotificationSender} у застосунку → стандартні канали не створюються.</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationAutoConfiguration {

    static final String PREFIX = "orchestrate.notification";

    @Bean
    @ConditionalOnMissingBean
    NotificationPublisher notificationPublisher(NotificationSender sender, NotificationProperties properties) {
        return new NotificationPublisher(sender, properties.getSource(), Clock.systemUTC());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = PREFIX, name = "enabled", havingValue = "false")
    static class DisabledConfiguration {

        @Bean
        @ConditionalOnMissingBean(NotificationSender.class)
        NoOpNotificationSender noOpNotificationSender() {
            return new NoOpNotificationSender();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(prefix = PREFIX, name = "channel", havingValue = "log", matchIfMissing = true)
    static class LogChannelConfiguration {

        @Bean
        @ConditionalOnMissingBean(NotificationSender.class)
        LoggingNotificationSender loggingNotificationSender(NotificationProperties properties) {
            return new LoggingNotificationSender(properties.isIncludeDetails());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestClient.class)
    @ConditionalOnProperty(prefix = PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(prefix = PREFIX, name = "channel", havingValue = "webhook")
    static class WebhookChannelConfiguration {

        @Bean
        @ConditionalOnMissingBean(NotificationSender.class)
        WebhookNotificationSender webhookNotificationSender(NotificationProperties properties) {
            NotificationProperties.Webhook webhook = properties.getWebhook();
            if (webhook.getUrl() == null) {
                throw new IllegalStateException(
                        "Property '" + PREFIX + ".webhook.url' is required when "
                                + PREFIX + ".channel=webhook");
            }
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(webhook.getTimeout());
            requestFactory.setReadTimeout(webhook.getTimeout());
            RestClient restClient = RestClient.builder().requestFactory(requestFactory).build();
            return new WebhookNotificationSender(restClient, webhook.getUrl());
        }
    }
}
