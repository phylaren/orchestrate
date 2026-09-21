package genius.project.orchestrate.chore.client;

import java.util.List;
import java.util.UUID;

public interface TemporarySwapReader {

    List<TemporarySwap> findAcceptedForCycle(UUID choreId, int cycleNumber);

    record TemporarySwap(UUID fromUserId, UUID toUserId) {}
}