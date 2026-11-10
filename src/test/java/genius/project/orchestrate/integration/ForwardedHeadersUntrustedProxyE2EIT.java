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
        "server.tomcat.remoteip.internal-proxies=192[.]0[.]2[.].*"
})
class ForwardedHeadersUntrustedProxyE2EIT {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("недовірений відправник: підроблені X-Forwarded-For і X-Forwarded-Proto ігноруються")
    void untrustedProxy_ForwardedHeadersAreIgnored() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + RequestInfoTestConfiguration.PATH))
                .header("X-Forwarded-For", "203.0.113.7")
                .header("X-Forwarded-Proto", "https")
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("remoteAddr=127.0.0.1")
                .contains("scheme=http")
                .contains("secure=false")
                .doesNotContain("203.0.113.7");
    }
}
