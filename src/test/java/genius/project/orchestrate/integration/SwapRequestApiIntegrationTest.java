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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class SwapRequestApiIntegrationTest extends IntegrationTestBase {

    private static final String URL = "/api/v1/chores/{c}/swap-requests";

    private UUID alice;
    private UUID bob;
    private UUID carol;
    private UUID chore;
    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void setUp() throws Exception {
        alice = db.user("alice@example.com");
        bob = db.user("bob@example.com");
        carol = db.user("carol@example.com");
        UUID household = db.household(alice);
        db.member(household, bob, MembershipPermission.CONFIRM_COMPLETIONS);
        db.member(household, carol, MembershipPermission.CONFIRM_COMPLETIONS);
        chore = db.chore(household, "Сміття", false);
        aliceToken = login("alice@example.com");
        bobToken = login("bob@example.com");
        // Alice і Bob у групі виконання (поточний цикл = 1), Carol не в групі.
        call(post("/api/v1/chores/{id}/participants", chore), aliceToken).andExpect(status().isCreated());
        call(post("/api/v1/chores/{id}/participants", chore), bobToken).andExpect(status().isCreated());
    }

    private String createPermanent() throws Exception {
        String body = call(post(URL, chore), aliceToken, """
                {"receiverUserId":"%s","swapType":"PERMANENT"}
                """.formatted(bob))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    @DisplayName("POST PERMANENT: рядок PENDING без cycle_number")
    void createPermanent_ShouldInsertPendingRow() throws Exception {
        String id = createPermanent();

        assertThat(db.count("swap_requests")).isEqualTo(1);
        assertThat(db.count("swap_requests",
                "id = cast(? as uuid) and status = 'PENDING' and swap_type = 'PERMANENT' "
                        + "and cycle_number is null and initiator_user_id = ? and receiver_user_id = ?",
                id, alice, bob)).isEqualTo(1);
    }

    @Test
    @DisplayName("POST TEMPORARY: у рядку збережено cycle_number")
    void createTemporary_ShouldStoreCycleNumber() throws Exception {
        call(post(URL, chore), aliceToken, """
                {"receiverUserId":"%s","swapType":"TEMPORARY","cycleNumber":3}
                """.formatted(bob))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cycleNumber").value(3));

        assertThat(db.count("swap_requests", "swap_type = 'TEMPORARY' and cycle_number = 3")).isEqualTo(1);
    }

    @Test
    @DisplayName("некоректні запити → 400, таблиця swap_requests порожня")
    void create_InvalidInput_ShouldReturn400AndInsertNothing() throws Exception {
        call(post(URL, chore), aliceToken, """
                {"receiverUserId":"%s","swapType":"PERMANENT"}
                """.formatted(alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SWAP_REQUEST_RECIPIENT"));

        call(post(URL, chore), aliceToken, """
                {"receiverUserId":"%s","swapType":"PERMANENT"}
                """.formatted(carol))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_CHORE_PARTICIPANT"));

        call(post(URL, chore), aliceToken, """
                {"receiverUserId":"%s"}
                """.formatted(bob))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        call(post(URL, chore), aliceToken, """
                {"receiverUserId":"%s","swapType":"TEMPORARY"}
                """.formatted(bob))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_CYCLE_NUMBER"));

        call(post(URL, chore), aliceToken, """
                {"receiverUserId":"%s","swapType":"TEMPORARY","cycleNumber":1}
                """.formatted(bob))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CYCLE_IN_PAST"));

        assertThat(db.count("swap_requests")).isZero();
    }

    @Test
    @DisplayName("дубль PENDING-запиту → 409, у таблиці один рядок")
    void create_Duplicate_ShouldReturn409AndKeepRowCount() throws Exception {
        createPermanent();

        call(post(URL, chore), aliceToken, """
                {"receiverUserId":"%s","swapType":"PERMANENT"}
                """.formatted(bob))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SWAP_REQUEST_ALREADY_EXISTS"));

        assertThat(db.count("swap_requests")).isEqualTo(1);
    }

    @Test
    @DisplayName("GET: список, один запит і зведення збігаються з таблицею")
    void read_ShouldMatchTable() throws Exception {
        String id = createPermanent();

        call(get(URL, chore), aliceToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        call(get(URL + "/{id}", chore, id), aliceToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
        call(get(URL + "/summary", chore), aliceToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.byStatus.PENDING").value(1));
        call(get(URL + "/{id}", chore, UUID.randomUUID()), aliceToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SWAP_REQUEST_NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT ініціатором змінює умови; отримувачем → 403 і рядок незмінний")
    void update_ByInitiator_ChangesRow_ByReceiver_Forbidden() throws Exception {
        String id = createPermanent();

        call(put(URL + "/{id}", chore, id), bobToken, """
                {"swapType":"TEMPORARY","cycleNumber":4}
                """)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_SWAP_REQUEST_INITIATOR"));
        assertThat(db.count("swap_requests", "swap_type = 'PERMANENT'")).isEqualTo(1);

        call(put(URL + "/{id}", chore, id), aliceToken, """
                {"swapType":"TEMPORARY","cycleNumber":4}
                """)
                .andExpect(status().isOk());
        assertThat(db.count("swap_requests", "swap_type = 'TEMPORARY' and cycle_number = 4")).isEqualTo(1);
    }

    @Test
    @DisplayName("PATCH REJECTED отримувачем: статус у таблиці; повторна відповідь → 422; ініціатор → 403")
    void reject_ShouldUpdateStatus() throws Exception {
        String id = createPermanent();

        call(patch(URL + "/{id}", chore, id), aliceToken, """
                {"status":"REJECTED"}
                """)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_SWAP_REQUEST_RECEIVER"));
        assertThat(db.count("swap_requests", "status = 'PENDING'")).isEqualTo(1);

        call(patch(URL + "/{id}", chore, id), bobToken, """
                {"status":"REJECTED"}
                """)
                .andExpect(status().isOk());
        assertThat(db.count("swap_requests", "status = 'REJECTED'")).isEqualTo(1);

        call(patch(URL + "/{id}", chore, id), bobToken, """
                {"status":"ACCEPTED"}
                """)
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("INVALID_SWAP_REQUEST_STATUS"));
        assertThat(db.count("swap_requests", "status = 'REJECTED'")).isEqualTo(1);
    }

    @Test
    @DisplayName("DELETE: отримувач → 403 (рядок лишається), ініціатор → 204 (рядок видалено)")
    void delete_OnlyInitiator() throws Exception {
        String id = createPermanent();

        call(delete(URL + "/{id}", chore, id), bobToken)
                .andExpect(status().isForbidden());
        assertThat(db.count("swap_requests")).isEqualTo(1);

        call(delete(URL + "/{id}", chore, id), aliceToken)
                .andExpect(status().isNoContent());
        assertThat(db.count("swap_requests")).isZero();
    }
}