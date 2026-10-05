package genius.project.orchestrate.swap;

import genius.project.orchestrate.swap.dto.SwapRequestRequest;
import genius.project.orchestrate.swap.dto.SwapRequestResponse;
import genius.project.orchestrate.swap.dto.SwapRequestStatusRequest;
import genius.project.orchestrate.swap.dto.SwapRequestSummaryResponse;
import genius.project.orchestrate.swap.dto.SwapRequestUpdateRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Обмін чергою", description = "Запити на обмін чергою виконання між учасниками групи обов'язку")
@RestController
@RequestMapping("/api/v1/chores/{choreId}/swap-requests")
public class SwapRequestController {

    private final SwapRequestService swapRequestService;

    public SwapRequestController(SwapRequestService swapRequestService) {
        this.swapRequestService = swapRequestService;
    }

    // TODO: add access check — current user must belong to the household that owns
    //  this chore. Pending the authorization layer and a public port from the
    //  household module to resolve household membership by choreId.
    @Operation(summary = "Отримати запити на обмін",
            description = "Усі запити по обов'язку; за потреби відфільтровані за статусом.")
    @ApiResponse(responseCode = "200", description = "Список запитів на обмін")
    @GetMapping
    public ResponseEntity<List<SwapRequestResponse>> getSwapRequests(
            @Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID choreId,
            @Parameter(description = "Фільтр за статусом (необов'язковий)", example = "PENDING")
            @RequestParam(required = false) SwapRequestStatus status) {
        return ResponseEntity.ok(swapRequestService.getSwapRequests(choreId, status));
    }

    @Operation(summary = "Отримати зведення запитів за статусами")
    @ApiResponse(responseCode = "200", description = "Кількість запитів за статусами")
    @GetMapping("/summary")
    public ResponseEntity<SwapRequestSummaryResponse> getSummary(
            @Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID choreId) {
        return ResponseEntity.ok(swapRequestService.getSummary(choreId));
    }

    @Operation(summary = "Отримати запит на обмін за ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Запит знайдено"),
            @ApiResponse(responseCode = "404", description = "Запит не знайдено (SWAP_REQUEST_NOT_FOUND)")
    })
    @GetMapping("/{requestId}")
    public ResponseEntity<SwapRequestResponse> getSwapRequest(
            @Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID choreId,
            @Parameter(description = "Ідентифікатор запиту на обмін", example = "e4b1d6a0-3c52-4f7e-8a19-6d2c9b0f5a73")
            @PathVariable UUID requestId) {
        return ResponseEntity.ok(swapRequestService.getSwapRequest(choreId, requestId));
    }

    @Operation(summary = "Створити запит на обмін чергою",
            description = "Ініціатор (поточний користувач) і отримувач мають бути в групі виконання обов'язку. "
                    + "Для TEMPORARY потрібен номер майбутнього циклу.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Запит створено",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SwapRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некоректні дані: обмін із самим собою, не учасник групи "
                    + "або хибний номер циклу (INVALID_SWAP_REQUEST_RECIPIENT / NOT_CHORE_PARTICIPANT / ...)"),
            @ApiResponse(responseCode = "409", description = "Такий запит уже очікує відповіді (SWAP_REQUEST_ALREADY_EXISTS)")
    })
    @PostMapping
    public ResponseEntity<SwapRequestResponse> createSwapRequest(
            @Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID choreId,
            @Valid @RequestBody SwapRequestRequest request) {
        SwapRequestResponse created = swapRequestService.createSwapRequest(choreId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Змінити запит на обмін",
            description = "Лише ініціатор і лише поки запит у статусі PENDING.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Запит оновлено"),
            @ApiResponse(responseCode = "400", description = "Некоректні вхідні дані або номер циклу"),
            @ApiResponse(responseCode = "403", description = "Користувач не є ініціатором (NOT_SWAP_REQUEST_INITIATOR)"),
            @ApiResponse(responseCode = "404", description = "Запит не знайдено (SWAP_REQUEST_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "Запит уже не можна змінити (SWAP_REQUEST_NOT_EDITABLE)")
    })
    @PutMapping("/{requestId}")
    public ResponseEntity<SwapRequestResponse> updateSwapRequest(
            @Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID choreId,
            @Parameter(description = "Ідентифікатор запиту на обмін", example = "e4b1d6a0-3c52-4f7e-8a19-6d2c9b0f5a73")
            @PathVariable UUID requestId,
            @Valid @RequestBody SwapRequestUpdateRequest request) {
        return ResponseEntity.ok(swapRequestService.updateSwapRequest(choreId, requestId, request));
    }

    @Operation(summary = "Прийняти або відхилити запит на обмін",
            description = "Лише отримувач. Прийняття запускає обмін чергою в ротації.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Відповідь збережено"),
            @ApiResponse(responseCode = "400", description = "Некоректні вхідні дані (VALIDATION_FAILED)"),
            @ApiResponse(responseCode = "403", description = "Користувач не є отримувачем (NOT_SWAP_REQUEST_RECEIVER)"),
            @ApiResponse(responseCode = "404", description = "Запит не знайдено (SWAP_REQUEST_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "Учасники вже не в одній групі (NOT_IN_SAME_GROUP)"),
            @ApiResponse(responseCode = "422", description = "Недозволений перехід статусу (INVALID_SWAP_REQUEST_STATUS)")
    })
    @PatchMapping("/{requestId}")
    public ResponseEntity<SwapRequestResponse> respondToSwapRequest(
            @Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID choreId,
            @Parameter(description = "Ідентифікатор запиту на обмін", example = "e4b1d6a0-3c52-4f7e-8a19-6d2c9b0f5a73")
            @PathVariable UUID requestId,
            @Valid @RequestBody SwapRequestStatusRequest request) {
        return ResponseEntity.ok(swapRequestService.respondToSwapRequest(choreId, requestId, request));
    }

    @Operation(summary = "Видалити запит на обмін",
            description = "Лише ініціатор і лише поки запит у статусі PENDING.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Запит видалено"),
            @ApiResponse(responseCode = "403", description = "Користувач не є ініціатором (NOT_SWAP_REQUEST_INITIATOR)"),
            @ApiResponse(responseCode = "404", description = "Запит не знайдено (SWAP_REQUEST_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "Запит уже не можна видалити (SWAP_REQUEST_NOT_EDITABLE)")
    })
    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> deleteSwapRequest(
            @Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID choreId,
            @Parameter(description = "Ідентифікатор запиту на обмін", example = "e4b1d6a0-3c52-4f7e-8a19-6d2c9b0f5a73")
            @PathVariable UUID requestId) {
        swapRequestService.deleteSwapRequest(choreId, requestId);
        return ResponseEntity.noContent().build();
    }
}
