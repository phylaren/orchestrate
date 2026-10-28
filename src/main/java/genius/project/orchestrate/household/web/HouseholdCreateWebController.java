package genius.project.orchestrate.household.web;

import genius.project.orchestrate.household.HouseholdService;
import genius.project.orchestrate.household.internal.domain.Household;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/households")
class HouseholdCreateWebController {

    static final String CREATE_FORM_VIEW = "households/create-form";

    private final HouseholdService householdService;

    HouseholdCreateWebController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @GetMapping("/new")
    String showCreateForm(Model model) {
        model.addAttribute("form", CreateHouseholdForm.empty());
        return CREATE_FORM_VIEW;
    }

    @PostMapping
    String createHousehold(@Valid @ModelAttribute("form") CreateHouseholdForm form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return CREATE_FORM_VIEW;
        }
        Household household = householdService.createHousehold(form.name());
        redirectAttributes.addFlashAttribute("successMessage",
                "Домогосподарство «" + household.name() + "» створено.");
        return "redirect:/households/" + household.id() + "/board";
    }
}
