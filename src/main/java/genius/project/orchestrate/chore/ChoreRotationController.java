package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.InsertRequest;
import genius.project.orchestrate.chore.dto.RotationScheduleResponse;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/insert")
    public RotationScheduleResponse insert(
            @PathVariable UUID choreId,
            @Valid @RequestBody InsertRequest request) {
        return rotationService.insert(choreId, request.userId(), request.position());
    }
}