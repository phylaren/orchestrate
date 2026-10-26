package genius.project.orchestrate.integration;

import genius.project.orchestrate.household.MembershipPermission;
import genius.project.orchestrate.support.IntegrationTest;
import genius.project.orchestrate.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ChoreApiIntegrationTest extends IntegrationTestBase {

    private UUID owner;
    private UUID resident;
    private UUID household;
    private String ownerToken;
    private String residentToken;

    @BeforeEach
    void setUp() throws Exception {
        owner = db.user("owner@example.com");
        resident = db.user("resident@example.com");
        household = db.household(owner);
        db.member(household, resident, MembershipPermission.CONFIRM_COMPLETIONS);
        ownerToken = login("owner@example.com");
        residentToken = login("resident@example.com");
    }

    private String createBody(String name, Object recurrenceDays) {
        return """
                {"householdId":"%s","name":"%s","description":"До 20:00","recurrenceDays":%s,"requiresConfirmation":true}
                """.formatted(household, name, recurrenceDays);
    }

    @Test
    @DisplayName("POST /chores: рядок у chores зі значеннями з запиту")
    void createChore_ShouldInsertRow() throws Exception {
        call(post("/api/v1/chores"), ownerToken, createBody("Винести сміття", 3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.needsAttention").value(true));

        assertThat(db.count("chores")).isEqualTo(1);
        assertThat(db.count("chores",
                "household_id = ? and name = ? and recurrence_days = 3 and requires_confirmation = true",
                household, "Винести сміття")).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /chores без права MANAGE_CHORES → 403, рядків немає")
    void createChore_WithoutPermission_ShouldReturn403() throws Exception {
        call(post("/api/v1/chores"), residentToken, createBody("Посуд", 3))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        assertThat(db.count("chores")).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 366, -5})
    @DisplayName("POST /chores з інтервалом поза 1–365 → 400, таблиця порожня")
    void createChore_InvalidRecurrence_ShouldReturn400(int recurrenceDays) throws Exception {
        call(post("/api/v1/chores"), ownerToken, createBody("Прибирання", recurrenceDays))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertThat(db.count("chores")).isZero();
    }

    @Test
    @DisplayName("POST /chores з порожньою назвою → 400, таблиця порожня")
    void createChore_BlankName_ShouldReturn400() throws Exception {
        call(post("/api/v1/chores"), ownerToken, createBody("", 3))
                .andExpect(status().isBadRequest());

        assertThat(db.count("chores")).isZero();
    }

    @Test
    @DisplayName("GET /chores?householdId= повертає всі обов'язки дому з needsAttention")
    void listChores_ShouldReturnRowsOfHousehold() throws Exception {
        db.chore(household, "Посуд", false);
        db.chore(household, "Сміття", true);

        call(get("/api/v1/chores").param("householdId", household.toString()), ownerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].needsAttention").value(true));
    }

    @Test
    @DisplayName("GET /chores/{id}: існуючий → 200, невідомий → 404")
    void getChore_ShouldReadRow() throws Exception {
        UUID chore = db.chore(household, "Посуд", false);

        call(get("/api/v1/chores/{id}", chore), ownerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Посуд"));

        call(get("/api/v1/chores/{id}", UUID.randomUUID()), ownerToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHORE_NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT /chores/{id} замінює поля рядка, кількість рядків не змінюється")
    void updateChore_ShouldChangeRow() throws Exception {
        UUID chore = db.chore(household, "Посуд", false);

        call(put("/api/v1/chores/{id}", chore), ownerToken, """
                {"name":"Пилосос","description":"Вітальня","recurrenceDays":14,"requiresConfirmation":true}
                """)
                .andExpect(status().isOk());

        assertThat(db.count("chores")).isEqualTo(1);
        assertThat(db.count("chores",
                "id = ? and name = 'Пилосос' and recurrence_days = 14 and requires_confirmation = true",
                chore)).isEqualTo(1);
    }

    @Test
    @DisplayName("PUT /chores/{id} з некоректними даними → 400, рядок не змінився")
    void updateChore_Invalid_ShouldReturn400AndKeepRow() throws Exception {
        UUID chore = db.chore(household, "Посуд", false);

        call(put("/api/v1/chores/{id}", chore), ownerToken, """
                {"name":" ","description":null,"recurrenceDays":14,"requiresConfirmation":true}
                """)
                .andExpect(status().isBadRequest());

        assertThat(db.text("select name from chores where id = ?", chore)).isEqualTo("Посуд");
    }

    @Test
    @DisplayName("DELETE /chores/{id} видаляє рядок; невідомий id → 404 і рядки лишаються")
    void deleteChore_ShouldRemoveRow() throws Exception {
        UUID chore = db.chore(household, "Посуд", false);
        db.chore(household, "Сміття", false);

        call(delete("/api/v1/chores/{id}", UUID.randomUUID()), ownerToken)
                .andExpect(status().isNotFound());
        assertThat(db.count("chores")).isEqualTo(2);

        call(delete("/api/v1/chores/{id}", chore), ownerToken)
                .andExpect(status().isNoContent());
        assertThat(db.count("chores")).isEqualTo(1);
        assertThat(db.count("chores", "id = ?", chore)).isZero();
    }
}