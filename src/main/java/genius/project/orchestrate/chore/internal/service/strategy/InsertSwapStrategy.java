package genius.project.orchestrate.chore.internal.service.strategy;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class InsertSwapStrategy implements SwapStrategy {

    @Override
    public SwapType getSwapType() {
        return SwapType.INSERT;
    }

    @Override
    public SwapOutcome execute(RotationSchedule current, SwapCommand command) {
        var insert = (SwapCommand.Insert) command;
        UUID userId = insert.userId();
        int targetPosition = insert.position();

        List<UUID> base = current.baseOrder();
        if (!base.contains(userId)) {
            return new SwapOutcome.ApplyNow(current);
        }
        if (targetPosition < 0 || targetPosition >= base.size()) {
            return new SwapOutcome.ApplyNow(current);
        }

        UUID responsible = current.currentResponsible();
        int oldIndex = base.indexOf(userId);
        if (oldIndex == targetPosition) {
            return new SwapOutcome.ApplyNow(current);
        }

        List<UUID> newOrder = new ArrayList<>(base);
        newOrder.remove(oldIndex);
        newOrder.add(targetPosition, userId);

        int newCurrentIndex;
        int newCycleNumber = current.currentCycleNumber();
        Instant newCycleStartedAt = current.cycleStartedAt();

        if (userId.equals(responsible)) {
            newCurrentIndex = Math.min(oldIndex, newOrder.size() - 1);
            newCycleNumber = current.currentCycleNumber() + 1;
            newCycleStartedAt = Instant.now();
        } else {
            newCurrentIndex = newOrder.indexOf(responsible);
        }

        return new SwapOutcome.ApplyNow(new RotationSchedule(
                current.choreId(),
                newOrder,
                newCurrentIndex,
                newCycleNumber,
                newCycleStartedAt));
    }
}