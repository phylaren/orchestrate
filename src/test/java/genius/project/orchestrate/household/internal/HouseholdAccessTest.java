package genius.project.orchestrate.household.internal;

import genius.project.orchestrate.household.MembershipPermission;
import genius.project.orchestrate.household.client.HouseholdClient;
import genius.project.orchestrate.user.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @DisplayName("Право перевіряється для користувача з UserPrincipal")
    void hasPermission_DelegatesToHouseholdClient() {
        when(householdClient.hasPermission(HOUSEHOLD_ID, USER_ID, MembershipPermission.MANAGE_MEMBERS))
                .thenReturn(true);

        assertThat(householdAccess.hasPermission(authenticated(), HOUSEHOLD_ID, "MANAGE_MEMBERS")).isTrue();
    }

    @Test
    @DisplayName("Відсутність права в домі — false")
    void hasPermission_MissingPermission_ReturnsFalse() {
        when(householdClient.hasPermission(HOUSEHOLD_ID, USER_ID, MembershipPermission.MANAGE_MEMBERS))
                .thenReturn(false);

        assertThat(householdAccess.hasPermission(authenticated(), HOUSEHOLD_ID, "MANAGE_MEMBERS")).isFalse();
    }

    @Test
    @DisplayName("Автентифікація без UserPrincipal — false без звернення до домогосподарств")
    void hasPermission_ForeignPrincipal_ReturnsFalse() {
        Authentication foreign = new TestingAuthenticationToken("someone", null, "ROLE_USER");

        assertThat(householdAccess.hasPermission(foreign, HOUSEHOLD_ID, "MANAGE_MEMBERS")).isFalse();
        verify(householdClient, never()).hasPermission(any(), any(), any());
    }

    @Test
    @DisplayName("Невідома назва права в SpEL-виразі — помилка конфігурації, а не тиха відмова")
    void hasPermission_UnknownPermission_Throws() {
        assertThatThrownBy(() -> householdAccess.hasPermission(authenticated(), HOUSEHOLD_ID, "FLY"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Authentication authenticated() {
        UserPrincipal principal = new UserPrincipal(USER_ID, "user@orchestrate.test", "User", "hash", true);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }
}
