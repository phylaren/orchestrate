package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.ParticipantJoinRequest;
import genius.project.orchestrate.chore.dto.ParticipantResponse;
import genius.project.orchestrate.chore.internal.domain.ChoreParticipant;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/chores/{choreId}/participants")
public class ChoreParticipantController {

    private final ChoreService choreService;

    public ChoreParticipantController(ChoreService choreService) {
        this.choreService = choreService;
    }

    @PostMapping
    public ResponseEntity<ParticipantResponse> join(@PathVariable UUID choreId,
                                                       @Valid @RequestBody ParticipantJoinRequest request,
                                                       UriComponentsBuilder uriBuilder) {
        ChoreParticipant participant = choreService.joinChore(choreId, request.userId(), false);
        URI location = uriBuilder
                .path("/api/v1/chores/{choreId}/participants/{userId}")
                .buildAndExpand(choreId, participant.userId())
                .toUri();
        return ResponseEntity.created(location).body(ParticipantResponse.from(participant));
    }

    @GetMapping
    public List<ParticipantResponse> list(@PathVariable UUID choreId) {
        return choreService.listParticipants(choreId).stream()
                .map(ParticipantResponse::from)
                .toList();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> leave(@PathVariable UUID choreId, @PathVariable UUID userId) {
        choreService.leaveChore(choreId, userId);
        return ResponseEntity.noContent().build();
    }
}
