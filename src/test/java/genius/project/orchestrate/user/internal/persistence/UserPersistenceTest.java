package genius.project.orchestrate.user.internal.persistence;

import genius.project.orchestrate.user.UserService;
import genius.project.orchestrate.user.exception.EmailAlreadyTakenException;
import genius.project.orchestrate.user.internal.domain.User;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserPersistenceTest {

    private static final Instant T1 = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-02-01T10:00:00Z");

    @Autowired
    private JpaUserRepository adapter;

    @Autowired
    private UserService userService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("зберігає користувача і читає всі колонки назад")
    void savesAndReadsBack() {
        UUID id = UUID.randomUUID();
        adapter.save(new User(id, "Анна", "anna@example.com", T1));
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findById(id)).contains(new User(id, "Анна", "anna@example.com", T1));
        assertThat(adapter.existsById(id)).isTrue();
        assertThat(adapter.findByEmail("anna@example.com")).map(User::id).contains(id);
    }

    @Test
    @DisplayName("повторне збереження оновлює ім'я, але не дату створення")
    void saveUpdatesExistingRow() {
        UUID id = UUID.randomUUID();
        adapter.save(new User(id, "Анна", "anna@example.com", T1));
        entityManager.flush();
        entityManager.clear();

        adapter.save(new User(id, "Ганна", "anna@example.com", T2));
        entityManager.flush();
        entityManager.clear();

        User reloaded = adapter.findById(id).orElseThrow();
        assertThat(reloaded.displayName()).isEqualTo("Ганна");
        assertThat(reloaded.createdAt()).isEqualTo(T1);
    }

    @Test
    @DisplayName("findAll повертає користувачів у порядку створення (derived query з OrderBy)")
    void findAllOrderedByCreatedAt() {
        UUID newer = UUID.randomUUID();
        UUID older = UUID.randomUUID();
        adapter.save(new User(newer, "Новий", "new@example.com", T2));
        adapter.save(new User(older, "Старий", "old@example.com", T1));
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findAll()).extracting(User::id).containsSubsequence(older, newer);
    }

    @Test
    @DisplayName("БД не дає зберегти два однакові email (uq_users_email)")
    void emailIsUniqueInDatabase() {
        adapter.save(new User(UUID.randomUUID(), "Анна", "same@example.com", T1));
        adapter.save(new User(UUID.randomUUID(), "Інша Анна", "same@example.com", T2));

        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("сервіс на реальній БД: email нормалізується, дубль без урахування регістру відхиляється")
    void serviceRejectsDuplicateEmail() {
        User created = userService.createUser("Анна", "Anna@Example.com");
        entityManager.flush();
        entityManager.clear();

        assertThat(userService.getUser(created.id()).email()).isEqualTo("anna@example.com");
        assertThatThrownBy(() -> userService.createUser("Анна 2", "ANNA@example.com"))
                .isInstanceOf(EmailAlreadyTakenException.class);
    }
}
