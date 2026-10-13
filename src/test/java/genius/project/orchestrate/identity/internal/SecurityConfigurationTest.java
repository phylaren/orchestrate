package genius.project.orchestrate.identity.internal;

import genius.project.orchestrate.identity.CurrentUserController;
import genius.project.orchestrate.identity.JwtValidatorService;
import genius.project.orchestrate.identity.WithUserPrincipal;
import genius.project.orchestrate.user.UserController;
import genius.project.orchestrate.user.UserPrincipal;
import genius.project.orchestrate.user.UserService;
import genius.project.orchestrate.user.internal.domain.User;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class, CurrentUserController.class})
@Import(SecurityWebMvcTestConfiguration.class)
class SecurityConfigurationTest {

    private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String EMAIL = "anna@example.com";

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtValidatorService jwtValidatorService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Запит без токена отримує 401 AUTHENTICATION_REQUIRED у форматі ProblemDetail")
    void protectedEndpoint_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Authentication required"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.instance").value("/api/v1/users"))
                .andExpect(jsonPath("$.timestamp").exists());
        verify(userService, never()).listUsers();
    }

    @Test
    @DisplayName("Підроблений токен отримує 401 INVALID_TOKEN і не доходить до контролера")
    void protectedEndpoint_WithInvalidToken_Returns401() throws Exception {
        when(jwtValidatorService.extractUsername("broken"))
                .thenThrow(new MalformedJwtException("Malformed JWT"));

        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer broken"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")))
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        verify(userService, never()).listUsers();
    }

    @Test
    @DisplayName("Валідний Bearer-токен автентифікує користувача: 200 і дані з токена")
    void protectedEndpoint_WithValidToken_Returns200() throws Exception {
        UserPrincipal principal = new UserPrincipal(USER_ID, EMAIL, "Анна", "{argon2}hash", true);
        when(jwtValidatorService.extractUsername("valid")).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        when(jwtValidatorService.isTokenValid("valid", principal)).thenReturn(true);

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    @WithMockUser
    @DisplayName("Автентифікований користувач отримує 200 на захищеному ендпоінті")
    void protectedEndpoint_Authenticated_Returns200() throws Exception {
        when(userService.listUsers()).thenReturn(List.of(new User(USER_ID, "Анна", EMAIL, Instant.now())));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(USER_ID.toString()));
    }

    @Test
    @WithUserPrincipal(id = "33333333-3333-3333-3333-333333333333", email = EMAIL)
    @DisplayName("GET /api/v1/users/me повертає користувача з контексту безпеки")
    void me_Authenticated_ReturnsPrincipal() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    @DisplayName("Реєстрація публічна: POST без токена і без CSRF-токена повертає 201")
    void registration_IsPublic_AndCsrfDisabled() throws Exception {
        when(userService.createUser(any(), any(), any()))
                .thenReturn(new User(USER_ID, "Анна", EMAIL, Instant.now()));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Анна","email":"anna@example.com","password":"correct-horse"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CORS: preflight із дозволеного джерела дозволяє заголовок Authorization")
    void corsPreflight_AllowedOrigin_Returns200() throws Exception {
        mockMvc.perform(options("/api/v1/users")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("Authorization")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        containsString("X-Trace-Id")));
    }

    @Test
    @DisplayName("CORS: preflight із недозволеного джерела відхиляється з 403")
    void corsPreflight_UnknownOrigin_Returns403() throws Exception {
        mockMvc.perform(options("/api/v1/users")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
