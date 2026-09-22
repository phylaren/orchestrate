package genius.project.orchestrate.household;

import genius.project.orchestrate.household.exception.InvitationCodeExpiredException;
import genius.project.orchestrate.household.exception.InvitationCodeNotFoundException;
import genius.project.orchestrate.household.internal.domain.InvitationCode;
import genius.project.orchestrate.household.internal.domain.Membership;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HouseholdInvitationController.class)
class HouseholdInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HouseholdService householdService;

    private static final UUID HOUSEHOLD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MEMBER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Test
    @DisplayName("POST /invitation-code повертає 201 з кодом і терміном дії")
    void create_Returns201() throws Exception {
        Instant now = Instant.now();
        when(householdService.createInvitationCode(HOUSEHOLD_ID))
                .thenReturn(new InvitationCode("ABCD2345", HOUSEHOLD_ID, OWNER_ID, now, now.plus(Duration.ofDays(7))));

        mockMvc.perform(post("/api/v1/households/{id}/invitation-code", HOUSEHOLD_ID))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.code").value("ABCD2345"))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @DisplayName("GET /invitation-code без активного коду повертає 404 INVITATION_CODE_NOT_FOUND")
    void get_None_Returns404() throws Exception {
        when(householdService.getInvitationCode(HOUSEHOLD_ID))
                .thenThrow(new InvitationCodeNotFoundException(HOUSEHOLD_ID));

        mockMvc.perform(get("/api/v1/households/{id}/invitation-code", HOUSEHOLD_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_CODE_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /invitation-code повертає 204")
    void revoke_Returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/households/{id}/invitation-code", HOUSEHOLD_ID))
                .andExpect(status().isNoContent());
        verify(householdService).revokeInvitationCode(HOUSEHOLD_ID);
    }

    @Test
    @DisplayName("POST /join з дійсним кодом повертає 201 і Location на членство")
    void join_Returns201() throws Exception {
        when(householdService.joinByInvitationCode("ABCD2345")).thenReturn(new Membership(
                HOUSEHOLD_ID, MEMBER_ID, MembershipPermission.defaultForNewMember(), Instant.now()));

        mockMvc.perform(post("/api/v1/households/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"ABCD2345"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        endsWith("/api/v1/households/" + HOUSEHOLD_ID + "/members/" + MEMBER_ID)))
                .andExpect(jsonPath("$.householdId").value(HOUSEHOLD_ID.toString()))
                .andExpect(jsonPath("$.permissions[0]").value("CONFIRM_COMPLETIONS"));
    }

    @Test
    @DisplayName("POST /join з простроченим кодом повертає 409 INVITATION_CODE_EXPIRED")
    void join_Expired_Returns409() throws Exception {
        when(householdService.joinByInvitationCode("OLD22222"))
                .thenThrow(new InvitationCodeExpiredException("OLD22222"));

        mockMvc.perform(post("/api/v1/households/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"OLD22222"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVITATION_CODE_EXPIRED"));
    }

    @Test
    @DisplayName("POST /join без коду повертає 400 VALIDATION_FAILED")
    void join_BlankCode_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/households/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
