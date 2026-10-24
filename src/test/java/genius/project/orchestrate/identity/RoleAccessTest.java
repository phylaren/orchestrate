package genius.project.orchestrate.identity;

import genius.project.orchestrate.user.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleAccessTest {

    private static final UUID USER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private final UserRoles userRoles = mock(UserRoles.class);
    private final RoleAccess roleAccess = new RoleAccess(userRoles);

    @Test
    @DisplayName("Користувач із роллю ADMIN отримує доступ до ролі ADMIN")
    void hasRole_Admin_ReturnsTrue() {
        when(userRoles.roleOf(USER_ID)).thenReturn(Optional.of(UserRole.ADMIN));

        assertThat(roleAccess.hasRole(authenticated(), "ADMIN")).isTrue();
    }

    @Test
    @DisplayName("Звичайний користувач не має ролі ADMIN")
    void hasRole_User_ReturnsFalseForAdmin() {
        when(userRoles.roleOf(USER_ID)).thenReturn(Optional.of(UserRole.USER));

        assertThat(roleAccess.hasRole(authenticated(), "ADMIN")).isFalse();
    }

    @Test
    @DisplayName("Анонімний запит не має жодної ролі і не звертається до сховища")
    void hasRole_Anonymous_ReturnsFalseWithoutLookup() {
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        assertThat(roleAccess.hasRole(anonymous, "ADMIN")).isFalse();
        verify(userRoles, never()).roleOf(any());
    }

    private Authentication authenticated() {
        UserPrincipal principal = new UserPrincipal(USER_ID, "user@orchestrate.test", "User", "hash", true);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }
}
