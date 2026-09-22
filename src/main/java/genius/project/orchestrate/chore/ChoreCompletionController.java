package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.CompletionResponse;
import genius.project.orchestrate.chore.dto.ConfirmationDecisionRequest;
import genius.project.orchestrate.identity.CurrentUserProvider;
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

    private final ChoreCompletionService choreCompletionService;
    private final CurrentUserProvider currentUserProvider;

    public ChoreCompletionController(ChoreCompletionService choreCompletionService,
                                     CurrentUserProvider currentUserProvider) {
        this.choreCompletionService = choreCompletionService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<CompletionResponse> markCompleted(@PathVariable UUID choreId,
                                                            UriComponentsBuilder uriBuilder) {
        UUID userId = currentUserProvider.getUserId();
        CompletionResponse completion = choreCompletionService.markCompleted(choreId, userId);
        URI location = uriBuilder
                .path("/api/v1/chores/{choreId}/completions/{completionId}")
                .buildAndExpand(choreId, completion.id())
                .toUri();
        return ResponseEntity.created(location).body(completion);
    }

    @GetMapping
    public List<CompletionResponse> list(@PathVariable UUID choreId) {
        return choreCompletionService.listCompletions(choreId);
    }

    @GetMapping("/{completionId}")
    public CompletionResponse get(@PathVariable UUID choreId, @PathVariable UUID completionId) {
        return choreCompletionService.getCompletion(choreId, completionId);
    }

    @PostMapping("/{completionId}/confirmation")
    public CompletionResponse decideConfirmation(@PathVariable UUID choreId,
                                                 @PathVariable UUID completionId,
                                                 @Valid @RequestBody ConfirmationDecisionRequest request) {
        UUID confirmedByUserId = currentUserProvider.getUserId();
        return choreCompletionService.decideConfirmation(
                choreId, completionId, confirmedByUserId, request.approved());
    }
}