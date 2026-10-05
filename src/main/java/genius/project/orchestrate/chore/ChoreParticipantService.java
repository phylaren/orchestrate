package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.AssignmentResponse;
import genius.project.orchestrate.chore.dto.ParticipantResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChoreParticipantService {

    ParticipantResponse joinChore(UUID choreId);

    ParticipantResponse addParticipant(UUID choreId, UUID targetUserId);

    void removeMember(UUID choreId, UUID targetUserId);

    List<ParticipantResponse> listParticipants(UUID choreId);

    Optional<AssignmentResponse> getCurrentAssignment(UUID choreId);
}