package genius.project.orchestrate.household;

import genius.project.orchestrate.household.dto.UserHouseholdResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Домогосподарства користувача", description = "Усі домогосподарства, до яких належить користувач")
@RestController
@RequestMapping("/api/v1/users/{userId}/households")
public class UserHouseholdController {

    private final HouseholdService householdService;

    public UserHouseholdController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @Operation(summary = "Отримати домогосподарства користувача",
            description = "Користувач може бути власником одного дому та мешканцем інших одночасно.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список домогосподарств, відсортований за часом приєднання"),
            @ApiResponse(responseCode = "404", description = "Користувача не знайдено (RESOURCE_NOT_FOUND)")
    })
    @GetMapping
    public List<UserHouseholdResponse> list(@Parameter(description = "Ідентифікатор користувача", example = "5d1c0e7a-2b3f-4c8d-9e6a-7f8b9c0d1e2f")
                                            @PathVariable UUID userId) {
        return householdService.listHouseholdsOfUser(userId).stream()
                .map(UserHouseholdResponse::from)
                .toList();
    }
}
