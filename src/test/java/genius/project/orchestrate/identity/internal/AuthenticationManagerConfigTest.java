package genius.project.orchestrate.identity.internal;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationManagerConfigTest {

    private final AuthenticationManagerConfig config = new AuthenticationManagerConfig();

    @Test
    void authenticate_succeedsWhenPasswordMatches() {
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        UserDetails user = User.withUsername("alice@example.com")
                .password("hashed")
                .authorities("ROLE_USER")
                .build();
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(user);
        when(passwordEncoder.matches("plain-password", "hashed")).thenReturn(true);

        AuthenticationManager manager = config.authenticationManager(userDetailsService, passwordEncoder);

        Authentication result = manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        "alice@example.com", "plain-password"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isEqualTo(user);
    }

    @Test
    void authenticate_failsWhenPasswordDoesNotMatch() {
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        UserDetails user = User.withUsername("alice@example.com")
                .password("hashed")
                .authorities("ROLE_USER")
                .build();
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(user);
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        AuthenticationManager manager = config.authenticationManager(userDetailsService, passwordEncoder);

        assertThatThrownBy(() -> manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        "alice@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }
}