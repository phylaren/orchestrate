package genius.project.orchestrate.household.web;

import genius.project.orchestrate.common.web.WebErrorAdvice;
import genius.project.orchestrate.common.web.WebViewConfiguration;
import genius.project.orchestrate.household.HouseholdService;
import genius.project.orchestrate.household.internal.domain.Household;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HouseholdCreateWebController.class)
@Import({SecurityWebMvcTestConfiguration.class, WebViewConfiguration.class, WebErrorAdvice.class, RoleAccess.class})
class HouseholdCreateWebControllerTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID HOUSEHOLD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private HouseholdService householdService;

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
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("Форма створення домогосподарства відкривається з полем назви")
    void showForm_Returns200WithNameField() throws Exception {
        mockMvc.perform(get("/households/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("households/create-form"))
                .andExpect(content().string(containsString("name=\"name\"")));
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("Порожня назва повертає форму з помилкою і нічого не створює")
    void create_BlankName_ReturnsFormWithError() throws Exception {
        mockMvc.perform(post("/households").with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("households/create-form"))
                .andExpect(content().string(containsString("Назва домогосподарства є обов’язковою")));
        verify(householdService, never()).createHousehold(any());
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("Успішне створення перенаправляє на дошку нового домогосподарства")
    void create_Valid_RedirectsToBoard() throws Exception {
        when(householdService.createHousehold("Квартира"))
                .thenReturn(new Household(HOUSEHOLD_ID, "Квартира", OWNER_ID, Instant.now()));

        mockMvc.perform(post("/households").with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Квартира"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/households/" + HOUSEHOLD_ID + "/board")));
        verify(householdService).createHousehold(eq("Квартира"));
    }

    @Test
    @WithUserPrincipal(id = "00000000-0000-0000-0000-000000000001")
    @DisplayName("POST без токена CSRF відхиляється з 403")
    void create_WithoutCsrf_Returns403() throws Exception {
        mockMvc.perform(post("/households")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Квартира"))
                .andExpect(status().isForbidden());
        verify(householdService, never()).createHousehold(any());
    }
}
