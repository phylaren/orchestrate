package genius.project.orchestrate.chore.internal.service.strategy;

import genius.project.orchestrate.chore.internal.domain.RotationSchedule;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SwapTwoUsersOp {

    private SwapTwoUsersOp() {}

    public static RotationSchedule apply(RotationSchedule current, UUID fromUserId, UUID toUserId) {
        List<UUID> newOrder = new ArrayList<>(current.baseOrder());
        int indexA = newOrder.indexOf(fromUserId);
        int indexB = newOrder.indexOf(toUserId);
        if (indexA < 0 || indexB < 0) {
            return current;
        }

        newOrder.set(indexA, toUserId);
        newOrder.set(indexB, fromUserId);

        int newCurrentIndex = current.currentIndex();
        if (current.currentIndex() == indexA) {
            newCurrentIndex = indexB;
        } else if (current.currentIndex() == indexB) {
            newCurrentIndex = indexA;
        }

        return new RotationSchedule(
                current.choreId(),
                newOrder,
                newCurrentIndex,
                current.currentCycleNumber(),
                current.cycleStartedAt());
    }
}