package genius.project.orchestrate.household;

import genius.project.orchestrate.household.dto.UserHouseholdResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Lives in the household module: memberships are owned here, and the user module must not depend on it.
@RestController
@RequestMapping("/api/v1/users/{userId}/households")
public class UserHouseholdController {

    private final HouseholdService householdService;

    public UserHouseholdController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @GetMapping
    public List<UserHouseholdResponse> list(@PathVariable UUID userId) {
        return householdService.listHouseholdsOfUser(userId).stream()
                .map(UserHouseholdResponse::from)
                .toList();
    }
}
