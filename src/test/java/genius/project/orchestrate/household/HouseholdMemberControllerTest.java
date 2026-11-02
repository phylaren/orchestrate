package genius.project.orchestrate.household;

import genius.project.orchestrate.household.internal.HouseholdAccess;
import genius.project.orchestrate.identity.JwtValidatorService;
import genius.project.orchestrate.identity.WithUserPrincipal;
import genius.project.orchestrate.identity.internal.SecurityWebMvcTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HouseholdMemberController.class)
@Import({SecurityWebMvcTestConfiguration.class, HouseholdAccess.class})
class HouseholdMemberControllerTest {

    private static final UUID HOUSEHOLD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private HouseholdService householdService;

    @MockitoBean
    private JwtValidatorService jwtValidatorService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("DELETE remove without MANAGE_MEMBERS for another user → 403")
    void removeOtherWithoutPermission_Returns403() throws Exception {
        when(householdService.hasPermission(eq(HOUSEHOLD_ID), any(), eq(MembershipPermission.MANAGE_MEMBERS)))
                .thenReturn(false);

        mockMvc.perform(delete("/api/v1/households/{h}/members/{u}", HOUSEHOLD_ID, OTHER_USER_ID).with(csrf()))
                .andExpect(status().isForbidden());

        verify(householdService, never()).removeMember(any(), any());
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("DELETE remove self is allowed even without MANAGE_MEMBERS")
    void removeSelf_AllowedWithoutPermission() throws Exception {
        mockMvc.perform(delete("/api/v1/households/{h}/members/{u}", HOUSEHOLD_ID, USER_ID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(householdService).removeMember(HOUSEHOLD_ID, USER_ID);
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("PUT permissions without MANAGE_MEMBERS → 403")
    void updatePermissionsWithoutPermission_Returns403() throws Exception {
        when(householdService.hasPermission(eq(HOUSEHOLD_ID), any(), eq(MembershipPermission.MANAGE_MEMBERS)))
                .thenReturn(false);

        mockMvc.perform(put("/api/v1/households/{h}/members/{u}/permissions", HOUSEHOLD_ID, OTHER_USER_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissions":[]}
                                """))
                .andExpect(status().isForbidden());

        verify(householdService, never()).updatePermissions(any(), any(), any());
    }
}