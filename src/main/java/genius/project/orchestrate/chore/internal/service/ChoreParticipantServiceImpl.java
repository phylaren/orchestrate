package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.ChoreParticipantService;
import genius.project.orchestrate.chore.RotationService;
import genius.project.orchestrate.chore.client.ChoreClient;
import genius.project.orchestrate.chore.dto.AssignmentResponse;
import genius.project.orchestrate.chore.dto.ParticipantResponse;
import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreParticipant;
import genius.project.orchestrate.chore.internal.repository.ChoreParticipantStore;
import genius.project.orchestrate.chore.internal.repository.ChoreStore;
import genius.project.orchestrate.chore.internal.repository.RotationRepository;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class ChoreParticipantServiceImpl implements ChoreParticipantService, ChoreClient {

    private final ChoreStore choreStore;
    private final ChoreParticipantStore participantStore;
    private final RotationService rotationService;
    private final RotationRepository rotationRepository;

    ChoreParticipantServiceImpl(ChoreStore choreStore,
                                ChoreParticipantStore participantStore,
                                RotationService rotationService,
                                RotationRepository rotationRepository) {
        this.choreStore = choreStore;
        this.participantStore = participantStore;
        this.rotationService = rotationService;
        this.rotationRepository = rotationRepository;
    }

    @Override
    public ParticipantResponse joinChore(UUID choreId, UUID userId, boolean addedByAdmin) {
        getChoreOrThrow(choreId);

        if (participantStore.existsByChoreIdAndUserId(choreId, userId)) {
            throw new BusinessRuleViolationException(
                    "ALREADY_PARTICIPANT",
                    "User '%s' has already joined the rotation group for this chore.".formatted(userId));
        }

        ChoreParticipant participant = new ChoreParticipant(choreId, userId, Instant.now(), addedByAdmin);
        participantStore.save(participant);
        rotationService.addParticipant(choreId, userId);

        return ParticipantResponse.from(participant);
    }

    @Override
    public void leaveChore(UUID choreId, UUID userId) {
        getChoreOrThrow(choreId);

        if (!participantStore.existsByChoreIdAndUserId(choreId, userId)) {
            throw ResourceNotFoundException.of("participant", userId);
        }

        participantStore.deleteByChoreIdAndUserId(choreId, userId);
        rotationService.removeParticipant(choreId, userId);
    }

    @Override
    public List<ParticipantResponse> listParticipants(UUID choreId) {
        getChoreOrThrow(choreId);
        return participantStore.findByChoreId(choreId).stream()
                .map(ParticipantResponse::from)
                .toList();
    }

    @Override
    public boolean isParticipant(UUID choreId, UUID userId) {
        getChoreOrThrow(choreId);
        return participantStore.existsByChoreIdAndUserId(choreId, userId);
    }

    @Override
    public Optional<AssignmentResponse> getCurrentAssignment(UUID choreId) {
        getChoreOrThrow(choreId);
        return rotationRepository.findByChoreId(choreId)
                .filter(s -> !s.isEmpty())
                .map(AssignmentResponse::from);
    }

    @Override
    public void swapTurns(UUID choreId, UUID fromUserId, UUID toUserId) {
        getChoreOrThrow(choreId);

        if (!participantStore.existsByChoreIdAndUserId(choreId, fromUserId)
                || !participantStore.existsByChoreIdAndUserId(choreId, toUserId)) {
            throw new BusinessRuleViolationException(
                    "NOT_IN_SAME_GROUP",
                    "Both users must be members of this chore's rotation group to swap turns.");
        }

        rotationService.swapPositions(choreId, fromUserId, toUserId);
    }

    private Chore getChoreOrThrow(UUID choreId) {
        return choreStore.findById(choreId)
                .orElseThrow(() -> ResourceNotFoundException.of("chore", choreId));
    }
}
