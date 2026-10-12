package genius.project.orchestrate.identity.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

    @Test
    void toString_masksPassword() {
        LoginRequest request = new LoginRequest("alice@example.com", "correct-horse-battery");

        assertThat(request.toString())
                .doesNotContain("correct-horse-battery")
                .contains("***")
                .contains("alice@example.com");
    }
}