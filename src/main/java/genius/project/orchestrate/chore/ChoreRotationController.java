package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.RotationScheduleResponse;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Ротація обов'язку", description = "Розклад ротації відповідальних")
@RestController
@RequestMapping("/api/v1/chores/{choreId}/rotation")
public class ChoreRotationController {

    private final RotationService rotationService;

    public ChoreRotationController(RotationService rotationService) {
        this.rotationService = rotationService;
    }

    @Operation(summary = "Отримати розклад ротації",
            description = "Порядок учасників, поточний відповідальний і номер циклу.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Розклад ротації"),
            @ApiResponse(responseCode = "404", description = "Розкладу ще немає (NO_ROTATION_SCHEDULE)")
    })
    @GetMapping
    public RotationScheduleResponse getRotation(@Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                                                @PathVariable UUID choreId) {
        return rotationService.getSchedule(choreId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NO_ROTATION_SCHEDULE",
                        "Chore '%s' has no rotation schedule yet.".formatted(choreId)));
    }
}
