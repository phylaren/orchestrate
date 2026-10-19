package genius.project.orchestrate.chore.web;

import genius.project.orchestrate.household.internal.HouseholdAccess;
import genius.project.orchestrate.identity.WithUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class ChoreWebSecurityTest {

    private static final UUID HOUSEHOLD_ID = UUID.randomUUID();

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private HouseholdAccess householdAccess;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("POST без CSRF-токена повертає 403 Forbidden (вимога ТЗ)")
    void postWithoutCsrf_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/households/{householdId}/chores", HOUSEHOLD_ID)
                        .param("name", "Прибирання")
                        .param("recurrenceDays", "7"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Анонімний GET перенаправляє на сторінку входу /login (вимога ТЗ)")
    void anonymousGet_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/households/{householdId}/chores", HOUSEHOLD_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithUserPrincipal
    @DisplayName("Користувач без права MANAGE_CHORES отримує 403 (вимога ТЗ)")
    void userWithoutPermission_PostWithCsrf_ReturnsForbidden() throws Exception {
        when(householdAccess.hasPermission(any(), any(), eq("MANAGE_CHORES")))
                .thenReturn(false);

        mockMvc.perform(post("/households/{householdId}/chores", HOUSEHOLD_ID)
                        .with(csrf())
                        .param("name", "Прибирання")
                        .param("recurrenceDays", "7"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserPrincipal
    @DisplayName("Користувач з правом MANAGE_CHORES успішно створює запис (вимога лекції: POST з CSRF = Redirect)")
    void userWithPermission_PostWithCsrf_Redirects() throws Exception {
        when(householdAccess.hasPermission(any(), any(), eq("MANAGE_CHORES")))
                .thenReturn(true);

        mockMvc.perform(post("/households/{householdId}/chores", HOUSEHOLD_ID)
                        .with(csrf())
                        .param("name", "Прибирання")
                        .param("recurrenceDays", "7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/households/" + HOUSEHOLD_ID + "/chores"));
    }

    @Test
    @DisplayName("Запит до REST API без токена повертає 401 (перевірка, що ланцюги не конфліктують)")
    void apiRequestWithoutToken_ReturnsUnauthorizedProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/chores"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }
}