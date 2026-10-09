package genius.project.orchestrate.user.internal.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserCredentialsTest {

    @Test
    @DisplayName("toString не містить хеша пароля")
    void toStringHidesHash() {
        UserCredentials credentials = new UserCredentials(
                UUID.randomUUID(), "a@b.c", "Анна", "$argon2id$secret-hash", true);

        assertThat(credentials.toString())
                .doesNotContain("$argon2id$")
                .doesNotContain("secret-hash")
                .contains("hasPassword=true");
    }
}