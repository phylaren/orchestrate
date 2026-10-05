package genius.project.starter.notification;

import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Канал {@code webhook}: надсилає сповіщення як JSON методом POST на зовнішній URL.
 * Типовий вибір для prod-середовища.
 */
public class WebhookNotificationSender implements NotificationSender {

    private final RestClient restClient;
    private final URI url;

    public WebhookNotificationSender(RestClient restClient, URI url) {
        this.restClient = restClient;
        this.url = url;
    }

    @Override
    public void send(Notification notification) {
        restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(notification)
                .retrieve()
                .toBodilessEntity();
    }
}
