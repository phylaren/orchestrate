package genius.project.orchestrate.household;

import genius.project.orchestrate.household.internal.HouseholdAccess;
import genius.project.orchestrate.household.internal.domain.Household;
import genius.project.orchestrate.household.internal.domain.Membership;
import genius.project.orchestrate.identity.JwtValidatorService;
import genius.project.orchestrate.identity.WithUserPrincipal;
import genius.project.orchestrate.identity.internal.SecurityWebMvcTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HouseholdPermissionsController.class)
@Import({SecurityWebMvcTestConfiguration.class, HouseholdAccess.class})
class HouseholdPermissionsControllerTest {

    private static final UUID HOUSEHOLD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RESIDENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String URL = "/api/v1/households/" + HOUSEHOLD_ID + "/members/permissions";

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private HouseholdService householdService;

    @MockitoBean
    private JwtValidatorService jwtValidatorService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Без токена — 401, метод контролера не викликається")
    void get_Anonymous_Returns401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        verify(householdService, never()).listMembers(any());
    }

    @Test
    @WithUserPrincipal(
            id = "00000000-0000-0000-0000-000000000001",
            households = "44444444-4444-4444-4444-444444444444:MANAGE_MEMBERS")
    @DisplayName("Токен із MANAGE_MEMBERS → 200, HouseholdClient не викликається")
    void get_WithManageMembersInToken_Returns200WithoutDbLookup() throws Exception {
        Instant now = Instant.now();
        when(householdService.getHousehold(HOUSEHOLD_ID))
                .thenReturn(new Household(HOUSEHOLD_ID, "Квартира", OWNER_ID, now));
        when(householdService.listMembers(HOUSEHOLD_ID)).thenReturn(List.of(
                new Membership(HOUSEHOLD_ID, OWNER_ID, MembershipPermission.all(), now),
                new Membership(HOUSEHOLD_ID, RESIDENT_ID, Set.of(MembershipPermission.CONFIRM_COMPLETIONS), now)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.householdId").value(HOUSEHOLD_ID.toString()))
                .andExpect(jsonPath("$.members.length()").value(2));

        verify(householdService, never()).hasPermission(any(), any(), any());
    }

    @Test
    @WithUserPrincipal(
            id = "55555555-5555-5555-5555-555555555555",
            households = "44444444-4444-4444-4444-444444444444:CONFIRM_COMPLETIONS")
    @DisplayName("Токен без MANAGE_MEMBERS → 403 ACCESS_DENIED")
    void get_WithoutManageMembersInToken_Returns403() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verify(householdService, never()).listMembers(any());
    }
}