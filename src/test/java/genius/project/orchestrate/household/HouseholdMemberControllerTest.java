package genius.project.orchestrate.household;

import genius.project.orchestrate.household.exception.MissingPermissionException;
import genius.project.orchestrate.household.exception.OwnerMustTransferOwnershipException;
import genius.project.orchestrate.household.internal.domain.Membership;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HouseholdMemberController.class)
class HouseholdMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HouseholdService householdService;

    private static final UUID HOUSEHOLD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MEMBER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Test
    @DisplayName("GET /members повертає учасників з правами")
    void list_Returns200() throws Exception {
        when(householdService.listMembers(HOUSEHOLD_ID)).thenReturn(List.of(
                new Membership(HOUSEHOLD_ID, MEMBER_ID, Set.of(MembershipPermission.CONFIRM_COMPLETIONS), Instant.now())));

        mockMvc.perform(get("/api/v1/households/{id}/members", HOUSEHOLD_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(MEMBER_ID.toString()))
                .andExpect(jsonPath("$[0].permissions[0]").value("CONFIRM_COMPLETIONS"));
    }

    @Test
    @DisplayName("DELETE /members/{userId} повертає 204")
    void remove_Returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/households/{id}/members/{userId}", HOUSEHOLD_ID, MEMBER_ID))
                .andExpect(status().isNoContent());
        verify(householdService).removeMember(HOUSEHOLD_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("DELETE /members/{ownerId} власником з іншими учасниками повертає 409 OWNER_MUST_TRANSFER_OWNERSHIP")
    void ownerLeave_Returns409() throws Exception {
        doThrow(new OwnerMustTransferOwnershipException(HOUSEHOLD_ID))
                .when(householdService).removeMember(HOUSEHOLD_ID, OWNER_ID);

        mockMvc.perform(delete("/api/v1/households/{id}/members/{userId}", HOUSEHOLD_ID, OWNER_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OWNER_MUST_TRANSFER_OWNERSHIP"));
    }

    @Test
    @DisplayName("PUT /members/{userId}/permissions оновлює права")
    void updatePermissions_Returns200() throws Exception {
        Set<MembershipPermission> permissions = EnumSet.of(MembershipPermission.MANAGE_CHORES, MembershipPermission.INVITE_MEMBERS);
        when(householdService.updatePermissions(HOUSEHOLD_ID, MEMBER_ID, permissions))
                .thenReturn(new Membership(HOUSEHOLD_ID, MEMBER_ID, permissions, Instant.now()));

        mockMvc.perform(put("/api/v1/households/{id}/members/{userId}/permissions", HOUSEHOLD_ID, MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissions":["MANAGE_CHORES","INVITE_MEMBERS"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.length()").value(2));
    }

    @Test
    @DisplayName("PUT /permissions з невідомим правом повертає 400 MALFORMED_REQUEST_BODY")
    void updatePermissions_UnknownPermission_Returns400() throws Exception {
        mockMvc.perform(put("/api/v1/households/{id}/members/{userId}/permissions", HOUSEHOLD_ID, MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissions":["BE_GOD"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));
        verify(householdService, never()).updatePermissions(any(), any(), any());
    }

    @Test
    @DisplayName("PUT /permissions без поля permissions повертає 400 VALIDATION_FAILED")
    void updatePermissions_Missing_Returns400() throws Exception {
        mockMvc.perform(put("/api/v1/households/{id}/members/{userId}/permissions", HOUSEHOLD_ID, MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("PUT /permissions без MANAGE_MEMBERS повертає 403 MISSING_PERMISSION")
    void updatePermissions_NoPermission_Returns403() throws Exception {
        when(householdService.updatePermissions(eq(HOUSEHOLD_ID), eq(MEMBER_ID), any()))
                .thenThrow(new MissingPermissionException(HOUSEHOLD_ID, OWNER_ID, MembershipPermission.MANAGE_MEMBERS));

        mockMvc.perform(put("/api/v1/households/{id}/members/{userId}/permissions", HOUSEHOLD_ID, MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissions":[]}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MISSING_PERMISSION"));
    }
}
