package genius.project.orchestrate.household;

import genius.project.orchestrate.household.client.HouseholdClient;
import genius.project.orchestrate.household.internal.HouseholdAccess;
import genius.project.orchestrate.identity.TokenAuthentication;
import genius.project.orchestrate.user.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HouseholdAccessTest {

    private static final UUID HOUSEHOLD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID USER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private final HouseholdClient householdClient = mock(HouseholdClient.class);
    private final HouseholdAccess householdAccess = new HouseholdAccess(householdClient);

    @Test
    @DisplayName("Гілка з токеном: права беруться з claims, БД не чіпаємо")
    void hasPermission_TokenAuthentication_ReadsFromClaims() {
        UserPrincipal principal = new UserPrincipal(USER_ID, "user@orchestrate.test", "User", "hash", true);
        Map<String, Object> claims = Map.of(
                "households", Map.of(HOUSEHOLD_ID.toString(), List.of("MANAGE_MEMBERS"))
        );
        Authentication auth = new TokenAuthentication(principal, claims, null);

        assertThat(householdAccess.hasPermission(auth, HOUSEHOLD_ID, "MANAGE_MEMBERS")).isTrue();
        assertThat(householdAccess.hasPermission(auth, HOUSEHOLD_ID, "MANAGE_CHORES")).isFalse();
        verify(householdClient, never()).hasPermission(any(), any(), any());
    }

    @Test
    @DisplayName("Fallback: звичайна автентифікація (сесія) йде в HouseholdClient")
    void hasPermission_FallbackToClient() {
        UserPrincipal principal = new UserPrincipal(USER_ID, "user@orchestrate.test", "User", "hash", true);
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());

        when(householdClient.hasPermission(HOUSEHOLD_ID, USER_ID, MembershipPermission.MANAGE_MEMBERS))
                .thenReturn(true);

        assertThat(householdAccess.hasPermission(auth, HOUSEHOLD_ID, "MANAGE_MEMBERS")).isTrue();
        verify(householdClient).hasPermission(HOUSEHOLD_ID, USER_ID, MembershipPermission.MANAGE_MEMBERS);
    }

    @Test
    @DisplayName("Автентифікація без UserPrincipal — false без звернення до домогосподарств")
    void hasPermission_ForeignPrincipal_ReturnsFalse() {
        Authentication foreign = new UsernamePasswordAuthenticationToken("someone", null, List.of());

        assertThat(householdAccess.hasPermission(foreign, HOUSEHOLD_ID, "MANAGE_MEMBERS")).isFalse();
        verify(householdClient, never()).hasPermission(any(), any(), any());
    }
}