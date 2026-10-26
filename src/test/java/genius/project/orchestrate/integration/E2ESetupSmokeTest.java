package genius.project.orchestrate.integration;

import genius.project.orchestrate.support.E2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;

@E2ETest
class E2ESetupSmokeTest {

    @Autowired
    RestTestClient client;

    @Test
    @DisplayName("справжній HTTP-запит без токена до захищеного ендпоінта дає 401")
    void protectedEndpoint_WithoutToken_ShouldReturn401() {
        client.get().uri("/api/v1/users/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}