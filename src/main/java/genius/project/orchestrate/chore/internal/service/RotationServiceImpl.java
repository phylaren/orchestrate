package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.RotationService;
import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.dto.AssignmentResponse;
import genius.project.orchestrate.chore.dto.RotationScheduleResponse;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import genius.project.orchestrate.chore.internal.domain.ScheduledSwap;
import genius.project.orchestrate.chore.internal.repository.RotationRepository;
import genius.project.orchestrate.chore.internal.repository.ScheduledSwapRepository;
import genius.project.orchestrate.chore.internal.service.strategy.SwapOutcome;
import genius.project.orchestrate.chore.internal.service.strategy.SwapStrategy;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class RotationServiceImpl implements RotationService {

    private final RotationRepository rotationRepository;
    private final ScheduledSwapRepository scheduledSwapRepository;
    private final Map<SwapType, SwapStrategy> strategies;

    RotationServiceImpl(RotationRepository rotationRepository,
                        ScheduledSwapRepository scheduledSwapRepository,
                        List<SwapStrategy> strategies) {
        this.rotationRepository = rotationRepository;
        this.scheduledSwapRepository = scheduledSwapRepository;
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(SwapStrategy::getSwapType, Function.identity()));
    }

    @Override
    public Optional<RotationScheduleResponse> getSchedule(UUID choreId) {
        return rotationRepository.findByChoreId(choreId)
                .map(RotationScheduleResponse::from);
    }

    @Override
    public Optional<AssignmentResponse> getCurrentAssignment(UUID choreId) {
        return rotationRepository.findByChoreId(choreId)
                .filter(s -> !s.isEmpty())
                .map(AssignmentResponse::from);
    }

    @Override
    public Optional<UUID> currentResponsible(UUID choreId) {
        return rotationRepository.findByChoreId(choreId)
                .filter(s -> !s.isEmpty())
                .map(RotationSchedule::currentResponsible);
    }

    @Override
    public boolean isEmpty(UUID choreId) {
        return rotationRepository.findByChoreId(choreId)
                .map(RotationSchedule::isEmpty)
                .orElse(true);
    }

    @Override
    public void swap(UUID choreId, UUID fromUserId, UUID toUserId,
                     SwapType swapType, Integer cycleNumber) {
        RotationSchedule current = rotationRepository.findByChoreId(choreId)
                .orElseThrow(() -> ResourceNotFoundException.of("rotationSchedule", choreId));

        SwapStrategy strategy = strategies.get(swapType);
        if (strategy == null) {
            throw new BusinessRuleViolationException(
                    "UNSUPPORTED_SWAP_TYPE",
                    "Swap type '%s' is not handled by the swap flow.".formatted(swapType));
        }

        SwapOutcome outcome = strategy.execute(current, fromUserId, toUserId, cycleNumber);
        apply(outcome);
    }

    @Override
    public RotationScheduleResponse addParticipant(UUID choreId, UUID userId) {
        RotationSchedule current = rotationRepository.findByChoreId(choreId)
                .orElseGet(() -> new RotationSchedule(
                        choreId, new ArrayList<>(), 0, 0, Instant.now()));

        boolean wasEmpty = current.baseOrder().isEmpty();
        List<UUID> newOrder = new ArrayList<>(current.baseOrder());
        newOrder.add(userId);

        int newIndex = wasEmpty ? 0 : current.currentIndex();
        int newCycle = wasEmpty ? current.currentCycleNumber() + 1 : current.currentCycleNumber();
        Instant newCycleStartedAt = wasEmpty ? Instant.now() : current.cycleStartedAt();

        RotationSchedule saved = rotationRepository.save(new RotationSchedule(
                choreId, newOrder, newIndex, newCycle, newCycleStartedAt));
        return RotationScheduleResponse.from(saved);
    }

    @Override
    public RotationScheduleResponse removeParticipant(UUID choreId, UUID userId) {
        RotationSchedule current = rotationRepository.findByChoreId(choreId)
                .orElseThrow(() -> ResourceNotFoundException.of("rotationSchedule", choreId));

        List<UUID> newOrder = new ArrayList<>(current.baseOrder());
        int removedIndex = newOrder.indexOf(userId);
        if (removedIndex < 0) {
            throw ResourceNotFoundException.of("participant", userId);
        }
        newOrder.remove(removedIndex);

        if (newOrder.isEmpty()) {
            return RotationScheduleResponse.from(rotationRepository.save(new RotationSchedule(
                    choreId, newOrder, 0, current.currentCycleNumber(), current.cycleStartedAt())));
        }

        int newIndex = current.currentIndex();
        int newCycle = current.currentCycleNumber();
        Instant newCycleStartedAt = current.cycleStartedAt();

        if (removedIndex < newIndex) {
            newIndex = newIndex - 1;
        } else if (removedIndex == newIndex) {
            if (newIndex >= newOrder.size()) newIndex = 0;
            newCycle = current.currentCycleNumber() + 1;
            newCycleStartedAt = Instant.now();
        }

        return RotationScheduleResponse.from(rotationRepository.save(new RotationSchedule(
                choreId, newOrder, newIndex, newCycle, newCycleStartedAt)));
    }

    @Override
    public RotationScheduleResponse advance(UUID choreId) {
        RotationSchedule current = rotationRepository.findByChoreId(choreId)
                .orElseThrow(() -> ResourceNotFoundException.of("rotationSchedule", choreId));

        if (current.isEmpty()) {
            return RotationScheduleResponse.from(current);
        }

        RotationSchedule advanced = current.advanced();
        RotationSchedule materialized = applyScheduledSwaps(choreId, advanced);
        RotationSchedule saved = rotationRepository.save(materialized);

        // Consumed swaps for the new and older cycles are removed; future ones stay.
        scheduledSwapRepository.deleteExpired(choreId, saved.currentCycleNumber() + 1);

        return RotationScheduleResponse.from(saved);
    }

    private void apply(SwapOutcome outcome) {
        switch (outcome) {
            case SwapOutcome.ApplyNow an -> rotationRepository.save(an.schedule());
            case SwapOutcome.ScheduleForCycle sc -> scheduledSwapRepository.save(sc.scheduled());
        }
    }

    private RotationSchedule applyScheduledSwaps(UUID choreId, RotationSchedule schedule) {
        if (schedule.isEmpty()) return schedule;

        List<ScheduledSwap> scheduled = scheduledSwapRepository
                .findByChoreIdAndCycleNumber(choreId, schedule.currentCycleNumber());
        if (scheduled.isEmpty()) return schedule;

        RotationSchedule result = schedule;
        for (ScheduledSwap s : scheduled) {
            if (result.baseOrder().contains(s.fromUserId())
                    && result.baseOrder().contains(s.toUserId())) {
                result = result.swapParticipants(s.fromUserId(), s.toUserId());
            }
        }
        return result;
    }
}