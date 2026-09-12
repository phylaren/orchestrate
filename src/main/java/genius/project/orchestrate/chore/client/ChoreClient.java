package genius.project.orchestrate.chore.client;

import java.util.UUID;

public interface ChoreClient {

    boolean isParticipant(UUID choreId, UUID userId);

    void swapTurns(UUID choreId, UUID fromUserId, UUID toUserId);
}
