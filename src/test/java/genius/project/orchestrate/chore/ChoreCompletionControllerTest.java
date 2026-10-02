package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.CompletionResponse;
import genius.project.orchestrate.chore.dto.ConfirmationDecisionRequest;
import genius.project.orchestrate.chore.exception.InvalidConfirmationStatusException;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    private ChoreCompletionService choreCompletionService;

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID COMPLETION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONFIRMER_ID = UUID.randomUUID();

    private static CompletionResponse pendingResponse() {
        return new CompletionResponse(COMPLETION_ID, CHORE_ID, USER_ID, Instant.now(),
                ConfirmationStatus.PENDING, null, null);
    }

    @Test
    @DisplayName("POST .../completions marks the chore complete and returns 201 Created")
    void markCompleted_ReturnsCreated() throws Exception {
        when(choreCompletionService.markCompleted(CHORE_ID)).thenReturn(pendingResponse());

        mockMvc.perform(post("/api/v1/chores/{choreId}/completions", CHORE_ID))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(COMPLETION_ID.toString()))
                .andExpect(jsonPath("$.choreId").value(CHORE_ID.toString()))
                .andExpect(jsonPath("$.completedByUserId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(choreCompletionService).markCompleted(CHORE_ID);
    }

    @Test
    @DisplayName("GET .../completions returns the list")
    void listCompletions_ReturnsList() throws Exception {
        CompletionResponse confirmed = new CompletionResponse(COMPLETION_ID, CHORE_ID, USER_ID, Instant.now(),
                ConfirmationStatus.CONFIRMED, CONFIRMER_ID, Instant.now());
        when(choreCompletionService.listCompletions(CHORE_ID)).thenReturn(List.of(confirmed));

        mockMvc.perform(get("/api/v1/chores/{choreId}/completions", CHORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(COMPLETION_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET .../completions/{completionId} returns the completion")
    void getCompletion_ReturnsSingleCompletion() throws Exception {
        CompletionResponse response = new CompletionResponse(COMPLETION_ID, CHORE_ID, USER_ID, Instant.now(),
                ConfirmationStatus.NOT_REQUIRED, null, null);
        when(choreCompletionService.getCompletion(CHORE_ID, COMPLETION_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/chores/{choreId}/completions/{completionId}", CHORE_ID, COMPLETION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(COMPLETION_ID.toString()))
                .andExpect(jsonPath("$.status").value("NOT_REQUIRED"));
    }

    @Test
    @DisplayName("POST .../confirmation with approved=true returns confirmed")
    void decideConfirmation_WhenApproved_ReturnsConfirmed() throws Exception {
        ConfirmationDecisionRequest request = new ConfirmationDecisionRequest(true);
        CompletionResponse confirmed = new CompletionResponse(COMPLETION_ID, CHORE_ID, USER_ID, Instant.now(),
                ConfirmationStatus.CONFIRMED, CONFIRMER_ID, Instant.now());

        when(choreCompletionService.decideConfirmation(CHORE_ID, COMPLETION_ID, true)).thenReturn(confirmed);

        mockMvc.perform(post("/api/v1/chores/{choreId}/completions/{completionId}/confirmation",
                        CHORE_ID, COMPLETION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedByUserId").value(CONFIRMER_ID.toString()));

        verify(choreCompletionService).decideConfirmation(CHORE_ID, COMPLETION_ID, true);
    }

    @Test
    @DisplayName("POST .../confirmation for an already-resolved completion returns 422")
    void decideConfirmation_WhenTransitionIllegal_ReturnsUnprocessableContent() throws Exception {
        ConfirmationDecisionRequest request = new ConfirmationDecisionRequest(false);

        when(choreCompletionService.decideConfirmation(CHORE_ID, COMPLETION_ID, false))
                .thenThrow(new InvalidConfirmationStatusException(
                        COMPLETION_ID, ConfirmationStatus.CONFIRMED, ConfirmationStatus.REJECTED));

        mockMvc.perform(post("/api/v1/chores/{choreId}/completions/{completionId}/confirmation",
                        CHORE_ID, COMPLETION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("INVALID_CONFIRMATION_STATUS"));
    }

    @Test
    @DisplayName("POST .../confirmation without approved returns 400 Bad Request")
    void decideConfirmation_WithMissingApproved_ReturnsBadRequest() throws Exception {
        ConfirmationDecisionRequest request = new ConfirmationDecisionRequest(null);

        mockMvc.perform(post("/api/v1/chores/{choreId}/completions/{completionId}/confirmation",
                        CHORE_ID, COMPLETION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE .../completions/{completionId} returns 204 No Content")
    void deleteCompletion_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/chores/{choreId}/completions/{completionId}", CHORE_ID, COMPLETION_ID))
                .andExpect(status().isNoContent());

        verify(choreCompletionService).deleteCompletion(CHORE_ID, COMPLETION_ID);
    }

    @Test
    @DisplayName("DELETE .../completions/{completionId} for a confirmed completion returns 409 Conflict")
    void deleteCompletion_WhenNotDeletable_ReturnsConflict() throws Exception {
        doThrow(new BusinessRuleViolationException("COMPLETION_NOT_DELETABLE", "cannot delete"))
                .when(choreCompletionService).deleteCompletion(CHORE_ID, COMPLETION_ID);

        mockMvc.perform(delete("/api/v1/chores/{choreId}/completions/{completionId}", CHORE_ID, COMPLETION_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPLETION_NOT_DELETABLE"));
    }
}