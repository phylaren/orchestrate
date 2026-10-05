package genius.project.starter.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

class NotificationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotificationAutoConfiguration.class));

    @Test
    void createsLoggingSenderByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(NotificationProperties.class);
            assertThat(context).hasSingleBean(NotificationPublisher.class);
            assertThat(context).hasSingleBean(NotificationSender.class);
            assertThat(context).hasSingleBean(LoggingNotificationSender.class);
            assertThat(context).doesNotHaveBean(WebhookNotificationSender.class);
            assertThat(context).doesNotHaveBean(NoOpNotificationSender.class);
        });
    }

    @Test
    void createsLoggingSenderWhenLogChannelIsSetExplicitly() {
        contextRunner
                .withPropertyValues("orchestrate.notification.channel=log")
                .run(context -> assertThat(context).hasSingleBean(LoggingNotificationSender.class));
    }

    @Test
    void createsWebhookSenderWhenWebhookChannelIsSelected() {
        contextRunner
                .withPropertyValues(
                        "orchestrate.notification.channel=webhook",
                        "orchestrate.notification.webhook.url=https://hooks.example.test/orchestrate")
                .run(context -> {
                    assertThat(context).hasSingleBean(WebhookNotificationSender.class);
                    assertThat(context).doesNotHaveBean(LoggingNotificationSender.class);
                    assertThat(context).hasSingleBean(NotificationPublisher.class);
                });
    }

    @Test
    void failsFastWhenWebhookChannelHasNoUrl() {
        contextRunner
                .withPropertyValues("orchestrate.notification.channel=webhook")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("orchestrate.notification.webhook.url"));
    }

    @Test
    void skipsWebhookSenderWhenSpringWebIsAbsent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(RestClient.class))
                .withPropertyValues(
                        "orchestrate.notification.channel=webhook",
                        "orchestrate.notification.webhook.url=https://hooks.example.test/orchestrate")
                // Webhook-канал не підхоплюється без spring-web, тож publisher'у бракує NotificationSender.
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasMessageContaining(NotificationSender.class.getName()));
    }

    @Test
    void createsNoOpSenderWhenNotificationsAreDisabled() {
        contextRunner
                .withPropertyValues(
                        "orchestrate.notification.enabled=false",
                        "orchestrate.notification.channel=webhook")
                .run(context -> {
                    assertThat(context).hasSingleBean(NoOpNotificationSender.class);
                    assertThat(context).doesNotHaveBean(LoggingNotificationSender.class);
                    assertThat(context).doesNotHaveBean(WebhookNotificationSender.class);
                    assertThat(context).hasSingleBean(NotificationPublisher.class);
                });
    }

    @Test
    void backsOffWhenApplicationDefinesItsOwnSender() {
        contextRunner
                .withUserConfiguration(CustomSenderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(NotificationSender.class);
                    assertThat(context).hasBean("customSender");
                    assertThat(context).doesNotHaveBean(LoggingNotificationSender.class);
                });
    }

    @Test
    void backsOffWhenApplicationDefinesItsOwnPublisher() {
        contextRunner
                .withUserConfiguration(CustomPublisherConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(NotificationPublisher.class);
                    assertThat(context).hasBean("customPublisher");
                });
    }

    @Test
    void bindsPropertiesWithDefaultsAndOverrides() {
        contextRunner.run(context -> {
            NotificationProperties properties = context.getBean(NotificationProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getChannel()).isEqualTo(NotificationProperties.Channel.LOG);
            assertThat(properties.getSource()).isEqualTo("orchestrate");
            assertThat(properties.isIncludeDetails()).isTrue();
        });

        contextRunner
                .withPropertyValues(
                        "orchestrate.notification.source=orchestrate-test",
                        "orchestrate.notification.include-details=false",
                        "orchestrate.notification.webhook.timeout=2s")
                .run(context -> {
                    NotificationProperties properties = context.getBean(NotificationProperties.class);
                    assertThat(properties.getSource()).isEqualTo("orchestrate-test");
                    assertThat(properties.isIncludeDetails()).isFalse();
                    assertThat(properties.getWebhook().getTimeout()).hasSeconds(2);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomSenderConfiguration {

        @Bean
        NotificationSender customSender() {
            return notification -> { };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomPublisherConfiguration {

        @Bean
        NotificationPublisher customPublisher() {
            return new NotificationPublisher(notification -> { }, "custom", java.time.Clock.systemUTC());
        }
    }
}
