package genius.project.orchestrate.chore.client;

import java.util.UUID;

public interface ChoreClient {

    boolean isParticipant(UUID choreId, UUID userId);

    int currentCycleNumber(UUID choreId);
}