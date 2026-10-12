package genius.project.orchestrate.identity.internal;

import genius.project.orchestrate.user.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityContextCurrentUserProviderTest {

    private final SecurityContextCurrentUserProvider provider =
            new SecurityContextCurrentUserProvider();

    @BeforeEach
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsUserIdFromUserPrincipalWhenAuthenticated() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId, "alice@example.com", "Alice", "$argon2id$hash", true);
        authenticate(principal);

        assertThat(provider.getUserId()).isEqualTo(userId);
    }

    @Test
    void throwsWhenNoAuthenticationInContext() {
        assertThatThrownBy(() -> provider.getUserId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated user");
    }

    @Test
    void throwsWhenPrincipalIsNotUserPrincipal() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("anonymous", null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> provider.getUserId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated user");
    }

    @Test
    void returnsDifferentIdsForDifferentPrincipals() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        authenticate(new UserPrincipal(first, "a@example.com", "A", "$argon2id$hash", true));
        assertThat(provider.getUserId()).isEqualTo(first);

        authenticate(new UserPrincipal(second, "b@example.com", "B", "$argon2id$hash", true));
        assertThat(provider.getUserId()).isEqualTo(second);
    }

    private void authenticate(UserPrincipal principal) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}