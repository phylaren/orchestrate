package genius.project.orchestrate.chore.internal.domain;

import genius.project.orchestrate.chore.exception.SameUserSwapException;
import genius.project.orchestrate.chore.exception.UserNotInRotationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record RotationSchedule(
        UUID choreId,
        List<UUID> baseOrder,
        int currentIndex,
        int currentCycleNumber,
        Instant cycleStartedAt) {

    public RotationSchedule {
        Objects.requireNonNull(choreId, "choreId");
        Objects.requireNonNull(cycleStartedAt, "cycleStartedAt");
        baseOrder = List.copyOf(baseOrder);
    }

    public boolean isEmpty() {
        return baseOrder.isEmpty();
    }

    public UUID currentResponsible() {
        if (isEmpty()) {
            throw new IllegalStateException("Empty rotation has no responsible");
        }
        return baseOrder.get(currentIndex);
    }

    /**
     * Moves the pointer to the next participant and bumps the cycle.
     * Empty rotation stays as is.
     */
    public RotationSchedule advanced() {
        if (isEmpty()) {
            return this;
        }
        int nextIndex = (currentIndex + 1) % baseOrder.size();
        return new RotationSchedule(choreId, baseOrder, nextIndex,
                currentCycleNumber + 1, Instant.now());
    }

    /**
     * Swaps positions of two participants. The current responsible stays the same user;
     * their index is re-resolved against the new order. Cycle metadata is preserved.
     */
    public RotationSchedule swapParticipants(UUID a, UUID b) {
        if (a.equals(b)) {
            throw new SameUserSwapException(a);
        }
        int indexA = baseOrder.indexOf(a);
        int indexB = baseOrder.indexOf(b);
        if (indexA < 0) throw new UserNotInRotationException(a);
        if (indexB < 0) throw new UserNotInRotationException(b);

        List<UUID> newOrder = new ArrayList<>(baseOrder);
        newOrder.set(indexA, b);
        newOrder.set(indexB, a);

        int newCurrentIndex = currentIndex;
        if (currentIndex == indexA) {
            newCurrentIndex = indexB;
        } else if (currentIndex == indexB) {
            newCurrentIndex = indexA;
        }

        return new RotationSchedule(choreId, newOrder, newCurrentIndex,
                currentCycleNumber, cycleStartedAt);
    }
}