package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.ChoreCompletionService;
import genius.project.orchestrate.chore.ConfirmationStatus;
import genius.project.orchestrate.chore.RotationService;
import genius.project.orchestrate.chore.dto.CompletionResponse;
import genius.project.orchestrate.chore.exception.InvalidConfirmationStatusException;
import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreCompletion;
import genius.project.orchestrate.chore.internal.repository.ChoreCompletionStore;
import genius.project.orchestrate.chore.internal.repository.ChoreStore;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
class ChoreCompletionServiceImpl implements ChoreCompletionService {

    private final ChoreStore choreStore;
    private final ChoreCompletionStore completionStore;
    private final RotationService rotationService;

    ChoreCompletionServiceImpl(ChoreStore choreStore,
                               ChoreCompletionStore completionStore,
                               RotationService rotationService) {
        this.choreStore = choreStore;
        this.completionStore = completionStore;
        this.rotationService = rotationService;
    }

    @Override
    public CompletionResponse markCompleted(UUID choreId, UUID userId) {
        Chore chore = getChoreOrThrow(choreId);

        UUID responsible = rotationService.currentResponsible(choreId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "NO_ACTIVE_ASSIGNMENT",
                        "This chore has no active participants; nobody can mark it as done yet."));

        if (!responsible.equals(userId)) {
            throw new BusinessRuleViolationException(
                    "NOT_CURRENT_RESPONSIBLE",
                    "User '%s' is not the current responsible for this chore's cycle.".formatted(userId));
        }

        ConfirmationStatus status = chore.requiresConfirmation()
                ? ConfirmationStatus.PENDING
                : ConfirmationStatus.NOT_REQUIRED;

        ChoreCompletion completion = completionStore.save(new ChoreCompletion(
                UUID.randomUUID(), choreId, userId, Instant.now(), status, null, null));

        if (status == ConfirmationStatus.NOT_REQUIRED) {
            rotationService.advance(choreId);
        }

        return CompletionResponse.from(completion);
    }

    @Override
    public List<CompletionResponse> listCompletions(UUID choreId) {
        getChoreOrThrow(choreId);
        return completionStore.findByChoreId(choreId).stream()
                .sorted(Comparator.comparing(ChoreCompletion::completedAt))
                .map(CompletionResponse::from)
                .toList();
    }

    @Override
    public CompletionResponse getCompletion(UUID choreId, UUID completionId) {
        getChoreOrThrow(choreId);
        ChoreCompletion completion = completionStore.findById(completionId)
                .filter(c -> c.choreId().equals(choreId))
                .orElseThrow(() -> ResourceNotFoundException.of("completion", completionId));
        return CompletionResponse.from(completion);
    }

    @Override
    public CompletionResponse decideConfirmation(UUID choreId, UUID completionId,
                                                  UUID confirmedByUserId, boolean approved) {
        getChoreOrThrow(choreId);

        ChoreCompletion completion = completionStore.findById(completionId)
                .filter(c -> c.choreId().equals(choreId))
                .orElseThrow(() -> ResourceNotFoundException.of("completion", completionId));

        ConfirmationStatus targetStatus = approved ? ConfirmationStatus.CONFIRMED : ConfirmationStatus.REJECTED;

        if (!completion.status().canTransitionTo(targetStatus)) {
            throw new InvalidConfirmationStatusException(completionId, completion.status(), targetStatus);
        }

        if (completion.completedByUserId().equals(confirmedByUserId)) {
            throw new BusinessRuleViolationException(
                    "SELF_CONFIRMATION_NOT_ALLOWED",
                    "A member cannot confirm or reject their own completion.");
        }

        ChoreCompletion resolved = completionStore.save(new ChoreCompletion(
                completion.id(),
                completion.choreId(),
                completion.completedByUserId(),
                completion.completedAt(),
                targetStatus,
                confirmedByUserId,
                Instant.now()));

        if (approved) {
            rotationService.advance(choreId);
        }

        return CompletionResponse.from(resolved);
    }

    private Chore getChoreOrThrow(UUID choreId) {
        return choreStore.findById(choreId)
                .orElseThrow(() -> ResourceNotFoundException.of("chore", choreId));
    }
}
