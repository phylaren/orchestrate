package genius.project.orchestrate.user.internal.security;

import genius.project.orchestrate.user.internal.UserRepository;
import genius.project.orchestrate.user.internal.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevUserSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DevUserSeeder seeder() {
        return new DevUserSeeder(userRepository, passwordEncoder, "dev-password-123");
    }

    @Test
    @DisplayName("створює трьох користувачів із хешем пароля та фіксованими id")
    void createsAllUsers() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("dev-password-123")).thenReturn("$argon2id$hash");

        seeder().seed();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(3)).register(captor.capture(), eq("$argon2id$hash"));
        assertThat(captor.getAllValues()).extracting(User::id)
                .containsExactly(
                        UUID.fromString("00000000-0000-0000-0000-0000000000d1"),
                        UUID.fromString("00000000-0000-0000-0000-0000000000d2"),
                        UUID.fromString("00000000-0000-0000-0000-0000000000d3"));
        assertThat(captor.getAllValues()).extracting(User::email)
                .containsExactly("dev.owner@orchestrate.test", "dev.admin@orchestrate.test",
                        "dev.resident@orchestrate.test");
    }

    @Test
    @DisplayName("повторний запуск ідемпотентний: наявних користувачів не чіпає")
    void skipsExistingUsers() {
        User existing = new User(UUID.randomUUID(), "x", "x@x.y", Instant.now());
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(existing));

        seeder().seed();

        verify(userRepository, never()).register(any(), any());
        verify(passwordEncoder, never()).encode(any());
    }
}
