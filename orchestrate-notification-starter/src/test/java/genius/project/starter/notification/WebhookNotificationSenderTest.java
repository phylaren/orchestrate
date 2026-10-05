package genius.project.starter.notification;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WebhookNotificationSenderTest {

    private static final URI URL = URI.create("https://hooks.example.test/orchestrate");

    @Test
    void postsNotificationAsJson() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WebhookNotificationSender sender = new WebhookNotificationSender(builder.build(), URL);

        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.source").value("orchestrate"))
                .andExpect(jsonPath("$.topic").value("chore.turn-swap-requested"))
                .andExpect(jsonPath("$.message").value("Swap requested"))
                .andExpect(jsonPath("$.details.cycle").value(3))
                .andRespond(withSuccess());

        sender.send(new Notification(
                "orchestrate", "chore.turn-swap-requested", "Swap requested",
                Map.of("cycle", 3), Instant.parse("2026-10-07T10:15:30Z")));

        server.verify();
    }
}
