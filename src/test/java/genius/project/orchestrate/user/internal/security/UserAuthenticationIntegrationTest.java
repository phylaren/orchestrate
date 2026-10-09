package genius.project.orchestrate.user.internal.security;

import genius.project.orchestrate.user.UserPrincipal;
import genius.project.orchestrate.user.UserService;
import genius.project.orchestrate.user.internal.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserAuthenticationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("зареєстрований користувач завантажується за email, пароль звіряється з Argon2id-хешем")
    void registeredUserCanBeLoadedAndVerified() {
        User created = userService.createUser("Анна", "Login@Example.com", "plain-pass-1");

        UserDetails details = userDetailsService.loadUserByUsername("login@example.com");

        assertThat(details).isInstanceOf(UserPrincipal.class);
        assertThat(((UserPrincipal) details).getId()).isEqualTo(created.id());
        assertThat(details.getPassword()).startsWith("$argon2id$");
        assertThat(passwordEncoder.matches("plain-pass-1", details.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("not-the-password", details.getPassword())).isFalse();
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("невідомий email — UsernameNotFoundException")
    void unknownUser() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}