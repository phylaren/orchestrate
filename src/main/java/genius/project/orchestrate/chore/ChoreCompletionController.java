package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.CompletionCreateRequest;
import genius.project.orchestrate.chore.dto.CompletionResponse;
import genius.project.orchestrate.chore.dto.ConfirmationDecisionRequest;
import genius.project.orchestrate.chore.internal.domain.ChoreCompletion;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chores/{choreId}/completions")
public class ChoreCompletionController {

    private final ChoreService choreService;

    public ChoreCompletionController(ChoreService choreService) {
        this.choreService = choreService;
    }

    @PostMapping
    public ResponseEntity<CompletionResponse> markCompleted(@PathVariable UUID choreId,
                                                               @Valid @RequestBody CompletionCreateRequest request,
                                                               UriComponentsBuilder uriBuilder) {
        ChoreCompletion completion = choreService.markCompleted(choreId, request.userId());
        URI location = uriBuilder
                .path("/api/v1/chores/{choreId}/completions/{completionId}")
                .buildAndExpand(choreId, completion.id())
                .toUri();
        return ResponseEntity.created(location).body(CompletionResponse.from(completion));
    }

    @GetMapping
    public List<CompletionResponse> list(@PathVariable UUID choreId) {
        return choreService.listCompletions(choreId).stream()
                .map(CompletionResponse::from)
                .toList();
    }

    @GetMapping("/{completionId}")
    public CompletionResponse get(@PathVariable UUID choreId, @PathVariable UUID completionId) {
        return CompletionResponse.from(choreService.getCompletion(choreId, completionId));
    }

    @PostMapping("/{completionId}/confirmation")
    public CompletionResponse decideConfirmation(@PathVariable UUID choreId,
                                                   @PathVariable UUID completionId,
                                                   @Valid @RequestBody ConfirmationDecisionRequest request) {
        ChoreCompletion completion = choreService.decideConfirmation(
                choreId, completionId, request.confirmedByUserId(), request.approved());
        return CompletionResponse.from(completion);
    }
}
