package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.CompletionResponse;
import genius.project.orchestrate.chore.dto.ConfirmationDecisionRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Виконання обов'язків", description = "Відмітка виконання та його підтвердження")
@RestController
@RequestMapping("/api/v1/chores/{choreId}/completions")
public class ChoreCompletionController {

    private final ChoreCompletionService choreCompletionService;

    public ChoreCompletionController(ChoreCompletionService choreCompletionService) {
        this.choreCompletionService = choreCompletionService;
    }

    @Operation(summary = "Відмітити обов'язок виконаним",
            description = "Може лише поточний відповідальний. Якщо обов'язок потребує підтвердження — запис отримує "
                    + "статус PENDING; інакше NOT_REQUIRED, а ротація одразу переходить до наступного циклу.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Виконання зафіксовано",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CompletionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Обов'язок не знайдено (RESOURCE_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "Користувач не є поточним відповідальним або група порожня "
                    + "(NOT_CURRENT_RESPONSIBLE / NO_ACTIVE_ASSIGNMENT)")
    })
    @PostMapping
    public ResponseEntity<CompletionResponse> markCompleted(@Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                                                            @PathVariable UUID choreId,
                                                            UriComponentsBuilder uriBuilder) {
        CompletionResponse completion = choreCompletionService.markCompleted(choreId);
        URI location = uriBuilder
                .path("/api/v1/chores/{choreId}/completions/{completionId}")
                .buildAndExpand(choreId, completion.id())
                .toUri();
        return ResponseEntity.created(location).body(completion);
    }

    @Operation(summary = "Отримати історію виконань", description = "Відсортована за часом виконання.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список записів про виконання"),
            @ApiResponse(responseCode = "404", description = "Обов'язок не знайдено (RESOURCE_NOT_FOUND)")
    })
    @GetMapping
    public List<CompletionResponse> list(@Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                                         @PathVariable UUID choreId) {
        return choreCompletionService.listCompletions(choreId);
    }

    @Operation(summary = "Отримати запис про виконання за ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Запис знайдено"),
            @ApiResponse(responseCode = "404", description = "Обов'язок або запис не знайдено (RESOURCE_NOT_FOUND)")
    })
    @GetMapping("/{completionId}")
    public CompletionResponse get(@Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                                  @PathVariable UUID choreId,
                                  @Parameter(description = "Ідентифікатор запису про виконання", example = "c0a8f3d2-6b1e-4f9a-9d3c-5e7b8a1f2c4d")
                                  @PathVariable UUID completionId) {
        return choreCompletionService.getCompletion(choreId, completionId);
    }

    @Operation(summary = "Підтвердити або відхилити виконання",
            description = "Рішення ухвалює учасник дому, відмінний від виконавця. Підтвердження переводить ротацію "
                    + "до наступного циклу. Запис має бути у статусі PENDING.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Рішення збережено"),
            @ApiResponse(responseCode = "400", description = "Некоректні вхідні дані (VALIDATION_FAILED)"),
            @ApiResponse(responseCode = "404", description = "Обов'язок або запис не знайдено (RESOURCE_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "Виконавець не може підтверджувати власне виконання "
                    + "(SELF_CONFIRMATION_NOT_ALLOWED)"),
            @ApiResponse(responseCode = "422", description = "Недозволений перехід статусу (INVALID_CONFIRMATION_STATUS)")
    })
    @PostMapping("/{completionId}/confirmation")
    public CompletionResponse decideConfirmation(@Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                                                 @PathVariable UUID choreId,
                                                 @Parameter(description = "Ідентифікатор запису про виконання", example = "c0a8f3d2-6b1e-4f9a-9d3c-5e7b8a1f2c4d")
                                                 @PathVariable UUID completionId,
                                                 @Valid @RequestBody ConfirmationDecisionRequest request) {
        return choreCompletionService.decideConfirmation(choreId, completionId, request.approved());
    }

    @Operation(summary = "Видалити запис про виконання",
            description = "Дозволено лише для записів у статусах PENDING або REJECTED.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Запис видалено"),
            @ApiResponse(responseCode = "404", description = "Обов'язок або запис не знайдено (RESOURCE_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "Запис уже не можна видалити (COMPLETION_NOT_DELETABLE)")
    })
    @DeleteMapping("/{completionId}")
    public ResponseEntity<Void> delete(@Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                                       @PathVariable UUID choreId,
                                       @Parameter(description = "Ідентифікатор запису про виконання", example = "c0a8f3d2-6b1e-4f9a-9d3c-5e7b8a1f2c4d")
                                       @PathVariable UUID completionId) {
        choreCompletionService.deleteCompletion(choreId, completionId);
        return ResponseEntity.noContent().build();
    }
}
