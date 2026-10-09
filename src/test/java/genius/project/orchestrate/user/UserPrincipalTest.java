package genius.project.orchestrate.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    private static final UUID ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static UserPrincipal principal(String hash, boolean enabled) {
        return new UserPrincipal(ID, "anna@example.com", "Анна", hash, enabled);
    }

    @Test
    @DisplayName("username — це email, password — хеш, є єдина authority ROLE_USER")
    void exposesCredentialsAndRole() {
        UserPrincipal principal = principal("$argon2id$hash", true);

        assertThat(principal.getUsername()).isEqualTo("anna@example.com");
        assertThat(principal.getPassword()).isEqualTo("$argon2id$hash");
        assertThat(principal.getId()).isEqualTo(ID);
        assertThat(principal.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("активний акаунт із паролем дозволений, решта прапорців true")
    void enabledWithPassword() {
        UserPrincipal principal = principal("$argon2id$hash", true);

        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("заблокований акаунт (enabled=false) вимкнений")
    void disabledFlag() {
        assertThat(principal("$argon2id$hash", false).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("акаунт без пароля вимкнений")
    void noPasswordMeansDisabled() {
        assertThat(principal(null, true).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("рівність за id; toString не містить хеша")
    void equalityAndToString() {
        assertThat(principal("a", true)).isEqualTo(principal("b", false));
        assertThat(principal("$argon2id$secret", true).toString()).doesNotContain("secret");
    }
}