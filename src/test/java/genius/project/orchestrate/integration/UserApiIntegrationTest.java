package genius.project.orchestrate.integration;

import genius.project.orchestrate.support.IntegrationTest;
import genius.project.orchestrate.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class UserApiIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("POST /users створює рядок: email у нижньому регістрі, пароль лише як Argon2id-хеш")
    void createUser_ShouldInsertRow() throws Exception {
        call(post("/api/v1/users"), null, """
                {"displayName":"Анна","email":"Anna@Example.com","password":"correct-horse-battery"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("anna@example.com"));

        assertThat(db.count("users")).isEqualTo(1);
        assertThat(db.text("select display_name from users where email = ?", "anna@example.com"))
                .isEqualTo("Анна");
        assertThat(db.text("select password_hash from users where email = ?", "anna@example.com"))
                .startsWith("$argon2id$").doesNotContain("correct-horse-battery");
    }

    @Test
    @DisplayName("POST /users з некоректним email → 400, таблиця users порожня")
    void createUser_InvalidEmail_ShouldReturn400AndInsertNothing() throws Exception {
        call(post("/api/v1/users"), null, """
                {"displayName":"Анна","email":"not-an-email","password":"correct-horse-battery"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertThat(db.count("users")).isZero();
    }

    @Test
    @DisplayName("POST /users із занадто коротким паролем → 400, кількість рядків не змінилась")
    void createUser_ShortPassword_ShouldReturn400AndKeepRowCount() throws Exception {
        db.user("existing@example.com");

        call(post("/api/v1/users"), null, """
                {"displayName":"Анна","email":"anna@example.com","password":"short"}
                """)
                .andExpect(status().isBadRequest());

        assertThat(db.count("users")).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /users із зайнятим email (без урахування регістру) → 409, дубля в таблиці немає")
    void createUser_DuplicateEmail_ShouldReturn409AndKeepRowCount() throws Exception {
        db.user("dup@example.com");

        call(post("/api/v1/users"), null, """
                {"displayName":"Інша","email":"DUP@example.com","password":"correct-horse-battery"}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_TAKEN"));

        assertThat(db.count("users")).isEqualTo(1);
    }

    @Test
    @DisplayName("GET /users/{id} читає рядок із таблиці; для невідомого id → 404")
    void getUser_ShouldReadRow() throws Exception {
        UUID id = db.user("reader@example.com");
        String token = login("reader@example.com");

        call(get("/api/v1/users/{id}", id), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("reader@example.com"));

        call(get("/api/v1/users/{id}", UUID.randomUUID()), token)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /users повертає стільки користувачів, скільки рядків у таблиці")
    void listUsers_ShouldMatchTable() throws Exception {
        db.user("first@example.com");
        db.user("second@example.com");
        String token = login("first@example.com");

        call(get("/api/v1/users"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value((int) db.count("users")));
    }

    @Test
    @DisplayName("GET /users без токена → 401")
    void listUsers_WithoutToken_ShouldReturn401() throws Exception {
        call(get("/api/v1/users"), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }
}