package genius.project.orchestrate.chore.web;

import genius.project.orchestrate.chore.ChoreLifecycleService;
import genius.project.orchestrate.common.web.WebErrorAdvice;
import genius.project.orchestrate.common.web.WebViewConfiguration;
import genius.project.orchestrate.common.exception.ForbiddenException;
import genius.project.orchestrate.common.exception.ValidationException;
import genius.project.orchestrate.household.exception.HouseholdNotFoundException;
import genius.project.orchestrate.identity.JwtValidatorService;
import genius.project.orchestrate.identity.RoleAccess;
import genius.project.orchestrate.identity.UserRoles;
import genius.project.orchestrate.identity.WithUserPrincipal;
import genius.project.orchestrate.identity.internal.SecurityWebMvcTestConfiguration;
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

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ChoreWebController.class)
@Import({SecurityWebMvcTestConfiguration.class, WebViewConfiguration.class, WebErrorAdvice.class, RoleAccess.class})
class WebErrorAdviceTest {

    private static final UUID HOUSEHOLD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private ChoreLifecycleService choreLifecycleService;

    @MockitoBean
    private JwtValidatorService jwtValidatorService;

    @MockitoBean
    private UserRoles userRoles;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @WithUserPrincipal
    @DisplayName("Неіснуючий дім у веб-сторінці дає 404 з HTML-шаблоном, а не JSON")
    void unknownHousehold_Returns404Page() throws Exception {
        when(choreLifecycleService.listChores(HOUSEHOLD_ID))
                .thenThrow(new HouseholdNotFoundException(HOUSEHOLD_ID));

        mockMvc.perform(get("/households/{id}/chores", HOUSEHOLD_ID))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(containsString("Сторінку не знайдено")));
    }

    @Test
    @WithUserPrincipal
    @DisplayName("Заборонена дія у веб-сторінці дає сторінку 403")
    void forbiddenAction_Returns403Page() throws Exception {
        when(choreLifecycleService.listChores(HOUSEHOLD_ID))
                .thenThrow(new ForbiddenException("MISSING_PERMISSION", "Немає права"));

        mockMvc.perform(get("/households/{id}/chores", HOUSEHOLD_ID))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }

    @Test
    @WithUserPrincipal
    @DisplayName("Помилка валідації у веб-сторінці дає 400 з текстом помилки")
    void validationError_Returns400Page() throws Exception {
        when(choreLifecycleService.listChores(HOUSEHOLD_ID))
                .thenThrow(new ValidationException("INVALID", "Некоректний інтервал"));

        mockMvc.perform(get("/households/{id}/chores", HOUSEHOLD_ID))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"))
                .andExpect(content().string(containsString("Некоректний інтервал")));
    }

    @Test
    @WithUserPrincipal
    @DisplayName("Несподіваний збій у веб-сторінці дає сторінку 500 без деталей")
    void unexpectedError_Returns500Page() throws Exception {
        when(choreLifecycleService.listChores(HOUSEHOLD_ID))
                .thenThrow(new IllegalStateException("db is down"));

        mockMvc.perform(get("/households/{id}/chores", HOUSEHOLD_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/5xx"))
                .andExpect(content().string(not(containsString("db is down"))));
    }
}
