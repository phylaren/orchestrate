package genius.project.orchestrate.common.openai;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfiguration {

    static final String USER_HEADER_SCHEME = "UserIdHeader";
    static final String PROBLEM_DETAIL_SCHEMA = "ProblemDetail";
    static final String PROBLEM_JSON = "application/problem+json";

    @Bean
    public OpenAPI orchestrateOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Orchestrate API")
                        .version("0.0.1")
                        .description("""
                                REST API застосунку Orchestrate — справедливий розподіл побутових обов'язків \
                                між мешканцями спільного домогосподарства: домогосподарства та членство, \
                                обов'язки, ротація відповідального, підтвердження виконання, обмін чергою.

                                **Автентифікація.** Spring Security + JWT ще не підключено: поточного користувача \
                                визначає заголовок `X-User-Id` (UUID користувача). Натисніть **Authorize** і \
                                вкажіть UUID — він додаватиметься до кожного запиту. Без заголовка \
                                використовується користувач за замовчуванням.

                                **Помилки** повертаються у форматі `application/problem+json` (RFC 9457) \
                                зі стабільним полем `code`. Кожна відповідь містить заголовок `X-Trace-Id` \
                                для кореляції із записами в журналі.""")
                        .contact(new Contact().name("Orchestrate Team")))
                .servers(List.of(new Server()
                        .url("http://localhost:8080")
                        .description("Локальне середовище")))
                .addSecurityItem(new SecurityRequirement().addList(USER_HEADER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(USER_HEADER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-User-Id")
                                .description("UUID поточного користувача (тимчасова заміна JWT)."))
                        .addSchemas(PROBLEM_DETAIL_SCHEMA, problemDetailSchema()));
    }

    @Bean
    public OperationCustomizer problemDetailForErrorResponses() {
        return (operation, handlerMethod) -> {
            if (operation.getResponses() != null) {
                operation.getResponses().forEach((code, response) -> {
                    boolean isError = code.startsWith("4") || code.startsWith("5");
                    if (isError && response.getContent() == null) {
                        response.setContent(new Content().addMediaType(PROBLEM_JSON,
                                new MediaType().schema(new Schema<>().$ref(
                                        "#/components/schemas/" + PROBLEM_DETAIL_SCHEMA))));
                    }
                });
            }
            return operation;
        };
    }

    private Schema<?> problemDetailSchema() {
        ArraySchema errors = new ArraySchema();
        errors.setDescription("Помилки полів (лише для 400 VALIDATION_FAILED).");
        errors.setItems(new ObjectSchema()
                .addProperty("field", new StringSchema().example("email"))
                .addProperty("message", new StringSchema()
                        .example("email must be a well-formed email address")));

        return new ObjectSchema()
                .description("Опис помилки за RFC 9457 (Problem Details for HTTP APIs).")
                .addProperty("type", new StringSchema()
                        .example("https://orchestrate.example.com/problems/resource-not-found"))
                .addProperty("title", new StringSchema().example("Resource not found"))
                .addProperty("status", new IntegerSchema().example(404))
                .addProperty("detail", new StringSchema()
                        .example("Chore '7c9e6679-7425-40de-944b-e07fc1f90ae7' was not found."))
                .addProperty("instance", new StringSchema()
                        .example("/api/v1/chores/7c9e6679-7425-40de-944b-e07fc1f90ae7"))
                .addProperty("code", new StringSchema()
                        .description("Стабільний машинозчитуваний код помилки.")
                        .example("RESOURCE_NOT_FOUND"))
                .addProperty("timestamp", new StringSchema().format("date-time")
                        .example("2026-10-05T10:15:30Z"))
                .addProperty("errors", errors);
    }
}
