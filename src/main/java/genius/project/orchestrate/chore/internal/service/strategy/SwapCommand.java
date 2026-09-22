package genius.project.orchestrate.chore.internal.service.strategy;

import java.util.UUID;

public sealed interface SwapCommand {

    record Swap(UUID fromUserId, UUID toUserId, Integer cycleNumber) implements SwapCommand {}

    record Insert(UUID userId, int position) implements SwapCommand {}
}