package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.AssignmentResponse;
import genius.project.orchestrate.chore.internal.domain.ChoreAssignment;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chores/{choreId}/assignment")
public class ChoreAssignmentController {

    private final ChoreService choreService;

    public ChoreAssignmentController(ChoreService choreService) {
        this.choreService = choreService;
    }

    @GetMapping
    public AssignmentResponse getCurrentAssignment(@PathVariable UUID choreId) {
        ChoreAssignment assignment = choreService.getCurrentAssignment(choreId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NO_ACTIVE_ASSIGNMENT",
                        "Chore '%s' has no active responsible: its rotation group is empty and it needs attention."
                                .formatted(choreId)));
        return AssignmentResponse.from(assignment);
    }
}
