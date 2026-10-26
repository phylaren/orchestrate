package genius.project.orchestrate.integration;

import com.jayway.jsonpath.JsonPath;
import genius.project.orchestrate.household.MembershipPermission;
import genius.project.orchestrate.support.IntegrationTest;
import genius.project.orchestrate.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class HouseholdApiIntegrationTest extends IntegrationTestBase {

    private UUID owner;
    private UUID resident;
    private String ownerToken;
    private String residentToken;

    @BeforeEach
    void setUp() throws Exception {
        owner = db.user("owner@example.com");
        resident = db.user("resident@example.com");
        ownerToken = login("owner@example.com");
        residentToken = login("resident@example.com");
    }

    @Test
    @DisplayName("POST /households: у БД дім, членство власника і 4 права")
    void createHousehold_ShouldInsertHouseholdMembershipAndPermissions() throws Exception {
        call(post("/api/v1/households"), ownerToken, """
                {"name":"Квартира"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(owner.toString()));

        assertThat(db.count("households")).isEqualTo(1);
        assertThat(db.count("households", "owner_id = ? and name = ?", owner, "Квартира")).isEqualTo(1);
        assertThat(db.count("memberships", "user_id = ?", owner)).isEqualTo(1);
        assertThat(db.count("membership_permissions")).isEqualTo(MembershipPermission.values().length);
    }

    @Test
    @DisplayName("POST /households з порожньою назвою → 400, жодного рядка")
    void createHousehold_BlankName_ShouldReturn400AndInsertNothing() throws Exception {
        call(post("/api/v1/households"), ownerToken, """
                {"name":"  "}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertThat(db.count("households")).isZero();
        assertThat(db.count("memberships")).isZero();
    }

    @Test
    @DisplayName("GET /households/{id}: учасник бачить дім, сторонній отримує 403")
    void getHousehold_MemberSeesIt_OutsiderForbidden() throws Exception {
        UUID household = db.household(owner);

        call(get("/api/v1/households/{id}", household), ownerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(household.toString()))
                .andExpect(jsonPath("$.name").value("Дім"));

        call(get("/api/v1/households/{id}", household), residentToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_HOUSEHOLD_MEMBER"));
    }

    @Test
    @DisplayName("DELETE /households/{id} власником каскадно прибирає членства, права і код")
    void deleteHousehold_Owner_ShouldCascade() throws Exception {
        UUID household = db.household(owner);
        db.member(household, resident, MembershipPermission.CONFIRM_COMPLETIONS);
        db.invitationCode("ABCD2345", household, owner);

        call(delete("/api/v1/households/{id}", household), ownerToken)
                .andExpect(status().isNoContent());

        assertThat(db.count("households")).isZero();
        assertThat(db.count("memberships")).isZero();
        assertThat(db.count("membership_permissions")).isZero();
        assertThat(db.count("invitation_codes")).isZero();
    }

    @Test
    @DisplayName("DELETE /households/{id} не власником → 403, рядки незмінні")
    void deleteHousehold_NonOwner_ShouldReturn403AndKeepRows() throws Exception {
        UUID household = db.household(owner);
        db.member(household, resident, MembershipPermission.CONFIRM_COMPLETIONS);

        call(delete("/api/v1/households/{id}", household), residentToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_HOUSEHOLD_OWNER"));

        assertThat(db.count("households")).isEqualTo(1);
        assertThat(db.count("memberships")).isEqualTo(2);
    }

    @Test
    @DisplayName("POST /ownership-transfer змінює owner_id і видає новому власнику всі права")
    void transferOwnership_ShouldUpdateOwnerRow() throws Exception {
        UUID household = db.household(owner);
        db.member(household, resident, MembershipPermission.CONFIRM_COMPLETIONS);

        call(post("/api/v1/households/{id}/ownership-transfer", household), ownerToken, """
                {"newOwnerUserId":"%s"}
                """.formatted(resident))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(resident.toString()));

        assertThat(db.count("households", "id = ? and owner_id = ?", household, resident)).isEqualTo(1);
        assertThat(db.count("membership_permissions", "user_id = ?", resident))
                .isEqualTo(MembershipPermission.values().length);
    }

    @Test
    @DisplayName("код запрошення → приєднання: рядки в invitation_codes і memberships")
    void invitationCodeAndJoin_ShouldWriteRows() throws Exception {
        UUID household = db.household(owner);

        String body = call(post("/api/v1/households/{id}/invitation-code", household), ownerToken)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String code = JsonPath.read(body, "$.code");

        assertThat(db.count("invitation_codes", "household_id = ? and code = ?", household, code)).isEqualTo(1);

        call(post("/api/v1/households/join"), residentToken, """
                {"code":"%s"}
                """.formatted(code))
                .andExpect(status().isCreated());

        assertThat(db.count("memberships", "household_id = ?", household)).isEqualTo(2);
        assertThat(db.count("membership_permissions",
                "user_id = ? and permission = 'CONFIRM_COMPLETIONS'", resident)).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /join з невідомим кодом → 404, членств не додалось")
    void join_UnknownCode_ShouldReturn404AndKeepRowCount() throws Exception {
        UUID household = db.household(owner);

        call(post("/api/v1/households/join"), residentToken, """
                {"code":"NOPE2222"}
                """)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_CODE_NOT_FOUND"));

        assertThat(db.count("memberships", "household_id = ?", household)).isEqualTo(1);
    }

    @Test
    @DisplayName("PUT /members/{id}/permissions замінює набір прав у membership_permissions")
    void updatePermissions_ShouldReplaceRows() throws Exception {
        UUID household = db.household(owner);
        db.member(household, resident, MembershipPermission.CONFIRM_COMPLETIONS);

        call(put("/api/v1/households/{h}/members/{u}/permissions", household, resident), ownerToken, """
                {"permissions":["MANAGE_CHORES","INVITE_MEMBERS"]}
                """)
                .andExpect(status().isOk());

        assertThat(db.count("membership_permissions", "user_id = ?", resident)).isEqualTo(2);
        assertThat(db.count("membership_permissions",
                "user_id = ? and permission = 'CONFIRM_COMPLETIONS'", resident)).isZero();
    }

    @Test
    @DisplayName("PUT /permissions з невідомим правом → 400, права в таблиці не змінились")
    void updatePermissions_UnknownPermission_ShouldReturn400AndKeepRows() throws Exception {
        UUID household = db.household(owner);
        db.member(household, resident, MembershipPermission.CONFIRM_COMPLETIONS);

        call(put("/api/v1/households/{h}/members/{u}/permissions", household, resident), ownerToken, """
                {"permissions":["BE_GOD"]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));

        assertThat(db.count("membership_permissions", "user_id = ?", resident)).isEqualTo(1);
    }

    @Test
    @DisplayName("DELETE /members/{id} власником прибирає членство учасника")
    void removeMember_ShouldDeleteMembershipRow() throws Exception {
        UUID household = db.household(owner);
        db.member(household, resident, MembershipPermission.CONFIRM_COMPLETIONS);

        call(delete("/api/v1/households/{h}/members/{u}", household, resident), ownerToken)
                .andExpect(status().isNoContent());

        assertThat(db.count("memberships", "household_id = ?", household)).isEqualTo(1);
        assertThat(db.count("membership_permissions", "user_id = ?", resident)).isZero();
    }
}