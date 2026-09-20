package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.CompletionCreateRequest;
import genius.project.orchestrate.chore.dto.ConfirmationDecisionRequest;
import genius.project.orchestrate.chore.exception.InvalidConfirmationStatusException;
import genius.project.orchestrate.chore.internal.domain.ChoreCompletion;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChoreCompletionController.class)
class ChoreCompletionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private ChoreService choreService;

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID COMPLETION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONFIRMER_ID = UUID.randomUUID();

    @Test
    @DisplayName("POST .../completions позначає Chore виконаним і повертає 201 Created")
    void markCompleted_ReturnsCreated() throws Exception {
        CompletionCreateRequest request = new CompletionCreateRequest(USER_ID);

        ChoreCompletion completion = new ChoreCompletion(
                COMPLETION_ID, CHORE_ID, USER_ID, Instant.now(),
                ConfirmationStatus.PENDING, null, null);

        when(choreService.markCompleted(CHORE_ID, USER_ID)).thenReturn(completion);

        mockMvc.perform(post("/api/v1/chores/{choreId}/completions", CHORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(COMPLETION_ID.toString()))
                .andExpect(jsonPath("$.choreId").value(CHORE_ID.toString()))
                .andExpect(jsonPath("$.completedByUserId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(choreService).markCompleted(CHORE_ID, USER_ID);
    }

    @Test
    @DisplayName("POST .../completions без userId повертає 400 Bad Request")
    void markCompleted_WithMissingUserId_ReturnsBadRequest() throws Exception {
        CompletionCreateRequest invalidRequest = new CompletionCreateRequest(null);

        mockMvc.perform(post("/api/v1/chores/{choreId}/completions", CHORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET .../completions повертає список підтверджень")
    void listCompletions_ReturnsList() throws Exception {
        ChoreCompletion completion = new ChoreCompletion(
                COMPLETION_ID, CHORE_ID, USER_ID, Instant.now(),
                ConfirmationStatus.CONFIRMED, CONFIRMER_ID, Instant.now());

        when(choreService.listCompletions(CHORE_ID)).thenReturn(List.of(completion));

        mockMvc.perform(get("/api/v1/chores/{choreId}/completions", CHORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(COMPLETION_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET .../completions/{completionId} повертає конкретне підтвердження")
    void getCompletion_ReturnsSingleCompletion() throws Exception {
        ChoreCompletion completion = new ChoreCompletion(
                COMPLETION_ID, CHORE_ID, USER_ID, Instant.now(),
                ConfirmationStatus.NOT_REQUIRED, null, null);

        when(choreService.getCompletion(CHORE_ID, COMPLETION_ID)).thenReturn(completion);

        mockMvc.perform(get("/api/v1/chores/{choreId}/completions/{completionId}", CHORE_ID, COMPLETION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(COMPLETION_ID.toString()))
                .andExpect(jsonPath("$.status").value("NOT_REQUIRED"));
    }

    @Test
    @DisplayName("POST .../confirmation з approved=true повертає CONFIRMED")
    void decideConfirmation_WhenApproved_ReturnsConfirmed() throws Exception {
        ConfirmationDecisionRequest request = new ConfirmationDecisionRequest(CONFIRMER_ID, true);

        ChoreCompletion confirmed = new ChoreCompletion(
                COMPLETION_ID, CHORE_ID, USER_ID, Instant.now(),
                ConfirmationStatus.CONFIRMED, CONFIRMER_ID, Instant.now());

        when(choreService.decideConfirmation(CHORE_ID, COMPLETION_ID, CONFIRMER_ID, true))
                .thenReturn(confirmed);

        mockMvc.perform(post("/api/v1/chores/{choreId}/completions/{completionId}/confirmation",
                        CHORE_ID, COMPLETION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedByUserId").value(CONFIRMER_ID.toString()));

        verify(choreService).decideConfirmation(CHORE_ID, COMPLETION_ID, CONFIRMER_ID, true);
    }

    @Test
    @DisplayName("POST .../confirmation для вже розглянутого запису повертає 422 Unprocessable Content")
    void decideConfirmation_WhenTransitionIllegal_ReturnsUnprocessableContent() throws Exception {
        ConfirmationDecisionRequest request = new ConfirmationDecisionRequest(CONFIRMER_ID, false);

        when(choreService.decideConfirmation(CHORE_ID, COMPLETION_ID, CONFIRMER_ID, false))
                .thenThrow(new InvalidConfirmationStatusException(
                        COMPLETION_ID, ConfirmationStatus.CONFIRMED, ConfirmationStatus.REJECTED));

        mockMvc.perform(post("/api/v1/chores/{choreId}/completions/{completionId}/confirmation",
                        CHORE_ID, COMPLETION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("INVALID_CONFIRMATION_STATUS"))
                .andExpect(jsonPath("$.detail").value(
                        "Illegal state transition for completion '%s' from CONFIRMED to REJECTED"
                                .formatted(COMPLETION_ID)));
    }

    @Test
    @DisplayName("POST .../confirmation без approved повертає 400 Bad Request")
    void decideConfirmation_WithMissingApproved_ReturnsBadRequest() throws Exception {
        ConfirmationDecisionRequest invalidRequest = new ConfirmationDecisionRequest(CONFIRMER_ID, null);

        mockMvc.perform(post("/api/v1/chores/{choreId}/completions/{completionId}/confirmation",
                        CHORE_ID, COMPLETION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}