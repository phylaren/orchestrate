package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.ParticipantResponse;
import genius.project.orchestrate.identity.CurrentUserProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chores/{choreId}/participants")
public class ChoreParticipantController {

    private final ChoreParticipantService choreParticipantService;
    private final CurrentUserProvider currentUserProvider;

    public ChoreParticipantController(ChoreParticipantService choreParticipantService,
                                      CurrentUserProvider currentUserProvider) {
        this.choreParticipantService = choreParticipantService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Self-service join: the caller joins the rotation group as themselves.
     */
    @PostMapping
    public ResponseEntity<ParticipantResponse> join(@PathVariable UUID choreId,
                                                    UriComponentsBuilder uriBuilder) {
        UUID userId = currentUserProvider.getUserId();
        ParticipantResponse participant = choreParticipantService.joinChore(choreId, userId, false);
        URI location = uriBuilder
                .path("/api/v1/chores/{choreId}/participants/{userId}")
                .buildAndExpand(choreId, participant.userId())
                .toUri();
        return ResponseEntity.created(location).body(participant);
    }

    /**
     * Admin-add: an administrator with the "assign participants" permission adds another
     * member to the chore's rotation group on their behalf.
     *
     * TODO: add access check — current user must have the "assign participants" permission
     *  for this chore's household. Pending the authorization layer and a public port from the
     *  household module to resolve permissions by householdId.
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ParticipantResponse> addParticipant(@PathVariable UUID choreId,
                                                              @PathVariable UUID userId,
                                                              UriComponentsBuilder uriBuilder) {
        ParticipantResponse participant = choreParticipantService.joinChore(choreId, userId, true);
        URI location = uriBuilder
                .path("/api/v1/chores/{choreId}/participants/{userId}")
                .buildAndExpand(choreId, participant.userId())
                .toUri();
        return ResponseEntity.created(location).body(participant);
    }

    @GetMapping
    public List<ParticipantResponse> list(@PathVariable UUID choreId) {
        return choreParticipantService.listParticipants(choreId);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> leave(@PathVariable UUID choreId, @PathVariable UUID userId) {
        choreParticipantService.leaveChore(choreId, userId);
        return ResponseEntity.noContent().build();
    }
}