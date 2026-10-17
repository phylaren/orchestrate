package genius.project.orchestrate.chore.web;

import genius.project.orchestrate.chore.ChoreLifecycleService;
import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/households/{householdId}/chores")
class ChoreWebController {

    static final String LIST_VIEW = "chores/list";
    static final String CREATE_FORM_VIEW = "chores/create-form";

    private final ChoreLifecycleService choreLifecycleService;

    ChoreWebController(ChoreLifecycleService choreLifecycleService) {
        this.choreLifecycleService = choreLifecycleService;
    }

    @InitBinder
    void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    String listChores(@PathVariable UUID householdId, Model model) {
        model.addAttribute("householdId", householdId);
        model.addAttribute("chores", choreLifecycleService.listChores(householdId));
        return LIST_VIEW;
    }

    @GetMapping("/new")
    String showCreateForm(@PathVariable UUID householdId, Model model) {
        model.addAttribute("householdId", householdId);
        model.addAttribute("form", CreateChoreForm.empty());
        return CREATE_FORM_VIEW;
    }

    @PostMapping
    String createChore(@PathVariable UUID householdId,
                       @Valid @ModelAttribute("form") CreateChoreForm form,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("householdId", householdId);
            return CREATE_FORM_VIEW;
        }

        choreLifecycleService.createChore(householdId, form.name(), form.description(),
                form.recurrenceDaysValue(), form.requiresConfirmation());

        redirectAttributes.addFlashAttribute("successMessage",
                "Обов’язок «" + form.name() + "» створено.");
        return redirectToList(householdId);
    }

    private static String redirectToList(UUID householdId) {
        return "redirect:/households/" + householdId + "/chores";
    }
}