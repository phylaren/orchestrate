package genius.project.orchestrate.household;

import genius.project.orchestrate.household.dto.MembershipResponse;
import genius.project.orchestrate.household.dto.PermissionsUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Учасники домогосподарства", description = "Мешканці домогосподарства та їхні адміністративні права")
@RestController
@RequestMapping("/api/v1/households/{householdId}/members")
public class HouseholdMemberController {

    private final HouseholdService householdService;

    public HouseholdMemberController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @Operation(summary = "Отримати список учасників", description = "Відсортований за часом приєднання. Лише для учасників.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список учасників"),
            @ApiResponse(responseCode = "403", description = "Користувач не є учасником (NOT_HOUSEHOLD_MEMBER)"),
            @ApiResponse(responseCode = "404", description = "Домогосподарство не знайдено (HOUSEHOLD_NOT_FOUND)")
    })
    @GetMapping
    public List<MembershipResponse> list(@Parameter(description = "Ідентифікатор домогосподарства", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
                                         @PathVariable UUID householdId) {
        return householdService.listMembers(householdId).stream()
                .map(MembershipResponse::from)
                .toList();
    }

    @Operation(summary = "Отримати учасника за ID користувача")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Учасника знайдено"),
            @ApiResponse(responseCode = "403", description = "Користувач не є учасником (NOT_HOUSEHOLD_MEMBER)"),
            @ApiResponse(responseCode = "404", description = "Домогосподарство або учасника не знайдено")
    })
    @GetMapping("/{userId}")
    public MembershipResponse get(@Parameter(description = "Ідентифікатор домогосподарства", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
                                  @PathVariable UUID householdId,
                                  @Parameter(description = "Ідентифікатор користувача", example = "5d1c0e7a-2b3f-4c8d-9e6a-7f8b9c0d1e2f")
                                  @PathVariable UUID userId) {
        return MembershipResponse.from(householdService.getMember(householdId, userId));
    }

    @Operation(summary = "Видалити учасника або залишити домогосподарство",
            description = "Якщо userId збігається з поточним користувачем — це вихід із домогосподарства "
                    + "(власник може вийти лише наодинці, інакше має передати власність). "
                    + "Видалення іншого учасника потребує права MANAGE_MEMBERS.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Учасника видалено / користувач залишив домогосподарство"),
            @ApiResponse(responseCode = "403", description = "Недостатньо прав (MISSING_PERMISSION / NOT_HOUSEHOLD_MEMBER)"),
            @ApiResponse(responseCode = "404", description = "Домогосподарство або учасника не знайдено"),
            @ApiResponse(responseCode = "409", description = "Власника не можна видалити "
                    + "(CANNOT_REMOVE_OWNER / OWNER_MUST_TRANSFER_OWNERSHIP)")
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> remove(@Parameter(description = "Ідентифікатор домогосподарства", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
                                       @PathVariable UUID householdId,
                                       @Parameter(description = "Ідентифікатор користувача", example = "5d1c0e7a-2b3f-4c8d-9e6a-7f8b9c0d1e2f")
                                       @PathVariable UUID userId) {
        householdService.removeMember(householdId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Змінити права учасника",
            description = "Потрібне право MANAGE_MEMBERS. Не можна змінювати права власника та власні права.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Права оновлено"),
            @ApiResponse(responseCode = "400", description = "Некоректні вхідні дані (VALIDATION_FAILED)"),
            @ApiResponse(responseCode = "403", description = "Недостатньо прав (MISSING_PERMISSION / NOT_HOUSEHOLD_MEMBER)"),
            @ApiResponse(responseCode = "404", description = "Домогосподарство або учасника не знайдено"),
            @ApiResponse(responseCode = "409", description = "Заборонена зміна прав "
                    + "(OWNER_PERMISSIONS_IMMUTABLE / SELF_PERMISSION_CHANGE_NOT_ALLOWED)")
    })
    @PutMapping("/{userId}/permissions")
    public MembershipResponse updatePermissions(@Parameter(description = "Ідентифікатор домогосподарства", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
                                                @PathVariable UUID householdId,
                                                @Parameter(description = "Ідентифікатор користувача", example = "5d1c0e7a-2b3f-4c8d-9e6a-7f8b9c0d1e2f")
                                                @PathVariable UUID userId,
                                                @Valid @RequestBody PermissionsUpdateRequest request) {
        return MembershipResponse.from(
                householdService.updatePermissions(householdId, userId, request.permissions()));
    }
}
