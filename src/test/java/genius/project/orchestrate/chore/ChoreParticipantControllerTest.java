package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.ParticipantResponse;
import genius.project.orchestrate.identity.CurrentUserProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChoreParticipantController.class)
class ChoreParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChoreParticipantService choreParticipantService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID CURRENT_USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    private static ParticipantResponse participantResponse(UUID userId, boolean addedByAdmin) {
        return new ParticipantResponse(CHORE_ID, userId, addedByAdmin, Instant.now());
    }

    // ---------- POST (self-join) ----------

    @Test
    @DisplayName("POST .../participants додає поточного користувача і повертає 201 Created")
    void join_ReturnsCreated() throws Exception {
        when(currentUserProvider.getUserId()).thenReturn(CURRENT_USER_ID);
        when(choreParticipantService.joinChore(CHORE_ID, CURRENT_USER_ID, false))
                .thenReturn(participantResponse(CURRENT_USER_ID, false));

        mockMvc.perform(post("/api/v1/chores/{choreId}/participants", CHORE_ID))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.choreId").value(CHORE_ID.toString()))
                .andExpect(jsonPath("$.userId").value(CURRENT_USER_ID.toString()))
                .andExpect(jsonPath("$.addedByAdmin").value(false));

        verify(choreParticipantService).joinChore(CHORE_ID, CURRENT_USER_ID, false);
    }

    // ---------- PUT (admin-add) ----------

    @Test
    @DisplayName("PUT .../participants/{userId} додає вказаного учасника і повертає 201 Created")
    void addParticipant_ReturnsCreated() throws Exception {
        when(choreParticipantService.joinChore(CHORE_ID, OTHER_USER_ID, true))
                .thenReturn(participantResponse(OTHER_USER_ID, true));

        mockMvc.perform(put("/api/v1/chores/{choreId}/participants/{userId}", CHORE_ID, OTHER_USER_ID))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.choreId").value(CHORE_ID.toString()))
                .andExpect(jsonPath("$.userId").value(OTHER_USER_ID.toString()))
                .andExpect(jsonPath("$.addedByAdmin").value(true));

        verify(choreParticipantService).joinChore(CHORE_ID, OTHER_USER_ID, true);
    }

    // ---------- GET ----------

    @Test
    @DisplayName("GET .../participants повертає список учасників")
    void list_ReturnsParticipants() throws Exception {
        when(choreParticipantService.listParticipants(CHORE_ID))
                .thenReturn(List.of(participantResponse(OTHER_USER_ID, true)));

        mockMvc.perform(get("/api/v1/chores/{choreId}/participants", CHORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(OTHER_USER_ID.toString()))
                .andExpect(jsonPath("$[0].addedByAdmin").value(true));
    }

    // ---------- DELETE ----------

    @Test
    @DisplayName("DELETE .../participants/{userId} видаляє учасника і повертає 204 No Content")
    void leave_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/chores/{choreId}/participants/{userId}", CHORE_ID, OTHER_USER_ID))
                .andExpect(status().isNoContent());

        verify(choreParticipantService).leaveChore(CHORE_ID, OTHER_USER_ID);
    }
}