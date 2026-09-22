package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.AssignmentResponse;
import genius.project.orchestrate.chore.dto.ParticipantResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChoreParticipantService {

    ParticipantResponse joinChore(UUID choreId, UUID userId, boolean addedByAdmin);

    void leaveChore(UUID choreId, UUID userId);

    List<ParticipantResponse> listParticipants(UUID choreId);

    boolean isParticipant(UUID choreId, UUID userId);

    Optional<AssignmentResponse> getCurrentAssignment(UUID choreId);
}