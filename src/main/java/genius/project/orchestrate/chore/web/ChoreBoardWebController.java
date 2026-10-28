package genius.project.orchestrate.chore.web;

import genius.project.orchestrate.chore.ChoreCompletionService;
import genius.project.orchestrate.chore.ChoreLifecycleService;
import genius.project.orchestrate.chore.ChoreParticipantService;
import genius.project.orchestrate.chore.dto.ChoreResponse;
import genius.project.orchestrate.common.exception.ForbiddenException;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import genius.project.orchestrate.household.HouseholdService;
import genius.project.orchestrate.household.MembershipPermission;
import genius.project.orchestrate.user.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * Дошка обов'язків для учасників: приєднання до групи виконання та позначка виконання.
 * Права перевіряються на сервері; приховані кнопки лише зручність інтерфейсу.
 */
@Controller
@RequestMapping("/households/{householdId}")
class ChoreBoardWebController {

    static final String BOARD_VIEW = "chores/board";

    private final ChoreLifecycleService choreLifecycleService;
    private final ChoreParticipantService choreParticipantService;
    private final ChoreCompletionService choreCompletionService;
    private final HouseholdService householdService;

    ChoreBoardWebController(ChoreLifecycleService choreLifecycleService,
                            ChoreParticipantService choreParticipantService,
                            ChoreCompletionService choreCompletionService,
                            HouseholdService householdService) {
        this.choreLifecycleService = choreLifecycleService;
        this.choreParticipantService = choreParticipantService;
        this.choreCompletionService = choreCompletionService;
        this.householdService = householdService;
    }

    @GetMapping("/board")
    String showBoard(@PathVariable UUID householdId,
                     @AuthenticationPrincipal UserPrincipal principal,
                     Model model) {
        UUID userId = principal.getId();
        requireMember(householdId, userId);

        model.addAttribute("householdId", householdId);
        model.addAttribute("canManageChores", canManageChores(householdId, userId));
        model.addAttribute("chores", choreLifecycleService.listChores(householdId).stream()
                .map(chore -> toBoardRow(chore, userId))
                .toList());
        return BOARD_VIEW;
    }

    @PostMapping("/chores/{choreId}/join")
    String joinChore(@PathVariable UUID householdId,
                     @PathVariable UUID choreId,
                     @AuthenticationPrincipal UserPrincipal principal,
                     RedirectAttributes redirectAttributes) {
        requireMember(householdId, principal.getId());
        requireChoreInHousehold(householdId, choreId);

        choreParticipantService.joinChore(choreId);
        redirectAttributes.addFlashAttribute("successMessage", "Ви приєдналися до виконання обов’язку.");
        return boardRedirect(householdId);
    }

    @PostMapping("/chores/{choreId}/completions")
    String markCompleted(@PathVariable UUID householdId,
                         @PathVariable UUID choreId,
                         @AuthenticationPrincipal UserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        requireMember(householdId, principal.getId());
        requireChoreInHousehold(householdId, choreId);

        choreCompletionService.markCompleted(choreId);
        redirectAttributes.addFlashAttribute("successMessage", "Виконання відмічено.");
        return boardRedirect(householdId);
    }

    private ChoreBoardRow toBoardRow(ChoreResponse chore, UUID userId) {
        boolean joined = choreParticipantService.listParticipants(chore.id()).stream()
                .anyMatch(participant -> participant.userId().equals(userId));
        boolean responsible = choreParticipantService.getCurrentAssignment(chore.id())
                .map(assignment -> userId.equals(assignment.currentResponsibleUserId()))
                .orElse(false);
        return new ChoreBoardRow(chore.id(), chore.name(), chore.needsAttention(), joined, responsible);
    }

    private boolean canManageChores(UUID householdId, UUID userId) {
        return householdService.hasPermission(householdId, userId, MembershipPermission.MANAGE_CHORES);
    }

    private void requireMember(UUID householdId, UUID userId) {
        if (!householdService.isMember(householdId, userId)) {
            throw new ForbiddenException("NOT_HOUSEHOLD_MEMBER", "Дані дому бачать лише учасники.");
        }
    }

    private void requireChoreInHousehold(UUID householdId, UUID choreId) {
        if (!choreLifecycleService.getChore(choreId).householdId().equals(householdId)) {
            throw ResourceNotFoundException.of("chore", choreId);
        }
    }

    private static String boardRedirect(UUID householdId) {
        return "redirect:/households/" + householdId + "/board";
    }
}
