package genius.project.orchestrate.chore.web;

import genius.project.orchestrate.chore.ChoreLifecycleService;
import genius.project.orchestrate.chore.dto.ChoreResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ChoreWebController.class)
class ChoreWebControllerTest {

    private static final UUID HOUSEHOLD_ID = UUID.randomUUID();
    private static final String BASE_URL = "/households/{householdId}/chores";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChoreLifecycleService choreLifecycleService;

    private static ChoreResponse chore(String name, boolean needsAttention) {
        return new ChoreResponse(UUID.randomUUID(), HOUSEHOLD_ID, name, "Опис", 7, true,
                needsAttention, Instant.parse("2026-10-05T10:25:00Z"));
    }

    @Test
    @DisplayName("GET /households/{id}/chores віддає представлення списку з обов'язками дому")
    void listChores_ReturnsListViewWithChores() throws Exception {
        List<ChoreResponse> chores = List.of(chore("Прибирання", true), chore("Посуд", false));
        when(choreLifecycleService.listChores(HOUSEHOLD_ID)).thenReturn(chores);

        mockMvc.perform(get(BASE_URL, HOUSEHOLD_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("chores/list"))
                .andExpect(model().attribute("householdId", HOUSEHOLD_ID))
                .andExpect(model().attribute("chores", chores));
    }

    @Test
    @DisplayName("GET /households/{id}/chores для дому без обов'язків повертає порожній список, а не помилку")
    void listChores_WithoutChores_ReturnsEmptyList() throws Exception {
        when(choreLifecycleService.listChores(HOUSEHOLD_ID)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL, HOUSEHOLD_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("chores/list"))
                .andExpect(model().attribute("chores", List.of()));
    }

    @Test
    @DisplayName("GET /households/{id}/chores/new віддає порожню форму зі значеннями за замовчуванням")
    void showCreateForm_ReturnsEmptyForm() throws Exception {
        mockMvc.perform(get(BASE_URL + "/new", HOUSEHOLD_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("chores/create-form"))
                .andExpect(model().attribute("householdId", HOUSEHOLD_ID))
                .andExpect(model().attribute("form", CreateChoreForm.empty()))
                .andExpect(model().hasNoErrors());
    }

    @Test
    @DisplayName("GET /households/{id}/chores/new нічого не створює в сервісі")
    void showCreateForm_DoesNotTouchService() throws Exception {
        mockMvc.perform(get(BASE_URL + "/new", HOUSEHOLD_ID)).andExpect(status().isOk());

        verifyNoInteractions(choreLifecycleService);
    }

    @Test
    @DisplayName("GET з некоректним UUID дому не доходить до сервісу")
    void listChores_WithMalformedHouseholdId_DoesNotCallService() throws Exception {
        mockMvc.perform(get(BASE_URL, "not-a-uuid"))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(choreLifecycleService);
    }

    @Test
    @DisplayName("список запитується в сервісу рівно один раз")
    void listChores_CallsServiceOnce() throws Exception {
        when(choreLifecycleService.listChores(HOUSEHOLD_ID)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL, HOUSEHOLD_ID)).andExpect(status().isOk());

        verify(choreLifecycleService).listChores(HOUSEHOLD_ID);
    }

    // ---------- POST /households/{id}/chores ----------

    @Test
    @DisplayName("POST з коректними даними створює обов'язок і перенаправляє на список (PRG)")
    void createChore_WithValidForm_CreatesAndRedirectsToList() throws Exception {
        mockMvc.perform(post(BASE_URL, HOUSEHOLD_ID)
                        .param("name", "Винести сміття")
                        .param("description", "До 20:00")
                        .param("recurrenceDays", "3")
                        .param("requiresConfirmation", "true"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/households/" + HOUSEHOLD_ID + "/chores"));

        verify(choreLifecycleService).createChore(HOUSEHOLD_ID, "Винести сміття", "До 20:00", 3, true);
    }

    @Test
    @DisplayName("PRG: успіх кладе flash-повідомлення, а не query-параметр")
    void createChore_OnSuccess_SetsFlashMessageWithoutQueryParams() throws Exception {
        mockMvc.perform(post(BASE_URL, HOUSEHOLD_ID)
                        .param("name", "Винести сміття")
                        .param("recurrenceDays", "3"))
                .andExpect(status().isFound())
                .andExpect(flash().attribute("successMessage", "Обов’язок «Винести сміття» створено."))
                .andExpect(redirectedUrl("/households/" + HOUSEHOLD_ID + "/chores"));
    }

    @Test
    @DisplayName("PRG: за помилок валідації flash-повідомлення немає")
    void createChore_OnValidationError_HasNoFlashMessage() throws Exception {
        mockMvc.perform(post(BASE_URL, HOUSEHOLD_ID)
                        .param("name", "")
                        .param("recurrenceDays", "7"))
                .andExpect(status().isOk())
                .andExpect(flash().attributeCount(0));
    }

    @Test
    @DisplayName("PRG: GET списку після редиректу бачить flash-повідомлення в моделі")
    void listChores_AfterRedirect_ExposesFlashMessage() throws Exception {
        when(choreLifecycleService.listChores(HOUSEHOLD_ID)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL, HOUSEHOLD_ID).flashAttr("successMessage", "Обов’язок «Посуд» створено."))
                .andExpect(status().isOk())
                .andExpect(model().attribute("successMessage", "Обов’язок «Посуд» створено."));
    }

    @Test
    @DisplayName("POST без прапорця підтвердження передає requiresConfirmation=false")
    void createChore_WithoutCheckbox_PassesFalse() throws Exception {
        mockMvc.perform(post(BASE_URL, HOUSEHOLD_ID)
                        .param("name", "Посуд")
                        .param("recurrenceDays", "1"))
                .andExpect(status().isFound());

        verify(choreLifecycleService).createChore(HOUSEHOLD_ID, "Посуд", null, 1, false);
    }

    @Test
    @DisplayName("POST обрізає пробіли в назві, а порожній опис передає як null")
    void createChore_TrimsTextFields() throws Exception {
        mockMvc.perform(post(BASE_URL, HOUSEHOLD_ID)
                        .param("name", "  Прибирання  ")
                        .param("description", "   ")
                        .param("recurrenceDays", "7"))
                .andExpect(status().isFound());

        verify(choreLifecycleService).createChore(HOUSEHOLD_ID, "Прибирання", null, 7, false);
    }

    @Test
    @DisplayName("POST з порожньою назвою повертає ту саму форму з помилкою поля name, без редиректу")
    void createChore_WithBlankName_ReRendersFormWithErrors() throws Exception {
        mockMvc.perform(post(BASE_URL, HOUSEHOLD_ID)
                        .param("name", "   ")
                        .param("recurrenceDays", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("chores/create-form"))
                .andExpect(model().attribute("householdId", HOUSEHOLD_ID))
                .andExpect(model().attributeHasFieldErrors("form", "name"));

        verifyNoInteractions(choreLifecycleService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "366", "abc", "3.5", "-1", ""})
    @DisplayName("POST з некоректним інтервалом повертає форму з помилкою recurrenceDays")
    void createChore_WithInvalidRecurrence_ReRendersFormWithErrors(String recurrenceDays) throws Exception {
        mockMvc.perform(post(BASE_URL, HOUSEHOLD_ID)
                        .param("name", "Прибирання")
                        .param("recurrenceDays", recurrenceDays))
                .andExpect(status().isOk())
                .andExpect(view().name("chores/create-form"))
                .andExpect(model().attributeHasFieldErrors("form", "recurrenceDays"));

        verify(choreLifecycleService, never()).createChore(any(), anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    @DisplayName("POST з назвою понад 120 символів відхиляється")
    void createChore_WithTooLongName_IsRejected() throws Exception {
        mockMvc.perform(post(BASE_URL, HOUSEHOLD_ID)
                        .param("name", "а".repeat(121))
                        .param("recurrenceDays", "7"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "name"));

        verifyNoInteractions(choreLifecycleService);
    }

    @Test
    @DisplayName("після помилки валідації форма зберігає введені значення")
    void createChore_OnError_KeepsSubmittedValues() throws Exception {
        mockMvc.perform(post(BASE_URL, HOUSEHOLD_ID)
                        .param("name", "Прибирання")
                        .param("description", "Підлога")
                        .param("recurrenceDays", "999")
                        .param("requiresConfirmation", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("form",
                        new CreateChoreForm("Прибирання", "Підлога", "999", true)));
    }
}