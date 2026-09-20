package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.ParticipantJoinRequest;
import genius.project.orchestrate.chore.dto.ParticipantResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChoreParticipantController.class)
class ChoreParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private ChoreParticipantService choreParticipantService;

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private static ParticipantResponse participantResponse(boolean addedByAdmin) {
        return new ParticipantResponse(CHORE_ID, USER_ID, addedByAdmin, Instant.now());
    }

    @Test
    @DisplayName("POST .../participants додає учасника і повертає 201 Created")
    void join_ReturnsCreated() throws Exception {
        ParticipantJoinRequest request = new ParticipantJoinRequest(USER_ID);
        when(choreParticipantService.joinChore(CHORE_ID, USER_ID, false))
                .thenReturn(participantResponse(false));

        mockMvc.perform(post("/api/v1/chores/{choreId}/participants", CHORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.choreId").value(CHORE_ID.toString()))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.addedByAdmin").value(false));

        verify(choreParticipantService).joinChore(CHORE_ID, USER_ID, false);
    }

    @Test
    @DisplayName("POST .../participants без userId повертає 400 Bad Request")
    void join_WithMissingUserId_ReturnsBadRequest() throws Exception {
        ParticipantJoinRequest request = new ParticipantJoinRequest(null);

        mockMvc.perform(post("/api/v1/chores/{choreId}/participants", CHORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET .../participants повертає список учасників")
    void list_ReturnsParticipants() throws Exception {
        when(choreParticipantService.listParticipants(CHORE_ID))
                .thenReturn(List.of(participantResponse(true)));

        mockMvc.perform(get("/api/v1/chores/{choreId}/participants", CHORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$[0].addedByAdmin").value(true));
    }

    @Test
    @DisplayName("DELETE .../participants/{userId} видаляє учасника і повертає 204 No Content")
    void leave_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/chores/{choreId}/participants/{userId}", CHORE_ID, USER_ID))
                .andExpect(status().isNoContent());

        verify(choreParticipantService).leaveChore(CHORE_ID, USER_ID);
    }
}