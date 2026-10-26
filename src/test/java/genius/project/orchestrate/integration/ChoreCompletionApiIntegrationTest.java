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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ChoreCompletionApiIntegrationTest extends IntegrationTestBase {

    private UUID alice;
    private UUID bob;
    private UUID chore;
    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void setUp() throws Exception {
        alice = db.user("alice@example.com");
        bob = db.user("bob@example.com");
        UUID household = db.household(alice);
        db.member(household, bob, MembershipPermission.CONFIRM_COMPLETIONS);
        chore = db.chore(household, "Сміття", true);
        aliceToken = login("alice@example.com");
        bobToken = login("bob@example.com");
    }

    private void bothJoin() throws Exception {
        call(post("/api/v1/chores/{id}/participants", chore), aliceToken).andExpect(status().isCreated());
        call(post("/api/v1/chores/{id}/participants", chore), bobToken).andExpect(status().isCreated());
    }

    private String aliceCompletes() throws Exception {
        String body = call(post("/api/v1/chores/{id}/completions", chore), aliceToken)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    // ---------- participants ----------

    @Test
    @DisplayName("POST /participants: рядки в chore_participants, rotation_schedules і rotation_order")
    void join_ShouldInsertParticipantAndRotation() throws Exception {
        call(post("/api/v1/chores/{id}/participants", chore), aliceToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addedByAdmin").value(false));

        assertThat(db.count("chore_participants", "chore_id = ? and user_id = ?", chore, alice)).isEqualTo(1);
        assertThat(db.count("rotation_order", "chore_id = ? and user_id = ? and position = 0", chore, alice))
                .isEqualTo(1);
        assertThat(db.count("rotation_schedules", "chore_id = ? and current_cycle_number = 1", chore))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("повторне приєднання → 409, кількість рядків не змінилась")
    void join_Twice_ShouldReturn409AndKeepRowCount() throws Exception {
        call(post("/api/v1/chores/{id}/participants", chore), aliceToken).andExpect(status().isCreated());

        call(post("/api/v1/chores/{id}/participants", chore), aliceToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_PARTICIPANT"));

        assertThat(db.count("chore_participants")).isEqualTo(1);
        assertThat(db.count("rotation_order")).isEqualTo(1);
    }

    @Test
    @DisplayName("приєднання до неіснуючого обов'язку → 404, рядків немає")
    void join_UnknownChore_ShouldReturn404() throws Exception {
        call(post("/api/v1/chores/{id}/participants", UUID.randomUUID()), aliceToken)
                .andExpect(status().isNotFound());

        assertThat(db.count("chore_participants")).isZero();
    }

    @Test
    @DisplayName("PUT /participants/{userId}: адміністратор додає учасника, added_by_admin = true")
    void adminAdd_ShouldMarkRowAddedByAdmin() throws Exception {
        call(put("/api/v1/chores/{c}/participants/{u}", chore, bob), aliceToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addedByAdmin").value(true));

        assertThat(db.count("chore_participants", "user_id = ? and added_by_admin = true", bob)).isEqualTo(1);
    }

    @Test
    @DisplayName("DELETE /participants/{userId}: учасник виходить, у ротації лишається інший")
    void leave_ShouldRemoveParticipantAndRotationRow() throws Exception {
        bothJoin();

        call(delete("/api/v1/chores/{c}/participants/{u}", chore, alice), aliceToken)
                .andExpect(status().isNoContent());

        assertThat(db.count("chore_participants", "chore_id = ?", chore)).isEqualTo(1);
        assertThat(db.count("rotation_order", "chore_id = ? and user_id = ?", chore, bob)).isEqualTo(1);
        assertThat(db.count("rotation_order", "chore_id = ?", chore)).isEqualTo(1);
    }

    // ---------- completions ----------

    @Test
    @DisplayName("відмітка виконання не відповідальним → 409, запису немає")
    void markCompleted_ByNonResponsible_ShouldReturn409() throws Exception {
        bothJoin();

        call(post("/api/v1/chores/{id}/completions", chore), bobToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_CURRENT_RESPONSIBLE"));

        assertThat(db.count("chore_completions")).isZero();
    }

    @Test
    @DisplayName("відмітка виконання: запис PENDING, ротація не рухається")
    void markCompleted_ShouldInsertPendingRowWithoutAdvancing() throws Exception {
        bothJoin();

        String completion = aliceCompletes();

        assertThat(db.count("chore_completions", "chore_id = ?", chore)).isEqualTo(1);
        assertThat(db.text("select status from chore_completions where id = cast(? as uuid)", completion))
                .isEqualTo("PENDING");
        assertThat(db.count("rotation_schedules", "chore_id = ? and current_index = 0", chore)).isEqualTo(1);
    }

    @Test
    @DisplayName("підтвердження іншим учасником: CONFIRMED, confirmed_by = Bob, ротація на наступного")
    void confirm_ByOtherMember_ShouldUpdateRowAndAdvanceRotation() throws Exception {
        bothJoin();
        String completion = aliceCompletes();

        call(post("/api/v1/chores/{c}/completions/{id}/confirmation", chore, completion), bobToken, """
                {"approved":true}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertThat(db.count("chore_completions",
                "id = cast(? as uuid) and status = 'CONFIRMED' and confirmed_by_user_id = ? "
                        + "and confirmed_at is not null", completion, bob)).isEqualTo(1);
        assertThat(db.count("rotation_schedules",
                "chore_id = ? and current_index = 1 and current_cycle_number = 2", chore)).isEqualTo(1);
    }

    @Test
    @DisplayName("самопідтвердження → 409, статус у таблиці не змінився")
    void confirm_BySelf_ShouldReturn409AndKeepStatus() throws Exception {
        bothJoin();
        String completion = aliceCompletes();

        call(post("/api/v1/chores/{c}/completions/{id}/confirmation", chore, completion), aliceToken, """
                {"approved":true}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_CONFIRMATION_NOT_ALLOWED"));

        assertThat(db.text("select status from chore_completions where id = cast(? as uuid)", completion))
                .isEqualTo("PENDING");
    }

    @Test
    @DisplayName("тіло без approved → 400, статус у таблиці не змінився")
    void confirm_MissingApproved_ShouldReturn400() throws Exception {
        bothJoin();
        String completion = aliceCompletes();

        call(post("/api/v1/chores/{c}/completions/{id}/confirmation", chore, completion), bobToken, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertThat(db.text("select status from chore_completions where id = cast(? as uuid)", completion))
                .isEqualTo("PENDING");
    }

    @Test
    @DisplayName("повторне рішення над CONFIRMED → 422, рядок незмінний")
    void confirm_Twice_ShouldReturn422AndKeepRow() throws Exception {
        bothJoin();
        String completion = aliceCompletes();
        String url = "/api/v1/chores/{c}/completions/{id}/confirmation";
        call(post(url, chore, completion), bobToken, """
                {"approved":true}
                """).andExpect(status().isOk());

        call(post(url, chore, completion), bobToken, """
                {"approved":false}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_CONFIRMATION_STATUS"));

        assertThat(db.count("chore_completions", "status = 'CONFIRMED'")).isEqualTo(1);
        assertThat(db.count("chore_completions")).isEqualTo(1);
    }

    @Test
    @DisplayName("DELETE completion: PENDING видаляється, CONFIRMED → 409 і рядок лишається")
    void deleteCompletion_OnlyPendingOrRejected() throws Exception {
        bothJoin();
        String pending = aliceCompletes();

        call(delete("/api/v1/chores/{c}/completions/{id}", chore, pending), aliceToken)
                .andExpect(status().isNoContent());
        assertThat(db.count("chore_completions")).isZero();

        String confirmed = aliceCompletes();
        call(post("/api/v1/chores/{c}/completions/{id}/confirmation", chore, confirmed), bobToken, """
                {"approved":true}
                """).andExpect(status().isOk());

        call(delete("/api/v1/chores/{c}/completions/{id}", chore, confirmed), aliceToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPLETION_NOT_DELETABLE"));
        assertThat(db.count("chore_completions")).isEqualTo(1);
    }
}