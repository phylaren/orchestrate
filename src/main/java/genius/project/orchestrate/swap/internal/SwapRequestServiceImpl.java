package genius.project.orchestrate.swap.internal;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.TurnSwapRequestedEvent;
import genius.project.orchestrate.chore.client.ChoreClient;
import genius.project.orchestrate.common.exception.InvalidCycleNumberException;
import genius.project.orchestrate.identity.CurrentUserProvider;
import genius.project.orchestrate.swap.SwapRequestService;
import genius.project.orchestrate.swap.SwapRequestStatus;
import genius.project.orchestrate.swap.dto.SwapRequestRequest;
import genius.project.orchestrate.swap.dto.SwapRequestResponse;
import genius.project.orchestrate.swap.dto.SwapRequestStatusRequest;
import genius.project.orchestrate.swap.dto.SwapRequestSummaryResponse;
import genius.project.orchestrate.swap.dto.SwapRequestUpdateRequest;
import genius.project.orchestrate.swap.exception.DuplicateSwapRequestException;
import genius.project.orchestrate.swap.exception.InvalidSwapRequestRecipientException;
import genius.project.orchestrate.swap.exception.InvalidSwapRequestStatusException;
import genius.project.orchestrate.swap.exception.NotChoreParticipantException;
import genius.project.orchestrate.swap.exception.NotInSameGroupException;
import genius.project.orchestrate.swap.exception.NotSwapRequestInitiatorException;
import genius.project.orchestrate.swap.exception.NotSwapRequestReceiverException;
import genius.project.orchestrate.swap.exception.SwapRequestNotEditableException;
import genius.project.orchestrate.swap.exception.SwapRequestNotFoundException;
import genius.project.orchestrate.swap.internal.domain.SwapRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SwapRequestServiceImpl implements SwapRequestService {

    private static final Logger log = LoggerFactory.getLogger(SwapRequestServiceImpl.class);

    private static final int MAX_CYCLE_LOOKAHEAD = 10;

    private final SwapRequestRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final ChoreClient choreClient;
    private final ApplicationEventPublisher eventPublisher;
    private final SwapRequestResponseMapper responseMapper;

    public SwapRequestServiceImpl(SwapRequestRepository repository,
                                  CurrentUserProvider currentUserProvider,
                                  ChoreClient choreClient,
                                  ApplicationEventPublisher eventPublisher,
                                  SwapRequestResponseMapper responseMapper) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.choreClient = choreClient;
        this.eventPublisher = eventPublisher;
        this.responseMapper = responseMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SwapRequestResponse> getSwapRequests(UUID choreId) {
        return repository.findByChoreId(choreId).stream()
                .map(responseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SwapRequestResponse> getSwapRequests(UUID choreId, SwapRequestStatus status) {
        if (status == null) {
            return getSwapRequests(choreId);
        }
        return repository.findByChoreIdAndStatus(choreId, status).stream()
                .map(responseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SwapRequestResponse getSwapRequest(UUID choreId, UUID requestId) {
        return responseMapper.toResponse(requireRequestOfChore(choreId, requestId));
    }

    @Override
    @Transactional(readOnly = true)
    public SwapRequestSummaryResponse getSummary(UUID choreId) {
        Map<SwapRequestStatus, Long> byStatus = repository.countGroupedByStatus(choreId);
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new SwapRequestSummaryResponse(choreId, total, byStatus);
    }

    @Override
    @Transactional
    public SwapRequestResponse createSwapRequest(UUID choreId, SwapRequestRequest request) {
        UUID initiatorId = currentUserProvider.getUserId();
        UUID receiverId = request.receiverUserId();
        SwapType swapType = request.swapType();

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

        log.info("Swap request created: requestId={}, choreId={}, initiatorId={}, receiverId={}, swapType={}",
                saved.id(), choreId, initiatorId, receiverId, swapType);
        return responseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SwapRequestResponse updateSwapRequest(UUID choreId,
                                                 UUID requestId,
                                                 SwapRequestUpdateRequest request) {
        UUID currentUserId = currentUserProvider.getUserId();

        SwapRequest existing = requireRequestOfChore(choreId, requestId);
        requireInitiator(existing, currentUserId);
        requirePending(existing);

        Integer cycleNumber = resolveCycleNumber(choreId, request.swapType(), request.cycleNumber());

        SwapRequest updated = repository.update(new SwapRequest(
                existing.id(),
                existing.choreId(),
                existing.initiatorUserId(),
                existing.receiverUserId(),
                existing.status(),
                request.swapType(),
                cycleNumber,
                existing.createdAt()));

        log.info("Swap request updated: requestId={}, choreId={}, by={}, swapType={}, cycleNumber={}",
                requestId, choreId, currentUserId, request.swapType(), cycleNumber);
        return responseMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public SwapRequestResponse respondToSwapRequest(UUID choreId,
                                                    UUID requestId,
                                                    SwapRequestStatusRequest request) {
        UUID currentUserId = currentUserProvider.getUserId();

        SwapRequest existing = requireRequestOfChore(choreId, requestId);

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

        log.info("Swap request {}: requestId={}, choreId={}, by={}",
                targetStatus, requestId, choreId, currentUserId);

        if (updated.status() == SwapRequestStatus.ACCEPTED) {
            eventPublisher.publishEvent(new TurnSwapRequestedEvent(
                    updated.choreId(),
                    updated.initiatorUserId(),
                    updated.receiverUserId(),
                    updated.swapType(),
                    updated.cycleNumber()));
        }

        return responseMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteSwapRequest(UUID choreId, UUID requestId) {
        UUID currentUserId = currentUserProvider.getUserId();

        SwapRequest existing = requireRequestOfChore(choreId, requestId);
        requireInitiator(existing, currentUserId);
        requirePending(existing);

        repository.deleteById(existing.id());
        log.info("Swap request deleted: requestId={}, choreId={}, by={}", requestId, choreId, currentUserId);
    }

    private SwapRequest requireRequestOfChore(UUID choreId, UUID requestId) {
        SwapRequest existing = repository.findById(requestId)
                .orElseThrow(() -> new SwapRequestNotFoundException(requestId));
        if (!existing.choreId().equals(choreId)) {
            throw new SwapRequestNotFoundException(requestId);
        }
        return existing;
    }

    private void requireInitiator(SwapRequest request, UUID userId) {
        if (!request.initiatorUserId().equals(userId)) {
            throw new NotSwapRequestInitiatorException(userId, request.id());
        }
    }

    private void requirePending(SwapRequest request) {
        if (request.status() != SwapRequestStatus.PENDING) {
            throw new SwapRequestNotEditableException(request.id(), request.status());
        }
    }

    private Integer resolveCycleNumber(UUID choreId, SwapType swapType, Integer requested) {
        if (swapType != SwapType.TEMPORARY) {
            return null;
        }
        if (requested == null) {
            throw InvalidCycleNumberException.missing();
        }

        int current = choreClient.currentCycleNumber(choreId);
        if (requested <= current) {
            throw InvalidCycleNumberException.inPast(requested, current);
        }
        if (requested > current + MAX_CYCLE_LOOKAHEAD) {
            throw InvalidCycleNumberException.tooFar(requested, MAX_CYCLE_LOOKAHEAD, current);
        }
        return requested;
    }

    private void validateStillParticipants(UUID choreId, SwapRequest existing) {
        if (!choreClient.isParticipant(choreId, existing.initiatorUserId())
                || !choreClient.isParticipant(choreId, existing.receiverUserId())) {
            throw new NotInSameGroupException();
        }
    }
}