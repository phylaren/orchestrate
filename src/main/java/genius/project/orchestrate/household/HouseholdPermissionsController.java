package genius.project.orchestrate.household;

import genius.project.orchestrate.household.dto.HouseholdPermissionsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Учасники домогосподарства", description = "Мешканці домогосподарства та їхні адміністративні права")
@RestController
@RequestMapping("/api/v1/households/{householdId}/members/permissions")
public class HouseholdPermissionsController {

    private final HouseholdService householdService;

    public HouseholdPermissionsController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @Operation(summary = "Отримати зведення прав учасників",
            description = "Доступно лише учаснику з правом MANAGE_MEMBERS (власник має всі права). "
                    + "Доступ перевіряється декларативно через @PreAuthorize до виклику методу.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Зведення прав учасників"),
            @ApiResponse(responseCode = "401", description = "Токен відсутній або недійсний "
                    + "(AUTHENTICATION_REQUIRED / INVALID_TOKEN)"),
            @ApiResponse(responseCode = "403", description = "Бракує права MANAGE_MEMBERS або користувач "
                    + "не є учасником (ACCESS_DENIED)")
    })
    @PreAuthorize("@householdAccess.hasPermission(authentication, #householdId, 'MANAGE_MEMBERS')")
    @GetMapping
    public HouseholdPermissionsResponse get(@Parameter(description = "Ідентифікатор домогосподарства", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
                                            @PathVariable UUID householdId) {
        return HouseholdPermissionsResponse.from(
                householdService.getHousehold(householdId),
                householdService.listMembers(householdId));
    }
}
