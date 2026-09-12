package genius.project.orchestrate.swap.internal;

import genius.project.orchestrate.chore.client.ChoreClient;
import genius.project.orchestrate.identity.CurrentUserProvider;
import genius.project.orchestrate.swap.DuplicateSwapRequestException;
import genius.project.orchestrate.swap.InvalidSwapRequestRecipientException;
import genius.project.orchestrate.swap.InvalidSwapRequestStatusException;
import genius.project.orchestrate.swap.NotChoreParticipantException;
import genius.project.orchestrate.swap.NotSwapRequestReceiverException;
import genius.project.orchestrate.swap.SwapRequestAcceptedEvent;
import genius.project.orchestrate.swap.SwapRequestNotFoundException;
import genius.project.orchestrate.swap.SwapRequestService;
import genius.project.orchestrate.swap.SwapRequestStatus;
import genius.project.orchestrate.swap.dto.SwapRequestRequest;
import genius.project.orchestrate.swap.dto.SwapRequestResponse;
import genius.project.orchestrate.swap.dto.SwapRequestStatusRequest;
import genius.project.orchestrate.swap.internal.domain.SwapRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SwapRequestServiceImpl implements SwapRequestService {

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
        return repository.findByChoreId(choreId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public SwapRequestResponse createSwapRequest(UUID choreId, SwapRequestRequest request) {
        UUID initiatorId = currentUserProvider.getUserId();
        UUID receiverId = request.receiverUserId();

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

        SwapRequest saved = repository.save(new SwapRequest(
                null,
                choreId,
                initiatorId,
                receiverId,
                SwapRequestStatus.PENDING,
                LocalDateTime.now()
        ));

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

        if (existing.status() != SwapRequestStatus.PENDING) {
            throw new InvalidSwapRequestStatusException(
                    "SwapRequest id=%s is already %s".formatted(requestId, existing.status()));
        }

        if (request.status() == SwapRequestStatus.PENDING) {
            throw new InvalidSwapRequestStatusException(
                    "Cannot set status back to PENDING");
        }

        SwapRequest updated = repository.update(new SwapRequest(
                existing.id(),
                existing.choreId(),
                existing.initiatorUserId(),
                existing.receiverUserId(),
                request.status(),
                existing.createdAt()
        ));

        if (updated.status() == SwapRequestStatus.ACCEPTED) {
            // Rotation algorithm belongs to the chore module. Swap does not know how
            // the exchange is applied (current cycle, ahead of time, how many cycles)
            // and deliberately does not validate relevance. If the event is no longer
            // relevant when processed, the chore module discards it.
            eventPublisher.publishEvent(new SwapRequestAcceptedEvent(
                    updated.choreId(),
                    updated.initiatorUserId(),
                    updated.receiverUserId()
            ));
        }

        return toResponse(updated);
    }

    private SwapRequestResponse toResponse(SwapRequest s) {
        return new SwapRequestResponse(
                s.id(),
                s.choreId(),
                s.initiatorUserId(),
                s.receiverUserId(),
                s.status(),
                s.createdAt()
        );
    }
}