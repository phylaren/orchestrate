package genius.project.orchestrate.chore.web;

import genius.project.orchestrate.chore.ChoreLifecycleService;
import genius.project.orchestrate.common.web.WebErrorAdvice;
import genius.project.orchestrate.common.web.WebViewConfiguration;
import genius.project.orchestrate.household.MembershipPermission;
import genius.project.orchestrate.household.client.HouseholdClient;
import genius.project.orchestrate.household.internal.HouseholdAccess;
import genius.project.orchestrate.identity.JwtValidatorService;
import genius.project.orchestrate.identity.RoleAccess;
import genius.project.orchestrate.identity.UserRoles;
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

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChoreWebController.class)
@Import({SecurityWebMvcTestConfiguration.class, WebViewConfiguration.class, WebErrorAdvice.class,
        RoleAccess.class, HouseholdAccess.class})
class ChoreCreateFormAccessTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID HOUSEHOLD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String NEW_CHORE_URL = "/households/" + HOUSEHOLD_ID + "/chores/new";

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private ChoreLifecycleService choreLifecycleService;

    @MockitoBean
    private HouseholdClient householdClient;

    @MockitoBean
    private UserRoles userRoles;

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
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("GET форми створення без права MANAGE_CHORES дає 403, навіть без POST")
    void createForm_WithoutPermission_Returns403() throws Exception {
        when(householdClient.hasPermission(HOUSEHOLD_ID, USER_ID, MembershipPermission.MANAGE_CHORES))
                .thenReturn(false);

        mockMvc.perform(get(NEW_CHORE_URL)).andExpect(status().isForbidden());
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("GET форми створення з правом MANAGE_CHORES відкривається")
    void createForm_WithPermission_Returns200() throws Exception {
        when(householdClient.hasPermission(HOUSEHOLD_ID, USER_ID, MembershipPermission.MANAGE_CHORES))
                .thenReturn(true);

        mockMvc.perform(get(NEW_CHORE_URL)).andExpect(status().isOk());
    }
}
