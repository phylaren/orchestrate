package genius.project.orchestrate.household;

import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import genius.project.orchestrate.household.internal.domain.Household;
import genius.project.orchestrate.household.internal.domain.Membership;
import genius.project.orchestrate.household.internal.domain.UserHousehold;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserHouseholdController.class)
class UserHouseholdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HouseholdService householdService;

    private static final UUID USER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OTHER_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Test
    @DisplayName("GET /api/v1/users/{userId}/households повертає всі доми користувача з ознакою власника")
    void list_ReturnsHouseholds() throws Exception {
        UUID owned = UUID.randomUUID();
        UUID joined = UUID.randomUUID();
        when(householdService.listHouseholdsOfUser(USER_ID)).thenReturn(List.of(
                new UserHousehold(new Household(owned, "Квартира", USER_ID, Instant.now()),
                        new Membership(owned, USER_ID, MembershipPermission.all(), Instant.now())),
                new UserHousehold(new Household(joined, "Дача", OTHER_ID, Instant.now()),
                        new Membership(joined, USER_ID, MembershipPermission.defaultForNewMember(), Instant.now()))));

        mockMvc.perform(get("/api/v1/users/{userId}/households", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].householdName").value("Квартира"))
                .andExpect(jsonPath("$[0].owner").value(true))
                .andExpect(jsonPath("$[1].owner").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/households для неіснуючого користувача повертає 404 USER_NOT_FOUND")
    void list_UnknownUser_Returns404() throws Exception {
        when(householdService.listHouseholdsOfUser(USER_ID)).thenThrow(ResourceNotFoundException.of("user", USER_ID));

        mockMvc.perform(get("/api/v1/users/{userId}/households", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }
}
