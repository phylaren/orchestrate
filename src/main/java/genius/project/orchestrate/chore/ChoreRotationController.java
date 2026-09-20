package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.RotationScheduleResponse;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chores/{choreId}/rotation")
public class ChoreRotationController {

    private final RotationService rotationService;

    public ChoreRotationController(RotationService rotationService) {
        this.rotationService = rotationService;
    }

    @GetMapping
    public RotationScheduleResponse getRotation(@PathVariable UUID choreId) {
        return rotationService.getSchedule(choreId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NO_ROTATION_SCHEDULE",
                        "Chore '%s' has no rotation schedule yet.".formatted(choreId)));
    }
}