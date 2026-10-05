package genius.project.orchestrate.household;

import genius.project.orchestrate.household.dto.HouseholdCreateRequest;
import genius.project.orchestrate.household.dto.HouseholdResponse;
import genius.project.orchestrate.household.dto.OwnershipTransferRequest;
import genius.project.orchestrate.household.internal.domain.Household;
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
import java.util.UUID;

@Tag(name = "Домогосподарства", description = "Створення домогосподарств, перегляд, видалення та передача власності")
@RestController
@RequestMapping("/api/v1/households")
public class HouseholdController {

    private final HouseholdService householdService;

    public HouseholdController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @Operation(summary = "Створити домогосподарство",
            description = "Поточний користувач (заголовок X-User-Id) стає власником і отримує всі адміністративні права.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Домогосподарство створено",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = HouseholdResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некоректні вхідні дані (VALIDATION_FAILED)"),
            @ApiResponse(responseCode = "404", description = "Поточного користувача не існує (RESOURCE_NOT_FOUND)")
    })
    @PostMapping
    public ResponseEntity<HouseholdResponse> createHousehold(@Valid @RequestBody HouseholdCreateRequest request,
                                                               UriComponentsBuilder uriBuilder) {
        Household household = householdService.createHousehold(request.name());
        URI location = uriBuilder.path("/api/v1/households/{id}").buildAndExpand(household.id()).toUri();
        return ResponseEntity.created(location).body(HouseholdResponse.from(household));
    }

    @Operation(summary = "Отримати домогосподарство за ID", description = "Доступно лише учасникам домогосподарства.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Домогосподарство знайдено"),
            @ApiResponse(responseCode = "403", description = "Користувач не є учасником (NOT_HOUSEHOLD_MEMBER)"),
            @ApiResponse(responseCode = "404", description = "Домогосподарство не знайдено (HOUSEHOLD_NOT_FOUND)")
    })
    @GetMapping("/{householdId}")
    public HouseholdResponse getHousehold(@Parameter(description = "Ідентифікатор домогосподарства", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
                                          @PathVariable UUID householdId) {
        return HouseholdResponse.from(householdService.getHousehold(householdId));
    }

    @Operation(summary = "Видалити домогосподарство",
            description = "Лише власник. Разом із домогосподарством видаляються членства та код запрошення.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Домогосподарство видалено"),
            @ApiResponse(responseCode = "403", description = "Користувач не є власником (NOT_HOUSEHOLD_OWNER)"),
            @ApiResponse(responseCode = "404", description = "Домогосподарство не знайдено (HOUSEHOLD_NOT_FOUND)")
    })
    @DeleteMapping("/{householdId}")
    public ResponseEntity<Void> deleteHousehold(@Parameter(description = "Ідентифікатор домогосподарства", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
                                                @PathVariable UUID householdId) {
        householdService.deleteHousehold(householdId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Передати право власності",
            description = "Лише поточний власник. Новий власник має бути учасником домогосподарства і отримує всі права.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Право власності передано"),
            @ApiResponse(responseCode = "400", description = "Некоректні дані або передача самому собі (OWNERSHIP_TRANSFER_TO_SELF)"),
            @ApiResponse(responseCode = "403", description = "Користувач не є власником (NOT_HOUSEHOLD_OWNER)"),
            @ApiResponse(responseCode = "404", description = "Домогосподарство чи учасника не знайдено")
    })
    @PostMapping("/{householdId}/ownership-transfer")
    public HouseholdResponse transferOwnership(@Parameter(description = "Ідентифікатор домогосподарства", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
                                               @PathVariable UUID householdId,
                                               @Valid @RequestBody OwnershipTransferRequest request) {
        return HouseholdResponse.from(householdService.transferOwnership(householdId, request.newOwnerUserId()));
    }
}
