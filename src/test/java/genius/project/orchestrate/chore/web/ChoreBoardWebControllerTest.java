package genius.project.orchestrate.chore.web;

import genius.project.orchestrate.chore.ChoreCompletionService;
import genius.project.orchestrate.chore.ChoreLifecycleService;
import genius.project.orchestrate.chore.ChoreParticipantService;
import genius.project.orchestrate.chore.dto.ChoreResponse;
import genius.project.orchestrate.chore.dto.ParticipantResponse;
import genius.project.orchestrate.common.web.WebErrorAdvice;
import genius.project.orchestrate.common.web.WebViewConfiguration;
import genius.project.orchestrate.household.HouseholdService;
import genius.project.orchestrate.household.MembershipPermission;
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
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChoreBoardWebController.class)
@Import({SecurityWebMvcTestConfiguration.class, WebViewConfiguration.class, WebErrorAdvice.class, RoleAccess.class})
class ChoreBoardWebControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID HOUSEHOLD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID OTHER_HOUSEHOLD_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID CHORE_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private ChoreLifecycleService choreLifecycleService;

    @MockitoBean
    private ChoreParticipantService choreParticipantService;

    @MockitoBean
    private ChoreCompletionService choreCompletionService;

    @MockitoBean
    private HouseholdService householdService;

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
        when(householdService.isMember(HOUSEHOLD_ID, USER_ID)).thenReturn(true);
        when(choreLifecycleService.listChores(HOUSEHOLD_ID)).thenReturn(List.of(chore(HOUSEHOLD_ID)));
        when(choreParticipantService.listParticipants(CHORE_ID)).thenReturn(List.of());
        when(choreParticipantService.getCurrentAssignment(CHORE_ID)).thenReturn(Optional.empty());
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("Учасник з правом MANAGE_CHORES бачить кнопку створення обов'язку")
    void board_WithManageChores_ShowsCreateButton() throws Exception {
        when(householdService.hasPermission(HOUSEHOLD_ID, USER_ID, MembershipPermission.MANAGE_CHORES))
                .thenReturn(true);

        mockMvc.perform(get("/households/{id}/board", HOUSEHOLD_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Додати обов’язок")))
                .andExpect(content().string(containsString("Приєднатися")));
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("Учасник без права MANAGE_CHORES не бачить кнопки створення")
    void board_WithoutManageChores_HidesCreateButton() throws Exception {
        when(householdService.hasPermission(HOUSEHOLD_ID, USER_ID, MembershipPermission.MANAGE_CHORES))
                .thenReturn(false);

        mockMvc.perform(get("/households/{id}/board", HOUSEHOLD_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Додати обов’язок"))));
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("Не учасник не бачить дошку: 403")
    void board_NotMember_Returns403() throws Exception {
        when(householdService.isMember(HOUSEHOLD_ID, USER_ID)).thenReturn(false);

        mockMvc.perform(get("/households/{id}/board", HOUSEHOLD_ID))
                .andExpect(status().isForbidden());
        verify(choreLifecycleService, never()).listChores(any());
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("Приєднання виконує сервіс і повертає на дошку")
    void join_Member_JoinsAndRedirects() throws Exception {
        when(choreLifecycleService.getChore(CHORE_ID)).thenReturn(chore(HOUSEHOLD_ID));
        when(choreParticipantService.joinChore(CHORE_ID)).thenReturn(new ParticipantResponse(CHORE_ID, USER_ID, false, Instant.now()));

        mockMvc.perform(post("/households/{h}/chores/{c}/join", HOUSEHOLD_ID, CHORE_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/households/" + HOUSEHOLD_ID + "/board"));
        verify(choreParticipantService).joinChore(CHORE_ID);
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("Обов'язок із чужого дому не можна відмітити: 404, сервіс не викликається")
    void complete_ChoreFromOtherHousehold_Returns404() throws Exception {
        when(choreLifecycleService.getChore(CHORE_ID)).thenReturn(chore(OTHER_HOUSEHOLD_ID));

        mockMvc.perform(post("/households/{h}/chores/{c}/completions", HOUSEHOLD_ID, CHORE_ID).with(csrf()))
                .andExpect(status().isNotFound());
        verify(choreCompletionService, never()).markCompleted(any());
    }

    private static ChoreResponse chore(UUID householdId) {
        return new ChoreResponse(CHORE_ID, householdId, "Винести сміття", null, 7, false, true, Instant.now());
    }
}
