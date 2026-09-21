package genius.project.orchestrate.swap.internal;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.TurnSwapRequestedEvent;
import genius.project.orchestrate.chore.client.ChoreClient;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import genius.project.orchestrate.identity.CurrentUserProvider;
import genius.project.orchestrate.swap.SwapRequestService;
import genius.project.orchestrate.swap.SwapRequestStatus;
import genius.project.orchestrate.swap.dto.SwapRequestRequest;
import genius.project.orchestrate.swap.dto.SwapRequestResponse;
import genius.project.orchestrate.swap.dto.SwapRequestStatusRequest;
import genius.project.orchestrate.swap.exception.DuplicateSwapRequestException;
import genius.project.orchestrate.swap.exception.InvalidSwapRequestRecipientException;
import genius.project.orchestrate.swap.exception.InvalidSwapRequestStatusException;
import genius.project.orchestrate.swap.exception.NotChoreParticipantException;
import genius.project.orchestrate.swap.exception.NotSwapRequestReceiverException;
import genius.project.orchestrate.swap.exception.SwapRequestNotFoundException;
import genius.project.orchestrate.swap.internal.domain.SwapRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SwapRequestServiceImpl implements SwapRequestService {

    private static final int MAX_CYCLE_LOOKAHEAD = 10;

    private final SwapRequestRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final ChoreClient choreClient;
    private final ApplicationEventPublisher eventPublisher;

    public SwapRequestServiceImpl(SwapRequestRepository repository,
                                  CurrentUserProvider currentUserProvider,
                                  ChoreClient choreClient,
                                  ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.choreClient = choreClient;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<SwapRequestResponse> getSwapRequests(UUID choreId) {
        return repository.findByChoreId(choreId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public SwapRequestResponse createSwapRequest(UUID choreId, SwapRequestRequest request) {
        UUID initiatorId = currentUserProvider.getUserId();
        UUID receiverId = request.receiverUserId();
        SwapType swapType = request.swapType();

        if (swapType == SwapType.INSERT) {
            throw new BusinessRuleViolationException(
                    "INSERT_NOT_SWAP_REQUEST",
                    "INSERT is an admin action and is not created via swap requests.");
        }

        if (initiatorId.equals(receiverId)) {
            throw new InvalidSwapRequestRecipientException(receiverId, choreId);
        }
        if (!choreClient.isParticipant(choreId, initiatorId)) {
            throw new NotChoreParticipantException(initiatorId, choreId);
        }
        if (!choreClient.isParticipant(choreId, receiverId)) {
            throw new NotChoreParticipantException(receiverId, choreId);
        }
        if (repository.existsPending(choreId, initiatorId, receiverId)) {
            throw new DuplicateSwapRequestException(choreId, initiatorId, receiverId);
        }

        Integer cycleNumber = resolveCycleNumber(choreId, swapType, request.cycleNumber());

        SwapRequest saved = repository.save(new SwapRequest(
                null,
                choreId,
                initiatorId,
                receiverId,
                SwapRequestStatus.PENDING,
                swapType,
                cycleNumber,
                LocalDateTime.now()));

        return toResponse(saved);
    }

    @Override
    public SwapRequestResponse respondToSwapRequest(UUID choreId,
                                                    UUID requestId,
                                                    SwapRequestStatusRequest request) {
        UUID currentUserId = currentUserProvider.getUserId();

        SwapRequest existing = repository.findById(requestId)
                .orElseThrow(() -> new SwapRequestNotFoundException(requestId));

        if (!existing.choreId().equals(choreId)) {
            throw new SwapRequestNotFoundException(requestId);
        }
        if (!existing.receiverUserId().equals(currentUserId)) {
            throw new NotSwapRequestReceiverException(currentUserId, requestId);
        }

        SwapRequestStatus currentStatus = existing.status();
        SwapRequestStatus targetStatus = request.status();
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new InvalidSwapRequestStatusException(requestId, currentStatus, targetStatus);
        }

        if (targetStatus == SwapRequestStatus.ACCEPTED) {
            validateStillParticipants(choreId, existing);
        }

        SwapRequest updated = repository.update(new SwapRequest(
                existing.id(),
                existing.choreId(),
                existing.initiatorUserId(),
                existing.receiverUserId(),
                targetStatus,
                existing.swapType(),
                existing.cycleNumber(),
                existing.createdAt()));

        if (updated.status() == SwapRequestStatus.ACCEPTED) {
            eventPublisher.publishEvent(new TurnSwapRequestedEvent(
                    updated.choreId(),
                    updated.initiatorUserId(),
                    updated.receiverUserId(),
                    updated.swapType(),
                    updated.cycleNumber()));
        }

        return toResponse(updated);
    }

    private Integer resolveCycleNumber(UUID choreId, SwapType swapType, Integer requested) {
        if (swapType != SwapType.TEMPORARY) {
            return null;
        }
        if (requested == null) {
            throw new BusinessRuleViolationException(
                    "MISSING_CYCLE_NUMBER",
                    "TEMPORARY swap requires a cycleNumber.");
        }

        int current = choreClient.currentCycleNumber(choreId);
        if (requested < current) {
            throw new BusinessRuleViolationException(
                    "CYCLE_IN_PAST",
                    "cycleNumber %d is in the past (current cycle is %d).".formatted(requested, current));
        }
        if (requested > current + MAX_CYCLE_LOOKAHEAD) {
            throw new BusinessRuleViolationException(
                    "CYCLE_TOO_FAR",
                    "cycleNumber %d is more than %d cycles ahead (current cycle is %d)."
                            .formatted(requested, MAX_CYCLE_LOOKAHEAD, current));
        }
        return requested;
    }

    private void validateStillParticipants(UUID choreId, SwapRequest existing) {
        if (!choreClient.isParticipant(choreId, existing.initiatorUserId())
                || !choreClient.isParticipant(choreId, existing.receiverUserId())) {
            throw new BusinessRuleViolationException(
                    "NOT_IN_SAME_GROUP",
                    "Both users must still be members of the rotation group.");
        }
    }

    private SwapRequestResponse toResponse(SwapRequest s) {
        return new SwapRequestResponse(
                s.id(),
                s.choreId(),
                s.initiatorUserId(),
                s.receiverUserId(),
                s.status(),
                s.swapType(),
                s.cycleNumber(),
                s.createdAt());
    }
}