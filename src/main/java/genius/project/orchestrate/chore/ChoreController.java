package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.ChoreCreateRequest;
import genius.project.orchestrate.chore.dto.ChoreResponse;
import genius.project.orchestrate.chore.internal.domain.Chore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chores")
public class ChoreController {

    private final ChoreService choreService;

    public ChoreController(ChoreService choreService) {
        this.choreService = choreService;
    }

    @PostMapping
    public ResponseEntity<ChoreResponse> createChore(@Valid @RequestBody ChoreCreateRequest request,
                                                       UriComponentsBuilder uriBuilder) {
        Chore chore = choreService.createChore(
                request.householdId(),
                request.name(),
                request.description(),
                request.recurrenceDays(),
                request.requiresConfirmation());

        URI location = uriBuilder.path("/api/v1/chores/{id}").buildAndExpand(chore.id()).toUri();
        return ResponseEntity.created(location).body(ChoreResponse.from(chore, true));
    }

    @GetMapping
    public List<ChoreResponse> listChores(@RequestParam(required = false) UUID householdId) {
        return choreService.listChores(householdId).stream()
                .map(chore -> ChoreResponse.from(chore, choreService.needsAttention(chore.id())))
                .toList();
    }

    @GetMapping("/{choreId}")
    public ChoreResponse getChore(@PathVariable UUID choreId) {
        Chore chore = choreService.getChore(choreId);
        return ChoreResponse.from(chore, choreService.needsAttention(choreId));
    }
}
