package genius.project.orchestrate.integration;

import com.jayway.jsonpath.JsonPath;
import genius.project.orchestrate.household.exception.NotHouseholdOwnerException;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AuditLogIntegrationTest extends IntegrationTestBase {

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
    @DisplayName("успішна операція: у audit_log рядок із користувачем, аргументами, результатом і часом")
    void successfulOperation_IsAudited() throws Exception {
        call(post("/api/v1/households"), ownerToken, """
                {"name":"Квартира"}
                """).andExpect(status().isCreated());

        assertThat(db.count("audit_log",
                "action = ? and success = true and user_id = ? and occurred_at is not null and duration_ms >= 0",
                "CREATE_HOUSEHOLD", owner)).isEqualTo(1);
        assertThat(db.text("select arguments from audit_log where action = ?", "CREATE_HOUSEHOLD"))
                .contains("Квартира");
        assertThat(db.text("select result from audit_log where action = ?", "CREATE_HOUSEHOLD"))
                .contains("Квартира");
    }

    @Test
    @DisplayName("бізнес-помилка: запис success=false збережено, оригінальний виняток дійшов клієнту (403)")
    void businessFailure_IsAuditedAndNotSwallowed() throws Exception {
        UUID household = db.household(owner);
        db.member(household, resident, MembershipPermission.CONFIRM_COMPLETIONS);

        call(delete("/api/v1/households/{id}", household), residentToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_HOUSEHOLD_OWNER"));

        assertThat(db.count("audit_log", "action = ? and success = false and user_id = ?",
                "DELETE_HOUSEHOLD", resident)).isEqualTo(1);
        assertThat(db.text("select error_type from audit_log where action = ?", "DELETE_HOUSEHOLD"))
                .isEqualTo(NotHouseholdOwnerException.class.getName());
        assertThat(db.text("select error_message from audit_log where action = ?", "DELETE_HOUSEHOLD"))
                .contains(household.toString());
        assertThat(db.count("households", "id = ?", household)).isEqualTo(1);
    }

    @Test
    @DisplayName("помилка бізнес-правила при приєднанні: запис є, код запрошення в журнал не потрапляє")
    void joinFailure_IsAuditedWithoutInvitationCode() throws Exception {
        call(post("/api/v1/households/join"), residentToken, """
                {"code":"NOPE2222"}
                """)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_CODE_NOT_FOUND"));

        assertThat(db.count("audit_log",
                "action = ? and success = false and arguments is null and error_message is null",
                "JOIN_HOUSEHOLD")).isEqualTo(1);
    }

    @Test
    @DisplayName("помилка валідації: запис success=false, оригінальний виняток дійшов клієнту (400)")
    void validationFailure_IsAuditedAndNotSwallowed() throws Exception {
        UUID household = db.household(owner);
        db.member(household, resident, MembershipPermission.CONFIRM_COMPLETIONS);
        UUID chore = db.chore(household, "Сміття", false);

        // Додаємо обох у групу виконання
        call(post("/api/v1/households/{h}/chores/{c}/participants", household, chore), ownerToken)
                .andExpect(status().isCreated());
        call(post("/api/v1/households/{h}/chores/{c}/participants", household, chore), residentToken)
                .andExpect(status().isCreated());

        // Спроба створити TEMPORARY swap без cycleNumber (кидає InvalidCycleNumberException -> ValidationException)
        call(post("/api/v1/chores/{c}/swap-requests", chore), ownerToken, """
                {"receiverUserId":"%s","swapType":"TEMPORARY"}
                """.formatted(resident))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_CYCLE_NUMBER"));

        // Перевіряємо, що аудит зафіксував помилку валідації
        assertThat(db.count("audit_log",
                "action = ? and success = false and user_id = ?",
                "CREATE_SWAP_REQUEST", owner)).isEqualTo(1);
        assertThat(db.text("select error_type from audit_log where action = ?", "CREATE_SWAP_REQUEST"))
                .isEqualTo("genius.project.orchestrate.common.exception.InvalidCycleNumberException");
    }

    @Test
    @DisplayName("створення коду запрошення: код не зберігається ні в аргументах, ні в результаті")
    void invitationCode_IsNeverStored() throws Exception {
        UUID household = db.household(owner);
        ownerToken = login("owner@example.com");

        String body = call(post("/api/v1/households/{id}/invitation-code", household), ownerToken)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String code = JsonPath.read(body, "$.code");

        assertThat(db.count("audit_log", "action = ? and success = true and result is null",
                "CREATE_INVITATION_CODE")).isEqualTo(1);
        assertThat(db.count("audit_log", "arguments like ? or result like ?",
                "%" + code + "%", "%" + code + "%")).isZero();
    }
}