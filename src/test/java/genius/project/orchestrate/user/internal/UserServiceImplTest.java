package genius.project.orchestrate.user.internal;

import genius.project.orchestrate.user.exception.EmailAlreadyTakenException;
import genius.project.orchestrate.user.exception.UserNotFoundException;
import genius.project.orchestrate.user.internal.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserServiceImpl service;

    private static final UUID USER_ID = UUID.randomUUID();

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("зберігає користувача з нормалізованим email та обрізаним іменем")
        void savesNormalizedUser() {
            when(repository.findByEmail("anna@example.com")).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            User created = service.createUser("  Анна  ", "  Anna@Example.COM ");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().displayName()).isEqualTo("Анна");
            assertThat(captor.getValue().email()).isEqualTo("anna@example.com");
            assertThat(captor.getValue().id()).isNotNull();
            assertThat(captor.getValue().createdAt()).isNotNull();
            assertThat(created).isEqualTo(captor.getValue());
        }

        @Test
        @DisplayName("зайнятий email (без урахування регістру) — EmailAlreadyTakenException, нічого не зберігається")
        void duplicateEmail_Throws() {
            when(repository.findByEmail("anna@example.com"))
                    .thenReturn(Optional.of(user(USER_ID, "anna@example.com")));

            assertThatThrownBy(() -> service.createUser("Анна", "ANNA@example.com"))
                    .isInstanceOf(EmailAlreadyTakenException.class)
                    .extracting("errorCode").isEqualTo("EMAIL_ALREADY_TAKEN");
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getUser / listUsers / existsById")
    class Queries {

        @Test
        @DisplayName("getUser повертає знайденого користувача")
        void getUser_Found() {
            User stored = user(USER_ID, "a@b.c");
            when(repository.findById(USER_ID)).thenReturn(Optional.of(stored));

            assertThat(service.getUser(USER_ID)).isEqualTo(stored);
        }

        @Test
        @DisplayName("getUser для неіснуючого — UserNotFoundException")
        void getUser_NotFound() {
            when(repository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getUser(USER_ID))
                    .isInstanceOf(UserNotFoundException.class)
                    .extracting("errorCode").isEqualTo("USER_NOT_FOUND");
        }

        @Test
        @DisplayName("listUsers сортує за датою створення")
        void listUsers_SortedByCreatedAt() {
            User older = new User(UUID.randomUUID(), "old", "o@x.y", Instant.parse("2026-01-01T00:00:00Z"));
            User newer = new User(UUID.randomUUID(), "new", "n@x.y", Instant.parse("2026-02-01T00:00:00Z"));
            when(repository.findAll()).thenReturn(List.of(newer, older));

            assertThat(service.listUsers()).containsExactly(older, newer);
        }

        @Test
        @DisplayName("existsById делегує репозиторію")
        void existsById_Delegates() {
            when(repository.existsById(USER_ID)).thenReturn(true);

            assertThat(service.existsById(USER_ID)).isTrue();
        }
    }

    private static User user(UUID id, String email) {
        return new User(id, "Анна", email, Instant.now());
    }
}
