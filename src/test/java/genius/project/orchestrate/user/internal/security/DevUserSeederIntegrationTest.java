package genius.project.orchestrate.user.internal.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class DevUserSeederIntegrationTest {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("профіль dev: тестові користувачі створені, пароль dev-password-123 перевіряється")
    void devProfileSeedsLoginableUsers() {
        for (DevUserSeeder.SeedUser seedUser : DevUserSeeder.USERS) {
            UserDetails details = userDetailsService.loadUserByUsername(seedUser.email());

            assertThat(details.isEnabled()).isTrue();
            assertThat(passwordEncoder.matches("dev-password-123", details.getPassword())).isTrue();
        }
    }
}

