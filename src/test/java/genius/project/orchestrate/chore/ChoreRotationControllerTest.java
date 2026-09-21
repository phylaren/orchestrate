package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.RotationScheduleResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChoreRotationController.class)
class ChoreRotationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RotationService rotationService;

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();

    @Test
    @DisplayName("GET rotation returns schedule")
    void getRotation() throws Exception {
        RotationScheduleResponse response = new RotationScheduleResponse(
                CHORE_ID, List.of(USER_A, USER_B), USER_A, 3, Instant.now());
        when(rotationService.getSchedule(CHORE_ID)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/v1/chores/{choreId}/rotation", CHORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.choreId").value(CHORE_ID.toString()))
                .andExpect(jsonPath("$.cycleNumber").value(3))
                .andExpect(jsonPath("$.order.length()").value(2));
    }

    @Test
    @DisplayName("GET rotation 404 when no schedule")
    void getRotation_NotFound() throws Exception {
        when(rotationService.getSchedule(CHORE_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/chores/{choreId}/rotation", CHORE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_ROTATION_SCHEDULE"));
    }

    @Test
    @DisplayName("POST insert returns updated schedule")
    void insert() throws Exception {
        RotationScheduleResponse response = new RotationScheduleResponse(
                CHORE_ID, List.of(USER_B, USER_A), USER_B, 1, Instant.now());
        when(rotationService.insert(eq(CHORE_ID), eq(USER_A), eq(0))).thenReturn(response);

        mockMvc.perform(post("/api/v1/chores/{choreId}/rotation/insert", CHORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","position":0}
                                """.formatted(USER_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order[0]").value(USER_B.toString()))
                .andExpect(jsonPath("$.order[1]").value(USER_A.toString()));
    }
}