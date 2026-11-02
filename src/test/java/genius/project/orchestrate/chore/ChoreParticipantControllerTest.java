package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.ParticipantResponse;
import genius.project.orchestrate.household.internal.HouseholdAccess;
import genius.project.orchestrate.identity.JwtValidatorService;
import genius.project.orchestrate.identity.WithUserPrincipal;
import genius.project.orchestrate.identity.internal.SecurityWebMvcTestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChoreParticipantController.class)
@Import({SecurityWebMvcTestConfiguration.class, HouseholdAccess.class})
class ChoreParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChoreParticipantService choreParticipantService;

    @MockitoBean
    private genius.project.orchestrate.household.client.HouseholdClient householdClient;

    @MockitoBean
    private JwtValidatorService jwtValidatorService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final UUID HOUSEHOLD_ID = UUID.randomUUID();
    private static final UUID CHORE_ID = UUID.randomUUID();
    // Має збігатися з дефолтним ID в @WithUserPrincipal
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    private static final String BASE_URL = "/api/v1/households/{householdId}/chores/{choreId}/participants";
    private static final String USER_URL = BASE_URL + "/{userId}";

    private static ParticipantResponse participantResponse(UUID userId, boolean addedByAdmin) {
        return new ParticipantResponse(CHORE_ID, userId, addedByAdmin, Instant.now());
    }

    @Test
    @WithUserPrincipal
    @DisplayName("POST join adds the current user as themselves and returns 201 Created")
    void join_ReturnsCreated() throws Exception {
        when(choreParticipantService.joinChore(CHORE_ID))
                .thenReturn(participantResponse(USER_ID, false));

        mockMvc.perform(post(BASE_URL, HOUSEHOLD_ID, CHORE_ID).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.choreId").value(CHORE_ID.toString()))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.addedByAdmin").value(false));

        verify(choreParticipantService).joinChore(CHORE_ID);
    }

    @Test
    @WithUserPrincipal
    @DisplayName("PUT addParticipant returns 403 without MANAGE_CHORES")
    void addParticipant_WithoutPermission_Returns403() throws Exception {
        when(householdClient.hasPermission(any(), any(), any())).thenReturn(false);

        mockMvc.perform(put(USER_URL, HOUSEHOLD_ID, CHORE_ID, OTHER_USER_ID).with(csrf()))
                .andExpect(status().isForbidden());

        verify(choreParticipantService, org.mockito.Mockito.never()).addParticipant(any(), any());
    }

    @Test
    @WithUserPrincipal
    @DisplayName("PUT addParticipant succeeds with MANAGE_CHORES")
    void addParticipant_WithPermission_ReturnsCreated() throws Exception {
        when(householdClient.hasPermission(eq(HOUSEHOLD_ID), any(), eq(genius.project.orchestrate.household.MembershipPermission.MANAGE_CHORES)))
                .thenReturn(true);
        when(choreParticipantService.addParticipant(CHORE_ID, OTHER_USER_ID))
                .thenReturn(participantResponse(OTHER_USER_ID, true));

        mockMvc.perform(put(USER_URL, HOUSEHOLD_ID, CHORE_ID, OTHER_USER_ID).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(OTHER_USER_ID.toString()))
                .andExpect(jsonPath("$.addedByAdmin").value(true));

        verify(choreParticipantService).addParticipant(CHORE_ID, OTHER_USER_ID);
    }

    @Test
    @WithUserPrincipal
    @DisplayName("GET list returns participants")
    void list_ReturnsParticipants() throws Exception {
        when(choreParticipantService.listParticipants(CHORE_ID))
                .thenReturn(List.of(participantResponse(OTHER_USER_ID, true)));

        mockMvc.perform(get(BASE_URL, HOUSEHOLD_ID, CHORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(OTHER_USER_ID.toString()));
    }

    @Test
    @WithUserPrincipal
    @DisplayName("DELETE remove succeeds for self-leave")
    void remove_Self_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete(USER_URL, HOUSEHOLD_ID, CHORE_ID, USER_ID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(choreParticipantService).removeMember(CHORE_ID, USER_ID);
    }

    @Test
    @WithUserPrincipal
    @DisplayName("DELETE remove other user returns 403 without MANAGE_CHORES")
    void remove_OtherWithoutPermission_Returns403() throws Exception {
        when(householdClient.hasPermission(any(), any(), any())).thenReturn(false);

        mockMvc.perform(delete(USER_URL, HOUSEHOLD_ID, CHORE_ID, OTHER_USER_ID).with(csrf()))
                .andExpect(status().isForbidden());

        verify(choreParticipantService, org.mockito.Mockito.never()).removeMember(any(), any());
    }

    @Test
    @WithUserPrincipal
    @DisplayName("DELETE remove other user succeeds with MANAGE_CHORES")
    void remove_OtherWithPermission_ReturnsNoContent() throws Exception {
        when(householdClient.hasPermission(eq(HOUSEHOLD_ID), any(), eq(genius.project.orchestrate.household.MembershipPermission.MANAGE_CHORES)))
                .thenReturn(true);

        mockMvc.perform(delete(USER_URL, HOUSEHOLD_ID, CHORE_ID, OTHER_USER_ID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(choreParticipantService).removeMember(CHORE_ID, OTHER_USER_ID);
    }
}