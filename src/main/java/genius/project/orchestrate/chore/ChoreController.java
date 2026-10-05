package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.ChoreCreateRequest;
import genius.project.orchestrate.chore.dto.ChoreResponse;
import genius.project.orchestrate.chore.dto.ChoreUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Обов'язки", description = "Побутові обов'язки домогосподарства: створення, перегляд, зміна, видалення")
@RestController
@RequestMapping("/api/v1/chores")
public class ChoreController {

    private final ChoreLifecycleService choreLifecycleService;

    public ChoreController(ChoreLifecycleService choreLifecycleService) {
        this.choreLifecycleService = choreLifecycleService;
    }

    @Operation(summary = "Створити обов'язок",
            description = "Новий обов'язок без учасників позначається як такий, що потребує уваги (needsAttention = true), "
                    + "доки хтось не приєднається до його виконання.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Обов'язок створено",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChoreResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некоректні вхідні дані (VALIDATION_FAILED)")
    })
    @PostMapping
    public ResponseEntity<ChoreResponse> createChore(@Valid @RequestBody ChoreCreateRequest request,
                                                     UriComponentsBuilder uriBuilder) {
        ChoreResponse chore = choreLifecycleService.createChore(
                request.householdId(),
                request.name(),
                request.description(),
                request.recurrenceDays(),
                request.requiresConfirmation());

        URI location = uriBuilder.path("/api/v1/chores/{id}").buildAndExpand(chore.id()).toUri();
        return ResponseEntity.created(location).body(chore);
    }

    @Operation(summary = "Отримати список обов'язків",
            description = "Без параметра householdId повертає всі обов'язки; відсортовані за часом створення.")
    @ApiResponse(responseCode = "200", description = "Список обов'язків")
    @GetMapping
    public List<ChoreResponse> listChores(
            @Parameter(description = "Фільтр за домогосподарством (необов'язковий)",
                    example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
            @RequestParam(required = false) UUID householdId) {
        return choreLifecycleService.listChores(householdId);
    }

    @Operation(summary = "Отримати обов'язок за ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обов'язок знайдено"),
            @ApiResponse(responseCode = "404", description = "Обов'язок не знайдено (RESOURCE_NOT_FOUND)")
    })
    @GetMapping("/{choreId}")
    public ChoreResponse getChore(@Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                                  @PathVariable UUID choreId) {
        return choreLifecycleService.getChore(choreId);
    }

    @Operation(summary = "Змінити обов'язок", description = "Повна заміна назви, опису, інтервалу та ознаки підтвердження.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обов'язок оновлено"),
            @ApiResponse(responseCode = "400", description = "Некоректні вхідні дані (VALIDATION_FAILED)"),
            @ApiResponse(responseCode = "404", description = "Обов'язок не знайдено (RESOURCE_NOT_FOUND)")
    })
    @PutMapping("/{choreId}")
    public ChoreResponse updateChore(@Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                                     @PathVariable UUID choreId,
                                     @Valid @RequestBody ChoreUpdateRequest request) {
        return choreLifecycleService.updateChore(
                choreId,
                request.name(),
                request.description(),
                request.recurrenceDays(),
                request.requiresConfirmation());
    }

    @Operation(summary = "Видалити обов'язок")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Обов'язок видалено"),
            @ApiResponse(responseCode = "404", description = "Обов'язок не знайдено (RESOURCE_NOT_FOUND)")
    })
    @DeleteMapping("/{choreId}")
    public ResponseEntity<Void> deleteChore(@Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                                            @PathVariable UUID choreId) {
        choreLifecycleService.deleteChore(choreId);
        return ResponseEntity.noContent().build();
    }
}
