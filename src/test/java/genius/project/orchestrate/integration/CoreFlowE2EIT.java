package genius.project.orchestrate.integration;

import com.jayway.jsonpath.JsonPath;
import genius.project.orchestrate.support.E2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@E2ETest
class CoreFlowE2EIT {

    @Autowired
    RestTestClient client;

    @Autowired
    JdbcClient jdbc;

    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void setUp() {
        createUser("Alice E2E", "alice.e2e@example.com");
        createUser("Bob E2E", "bob.e2e@example.com");
        aliceToken = login("alice.e2e@example.com");
        bobToken = login("bob.e2e@example.com");
    }

    private void createUser(String name, String email) {
        client.post().uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"displayName":"%s","email":"%s","password":"correct-horse-battery"}
                        """.formatted(name, email))
                .exchange()
                .expectStatus().isCreated();
    }

    private String login(String email) {
        String body = client.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"username":"%s","password":"correct-horse-battery"}
                        """.formatted(email))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        return JsonPath.read(body, "$.accessToken");
    }

    @Test
    @DisplayName("2.3 E2E: основний сценарій від HTTP до БД")
    void fullE2E_Scenario() {
        String householdBody = client.post().uri("/api/v1/households")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name":"E2E Дім"}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody();
        UUID householdId = UUID.fromString(JsonPath.read(householdBody, "$.id"));

        String codeBody = client.post().uri("/api/v1/households/{id}/invitation-code", householdId)
                .header("Authorization", "Bearer " + aliceToken)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody();
        String inviteCode = JsonPath.read(codeBody, "$.code");

        client.post().uri("/api/v1/households/join")
                .header("Authorization", "Bearer " + bobToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"code":"%s"}
                        """.formatted(inviteCode))
                .exchange()
                .expectStatus().isCreated();

        String choreBody = client.post().uri("/api/v1/chores")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"householdId":"%s","name":"E2E Обов'язок","recurrenceDays":1,"requiresConfirmation":true}
                        """.formatted(householdId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody();
        UUID choreId = UUID.fromString(JsonPath.read(choreBody, "$.id"));

        client.post().uri("/api/v1/chores/{id}/participants", choreId)
                .header("Authorization", "Bearer " + aliceToken)
                .exchange()
                .expectStatus().isCreated();

        client.post().uri("/api/v1/chores/{id}/participants", choreId)
                .header("Authorization", "Bearer " + bobToken)
                .exchange()
                .expectStatus().isCreated();

        String completionBody = client.post().uri("/api/v1/chores/{id}/completions", choreId)
                .header("Authorization", "Bearer " + aliceToken)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody();
        UUID completionId = UUID.fromString(JsonPath.read(completionBody, "$.id"));

        client.post().uri("/api/v1/chores/{c}/completions/{cid}/confirmation", choreId, completionId)
                .header("Authorization", "Bearer " + bobToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"approved":true}
                        """)
                .exchange()
                .expectStatus().isOk();

        assertThat(jdbc.sql("select count(*) from households where id = ?")
                .param(householdId).query(Long.class).single()).isEqualTo(1);

        String status = jdbc.sql("select status from chore_completions where id = ?")
                .param(completionId).query(String.class).single();
        assertThat(status).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("2.3 E2E: 403 без токена та з токеном без прав")
    void e2e_Forbidden() {
        client.get().uri("/api/v1/users/me")
                .exchange()
                .expectStatus().isUnauthorized();

        String householdBody = client.post().uri("/api/v1/households")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name":"E2E 403"}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody();
        UUID householdId = UUID.fromString(JsonPath.read(householdBody, "$.id"));

        client.post().uri("/api/v1/chores")
                .header("Authorization", "Bearer " + bobToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"householdId":"%s","name":"Заборонено","recurrenceDays":1,"requiresConfirmation":false}
                        """.formatted(householdId))
                .exchange()
                .expectStatus().isForbidden();
    }
}