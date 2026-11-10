package genius.project.orchestrate.integration;

import genius.project.orchestrate.support.E2ETest;
import genius.project.orchestrate.support.RequestInfoTestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@E2ETest
@Import(RequestInfoTestConfiguration.class)
@TestPropertySource(properties = {
        "server.forward-headers-strategy=native",
        "server.tomcat.remoteip.internal-proxies=127[.]0[.]0[.]1|0:0:0:0:0:0:0:1|::1"
})
class ForwardedHeadersE2EIT {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    @DisplayName("довірений проксі: реальна IP клієнта і схема https беруться з X-Forwarded-*")
    void trustedProxy_ForwardedHeadersAreApplied() throws Exception {
        HttpResponse<String> response = get("X-Forwarded-For", "203.0.113.7", "X-Forwarded-Proto", "https");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("remoteAddr=203.0.113.7")
                .contains("scheme=https")
                .contains("secure=true");
    }

    @Test
    @DisplayName("без X-Forwarded-*: звичайний HTTP-запит, адреса — безпосереднього клієнта")
    void withoutForwardedHeaders_RequestStaysPlainHttp() throws Exception {
        HttpResponse<String> response = get();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("remoteAddr=127.0.0.1")
                .contains("scheme=http")
                .contains("secure=false");
    }

    private HttpResponse<String> get(String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + RequestInfoTestConfiguration.PATH));
        if (headers.length > 0) {
            request.headers(headers);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
