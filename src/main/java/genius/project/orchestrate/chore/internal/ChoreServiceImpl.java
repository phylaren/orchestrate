package genius.project.orchestrate.chore.internal;

import genius.project.orchestrate.chore.ChoreService;
import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreAssignment;
import genius.project.orchestrate.chore.internal.domain.ChoreCompletion;
import genius.project.orchestrate.chore.internal.domain.ChoreParticipant;
import genius.project.orchestrate.chore.internal.domain.ConfirmationStatus;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChoreServiceImpl implements ChoreService {

    private final ChoreRepository repository;

    public ChoreServiceImpl(ChoreRepository repository) {
        this.repository = repository;
    }

    @Override
    public Chore createChore(UUID householdId, String name, String description, int recurrenceDays, boolean requiresConfirmation) {
        Chore chore = new Chore(UUID.randomUUID(), householdId, name, description, recurrenceDays, requiresConfirmation, Instant.now());
        return repository.saveChore(chore);
    }

    @Override
    public List<Chore> listChores(UUID householdId) {
        return repository.findAllChores().stream()
                .filter(c -> householdId == null || householdId.equals(c.householdId()))
                .sorted(Comparator.comparing(Chore::createdAt))
                .toList();
    }

    @Override
    public Chore getChore(UUID choreId) {
        return getChoreOrThrow(choreId);
    }

    @Override
    public boolean needsAttention(UUID choreId) {
        getChoreOrThrow(choreId);
        return repository.findAssignment(choreId)
                .map(a -> a.currentResponsibleUserId() == null)
                .orElse(true);
    }

    @Override
    public ChoreParticipant joinChore(UUID choreId, UUID userId, boolean addedByAdmin) {
        getChoreOrThrow(choreId);
        LinkedHashMap<UUID, ChoreParticipant> group = repository.participantGroup(choreId);

        synchronized (group) {
            if (group.containsKey(userId)) {
                throw new BusinessRuleViolationException(
                        "ALREADY_PARTICIPANT",
                        "User '%s' has already joined the rotation group for this chore.".formatted(userId));
            }

            ChoreParticipant participant = new ChoreParticipant(choreId, userId, Instant.now(), addedByAdmin);
            group.put(userId, participant);

            ChoreAssignment current = repository.findAssignment(choreId).orElse(null);
            if (current == null || current.currentResponsibleUserId() == null) {
                int nextCycle = current == null ? 1 : current.cycleNumber() + 1;
                repository.saveAssignment(new ChoreAssignment(choreId, userId, nextCycle, Instant.now()));
            }
            return participant;
        }
    }

    @Override
    public List<ChoreParticipant> listParticipants(UUID choreId) {
        getChoreOrThrow(choreId);
        return List.copyOf(repository.participantGroup(choreId).values());
    }

    @Override
    public void leaveChore(UUID choreId, UUID userId) {
        getChoreOrThrow(choreId);
        LinkedHashMap<UUID, ChoreParticipant> group = repository.participantGroup(choreId);

        synchronized (group) {
            if (group.remove(userId) == null) {
                throw ResourceNotFoundException.of("participant", userId);
            }
            reassignIfCurrentResponsibleIsGone(choreId, userId, group);
        }
    }

    @Override
    public Optional<ChoreAssignment> getCurrentAssignment(UUID choreId) {
        getChoreOrThrow(choreId);
        return repository.findAssignment(choreId)
                .filter(a -> a.currentResponsibleUserId() != null);
    }

    @Override
    public ChoreCompletion markCompleted(UUID choreId, UUID userId) {
        Chore chore = getChoreOrThrow(choreId);
        ChoreAssignment current = repository.findAssignment(choreId).orElse(null);

        if (current == null || current.currentResponsibleUserId() == null) {
            throw new BusinessRuleViolationException(
                    "NO_ACTIVE_ASSIGNMENT",
                    "This chore has no active participants; nobody can mark it as done yet.");
        }
        if (!current.currentResponsibleUserId().equals(userId)) {
            throw new BusinessRuleViolationException(
                    "NOT_CURRENT_RESPONSIBLE",
                    "User '%s' is not the current responsible for this chore's cycle.".formatted(userId));
        }

        ConfirmationStatus status = chore.requiresConfirmation()
                ? ConfirmationStatus.PENDING
                : ConfirmationStatus.NOT_REQUIRED;

        ChoreCompletion completion = repository.saveCompletion(new ChoreCompletion(
                UUID.randomUUID(), choreId, userId, Instant.now(), status, null, null));

        if (status == ConfirmationStatus.NOT_REQUIRED) {
            advanceRotation(choreId);
        }
        return completion;
    }

    @Override
    public List<ChoreCompletion> listCompletions(UUID choreId) {
        getChoreOrThrow(choreId);
        return repository.findCompletionsByChoreId(choreId).stream()
                .sorted(Comparator.comparing(ChoreCompletion::completedAt))
                .toList();
    }

    @Override
    public ChoreCompletion getCompletion(UUID choreId, UUID completionId) {
        getChoreOrThrow(choreId);
        return repository.findCompletionById(completionId)
                .filter(c -> c.choreId().equals(choreId))
                .orElseThrow(() -> ResourceNotFoundException.of("completion", completionId));
    }

    @Override
    public ChoreCompletion decideConfirmation(UUID choreId, UUID completionId, UUID confirmedByUserId, boolean approved) {
        getChoreOrThrow(choreId);
        ChoreCompletion completion = repository.findCompletionById(completionId)
                .filter(c -> c.choreId().equals(choreId))
                .orElseThrow(() -> ResourceNotFoundException.of("completion", completionId));

        if (completion.status() != ConfirmationStatus.PENDING) {
            throw new BusinessRuleViolationException(
                    "COMPLETION_ALREADY_RESOLVED",
                    "This completion has already been resolved with status '%s'.".formatted(completion.status()));
        }
        if (completion.completedByUserId().equals(confirmedByUserId)) {
            throw new BusinessRuleViolationException(
                    "SELF_CONFIRMATION_NOT_ALLOWED",
                    "A member cannot confirm or reject their own completion.");
        }

        ChoreCompletion resolved = repository.saveCompletion(new ChoreCompletion(
                completion.id(),
                completion.choreId(),
                completion.completedByUserId(),
                completion.completedAt(),
                approved ? ConfirmationStatus.CONFIRMED : ConfirmationStatus.REJECTED,
                confirmedByUserId,
                Instant.now()));

        if (approved) {
            advanceRotation(choreId);
        }
        return resolved;
    }

    @Override
    public boolean isParticipant(UUID choreId, UUID userId) {
        getChoreOrThrow(choreId);
        return repository.isParticipant(choreId, userId);
    }

    @Override
    public void swapTurns(UUID choreId, UUID fromUserId, UUID toUserId) {
        getChoreOrThrow(choreId);
        LinkedHashMap<UUID, ChoreParticipant> group = repository.participantGroup(choreId);

        synchronized (group) {
            if (!group.containsKey(fromUserId) || !group.containsKey(toUserId)) {
                throw new BusinessRuleViolationException(
                        "NOT_IN_SAME_GROUP",
                        "Both users must be members of this chore's rotation group to swap turns.");
            }

            repository.swapParticipantOrder(choreId, fromUserId, toUserId);

            ChoreAssignment current = repository.findAssignment(choreId).orElse(null);
            if (current != null && fromUserId.equals(current.currentResponsibleUserId())) {
                repository.saveAssignment(new ChoreAssignment(
                        choreId, toUserId, current.cycleNumber(), current.cycleStartedAt()));
            }
        }
    }

    private Chore getChoreOrThrow(UUID choreId) {
        return repository.findChoreById(choreId)
                .orElseThrow(() -> ResourceNotFoundException.of("chore", choreId));
    }

    private void reassignIfCurrentResponsibleIsGone(UUID choreId, UUID removedUserId, LinkedHashMap<UUID, ChoreParticipant> group) {
        ChoreAssignment current = repository.findAssignment(choreId).orElse(null);
        if (current == null || !removedUserId.equals(current.currentResponsibleUserId())) {
            return;
        }
        if (group.isEmpty()) {
            repository.saveAssignment(new ChoreAssignment(choreId, null, current.cycleNumber(), Instant.now()));
        } else {
            UUID next = group.keySet().iterator().next();
            repository.saveAssignment(new ChoreAssignment(choreId, next, current.cycleNumber() + 1, Instant.now()));
        }
    }

    private void advanceRotation(UUID choreId) {
        LinkedHashMap<UUID, ChoreParticipant> group = repository.participantGroup(choreId);
        ChoreAssignment current = repository.findAssignment(choreId).orElse(null);
        int nextCycle = current == null ? 1 : current.cycleNumber() + 1;

        if (group.isEmpty()) {
            repository.saveAssignment(new ChoreAssignment(choreId, null, nextCycle, Instant.now()));
            return;
        }

        List<UUID> order = new ArrayList<>(group.keySet());
        int nextIndex = 0;
        if (current != null && current.currentResponsibleUserId() != null) {
            int currentIndex = order.indexOf(current.currentResponsibleUserId());
            if (currentIndex >= 0) {
                nextIndex = (currentIndex + 1) % order.size();
            }
        }
        repository.saveAssignment(new ChoreAssignment(choreId, order.get(nextIndex), nextCycle, Instant.now()));
    }
}
