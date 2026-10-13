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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("Учасник із правом MANAGE_MEMBERS отримує 200 і зведення прав")
    void get_WithManageMembers_Returns200() throws Exception {
        Instant now = Instant.now();
        when(householdService.hasPermission(HOUSEHOLD_ID, OWNER_ID, MembershipPermission.MANAGE_MEMBERS))
                .thenReturn(true);
        when(householdService.getHousehold(HOUSEHOLD_ID))
                .thenReturn(new Household(HOUSEHOLD_ID, "Квартира", OWNER_ID, now));
        when(householdService.listMembers(HOUSEHOLD_ID)).thenReturn(List.of(
                new Membership(HOUSEHOLD_ID, OWNER_ID, MembershipPermission.all(), now),
                new Membership(HOUSEHOLD_ID, RESIDENT_ID, Set.of(MembershipPermission.CONFIRM_COMPLETIONS), now)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.householdId").value(HOUSEHOLD_ID.toString()))
                .andExpect(jsonPath("$.ownerId").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.members[1].userId").value(RESIDENT_ID.toString()))
                .andExpect(jsonPath("$.members[1].permissions[0]").value("CONFIRM_COMPLETIONS"));
    }

    @Test
    @WithUserPrincipal(id = "55555555-5555-5555-5555-555555555555")
    @DisplayName("Учасник без права MANAGE_MEMBERS отримує 403 ACCESS_DENIED від @PreAuthorize")
    void get_WithoutManageMembers_Returns403() throws Exception {
        when(householdService.hasPermission(HOUSEHOLD_ID, RESIDENT_ID, MembershipPermission.MANAGE_MEMBERS))
                .thenReturn(false);

        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.instance").value(URL));
        verify(householdService, never()).listMembers(any());
    }

    @Test
    @WithMockUser
    @DisplayName("Автентифікація без UserPrincipal не дає прав на дім — 403")
    void get_ForeignPrincipal_Returns403() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verify(householdService, never()).hasPermission(any(), any(), any());
    }
}
