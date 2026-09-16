package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.ChoreCreateRequest;
import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChoreController.class)
class ChoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private ChoreService choreService;

    private static final UUID HOUSEHOLD_ID = UUID.randomUUID();
    private static final UUID CHORE_ID = UUID.randomUUID();

    @Test
    @DisplayName("POST /api/v1/chores з валідними даними повертає 201 Created")
    void createChore_WithValidData_ReturnsCreated() throws Exception {
        ChoreCreateRequest request = new ChoreCreateRequest(
                HOUSEHOLD_ID, "Прибирання", "Помити підлогу", 7, true);

        Chore chore = new Chore(
                CHORE_ID, HOUSEHOLD_ID, "Прибирання", "Помити підлогу", 7, true, Instant.now());

        when(choreService.createChore(HOUSEHOLD_ID, "Прибирання", "Помити підлогу", 7, true))
                .thenReturn(chore);

        mockMvc.perform(post("/api/v1/chores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(CHORE_ID.toString()))
                .andExpect(jsonPath("$.householdId").value(HOUSEHOLD_ID.toString()))
                .andExpect(jsonPath("$.name").value("Прибирання"))
                .andExpect(jsonPath("$.recurrenceDays").value(7))
                .andExpect(jsonPath("$.requiresConfirmation").value(true))
                // при створенні контролер завжди передає needsAttention = true
                .andExpect(jsonPath("$.needsAttention").value(true));

        verify(choreService).createChore(HOUSEHOLD_ID, "Прибирання", "Помити підлогу", 7, true);
    }

    @Test
    @DisplayName("POST /api/v1/chores без обов'язкового householdId повертає 400 Bad Request")
    void createChore_WithMissingHouseholdId_ReturnsBadRequest() throws Exception {
        ChoreCreateRequest invalidRequest = new ChoreCreateRequest(
                null, "Прибирання", "Помити підлогу", 7, true);

        mockMvc.perform(post("/api/v1/chores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/chores з пустим name повертає 400 Bad Request")
    void createChore_WithBlankName_ReturnsBadRequest() throws Exception {
        ChoreCreateRequest invalidRequest = new ChoreCreateRequest(
                HOUSEHOLD_ID, "", "Помити підлогу", 7, true);

        mockMvc.perform(post("/api/v1/chores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/chores?householdId= повертає список Chore з прапорцем needsAttention")
    void listChores_ReturnsChoresForHousehold() throws Exception {
        Chore chore = new Chore(
                CHORE_ID, HOUSEHOLD_ID, "Прибирання", "Помити підлогу", 7, true, Instant.now());

        when(choreService.listChores(HOUSEHOLD_ID)).thenReturn(List.of(chore));
        when(choreService.needsAttention(CHORE_ID)).thenReturn(false);

        mockMvc.perform(get("/api/v1/chores").param("householdId", HOUSEHOLD_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CHORE_ID.toString()))
                .andExpect(jsonPath("$[0].needsAttention").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/chores/{choreId} повертає конкретний Chore")
    void getChore_WithExistingId_ReturnsChore() throws Exception {
        Chore chore = new Chore(
                CHORE_ID, HOUSEHOLD_ID, "Прибирання", "Помити підлогу", 7, true, Instant.now());

        when(choreService.getChore(CHORE_ID)).thenReturn(chore);
        when(choreService.needsAttention(CHORE_ID)).thenReturn(true);

        mockMvc.perform(get("/api/v1/chores/{choreId}", CHORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CHORE_ID.toString()))
                .andExpect(jsonPath("$.needsAttention").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/chores/{choreId} для неіснуючого Chore повертає 404 Not Found")
    void getChore_WithMissingId_ReturnsNotFound() throws Exception {
        when(choreService.getChore(any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("CHORE_NOT_FOUND", "Chore not found"));

        mockMvc.perform(get("/api/v1/chores/{choreId}", CHORE_ID))
                .andExpect(status().isNotFound());
    }
}