package genius.project.orchestrate.user;

import genius.project.orchestrate.user.exception.EmailAlreadyTakenException;
import genius.project.orchestrate.user.exception.UserNotFoundException;
import genius.project.orchestrate.user.internal.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    @DisplayName("POST /api/v1/users з валідними даними повертає 201 і Location")
    void create_Valid_Returns201() throws Exception {
        when(userService.createUser("Анна", "anna@example.com"))
                .thenReturn(new User(USER_ID, "Анна", "anna@example.com", Instant.now()));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Анна","email":"anna@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/users/" + USER_ID)))
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.displayName").value("Анна"))
                .andExpect(jsonPath("$.email").value("anna@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/users з некоректним email повертає 400 VALIDATION_FAILED")
    void create_InvalidEmail_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Анна","email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
        verify(userService, never()).createUser(any(), any());
    }

    @Test
    @DisplayName("POST /api/v1/users із зайнятим email повертає 409 EMAIL_ALREADY_TAKEN")
    void create_DuplicateEmail_Returns409() throws Exception {
        when(userService.createUser(any(), any())).thenThrow(new EmailAlreadyTakenException("anna@example.com"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Анна","email":"anna@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_TAKEN"));
    }

    @Test
    @DisplayName("GET /api/v1/users повертає список")
    void list_ReturnsUsers() throws Exception {
        when(userService.listUsers()).thenReturn(List.of(new User(USER_ID, "Анна", "a@b.c", Instant.now())));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(USER_ID.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} для неіснуючого повертає 404 USER_NOT_FOUND")
    void get_NotFound_Returns404() throws Exception {
        when(userService.getUser(USER_ID)).thenThrow(new UserNotFoundException(USER_ID));

        mockMvc.perform(get("/api/v1/users/{id}", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }
}
