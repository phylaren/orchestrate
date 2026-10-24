package genius.project.orchestrate.household.web;

import genius.project.orchestrate.common.web.WebErrorAdvice;
import genius.project.orchestrate.common.web.WebViewConfiguration;
import genius.project.orchestrate.household.HouseholdService;
import genius.project.orchestrate.household.MembershipPermission;
import genius.project.orchestrate.household.internal.domain.Household;
import genius.project.orchestrate.household.internal.domain.Membership;
import genius.project.orchestrate.household.internal.domain.UserHousehold;
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

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HouseholdWebController.class)
@Import({SecurityWebMvcTestConfiguration.class, WebViewConfiguration.class, WebErrorAdvice.class, RoleAccess.class})
class HouseholdWebControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID HOUSEHOLD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private HouseholdService householdService;

    @MockitoBean
    private JwtValidatorService jwtValidatorService;

    @MockitoBean
    private UserRoles userRoles;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("Список показує домогосподарства користувача з роллю і посиланням на обов'язки")
    void list_ShowsHouseholds() throws Exception {
        Instant now = Instant.now();
        Household household = new Household(HOUSEHOLD_ID, "Квартира", USER_ID, now);
        Membership membership = new Membership(HOUSEHOLD_ID, USER_ID, MembershipPermission.all(), now);
        when(householdService.listHouseholdsOfUser(USER_ID))
                .thenReturn(List.of(new UserHousehold(household, membership)));

        mockMvc.perform(get("/households"))
                .andExpect(status().isOk())
                .andExpect(view().name("households/list"))
                .andExpect(content().string(containsString("Квартира")))
                .andExpect(content().string(containsString("Власник")))
                .andExpect(content().string(containsString("/households/" + HOUSEHOLD_ID + "/chores")));
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("Порожній список показує пояснення замість таблиці")
    void list_Empty_ShowsPlaceholder() throws Exception {
        when(householdService.listHouseholdsOfUser(any())).thenReturn(List.of());

        mockMvc.perform(get("/households"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ви ще не належите до жодного домогосподарства.")))
                .andExpect(content().string(not(containsString("<table"))));
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("Назва домогосподарства з HTML екранується, скрипт не виконується")
    void list_EscapesHouseholdName() throws Exception {
        Instant now = Instant.now();
        Household household = new Household(HOUSEHOLD_ID, "<script>alert(1)</script>", USER_ID, now);
        Membership membership = new Membership(HOUSEHOLD_ID, USER_ID, Set.of(), now);
        when(householdService.listHouseholdsOfUser(USER_ID))
                .thenReturn(List.of(new UserHousehold(household, membership)));

        mockMvc.perform(get("/households"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }

    @Test
    @DisplayName("Анонімний запит перенаправляється на сторінку входу")
    void list_Anonymous_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/households"))
                .andExpect(status().is3xxRedirection());
    }
}
