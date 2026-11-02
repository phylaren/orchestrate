package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.ParticipantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Учасники обов'язку", description = "Група виконання (пул ротації) обов'язку")
@RestController
@RequestMapping("/api/v1/households/{householdId}/chores/{choreId}/participants")
public class ChoreParticipantController {

    private final ChoreParticipantService choreParticipantService;

    public ChoreParticipantController(ChoreParticipantService choreParticipantService) {
        this.choreParticipantService = choreParticipantService;
    }

    @Operation(summary = "Приєднатися до виконання обов'язку",
            description = "Поточний користувач добровільно входить до групи виконання. "
                    + "Якщо група була порожня, він одразу стає відповідальним на поточний цикл.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Користувач приєднався до групи виконання",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ParticipantResponse.class))),
            @ApiResponse(responseCode = "404", description = "Обов'язок не знайдено (RESOURCE_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "Користувач уже в групі (ALREADY_PARTICIPANT)")
    })
    @PostMapping
    public ResponseEntity<ParticipantResponse> join(@PathVariable UUID householdId,
                                                    @Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                                                    @PathVariable UUID choreId,
                                                    UriComponentsBuilder uriBuilder) {
        ParticipantResponse participant = choreParticipantService.joinChore(choreId);
        URI location = uriBuilder
                .path("/api/v1/households/{householdId}/chores/{choreId}/participants/{userId}")
                .buildAndExpand(householdId, choreId, participant.userId())
                .toUri();
        return ResponseEntity.created(location).body(participant);
    }

    @Operation(summary = "Додати учасника до групи виконання (адміністратор)",
            description = "Адміністратор додає іншого мешканця до групи виконання вручну. Потребує права MANAGE_CHORES.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Учасника додано",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ParticipantResponse.class))),
            @ApiResponse(responseCode = "403", description = "Недостатньо прав (ACCESS_DENIED)"),
            @ApiResponse(responseCode = "404", description = "Обов'язок не знайдено (RESOURCE_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "Користувач уже в групі (ALREADY_PARTICIPANT)")
    })
    @PreAuthorize("@householdAccess.hasPermission(authentication, #householdId, 'MANAGE_CHORES')")
    @PutMapping("/{userId}")
    public ResponseEntity<ParticipantResponse> addParticipant(@PathVariable UUID householdId,
                                                              @Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                                                              @PathVariable UUID choreId,
                                                              @Parameter(description = "Ідентифікатор користувача", example = "5d1c0e7a-2b3f-4c8d-9e6a-7f8b9c0d1e2f")
                                                              @PathVariable UUID userId,
                                                              UriComponentsBuilder uriBuilder) {
        ParticipantResponse participant = choreParticipantService.addParticipant(choreId, userId);
        URI location = uriBuilder
                .path("/api/v1/households/{householdId}/chores/{choreId}/participants/{userId}")
                .buildAndExpand(householdId, choreId, participant.userId())
                .toUri();
        return ResponseEntity.created(location).body(participant);
    }

    @Operation(summary = "Отримати учасників групи виконання")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список учасників"),
            @ApiResponse(responseCode = "404", description = "Обов'язок не знайдено (RESOURCE_NOT_FOUND)")
    })
    @GetMapping
    public List<ParticipantResponse> list(@PathVariable UUID householdId,
                                          @Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                                          @PathVariable UUID choreId) {
        return choreParticipantService.listParticipants(choreId);
    }

    @Operation(summary = "Вийти з групи виконання або видалити учасника",
            description = "Користувач може вийти сам, або адміністратор (MANAGE_CHORES) може видалити іншого.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Учасника видалено з групи"),
            @ApiResponse(responseCode = "403", description = "Недостатньо прав (ACCESS_DENIED)"),
            @ApiResponse(responseCode = "404", description = "Обов'язок або учасника не знайдено (RESOURCE_NOT_FOUND)")
    })
    @PreAuthorize("#userId == authentication.principal.id or @householdAccess.hasPermission(authentication, #householdId, 'MANAGE_CHORES')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> remove(@PathVariable UUID householdId,
                                       @Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                                       @PathVariable UUID choreId,
                                       @Parameter(description = "Ідентифікатор користувача", example = "5d1c0e7a-2b3f-4c8d-9e6a-7f8b9c0d1e2f")
                                       @PathVariable UUID userId) {
        choreParticipantService.removeMember(choreId, userId);
        return ResponseEntity.noContent().build();
    }
}