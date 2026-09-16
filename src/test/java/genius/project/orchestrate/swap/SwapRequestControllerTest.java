package genius.project.orchestrate.swap;

import genius.project.orchestrate.swap.dto.SwapRequestResponse;
import genius.project.orchestrate.swap.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SwapRequestController.class)
class SwapRequestControllerTest {

    private static final UUID CHORE_ID     = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_CHORE  = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID REQUEST_ID   = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID INITIATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RECEIVER_ID  = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SwapRequestService swapRequestService;

    // ---------- POST /swap-requests ----------

    @Test
    void should_return201_when_createSwapRequestSucceeds() throws Exception {
        SwapRequestResponse response = response(REQUEST_ID, SwapRequestStatus.PENDING);
        when(swapRequestService.createSwapRequest(eq(CHORE_ID), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/chores/{choreId}/swap-requests", CHORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUserId":"%s"}
                                """.formatted(RECEIVER_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.choreId").value(CHORE_ID.toString()))
                .andExpect(jsonPath("$.initiatorUserId").value(INITIATOR_ID.toString()))
                .andExpect(jsonPath("$.receiverUserId").value(RECEIVER_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void should_return400_when_initiatorEqualsReceiver() throws Exception {
        when(swapRequestService.createSwapRequest(eq(CHORE_ID), any()))
                .thenThrow(new InvalidSwapRequestRecipientException(INITIATOR_ID, CHORE_ID));

        mockMvc.perform(post("/api/v1/chores/{choreId}/swap-requests", CHORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUserId":"%s"}
                                """.formatted(INITIATOR_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_SWAP_REQUEST_RECIPIENT"));
    }

    @Test
    void should_return400_when_initiatorIsNotChoreParticipant() throws Exception {
        when(swapRequestService.createSwapRequest(eq(CHORE_ID), any()))
                .thenThrow(new NotChoreParticipantException(INITIATOR_ID, CHORE_ID));

        mockMvc.perform(post("/api/v1/chores/{choreId}/swap-requests", CHORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUserId":"%s"}
                                """.formatted(RECEIVER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("NOT_CHORE_PARTICIPANT"));
    }

    @Test
    void should_return400_when_receiverIsNotChoreParticipant() throws Exception {
        when(swapRequestService.createSwapRequest(eq(CHORE_ID), any()))
                .thenThrow(new NotChoreParticipantException(RECEIVER_ID, CHORE_ID));

        mockMvc.perform(post("/api/v1/chores/{choreId}/swap-requests", CHORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUserId":"%s"}
                                """.formatted(RECEIVER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("NOT_CHORE_PARTICIPANT"));
    }

    @Test
    void should_return409_when_duplicatePendingExists() throws Exception {
        when(swapRequestService.createSwapRequest(eq(CHORE_ID), any()))
                .thenThrow(new DuplicateSwapRequestException(CHORE_ID, INITIATOR_ID, RECEIVER_ID));

        mockMvc.perform(post("/api/v1/chores/{choreId}/swap-requests", CHORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUserId":"%s"}
                                """.formatted(RECEIVER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("SWAP_REQUEST_ALREADY_EXISTS"));
    }

    @Test
    void should_return400_when_receiverUserIdMissing() throws Exception {
        mockMvc.perform(post("/api/v1/chores/{choreId}/swap-requests", CHORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void should_return400_when_receiverUserIdMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/chores/{choreId}/swap-requests", CHORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUserId":"not-a-uuid"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));
    }

    // ---------- PATCH /swap-requests/{id} ----------

    @Test
    void should_return200_when_acceptSwapRequest() throws Exception {
        SwapRequestResponse response = response(REQUEST_ID, SwapRequestStatus.ACCEPTED);
        when(swapRequestService.respondToSwapRequest(eq(CHORE_ID), eq(REQUEST_ID), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/chores/{choreId}/swap-requests/{id}", CHORE_ID, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACCEPTED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void should_return200_when_rejectSwapRequest() throws Exception {
        SwapRequestResponse response = response(REQUEST_ID, SwapRequestStatus.REJECTED);
        when(swapRequestService.respondToSwapRequest(eq(CHORE_ID), eq(REQUEST_ID), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/chores/{choreId}/swap-requests/{id}", CHORE_ID, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"REJECTED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void should_return403_when_currentUserIsNotReceiver() throws Exception {
        when(swapRequestService.respondToSwapRequest(eq(CHORE_ID), eq(REQUEST_ID), any()))
                .thenThrow(new NotSwapRequestReceiverException(INITIATOR_ID, REQUEST_ID));

        mockMvc.perform(patch("/api/v1/chores/{choreId}/swap-requests/{id}", CHORE_ID, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACCEPTED"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("NOT_SWAP_REQUEST_RECEIVER"));
    }

    @Test
    void should_return404_when_requestNotFound() throws Exception {
        when(swapRequestService.respondToSwapRequest(eq(CHORE_ID), eq(REQUEST_ID), any()))
                .thenThrow(new SwapRequestNotFoundException(REQUEST_ID));

        mockMvc.perform(patch("/api/v1/chores/{choreId}/swap-requests/{id}", CHORE_ID, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACCEPTED"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("SWAP_REQUEST_NOT_FOUND"));
    }

    @Test
    void should_return404_when_requestBelongsToOtherChore() throws Exception {
        when(swapRequestService.respondToSwapRequest(eq(OTHER_CHORE), eq(REQUEST_ID), any()))
                .thenThrow(new SwapRequestNotFoundException(REQUEST_ID));

        mockMvc.perform(patch("/api/v1/chores/{choreId}/swap-requests/{id}", OTHER_CHORE, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACCEPTED"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("SWAP_REQUEST_NOT_FOUND"));
    }

    @Test
    void should_return400_when_requestAlreadyResolved() throws Exception {
        when(swapRequestService.respondToSwapRequest(eq(CHORE_ID), eq(REQUEST_ID), any()))
                .thenThrow(new InvalidSwapRequestStatusException(
                        "SwapRequest id=%s is already ACCEPTED".formatted(REQUEST_ID)));

        mockMvc.perform(patch("/api/v1/chores/{choreId}/swap-requests/{id}", CHORE_ID, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"REJECTED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_SWAP_REQUEST_STATUS"));
    }

    @Test
    void should_return400_when_statusSetBackToPending() throws Exception {
        when(swapRequestService.respondToSwapRequest(eq(CHORE_ID), eq(REQUEST_ID), any()))
                .thenThrow(new InvalidSwapRequestStatusException("Cannot set status back to PENDING"));

        mockMvc.perform(patch("/api/v1/chores/{choreId}/swap-requests/{id}", CHORE_ID, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PENDING"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_SWAP_REQUEST_STATUS"));
    }

    // ---------- GET /swap-requests ----------

    @Test
    void should_return200WithList_when_getSwapRequests() throws Exception {
        when(swapRequestService.getSwapRequests(CHORE_ID))
                .thenReturn(List.of(response(REQUEST_ID, SwapRequestStatus.PENDING)));

        mockMvc.perform(get("/api/v1/chores/{choreId}/swap-requests", CHORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    // ---------- helpers ----------

    private SwapRequestResponse response(UUID id, SwapRequestStatus status) {
        return new SwapRequestResponse(id, CHORE_ID, INITIATOR_ID, RECEIVER_ID, status, NOW);
    }
}