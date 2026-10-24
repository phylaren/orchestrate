package genius.project.orchestrate.household.web;

import genius.project.orchestrate.household.HouseholdService;
import genius.project.orchestrate.household.dto.UserHouseholdResponse;
import genius.project.orchestrate.user.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/households")
class HouseholdWebController {

    static final String LIST_VIEW = "households/list";

    private final HouseholdService householdService;

    HouseholdWebController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @GetMapping
    String listHouseholds(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("households", householdService.listHouseholdsOfUser(principal.getId()).stream()
                .map(UserHouseholdResponse::from)
                .toList());
        return LIST_VIEW;
    }
}
