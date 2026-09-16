package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.internal.domain.ChoreAssignment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChoreAssignmentController.class)
class ChoreAssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChoreService choreService;

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("GET .../assignment повертає поточне призначення, якщо воно активне")
    void getCurrentAssignment_WhenActive_ReturnsAssignment() throws Exception {
        ChoreAssignment assignment = new ChoreAssignment(CHORE_ID, USER_ID, 3, Instant.now());

        when(choreService.getCurrentAssignment(CHORE_ID)).thenReturn(Optional.of(assignment));

        mockMvc.perform(get("/api/v1/chores/{choreId}/assignment", CHORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.choreId").value(CHORE_ID.toString()))
                .andExpect(jsonPath("$.currentResponsibleUserId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.cycleNumber").value(3));
    }

    @Test
    @DisplayName("GET .../assignment повертає 404, якщо ротаційна група порожня")
    void getCurrentAssignment_WhenEmpty_ReturnsNotFound() throws Exception {
        when(choreService.getCurrentAssignment(CHORE_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/chores/{choreId}/assignment", CHORE_ID))
                .andExpect(status().isNotFound());
    }
}