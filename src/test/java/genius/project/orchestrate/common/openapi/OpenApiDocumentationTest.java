package genius.project.orchestrate.common.openapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class OpenApiDocumentationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("GET /v3/api-docs повертає специфікацію OpenAPI з метаданими сервісу")
    void apiDocs_ExposeSpecificationWithServiceInfo() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("Orchestrate API"));
    }

    @Test
    @DisplayName("Специфікація містить ендпоінти ключового сценарію")
    void apiDocs_ContainKeyScenarioEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/users'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/households'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/households/join'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/chores'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/chores/{choreId}/completions'].post").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/chores/{choreId}/completions/{completionId}/confirmation'].post")
                        .exists());
    }

    @Test
    @DisplayName("Схеми моделей містять приклади значень (example)")
    void apiDocs_ContainSchemaExamples() throws Exception {
        String spec = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.UserCreateRequest").exists())
                .andExpect(jsonPath("$.components.schemas.ChoreCreateRequest").exists())
                .andReturn().getResponse().getContentAsString();

        assertThat(spec).contains("anna.petrenko@example.com");
        assertThat(spec).contains("Винести сміття");
    }

    @Test
    @DisplayName("Помилкові відповіді описані схемою ProblemDetail (application/problem+json)")
    void apiDocs_DescribeErrorResponsesWithProblemDetail() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.code").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/users'].post.responses['409'].content['application/problem+json']")
                        .exists());
    }

    @Test
    @DisplayName("Специфікація оголошує схему автентифікації через заголовок X-User-Id")
    void apiDocs_DeclareUserIdSecurityScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.UserIdHeader.name").value("X-User-Id"))
                .andExpect(jsonPath("$.components.securitySchemes.UserIdHeader.in").value("header"));
    }
}
