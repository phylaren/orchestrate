package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.ChoreParticipantService;
import genius.project.orchestrate.chore.RotationService;
import genius.project.orchestrate.chore.client.ChoreClient;
import genius.project.orchestrate.chore.dto.AssignmentResponse;
import genius.project.orchestrate.chore.dto.ParticipantResponse;
import genius.project.orchestrate.chore.dto.RotationScheduleResponse;
import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreParticipant;
import genius.project.orchestrate.chore.internal.repository.ChoreParticipantStore;
import genius.project.orchestrate.chore.internal.repository.ChoreStore;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import genius.project.orchestrate.identity.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class ChoreParticipantServiceImpl implements ChoreParticipantService, ChoreClient {

    private static final Logger log = LoggerFactory.getLogger(ChoreParticipantServiceImpl.class);

    private final ChoreStore choreStore;
    private final ChoreParticipantStore participantStore;
    private final RotationService rotationService;
    private final CurrentUserProvider currentUserProvider;

    ChoreParticipantServiceImpl(ChoreStore choreStore,
                                ChoreParticipantStore participantStore,
                                RotationService rotationService,
                                CurrentUserProvider currentUserProvider) {
        this.choreStore = choreStore;
        this.participantStore = participantStore;
        this.rotationService = rotationService;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public ParticipantResponse joinChore(UUID choreId) {
        UUID userId = currentUserProvider.getUserId();
        ParticipantResponse response = doJoin(choreId, userId, false);
        log.info("Participant joined: choreId={}, userId={}", choreId, userId);
        return response;
    }

    @Override
    public ParticipantResponse addParticipant(UUID choreId, UUID targetUserId) {
        UUID actorId = currentUserProvider.getUserId();
        ParticipantResponse response = doJoin(choreId, targetUserId, true);
        log.info("Participant added: choreId={}, userId={}, by={}", choreId, targetUserId, actorId);
        return response;
    }

    @Override
    public void removeMember(UUID choreId, UUID targetUserId) {
        UUID actorId = currentUserProvider.getUserId();
        if (actorId.equals(targetUserId)) {
            doRemove(choreId, actorId);
            log.info("Participant left: choreId={}, userId={}", choreId, actorId);
        } else {
            doRemove(choreId, targetUserId);
            log.info("Participant removed: choreId={}, userId={}, by={}", choreId, targetUserId, actorId);
        }
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
    public int currentCycleNumber(UUID choreId) {
        getChoreOrThrow(choreId);
        return rotationService.getSchedule(choreId)
                .map(RotationScheduleResponse::cycleNumber)
                .orElseThrow(() -> ResourceNotFoundException.of("rotationSchedule", choreId));
    }

    @Override
    public Optional<AssignmentResponse> getCurrentAssignment(UUID choreId) {
        getChoreOrThrow(choreId);
        return rotationService.getCurrentAssignment(choreId);
    }

    private ParticipantResponse doJoin(UUID choreId, UUID userId, boolean addedByAdmin) {
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

    private void doRemove(UUID choreId, UUID userId) {
        getChoreOrThrow(choreId);

        if (!participantStore.existsByChoreIdAndUserId(choreId, userId)) {
            throw ResourceNotFoundException.of("participant", userId);
        }

        participantStore.deleteByChoreIdAndUserId(choreId, userId);
        rotationService.removeParticipant(choreId, userId);
    }

    private Chore getChoreOrThrow(UUID choreId) {
        return choreStore.findById(choreId)
                .orElseThrow(() -> ResourceNotFoundException.of("chore", choreId));
    }
}