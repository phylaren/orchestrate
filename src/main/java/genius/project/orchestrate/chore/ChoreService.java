package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.client.ChoreClient;
import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreAssignment;
import genius.project.orchestrate.chore.internal.domain.ChoreCompletion;
import genius.project.orchestrate.chore.internal.domain.ChoreParticipant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChoreService extends ChoreClient {

    Chore createChore(UUID householdId, String name, String description, int recurrenceDays, boolean requiresConfirmation);

    List<Chore> listChores(UUID householdId);

    Chore getChore(UUID choreId);

    boolean needsAttention(UUID choreId);

    ChoreParticipant joinChore(UUID choreId, UUID userId, boolean addedByAdmin);

    List<ChoreParticipant> listParticipants(UUID choreId);

    void leaveChore(UUID choreId, UUID userId);

    Optional<ChoreAssignment> getCurrentAssignment(UUID choreId);

    ChoreCompletion markCompleted(UUID choreId, UUID userId);

    List<ChoreCompletion> listCompletions(UUID choreId);

    ChoreCompletion getCompletion(UUID choreId, UUID completionId);

    ChoreCompletion decideConfirmation(UUID choreId, UUID completionId, UUID confirmedByUserId, boolean approved);
}
