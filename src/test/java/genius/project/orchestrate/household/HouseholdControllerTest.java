package genius.project.orchestrate.household;

import genius.project.orchestrate.household.exception.NotHouseholdOwnerException;
import genius.project.orchestrate.household.exception.OwnershipTransferToSelfException;
import genius.project.orchestrate.household.internal.domain.Household;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HouseholdController.class)
class HouseholdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HouseholdService householdService;

    private static final UUID HOUSEHOLD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MEMBER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Test
    @DisplayName("POST /api/v1/households повертає 201, Location і ownerId")
    void create_Returns201() throws Exception {
        when(householdService.createHousehold("Квартира"))
                .thenReturn(new Household(HOUSEHOLD_ID, "Квартира", OWNER_ID, Instant.now()));

        mockMvc.perform(post("/api/v1/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Квартира"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/households/" + HOUSEHOLD_ID)))
                .andExpect(jsonPath("$.id").value(HOUSEHOLD_ID.toString()))
                .andExpect(jsonPath("$.ownerId").value(OWNER_ID.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/households з порожньою назвою повертає 400 VALIDATION_FAILED")
    void create_BlankName_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verify(householdService, never()).createHousehold(any());
    }

    @Test
    @DisplayName("POST /api/v1/households з ownerId у тілі повертає 400: власника не можна задати вручну")
    void create_WithOwnerIdField_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Квартира","ownerId":"%s"}
                                """.formatted(MEMBER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));
    }

    @Test
    @DisplayName("GET /api/v1/households/{id} повертає дім")
    void get_Returns200() throws Exception {
        when(householdService.getHousehold(HOUSEHOLD_ID))
                .thenReturn(new Household(HOUSEHOLD_ID, "Квартира", OWNER_ID, Instant.now()));

        mockMvc.perform(get("/api/v1/households/{id}", HOUSEHOLD_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Квартира"));
    }

    @Test
    @DisplayName("DELETE /api/v1/households/{id} не власником повертає 403 NOT_HOUSEHOLD_OWNER")
    void delete_NonOwner_Returns403() throws Exception {
        doThrow(new NotHouseholdOwnerException(HOUSEHOLD_ID, MEMBER_ID))
                .when(householdService).deleteHousehold(HOUSEHOLD_ID);

        mockMvc.perform(delete("/api/v1/households/{id}", HOUSEHOLD_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_HOUSEHOLD_OWNER"));
    }

    @Test
    @DisplayName("DELETE /api/v1/households/{id} власником повертає 204")
    void delete_Owner_Returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/households/{id}", HOUSEHOLD_ID))
                .andExpect(status().isNoContent());
        verify(householdService).deleteHousehold(HOUSEHOLD_ID);
    }

    @Test
    @DisplayName("POST /ownership-transfer повертає дім з новим ownerId")
    void transfer_Returns200() throws Exception {
        when(householdService.transferOwnership(HOUSEHOLD_ID, MEMBER_ID))
                .thenReturn(new Household(HOUSEHOLD_ID, "Квартира", MEMBER_ID, Instant.now()));

        mockMvc.perform(post("/api/v1/households/{id}/ownership-transfer", HOUSEHOLD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newOwnerUserId":"%s"}
                                """.formatted(MEMBER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(MEMBER_ID.toString()));
    }

    @Test
    @DisplayName("POST /ownership-transfer самому собі повертає 400 OWNERSHIP_TRANSFER_TO_SELF")
    void transfer_ToSelf_Returns400() throws Exception {
        when(householdService.transferOwnership(HOUSEHOLD_ID, OWNER_ID))
                .thenThrow(new OwnershipTransferToSelfException(HOUSEHOLD_ID));

        mockMvc.perform(post("/api/v1/households/{id}/ownership-transfer", HOUSEHOLD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newOwnerUserId":"%s"}
                                """.formatted(OWNER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OWNERSHIP_TRANSFER_TO_SELF"));
    }
}
