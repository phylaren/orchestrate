package genius.project.orchestrate.user.internal.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderConfigTest {

    private final PasswordEncoder encoder = new PasswordEncoderConfig().passwordEncoder();

    @Test
    @DisplayName("хеш має формат Argon2id і не містить відкритого пароля")
    void hashIsArgon2id() {
        String hash = encoder.encode("s3cret-Passw0rd");

        assertThat(hash).startsWith("$argon2id$");
        assertThat(hash).doesNotContain("s3cret-Passw0rd");
    }

    @Test
    @DisplayName("matches приймає правильний пароль і відхиляє неправильний")
    void matchesOnlyCorrectPassword() {
        String hash = encoder.encode("s3cret-Passw0rd");

        assertThat(encoder.matches("s3cret-Passw0rd", hash)).isTrue();
        assertThat(encoder.matches("wrong-password", hash)).isFalse();
    }

    @Test
    @DisplayName("однаковий пароль щоразу дає різний хеш (випадкова сіль)")
    void saltMakesHashesUnique() {
        String first = encoder.encode("same-password");
        String second = encoder.encode("same-password");

        assertThat(first).isNotEqualTo(second);
        assertThat(encoder.matches("same-password", first)).isTrue();
        assertThat(encoder.matches("same-password", second)).isTrue();
    }
}