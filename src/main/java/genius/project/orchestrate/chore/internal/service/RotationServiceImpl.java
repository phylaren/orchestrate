package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.RotationService;
import genius.project.orchestrate.chore.dto.RotationScheduleResponse;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import genius.project.orchestrate.chore.internal.repository.RotationRepository;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class RotationServiceImpl implements RotationService {

    private final RotationRepository rotationRepository;

    RotationServiceImpl(RotationRepository rotationRepository) {
        this.rotationRepository = rotationRepository;
    }

    @Override
    public Optional<RotationScheduleResponse> getSchedule(UUID choreId) {
        return rotationRepository.findByChoreId(choreId)
                .map(RotationScheduleResponse::from);
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
    public RotationScheduleResponse addParticipant(UUID choreId, UUID userId) {
        RotationSchedule current = rotationRepository.findByChoreId(choreId)
                .orElse(new RotationSchedule(choreId, new ArrayList<>(), 0, 1, Instant.now()));

        boolean wasEmpty = current.baseOrder().isEmpty();

        List<UUID> newOrder = new ArrayList<>(current.baseOrder());
        newOrder.add(userId);

        int newIndex = wasEmpty ? 0 : current.currentIndex();
        int newCycle = wasEmpty ? current.currentCycleNumber() + 1 : current.currentCycleNumber();
        Instant newCycleStartedAt = wasEmpty ? Instant.now() : current.cycleStartedAt();

        RotationSchedule saved = rotationRepository.save(
                new RotationSchedule(choreId, newOrder, newIndex, newCycle, newCycleStartedAt));
        return RotationScheduleResponse.from(saved);
    }

    @Override
    public RotationScheduleResponse removeParticipant(UUID choreId, UUID userId) {
        RotationSchedule current = rotationRepository.findByChoreId(choreId)
                .orElseThrow(() -> ResourceNotFoundException.of("rotationSchedule", choreId));

        List<UUID> newOrder = new ArrayList<>(current.baseOrder());
        int removedIndex = newOrder.indexOf(userId);
        newOrder.remove(userId);

        if (newOrder.isEmpty()) {
            RotationSchedule saved = rotationRepository.save(new RotationSchedule(
                    choreId, newOrder, 0, current.currentCycleNumber(), current.cycleStartedAt()));
            return RotationScheduleResponse.from(saved);
        }

        int newIndex = current.currentIndex();
        int newCycle = current.currentCycleNumber();
        Instant newCycleStartedAt = current.cycleStartedAt();

        if (removedIndex < newIndex) {
            newIndex = newIndex - 1;
        } else if (removedIndex == newIndex) {
            if (newIndex >= newOrder.size()) {
                newIndex = 0;
            }
            newCycle = current.currentCycleNumber() + 1;
            newCycleStartedAt = Instant.now();
        }

        RotationSchedule saved = rotationRepository.save(
                new RotationSchedule(choreId, newOrder, newIndex, newCycle, newCycleStartedAt));
        return RotationScheduleResponse.from(saved);
    }

    @Override
    public RotationScheduleResponse advance(UUID choreId) {
        RotationSchedule current = rotationRepository.findByChoreId(choreId)
                .orElseThrow(() -> ResourceNotFoundException.of("rotationSchedule", choreId));

        if (current.isEmpty()) {
            return RotationScheduleResponse.from(current);
        }

        int nextIndex = (current.currentIndex() + 1) % current.baseOrder().size();
        int nextCycle = current.currentCycleNumber() + 1;

        RotationSchedule saved = rotationRepository.save(
                new RotationSchedule(choreId, current.baseOrder(), nextIndex, nextCycle, Instant.now()));
        return RotationScheduleResponse.from(saved);
    }

    @Override
    public RotationScheduleResponse swapPositions(UUID choreId, UUID userA, UUID userB) {
        RotationSchedule current = rotationRepository.findByChoreId(choreId)
                .orElseThrow(() -> ResourceNotFoundException.of("rotationSchedule", choreId));

        List<UUID> newOrder = new ArrayList<>(current.baseOrder());
        int indexA = newOrder.indexOf(userA);
        int indexB = newOrder.indexOf(userB);

        newOrder.set(indexA, userB);
        newOrder.set(indexB, userA);

        int newCurrentIndex = current.currentIndex();
        if (current.currentIndex() == indexA) {
            newCurrentIndex = indexB;
        } else if (current.currentIndex() == indexB) {
            newCurrentIndex = indexA;
        }

        RotationSchedule saved = rotationRepository.save(new RotationSchedule(
                choreId, newOrder, newCurrentIndex,
                current.currentCycleNumber(), current.cycleStartedAt()));
        return RotationScheduleResponse.from(saved);
    }
}