package genius.project.orchestrate.integration;

import com.jayway.jsonpath.JsonPath;
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
class CoreDomainScenarioIntegrationTest extends IntegrationTestBase {

    private UUID aliceId;
    private UUID bobId;
    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void setUp() throws Exception {
        aliceId = db.user("alice@example.com");
        bobId = db.user("bob@example.com");

        aliceToken = login("alice@example.com");
        bobToken = login("bob@example.com");
    }

    @Test
    @DisplayName("2.1 Ключовий сценарій: Дім -> Запрошення -> Права -> Обов'язок -> Ротація -> Виконання -> Підтвердження")
    void fullDomainScenario_HappyPath() throws Exception {
        String householdJson = call(post("/api/v1/households"), aliceToken, """
                {"name":"Родинне гніздо"}
                """)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID householdId = UUID.fromString(JsonPath.read(householdJson, "$.id"));

        String codeJson = call(post("/api/v1/households/{id}/invitation-code", householdId), aliceToken)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String inviteCode = JsonPath.read(codeJson, "$.code");

        call(post("/api/v1/households/join"), bobToken, """
                {"code":"%s"}
                """.formatted(inviteCode))
                .andExpect(status().isCreated());

        assertThat(db.count("memberships", "household_id = ?", householdId)).isEqualTo(2);

        String choreJson = call(post("/api/v1/chores"), aliceToken, """
                {"householdId":"%s","name":"Помити посуд","description":"Всі тарілки","recurrenceDays":1,"requiresConfirmation":true}
                """.formatted(householdId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID choreId = UUID.fromString(JsonPath.read(choreJson, "$.id"));

        call(post("/api/v1/chores/{id}/participants", choreId), aliceToken).andExpect(status().isCreated());
        call(post("/api/v1/chores/{id}/participants", choreId), bobToken).andExpect(status().isCreated());

        call(get("/api/v1/chores/{id}/assignment", choreId), aliceToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentResponsibleUserId").value(aliceId.toString()))
                .andExpect(jsonPath("$.cycleNumber").value(1));

        String completionJson = call(post("/api/v1/chores/{id}/completions", choreId), aliceToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        UUID completionId = UUID.fromString(JsonPath.read(completionJson, "$.id"));

        call(post("/api/v1/chores/{c}/completions/{cid}/confirmation", choreId, completionId), bobToken, """
                {"approved":true}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedByUserId").value(bobId.toString()));

        assertThat(db.text("select status from chore_completions where id = ?", completionId))
                .isEqualTo("CONFIRMED");
        assertThat(db.count("rotation_schedules", "chore_id = ? and current_index = 1 and current_cycle_number = 2", choreId))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("2.1 Права ролей: 403 без прав, 409 при спробі підтвердити самого себе")
    void rolePermissions_and_SelfConfirmationRestriction() throws Exception {
        String householdJson = call(post("/api/v1/households"), aliceToken, """
                {"name":"Квартира"}
                """).andReturn().getResponse().getContentAsString();
        UUID householdId = UUID.fromString(JsonPath.read(householdJson, "$.id"));

        db.member(householdId, bobId);
        call(put("/api/v1/households/{h}/members/{u}/permissions", householdId, bobId), aliceToken, """
                {"permissions":[]}
                """).andExpect(status().isOk());

        call(post("/api/v1/chores"), bobToken, """
                {"householdId":"%s","name":"Незаконний обов'язок","description":"","recurrenceDays":3,"requiresConfirmation":false}
                """.formatted(householdId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        String choreJson = call(post("/api/v1/chores"), aliceToken, """
                {"householdId":"%s","name":"Прибирання","recurrenceDays":7,"requiresConfirmation":true}
                """.formatted(householdId)).andReturn().getResponse().getContentAsString();
        UUID choreId = UUID.fromString(JsonPath.read(choreJson, "$.id"));

        call(post("/api/v1/chores/{id}/participants", choreId), aliceToken).andExpect(status().isCreated());
        String compJson = call(post("/api/v1/chores/{id}/completions", choreId), aliceToken)
                .andReturn().getResponse().getContentAsString();
        UUID completionId = UUID.fromString(JsonPath.read(compJson, "$.id"));

        call(post("/api/v1/chores/{c}/completions/{cid}/confirmation", choreId, completionId), aliceToken, """
                {"approved":true}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_CONFIRMATION_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("2.1 Крайні випадки: порожня група та вихід власника, коли він єдиний")
    void edgeCases_emptyGroup_and_OwnerLeavesLast() throws Exception {
        String householdJson = call(post("/api/v1/households"), aliceToken, """
                {"name":"Порожній дім"}
                """).andReturn().getResponse().getContentAsString();
        UUID householdId = UUID.fromString(JsonPath.read(householdJson, "$.id"));

        String choreJson = call(post("/api/v1/chores"), aliceToken, """
                {"householdId":"%s","name":"Самотній обов'язок","recurrenceDays":1,"requiresConfirmation":false}
                """.formatted(householdId)).andReturn().getResponse().getContentAsString();
        UUID choreId = UUID.fromString(JsonPath.read(choreJson, "$.id"));

        call(get("/api/v1/chores/{id}", choreId), aliceToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsAttention").value(true));

        call(get("/api/v1/chores/{id}/assignment", choreId), aliceToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_ACTIVE_ASSIGNMENT"));

        call(delete("/api/v1/households/{h}/members/{u}", householdId, aliceId), aliceToken)
                .andExpect(status().isNoContent());

        assertThat(db.count("households", "id = ?", householdId)).isZero();
        assertThat(db.count("memberships", "household_id = ?", householdId)).isZero();
        assertThat(db.count("chores", "household_id = ?", householdId)).isZero();
    }

    @Test
    @DisplayName("2.1 Крайній випадок: вибуття поточного відповідального передає чергу наступному")
    void edgeCase_responsibleLeaves_RotationAdvances() throws Exception {
        String householdJson = call(post("/api/v1/households"), aliceToken, """
                {"name":"Дім для вибуття"}
                """).andReturn().getResponse().getContentAsString();
        UUID householdId = UUID.fromString(JsonPath.read(householdJson, "$.id"));

        String choreJson = call(post("/api/v1/chores"), aliceToken, """
                {"householdId":"%s","name":"Черговий обов'язок","recurrenceDays":1,"requiresConfirmation":false}
                """.formatted(householdId)).andReturn().getResponse().getContentAsString();
        UUID choreId = UUID.fromString(JsonPath.read(choreJson, "$.id"));

        call(post("/api/v1/chores/{id}/participants", choreId), aliceToken).andExpect(status().isCreated());
        call(post("/api/v1/chores/{id}/participants", choreId), bobToken).andExpect(status().isCreated());

        assertThat(db.count("rotation_schedules", "chore_id = ? and current_index = 0 and current_cycle_number = 1", choreId))
                .isEqualTo(1);

        call(delete("/api/v1/chores/{id}/participants/{userId}", choreId, aliceId), aliceToken)
                .andExpect(status().isNoContent());

        assertThat(db.count("chore_participants", "chore_id = ? and user_id = ?", choreId, aliceId)).isZero();
        assertThat(db.count("rotation_schedules", "chore_id = ? and current_index = 0 and current_cycle_number = 2", choreId))
                .isEqualTo(1);

        call(get("/api/v1/chores/{id}/assignment", choreId), aliceToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentResponsibleUserId").value(bobId.toString()))
                .andExpect(jsonPath("$.cycleNumber").value(2));
    }
}