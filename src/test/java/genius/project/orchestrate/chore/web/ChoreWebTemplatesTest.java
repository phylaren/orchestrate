package genius.project.orchestrate.chore.web;

import genius.project.orchestrate.chore.ChoreLifecycleService;
import genius.project.orchestrate.chore.dto.ChoreResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChoreWebController.class)
class ChoreWebTemplatesTest {

    private static final UUID HOUSEHOLD_ID = UUID.randomUUID();
    private static final String BASE_URL = "/households/{householdId}/chores";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChoreLifecycleService choreLifecycleService;

    private static ChoreResponse chore(String name, boolean needsAttention) {
        return new ChoreResponse(UUID.randomUUID(), HOUSEHOLD_ID, name, "Опис обов’язку", 7, true,
                needsAttention, Instant.parse("2026-10-05T10:25:00Z"));
    }

    // ---------- list.html ----------

    @Test
    @DisplayName("list.html: рядок таблиці містить назву, інтервал та посилання «Додати» з id дому")
    void list_RendersChoreRowAndCreateLink() throws Exception {
        when(choreLifecycleService.listChores(HOUSEHOLD_ID)).thenReturn(List.of(chore("Прибирання", false)));

        mockMvc.perform(get(BASE_URL, HOUSEHOLD_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Прибирання")))
                .andExpect(content().string(containsString("Опис обов’язку")))
                .andExpect(content().string(containsString("href=\"/households/" + HOUSEHOLD_ID + "/chores/new\"")))
                .andExpect(content().string(containsString("Є виконавці")))
                .andExpect(content().string(not(containsString("needs-attention"))));
    }

    @Test
    @DisplayName("list.html: обов'язок без учасників виділяється кольором і бейджем «Потребує уваги»")
    void list_HighlightsChoreThatNeedsAttention() throws Exception {
        when(choreLifecycleService.listChores(HOUSEHOLD_ID)).thenReturn(List.of(chore("Посуд", true)));

        mockMvc.perform(get(BASE_URL, HOUSEHOLD_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("needs-attention")))
                .andExpect(content().string(containsString("Потребує уваги")));
    }

    @Test
    @DisplayName("list.html: порожній список показує підказку замість таблиці")
    void list_WithoutChores_ShowsEmptyState() throws Exception {
        when(choreLifecycleService.listChores(HOUSEHOLD_ID)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL, HOUSEHOLD_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ще немає обов’язків")))
                .andExpect(content().string(not(containsString("<table"))));
    }

    @Test
    @DisplayName("list.html: flash-повідомлення відображається один раз із екрануванням")
    void list_ShowsFlashMessage() throws Exception {
        when(choreLifecycleService.listChores(HOUSEHOLD_ID)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL, HOUSEHOLD_ID).flashAttr("successMessage", "Обов’язок «Посуд» створено."))
                .andExpect(content().string(containsString("alert-success")))
                .andExpect(content().string(containsString("Обов’язок «Посуд» створено.")));

        mockMvc.perform(get(BASE_URL, HOUSEHOLD_ID))
                .andExpect(content().string(not(containsString("alert-success"))));
    }

    @Test
    @DisplayName("list.html: користувацький текст екранується (th:text), скрипт не виконується")
    void list_EscapesUserProvidedText() throws Exception {
        when(choreLifecycleService.listChores(HOUSEHOLD_ID))
                .thenReturn(List.of(chore("<script>alert(1)</script>", false)));

        mockMvc.perform(get(BASE_URL, HOUSEHOLD_ID))
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }

    // ---------- create-form.html ----------

    @Test
    @DisplayName("create-form.html: форма постить на /households/{id}/chores і містить усі поля")
    void createForm_RendersFieldsAndAction() throws Exception {
        mockMvc.perform(get(BASE_URL + "/new", HOUSEHOLD_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/households/" + HOUSEHOLD_ID + "/chores\"")))
                .andExpect(content().string(containsString("method=\"post\"")))
                .andExpect(content().string(containsString("name=\"name\"")))
                .andExpect(content().string(containsString("name=\"description\"")))
                .andExpect(content().string(containsString("name=\"recurrenceDays\"")))
                .andExpect(content().string(containsString("name=\"requiresConfirmation\"")))
                .andExpect(content().string(containsString("value=\"7\"")))
                .andExpect(content().string(not(containsString("field-error"))));
    }

    @Test
    @DisplayName("create-form.html: помилки валідації з'являються під полями, введені значення зберігаються")
    void createForm_OnValidationErrors_ShowsMessagesAndKeepsValues() throws Exception {
        mockMvc.perform(post(BASE_URL, HOUSEHOLD_ID)
                        .param("name", "")
                        .param("description", "Підлога у вітальні")
                        .param("recurrenceDays", "400"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Назва обов’язку є обов’язковою")))
                .andExpect(content().string(containsString("Інтервал має бути цілим числом від 1 до 365")))
                .andExpect(content().string(containsString("Підлога у вітальні")))
                .andExpect(content().string(containsString("value=\"400\"")))
                .andExpect(content().string(containsString("is-invalid")));
    }

    @Test
    @DisplayName("create-form.html: введена назва з HTML екранується при поверненні форми")
    void createForm_EscapesSubmittedValue() throws Exception {
        mockMvc.perform(post(BASE_URL, HOUSEHOLD_ID)
                        .param("name", "\"><script>alert(1)</script>")
                        .param("recurrenceDays", "999"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));
    }
}
