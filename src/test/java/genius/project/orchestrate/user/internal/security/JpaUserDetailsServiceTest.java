package genius.project.orchestrate.user.internal.security;

import genius.project.orchestrate.user.UserPrincipal;
import genius.project.orchestrate.user.internal.UserRepository;
import genius.project.orchestrate.user.internal.domain.UserCredentials;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserDetailsServiceTest {

    private static final UUID ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JpaUserDetailsService service;

    @Test
    @DisplayName("знайдений користувач перетворюється на UserPrincipal з хешем і роллю")
    void found_ReturnsPrincipal() {
        when(userRepository.findCredentialsByEmail("anna@example.com"))
                .thenReturn(Optional.of(new UserCredentials(ID, "anna@example.com", "Анна", "$argon2id$hash", true)));

        UserDetails details = service.loadUserByUsername("anna@example.com");

        assertThat(details).isInstanceOf(UserPrincipal.class);
        assertThat(((UserPrincipal) details).getId()).isEqualTo(ID);
        assertThat(details.getUsername()).isEqualTo("anna@example.com");
        assertThat(details.getPassword()).isEqualTo("$argon2id$hash");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("логін нормалізується: пробіли та регістр не мають значення")
    void emailIsNormalized() {
        when(userRepository.findCredentialsByEmail("anna@example.com"))
                .thenReturn(Optional.of(new UserCredentials(ID, "anna@example.com", "Анна", "$argon2id$hash", true)));

        assertThat(service.loadUserByUsername("  Anna@Example.COM ").getUsername())
                .isEqualTo("anna@example.com");
    }

    @Test
    @DisplayName("невідомий логін — UsernameNotFoundException без email у повідомленні")
    void notFound_Throws() {
        when(userRepository.findCredentialsByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageNotContaining("ghost@example.com");
    }

    @Test
    @DisplayName("порожній або null логін — UsernameNotFoundException, до БД не звертаємось")
    void blank_Throws() {
        assertThatThrownBy(() -> service.loadUserByUsername("  "))
                .isInstanceOf(UsernameNotFoundException.class);
        assertThatThrownBy(() -> service.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("акаунт без пароля або заблокований повертається як вимкнений")
    void disabledAccounts() {
        when(userRepository.findCredentialsByEmail("nopass@example.com"))
                .thenReturn(Optional.of(new UserCredentials(ID, "nopass@example.com", "Анна", null, true)));
        when(userRepository.findCredentialsByEmail("blocked@example.com"))
                .thenReturn(Optional.of(new UserCredentials(ID, "blocked@example.com", "Анна", "$argon2id$hash", false)));

        assertThat(service.loadUserByUsername("nopass@example.com").isEnabled()).isFalse();
        assertThat(service.loadUserByUsername("blocked@example.com").isEnabled()).isFalse();
    }
}