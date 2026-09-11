package genius.project.orchestrate.swap.internal;

import genius.project.orchestrate.identity.CurrentUserProvider;
import genius.project.orchestrate.swap.DuplicateSwapRequestException;
import genius.project.orchestrate.swap.InvalidSwapRequestRecipientException;
import genius.project.orchestrate.swap.InvalidSwapRequestStatusException;
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

@Service
public class SwapRequestServiceImpl implements SwapRequestService {

    private final SwapRequestRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher eventPublisher;

    public SwapRequestServiceImpl(SwapRequestRepository repository,
                                  CurrentUserProvider currentUserProvider,
                                  ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<SwapRequestResponse> getSwapRequests(Long choreId) {
        return repository.findByChoreId(choreId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public SwapRequestResponse createSwapRequest(Long choreId, SwapRequestRequest request) {
        Long initiatorId = currentUserProvider.getMembershipId();
        Long receiverId = request.receiverMembershipId();

        if (initiatorId.equals(receiverId)) {
            throw new InvalidSwapRequestRecipientException(receiverId, choreId);
        }

        if (repository.existsPending(choreId, initiatorId, receiverId)) {
            throw new DuplicateSwapRequestException(choreId, initiatorId, receiverId);
        }

        // TODO (chore-module integration): verify that both initiator and receiver
        //  are ChoreParticipants of this chore. Requires a public port from the chore
        //  module (e.g. isParticipant(choreId, membershipId) exposed in package `chore`,
        //  not `chore.internal`). Once available, call before saving and reject with a
        //  validation error (400) if either side is not a participant. This also covers
        //  two edge cases: (1) choreId that does not exist, and (2) membershipId that
        //  does not exist — both naturally return false from isParticipant.
        //  BLOCKER: depends on the chore module's public contract.

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
    public SwapRequestResponse respondToSwapRequest(Long choreId,
                                                    Long requestId,
                                                    SwapRequestStatusRequest request) {
        Long currentMembershipId = currentUserProvider.getMembershipId();

        SwapRequest existing = repository.findById(requestId)
                .orElseThrow(() -> new SwapRequestNotFoundException(requestId));

        if (!existing.choreId().equals(choreId)) {
            throw new SwapRequestNotFoundException(requestId);
        }

        if (!existing.receiverMembershipId().equals(currentMembershipId)) {
            throw new NotSwapRequestReceiverException(currentMembershipId, requestId);
        }

        if (existing.status() != SwapRequestStatus.PENDING) {
            throw new InvalidSwapRequestStatusException(
                    "SwapRequest id=%d is already %s".formatted(requestId, existing.status()));
        }

        if (request.status() == SwapRequestStatus.PENDING) {
            throw new InvalidSwapRequestStatusException(
                    "Cannot set status back to PENDING");
        }

        SwapRequest updated = repository.update(new SwapRequest(
                existing.id(),
                existing.choreId(),
                existing.initiatorMembershipId(),
                existing.receiverMembershipId(),
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
                    updated.initiatorMembershipId(),
                    updated.receiverMembershipId()
            ));
        }

        return toResponse(updated);
    }

    private SwapRequestResponse toResponse(SwapRequest s) {
        return new SwapRequestResponse(
                s.id(),
                s.choreId(),
                s.initiatorMembershipId(),
                s.receiverMembershipId(),
                s.status(),
                s.createdAt()
        );
    }
}