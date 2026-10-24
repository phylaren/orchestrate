package genius.project.orchestrate.identity.web;

import genius.project.orchestrate.identity.JwtValidatorService;
import genius.project.orchestrate.identity.RoleAccess;
import genius.project.orchestrate.identity.UserRole;
import genius.project.orchestrate.identity.UserRoles;
import genius.project.orchestrate.identity.WithUserPrincipal;
import genius.project.orchestrate.identity.internal.SecurityWebMvcTestConfiguration;
import genius.project.orchestrate.common.web.WebErrorAdvice;
import genius.project.orchestrate.common.web.WebViewConfiguration;
import genius.project.orchestrate.user.UserSummaries;
import genius.project.orchestrate.user.UserSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(UserAdminWebController.class)
@Import({SecurityWebMvcTestConfiguration.class, WebViewConfiguration.class, WebErrorAdvice.class, RoleAccess.class})
class UserAdminWebControllerTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000d3");

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UserSummaries userSummaries;

    @MockitoBean
    private UserRoles userRoles;

    @MockitoBean
    private JwtValidatorService jwtValidatorService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        when(userSummaries.listAll()).thenReturn(List.of(
                new UserSummary(ADMIN_ID, "Dev Admin", "dev.admin@orchestrate.test"),
                new UserSummary(USER_ID, "Dev Resident", "dev.resident@orchestrate.test")));
        when(userRoles.roleOf(any())).thenReturn(Optional.of(UserRole.USER));
        when(userRoles.roleOf(ADMIN_ID)).thenReturn(Optional.of(UserRole.ADMIN));
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("Адміністратор бачить список користувачів з ролями")
    void list_Admin_ShowsUsersAndRoles() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(content().string(containsString("dev.resident@orchestrate.test")))
                .andExpect(content().string(containsString("Адміністратор")))
                .andExpect(content().string(containsString("/admin/users")));
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-0000000000d3")
    @DisplayName("Звичайний користувач отримує 403 на адмінський розділ")
    void list_User_Returns403Page() throws Exception {
        when(userRoles.roleOf(USER_ID)).thenReturn(Optional.of(UserRole.USER));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("dev.resident@orchestrate.test"))));
    }

    @Test
    @DisplayName("Анонімний запит до адмінського розділу перенаправляється на вхід")
    void list_Anonymous_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
