package genius.project.orchestrate.common.web;

import genius.project.orchestrate.identity.WithUserPrincipal;
import genius.project.orchestrate.user.UserPrincipal;
import genius.project.orchestrate.user.UserService;
import genius.project.orchestrate.user.internal.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
class WebUiIntegrationTest {

    private static final Pattern HASHED_CSS = Pattern.compile("/css/app-[0-9a-f]{32}\\.css");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("Сторінка входу відкривається анонімно і містить токен CSRF у формі")
    void loginPage_RendersFormWithCsrf() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("action=\"/login\"")));
    }

    @Test
    @DisplayName("Посилання на стилі містять хеш вмісту у назві файлу")
    void stylesheets_AreVersionedByContentHash() throws Exception {
        String html = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(HASHED_CSS.matcher(html).find()).isTrue();
    }

    @Test
    @DisplayName("Версійний файл стилів віддається з довгим публічним кешем")
    void versionedStylesheet_HasLongPublicCache() throws Exception {
        String html = mockMvc.perform(get("/login")).andReturn().getResponse().getContentAsString();
        var matcher = HASHED_CSS.matcher(html);
        assertThat(matcher.find()).isTrue();

        mockMvc.perform(get(matcher.group()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")))
                .andExpect(header().string("Cache-Control", containsString("public")));
    }

    @Test
    @DisplayName("Анонімний запит до списку домогосподарств перенаправляється на /login")
    void households_Anonymous_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/households"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Помилка входу показує повідомлення, а не порожню сторінку")
    void loginError_ShowsMessage() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Невірне ім’я користувача або пароль.")));
    }

    @Test
    @DisplayName("Шапка для автентифікованого користувача показує ім'я і форму виходу з CSRF")
    void header_Authenticated_ShowsNameAndPostLogout() throws Exception {
        UserPrincipal principal = newPrincipal();

        String html = mockMvc.perform(get("/households").with(user(principal)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(principal.getUsername());
        assertThat(html).contains("action=\"/logout\"");
        assertThat(html).contains("name=\"_csrf\"");
        assertThat(html).doesNotContain("/admin/users");
    }

    @Test
    @DisplayName("Сторінка 404 для неіснуючого сторінкового маршруту відповідає HTML")
    void unknownPage_Authenticated_Returns404() throws Exception {
        mockMvc.perform(get("/no-such-page").with(user(newPrincipal())))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Сторінку не знайдено")))
                .andExpect(content().string(not(containsString("\"instance\""))));
    }

    @Test
    @DisplayName("Відповідь вебсторінки не містить JSON-помилки ProblemDetail")
    void webPage_DoesNotReturnProblemJson() throws Exception {
        mockMvc.perform(get("/households").with(user(newPrincipal())))
                .andExpect(content().string(not(containsString("\"instance\""))));
    }

    @Test
    @WithUserPrincipal
    @DisplayName("Неіснуючий API-шлях зберігає JSON ProblemDetail")
    void unknownApiPath_KeepsProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/no-such-endpoint").with(user(newPrincipal())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private UserPrincipal newPrincipal() {
        String email = "web." + System.nanoTime() + "@orchestrate.test";
        User user = userService.createUser("Web User", email, "correct-horse-battery");
        return new UserPrincipal(user.id(), email, "Web User", "{argon2}hash", true);
    }
}
