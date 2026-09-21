package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.InsertRequest;
import genius.project.orchestrate.chore.dto.RotationScheduleResponse;
import genius.project.orchestrate.chore.exception.InsertSelfNotAllowedException;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import genius.project.orchestrate.identity.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chores/{choreId}/rotation")
public class ChoreRotationController {

    private final RotationService rotationService;
    private final CurrentUserProvider currentUserProvider;

    public ChoreRotationController(RotationService rotationService,
                                   CurrentUserProvider currentUserProvider) {
        this.rotationService = rotationService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public RotationScheduleResponse getRotation(@PathVariable UUID choreId) {
        return rotationService.getSchedule(choreId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NO_ROTATION_SCHEDULE",
                        "Chore '%s' has no rotation schedule yet.".formatted(choreId)));
    }

    // TODO: add admin check via household module once authorization layer exists.
    @PostMapping("/insert")
    public RotationScheduleResponse insert(
            @PathVariable UUID choreId,
            @Valid @RequestBody InsertRequest request) {
        UUID currentUser = currentUserProvider.getUserId();
        if (request.userId().equals(currentUser)) {
            throw new InsertSelfNotAllowedException(currentUser);
        }
        return rotationService.insert(choreId, request.userId(), request.position());
    }
}