package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.AssignmentResponse;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Призначення обов'язку", description = "Поточний відповідальний за обов'язок")
@RestController
@RequestMapping("/api/v1/chores/{choreId}/assignment")
public class ChoreAssignmentController {

    private final ChoreParticipantService choreParticipantService;

    public ChoreAssignmentController(ChoreParticipantService choreParticipantService) {
        this.choreParticipantService = choreParticipantService;
    }

    @Operation(summary = "Отримати поточного відповідального",
            description = "Відповідального визначає ротація серед групи виконання.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Поточне призначення"),
            @ApiResponse(responseCode = "404", description = "Обов'язок не знайдено або група порожня (NO_ACTIVE_ASSIGNMENT)")
    })
    @GetMapping
    public AssignmentResponse getCurrentAssignment(@Parameter(description = "Ідентифікатор обов'язку", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                                                   @PathVariable UUID choreId) {
        return choreParticipantService.getCurrentAssignment(choreId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NO_ACTIVE_ASSIGNMENT",
                        "Chore '%s' has no active responsible: its rotation group is empty and it needs attention."
                                .formatted(choreId)));
    }
}
