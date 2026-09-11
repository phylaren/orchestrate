package genius.project.orchestrate.swap.internal;

import genius.project.orchestrate.identity.CurrentUserProvider;
import genius.project.orchestrate.swap.DuplicateSwapRequestException;
import genius.project.orchestrate.swap.InvalidSwapRequestRecipientException;
import genius.project.orchestrate.swap.InvalidSwapRequestStatusException;
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

        // TODO: validate that both initiator and receiver are ChoreParticipants of this chore
        // pending chore module integration

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
            throw new InvalidSwapRequestRecipientException(currentMembershipId, choreId);
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